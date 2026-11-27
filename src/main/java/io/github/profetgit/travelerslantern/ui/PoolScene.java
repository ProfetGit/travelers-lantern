package io.github.profetgit.travelerslantern.ui;

//? if >=26.3 {
import io.github.profetgit.profetsettings.Ctx;
import io.github.profetgit.profetsettings.Gfx;
import io.github.profetgit.travelerslantern.Config;
import io.github.profetgit.travelerslantern.Lanterns;
import io.github.profetgit.travelerslantern.client.DynamicLight;
import io.github.profetgit.travelerslantern.client.PreviewRig;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

// The walker seen from above, going round on open ground: the light the lantern casts, with the mod's own light maths.
public final class PoolScene extends Stage {
    public enum Show {
        LIGHT, DAYLIGHT, SMOOTH
    }

    private static final double LOOP = 8 * PreviewRig.STRIDE;
    private static final double RADIUS = LOOP * 4.317 / (2 * Math.PI);

    private final Show show;
    private final double period;
    private PreviewRig.Frame[] frames;
    // per frame: the smooth light's place (as often as the setting updates it) and the ghost light's cell (every tick)
    private Vector3f[] smooth, cell;
    // per frame: the eased brightness of the surroundings, as DynamicLight eases it
    private float[] bright;
    private int baked = Integer.MIN_VALUE;

    public PoolScene(Show show) {
        this.show = show;
        // the day goes by in two laps
        this.period = show == Show.DAYLIGHT ? 2 * LOOP : LOOP;
    }

    private static PreviewRig.Move move(double t) {
        double a = 2 * Math.PI * (t % LOOP) / LOOP;
        // heading follows the circle; walked keeps counting through the day scene's second lap
        return new PreviewRig.Move(RADIUS * Math.cos(a), RADIUS * Math.sin(a), (float) Math.toDegrees(a), t, 1);
    }

    // the world light around the lantern over the day scene: evening bright, then night, then bright again
    private float raw(double t) {
        if (show != Show.DAYLIGHT) return 0;
        double u = t / period;
        double night = smooth((u - 0.34) / 0.14) - smooth((u - 0.82) / 0.14);
        return (float) (13 - 11 * night);
    }

    private static double smooth(double u) {
        u = Math.max(0, Math.min(1, u));
        return u * u * (3 - 2 * u);
    }

    private void bake(Config cfg) {
        int key = (cfg.leftSide ? 1 : 0) | cfg.lightUpdatesPerSecond << 1 | (cfg.softInDaylight ? 1 << 8 : 0);
        if (frames != null && key == baked) return;
        baked = key;
        frames = PreviewRig.bake(PoolScene::move, period, cfg.leftSide);
        int n = frames.length;
        smooth = new Vector3f[n];
        cell = new Vector3f[n];
        bright = new float[n];
        Vector3f lit = null, block = null;
        float b = DynamicLight.brightness(raw(0));
        int rate = cfg.lightUpdatesPerSecond;
        // the second pass starts where the first ended, so the loop has no jump
        for (int pass = 0; pass < 2; pass++) {
            int update = -1, tick = -1;
            for (int i = 0; i < n; i++) {
                double t = i / (double) PreviewRig.FPS;
                if ((int) (t * rate) != update) {
                    update = (int) (t * rate);
                    Vector3f at = frames[i].weight();
                    if (lit == null || lit.distanceSquared(at) > 0.08F * 0.08F) lit = at;
                }
                if ((int) (t * 20) != tick) {
                    tick = (int) (t * 20);
                    Vector3f at = frames[i].weight();
                    block = new Vector3f((float) Math.floor(at.x), (float) Math.floor(at.y), (float) Math.floor(at.z));
                }
                float target = cfg.softInDaylight ? DynamicLight.brightness(raw(t)) : 0;
                b += (target - b) * (float) (1 - Math.exp(-1.0 / PreviewRig.FPS / 0.7));
                smooth[i] = lit;
                cell[i] = block;
                bright[i] = b;
            }
        }
    }

