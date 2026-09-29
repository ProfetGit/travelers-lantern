package io.github.profetgit.travelerslantern.client;

import io.github.profetgit.travelerslantern.Config;
import io.github.profetgit.travelerslantern.Lanterns;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Smooth light from belt lanterns, drawn by this client: no light blocks, so it moves with the lantern instead of a
 * block at a time. Light from a lantern at distance d is level × (1 − (d / r)²), r = 7.5 blocks for a full lantern, and
 * it is added (as the brighter of the two) wherever the game looks up block light: chunk meshes (vanilla through
 * LightCoordsUtil, Sodium per vertex), entities, particles and block entities. The level may be fractional: the
 * lightmap's block coordinate has sixteenths. Meshes only take light when they are built, so the chunk sections a
 * lantern's light touches are rebuilt whenever it moves, up to lightUpdatesPerSecond times a second (60: measured on a
 * patch of ground the light slides over, the biggest brightness step was 5.3 levels of 255 with light blocks, 3.0 at 20
 * updates a second and 2.1 at 60); with r = 7.5 that is at most 2×2×2 sections per lantern, and a lantern far from the
 * camera waits for a bigger move. Light passes through walls, as in other dynamic light mods; Sodium's per-vertex light skips
 * faces turned away from the lantern.
 * Adaptive light (config adaptiveLight): the world's own light at the lantern (sky light minus the time of day and the
 * weather, or block light from torches) sets how strong it is. Around light 12 and up (daylight, a lit room) it gives
 * DIM of its level over DIM_REACH of its radius; below about 4 (night, caves) full level over DARK_REACH. It eases
 * between the two over EASE seconds, so walking into a cave brightens it gradually instead of switching.
 * Body shadow (config bodyShadow): the wearer's body stands between the lantern and whatever is on its other side, so a
 * point whose line to the lantern passes through the body (a vertical cylinder, BODY_RADIUS, feet to head) gets
 * 1 − SHADOW of the light. The edge is soft: a penumbra from the lantern's size, wider the farther the point is behind
 * the body. Light is only known per block corner, so it is a soft dark crescent on the far side, not a silhouette; it
 * works with shader packs too, since they read this block light (Complementary's own held light has no shadows).
 */
public final class DynamicLight {
    /** Radius per light level: a lantern (15) reaches 7.5 blocks. */
    static final float RADIUS_PER_LEVEL = 0.5F;
    /** A lantern that moved less than this since its light was last rebuilt waits; farther than FAR from the camera, FAR_MOVE. */
    static final double MOVE = 0.08, FAR = 24, FAR_MOVE = 0.5;
    /** Adaptive light: level and reach in bright surroundings, reach in the dark, and the easing time in seconds. */
    static final float DIM = 0.45F, DIM_REACH = 0.8F, DARK_REACH = 1.15F, EASE = 0.7F;
    /** A lantern whose level drifted by more than this (adaptive light easing) has its light rebuilt. */
    static final float LEVEL_STEP = 0.25F;
    /** Body shadow: the wearer as a cylinder of this radius, how much light it takes away, the lantern's size (penumbra). */
    static final float BODY_RADIUS = 0.25F, SHADOW = 0.7F, LAMP_SIZE = 0.35F;
    /** Floats per source in `sources`: x, y, z, level, radius, body x, body z, feet y, head y, body radius (0: no shadow). */
    static final int STRIDE = 10;
    /** Faces: light arriving at this cosine to the normal (behind the plane) gives nothing, at this one all of it. */
    static final double FACE_BEHIND = -0.35, FACE_FRONT = 0.2;
    /** Easing time (seconds) of the light's height behind the lantern's. */
    static final float LIFT = 0.15F;

    /** STRIDE floats per lantern (see STRIDE): read by the chunk-building threads, replaced (never changed) each tick. */
    private static volatile float[] sources = new float[0];
    private static final Int2ObjectOpenHashMap<Lit> LIT = new Int2ObjectOpenHashMap<>();
    private static ClientLevel level;
    private static volatile boolean on;
    /** Test hooks: sections marked for a rebuild so far, updates that rebuilt anything, most other players' lanterns lit at once. */
    public static long rebuilt, busyTicks;
    public static int mostOthers;

    /** Per lantern: how bright its surroundings are, eased (0 dark .. 1 bright). */
    private static final Int2FloatOpenHashMap AMBIENT = new Int2FloatOpenHashMap();
    /** Per lantern: the eased height of its light (see LIFT). */
    private static final Int2FloatOpenHashMap HEIGHT = new Int2FloatOpenHashMap();
    private static long lastAmbient;

    private record Lit(double x, double y, double z, float level, float radius, double bx, double bz, double by0, double by1) {
    }

    private DynamicLight() {
    }

    public static boolean enabled() {
        return Config.get().clientLight && Config.get().smoothLight;
    }

    private static long lastUpdate;

    /** Client tick: at 20 updates a second (the light's rate setting), follow the lanterns. */
    public static void tick(Minecraft mc) {
        if (Config.get().lightUpdatesPerSecond <= 20) update(mc);
    }

    /** Every frame: at a higher rate setting, follow the lanterns up to that many times a second. */
    public static void frame(Minecraft mc) {
        int rate = Config.get().lightUpdatesPerSecond;
        if (rate <= 20) return;
        long now = System.nanoTime();
        if (now - lastUpdate < 1_000_000_000L / rate - 500_000L) return;
        lastUpdate = now;
        update(mc);
    }

    /** Gather every belt lantern this client can see and rebuild the sections whose light changed. */
    static void update(Minecraft mc) {
        ClientLevel lv = mc.level;
        if (lv != level) {
            level = lv;
            LIT.clear();
            AMBIENT.clear();
            HEIGHT.clear();
            sources = new float[0];
        }
        if (lv == null) return;
        boolean enabled = enabled();
        LongOpenHashSet dirty = new LongOpenHashSet();
        Int2ObjectOpenHashMap<Lit> seen = new Int2ObjectOpenHashMap<>();
        Vec3 cam = mc.gameRenderer.mainCamera().position();
        long now = System.nanoTime();
        float dt = lastAmbient == 0 ? 1 : Math.min(1, (now - lastAmbient) / 1e9F);
        lastAmbient = now;
        if (enabled) {
            for (Player p : lv.players()) {
                if (p.isSpectator() || !p.isAlive()) continue;
                ItemStack belt = BeltClient.beltOf(p);
                if (belt.isEmpty()) continue;
                int base = Lanterns.light(belt);
                if (base <= 0) continue;
                Vec3 at = Swing.bobOf(p.getId());
                if (at == null) at = estimate(p);
                // the light's height trails the lantern a little (LIFT seconds), so a jump or a step up eases the light
                // over the new ground instead of snapping it; a teleport-sized move snaps
                float hy = HEIGHT.getOrDefault(p.getId(), (float) at.y);
                hy = Math.abs(at.y - hy) > 2 ? (float) at.y : hy + (float) (at.y - hy) * (1 - (float) Math.exp(-dt / LIFT));
                HEIGHT.put(p.getId(), hy);
                at = new Vec3(at.x, hy, at.z);
                float bright = 0;
                if (Config.get().adaptiveLight) {
                    float target = ambient(lv, at);
                    float was = AMBIENT.getOrDefault(p.getId(), target);
                    bright = was + (target - was) * (1 - (float) Math.exp(-dt / EASE));
                    if (Math.abs(bright - target) < 0.01F) bright = target;
                }
                AMBIENT.put(p.getId(), bright);
                float light = base * (1 + (DIM - 1) * bright);
                float radius = base * RADIUS_PER_LEVEL * (DARK_REACH + (DIM_REACH - DARK_REACH) * bright);
                Lit was = LIT.get(p.getId());
                // a lantern far from the camera shows little of its motion: it waits for a bigger move (fewer rebuilds)
                double move = at.distanceToSqr(cam) > FAR * FAR ? FAR_MOVE : MOVE;
                if (was == null || Math.abs(was.level() - light) > LEVEL_STEP || sq(was.x() - at.x, was.y() - at.y, was.z() - at.z) > move * move) {
                    if (was != null) sections(was.x(), was.y(), was.z(), was.radius(), dirty);
                    sections(at.x, at.y, at.z, radius, dirty);
                    seen.put(p.getId(), new Lit(at.x, at.y, at.z, light, radius, p.getX(), p.getZ(), p.getY(), p.getY() + p.getBbHeight()));
                } else {
                    seen.put(p.getId(), was);
                }
            }
        }
        // lanterns that went away (taken off, out of sight, the mod's light turned off) take their light along
        for (var e : LIT.int2ObjectEntrySet()) {
            if (!seen.containsKey(e.getIntKey())) {
                Lit was = e.getValue();
                sections(was.x(), was.y(), was.z(), was.radius(), dirty);
            }
        }
        AMBIENT.keySet().retainAll(seen.keySet());
        HEIGHT.keySet().retainAll(seen.keySet());
        LIT.clear();
        LIT.putAll(seen);
        // the positions the chunk meshes are built with: the ones whose light was just marked for a rebuild
        float[] pack = new float[seen.size() * STRIDE];
        float body = Config.get().bodyShadow ? BODY_RADIUS : 0;
        int i = 0;
        for (Lit l : seen.values()) {
            pack[i++] = (float) l.x();
            pack[i++] = (float) l.y();
            pack[i++] = (float) l.z();
            pack[i++] = l.level();
            pack[i++] = l.radius();
            pack[i++] = (float) l.bx();
            pack[i++] = (float) l.bz();
            pack[i++] = (float) l.by0();
            pack[i++] = (float) l.by1();
            pack[i++] = body;
        }
        sources = pack;
        on = pack.length > 0;
        int others = seen.size() - (mc.player != null && seen.containsKey(mc.player.getId()) ? 1 : 0);
        mostOthers = Math.max(mostOthers, others);
        if (!dirty.isEmpty()) {
            busyTicks++;
            for (long s : dirty) {
                mc.levelExtractor.setSectionDirty(SectionPos.x(s), SectionPos.y(s), SectionPos.z(s));
                rebuilt++;
            }
        }
    }

    /**
     * How bright the world is around a lantern, 0 (dark) to 1 (daylight, a lit room): the world's light at its cell (sky
     * light less the time of day and the weather, or block light), or the cell above when that one is solid. The lanterns'
     * smooth light isn't in the world's light, so it doesn't count itself.
     */
    static float ambient(ClientLevel lv, Vec3 at) {
        BlockPos pos = BlockPos.containing(at);
        int raw = Math.max(lv.getMaxLocalRawBrightness(pos), lv.getMaxLocalRawBrightness(pos.above()));
        float u = Math.max(0, Math.min(1, (raw - 4) / 8F));
        return u * u * (3 - 2 * u);
    }

    /** Test hook: a lantern's eased brightness of its surroundings (0 dark .. 1 bright), or -1. */
    public static float ambientOf(int entityId) {
        return AMBIENT.containsKey(entityId) ? AMBIENT.get(entityId) : -1;
    }

    /** Where a lantern hangs on a player the swing hasn't simulated yet (not drawn this frame): the hip. */
    static Vec3 estimate(Player p) {
        double yaw = Math.toRadians(p.yBodyRot), side = Config.get().leftSide ? 0.3 : -0.3;
        return p.position().add(Math.cos(yaw) * side, p.getBbHeight() * 0.32, Math.sin(yaw) * side);
    }

    /** Every section a lantern's light touches (it is 0 at r, so a vertex on the sphere gets nothing). */
    private static void sections(double x, double y, double z, double r, LongOpenHashSet out) {
        int x0 = SectionPos.blockToSectionCoord(Math.floor(x - r)), x1 = SectionPos.blockToSectionCoord(Math.floor(x + r));
        int y0 = SectionPos.blockToSectionCoord(Math.floor(y - r)), y1 = SectionPos.blockToSectionCoord(Math.floor(y + r));
        int z0 = SectionPos.blockToSectionCoord(Math.floor(z - r)), z1 = SectionPos.blockToSectionCoord(Math.floor(z + r));
        for (int sx = x0; sx <= x1; sx++) for (int sy = y0; sy <= y1; sy++) for (int sz = z0; sz <= z1; sz++) out.add(SectionPos.asLong(sx, sy, sz));
    }

    /** Light level (0 to 15, fractional) the lanterns give at a point. Safe from any thread. */
    public static float at(double x, double y, double z) {
        float[] s = sources;
        float best = 0;
        for (int i = 0; i < s.length; i += STRIDE) {
            double dx = x - s[i], dy = y - s[i + 1], dz = z - s[i + 2];
            double d2 = dx * dx + dy * dy + dz * dz, r2 = s[i + 4] * s[i + 4];
            if (d2 >= r2) continue;
            best = Math.max(best, (float) (s[i + 3] * (1 - d2 / r2)) * shade(s, i, x, y, z));
        }
        return best;
    }

    /**
     * Like at(), for a face (normal nx, ny, nz): a lantern behind the face's plane doesn't light it, so a wall's far side
     * stays dark. The cut is a fade over the angle the light arrives at (its cosine to the normal, FACE_BEHIND to
     * FACE_FRONT), not a switch: with a hard cut the whole top of a ledge lit up in one frame when the lantern rose past it
     * (jumping up a step). By the angle, light sweeps across a surface as the lantern rises, near spots first.
     */
    public static float atFace(double x, double y, double z, float nx, float ny, float nz) {
        float[] s = sources;
        float best = 0;
        for (int i = 0; i < s.length; i += STRIDE) {
            double dx = x - s[i], dy = y - s[i + 1], dz = z - s[i + 2];
            double d2 = dx * dx + dy * dy + dz * dz, r2 = s[i + 4] * s[i + 4];
            if (d2 >= r2) continue;
            double cos = -(dx * nx + dy * ny + dz * nz) / Math.sqrt(Math.max(d2, 1e-4));
            double front = smooth((cos - FACE_BEHIND) / (FACE_FRONT - FACE_BEHIND));
            if (front <= 0) continue;
            best = Math.max(best, (float) (s[i + 3] * (1 - d2 / r2) * front) * shade(s, i, x, y, z));
        }
        return best;
    }

    /**
     * How much of source i's light reaches a point past its wearer's body: 1, or down to 1 − SHADOW where the line from
     * the lantern to the point runs through the body cylinder, with soft edges. Points in or at the body (the wearer's own
     * light probe) and points between the lantern and the body are never shaded.
     */
    static float shade(float[] s, int i, double x, double y, double z) {
        float br = s[i + 9];
        if (br <= 0) return 1;
        double sx = s[i], sy = s[i + 1], sz = s[i + 2], bx = s[i + 5], bz = s[i + 6];
        double px = x - bx, pz = z - bz;
        if (px * px + pz * pz < (br + 0.1) * (br + 0.1)) return 1;
        double ux = x - sx, uz = z - sz, len2 = ux * ux + uz * uz;
        if (len2 < 1e-6) return 1;
        // closest approach of the line (seen from above) to the body's axis, as a fraction t of the way to the point
        double t = ((bx - sx) * ux + (bz - sz) * uz) / len2;
        if (t <= 0 || t >= 1) return 1;
        double cx = sx + t * ux - bx, cz = sz + t * uz - bz, d = Math.sqrt(cx * cx + cz * cz);
        // penumbra: the lantern seen from the point spans LAMP_SIZE·(1 − t) where the line passes the body
        double soft = Math.max(0.04, 0.5 * LAMP_SIZE * (1 - t));
        double across = smooth((br + soft - d) / (2 * soft));
        if (across <= 0) return 1;
        double yy = sy + t * (y - sy);
        double along = smooth((yy - s[i + 7] + soft) / (2 * soft)) * smooth((s[i + 8] + soft - yy) / (2 * soft));
        return (float) (1 - SHADOW * across * along);
    }

    private static double smooth(double u) {
        u = Math.max(0, Math.min(1, u));
        return u * u * (3 - 2 * u);
    }

    /** A packed light value with the lanterns' light as its block light, if brighter. */
    public static int apply(int packed, double x, double y, double z) {
        if (!on) return packed;
        return raise(packed, at(x, y, z));
    }

    public static int applyFace(int packed, double x, double y, double z, float nx, float ny, float nz) {
        if (!on) return packed;
        return raise(packed, atFace(x, y, z, nx, ny, nz));
    }

    private static int raise(int packed, float level) {
        if (level <= 0) return packed;
        int dyn = Math.min(240, Math.round(level * 16));
        return (packed & 0xFFFF) >= dyn ? packed : (packed & 0xFFFF0000) | dyn;
    }

    public static boolean active() {
        return on;
    }

    /** Test hook: where an entity's lantern light is now, or null. */
    public static Vec3 sourceOf(int entityId) {
        Lit l = LIT.get(entityId);
        return l == null ? null : new Vec3(l.x(), l.y(), l.z());
    }

    private static double sq(double x, double y, double z) {
        return x * x + y * y + z * z;
    }
}