    @Override
    public void render(Gfx g, Ctx c, int x, int y, int w, int h) {
        Config cfg = Config.get();
        float time = time(c, (float) (period * 1000), cfg.clientLight, cfg.smoothLight, cfg.softInDaylight);
        bake(cfg);
        int now = (int) (time * PreviewRig.FPS / 1000) % frames.length;
        PreviewRig.Frame f = PreviewRig.sample(frames, time, period);
        BlockState lantern = Props.lantern();
        int base = lantern.getLightEmission();
        float sky = show == Show.DAYLIGHT ? Math.max(0, Math.min(1, (raw(time / 1000.0) - 2) / 11)) * 0.75F : 0;

        int ox = x + (w - W) / 2;
        backdrop(g, ox, y);
        Cam cam = new Cam(45, 34, 19, ox + 99, y + 77, 0, 0.5F, 0);
        float level = DynamicLight.levelIn(base, bright[now]), reach = DynamicLight.radiusIn(base, bright[now]);
        Vector3f src = smooth[now], block = cell[now];
        boolean on = cfg.clientLight, soft = cfg.smoothLight;
        // block light at a ground corner: the four cells that meet there, averaged as vanilla's smooth lighting does
        java.util.function.BiFunction<Float, Float, Float> field = (px, pz) -> {
            if (!on) return 0F;
            float sum = 0;
            for (int i = 0; i < 4; i++) {
                float bx = (float) Math.floor(px + (i % 2 == 0 ? -0.5F : 0.5F)), bz = (float) Math.floor(pz + (i < 2 ? -0.5F : 0.5F));
                if (soft) {
                    float dx = bx + 0.5F - src.x, dy = 0.5F - src.y, dz = bz + 0.5F - src.z;
                    sum += DynamicLight.falloff(level, reach, dx * dx + dy * dy + dz * dz);
                } else {
                    sum += Math.max(0, base - Math.abs(bx - block.x) - Math.abs(block.y) - Math.abs(bz - block.z));
                }
            }
            return sum / 4;
        };

        Mesh mesh = new Mesh();
        float cx = ox + 99, cy = y + 78;
        for (int ix = -11; ix <= 10; ix++) {
            for (int iz = -11; iz <= 10; iz++) {
                int kind = (ix * 73856093 ^ iz * 19349663) >>> 3;
                Props.ground(mesh, cam, ix, iz, kind, 1, (px, pz, s) -> {
                    float l = field.apply(px, pz);
                    float[] col = Props.lit(l, sky);
                    // the lit ground is what there is to see; unlit ground fades into the box's dark
                    float pool = Math.max(0, Math.min(1, (l - 3) / 8));
                    float floor = on ? sky : Math.max(0.9F, sky);
                    float alpha = Math.max(pool, floor) * Props.oval(s.x, s.y, cx, cy, 100, 72, 0.3F);
                    return ARGB.colorFromFloat(alpha, col[0], col[1], col[2]);
                });
            }
        }
        mesh.draw(g, x, y, w, h, false);
        Props.shadow(mesh, cam, (float) f.move().x(), (float) f.move().z(), f.move().yaw());
        mesh.draw(g, x, y, w, h, false);
        float[] near = Props.lit(on ? Math.min(15, soft ? level : base) : 0, sky);
        float[] tint = new float[3];
        for (int i = 0; i < 3; i++) tint[i] = Math.max(i == 2 ? 0.55F : 0.42F, Math.min(1, near[i] * 0.95F));
        Figure.add(mesh, cam, new org.joml.Matrix4f().translate((float) f.move().x(), 0, (float) f.move().z()).mul(PreviewRig.toWorld(f.move().yaw())),
            PreviewRig.pose(f.move()), tint);
        Props.lantern(mesh, cam, lantern, f.lantern());
        mesh.draw(g, x, y, w, h, true);
        Vector3f at = cam.project(new Vector3f(f.weight()));
        float[] flame = Lanterns.flame(lantern);
        halo(g, at.x, at.y, 13, ARGB.colorFromFloat(0.35F * (1 - sky * 0.6F), flame[0], flame[1], flame[2]));
        if (show == Show.DAYLIGHT) clock(g, ox + W - 24, y + 8, time / (float) (period * 1000));

        if (!on) note(g, x, y, w, h, t("off.light"));
        else if (show == Show.SMOOTH && !soft) note(g, x, y, w, h, t("off.smooth"));
        else if (show == Show.DAYLIGHT && !cfg.softInDaylight) note(g, x, y, w, h, t("off.daylight"));
    }

    // the game's own clock, showing the time of day the scene is at
    private static void clock(Gfx g, int x, int y, float u) {
        double night = smooth((u - 0.34) / 0.14) + smooth((u - 0.82) / 0.14);
        // clock_00 is noon, 32 midnight; it only turns forward
        int frame = (int) Math.round(night * 32) % 64;
        Identifier tex = Identifier.withDefaultNamespace(String.format(java.util.Locale.ROOT, "textures/item/clock_%02d.png", frame));
        g.g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, 0, 0, 16, 16, 16, 16, g.a(0xFFFFFFFF));
    }
}
//?}
