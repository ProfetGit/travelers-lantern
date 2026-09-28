package io.github.profetgit.beltlantern.demo;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.profetgit.beltlantern.BeltLantern;
import io.github.profetgit.beltlantern.Lanterns;
import io.github.profetgit.beltlantern.client.BeltClient;
import io.github.profetgit.beltlantern.client.BeltLayer;
import io.github.profetgit.beltlantern.client.DynamicLight;
import io.github.profetgit.beltlantern.client.Keys;
import io.github.profetgit.beltlantern.client.Swing;
import io.github.profetgit.beltlantern.server.Belt;
import io.github.profetgit.beltlantern.server.Lights;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/**
 * Dev-only scene director and check runner; does nothing unless the JVM runs with -Dbeltlantern.demo=<dir> (ModTest).
 * In the flat demo world (surface y -10) the real player hangs a lantern with the real key, walks, sprints, jumps,
 * turns and sneaks with the real movement keys, while a side-on camera follows (the player stays the camera entity,
 * so its movement reaches the server as usual). Every frame's swing is logged to <dir>/<scene>/timing.txt (and saved
 * as a PNG with frames on); the checks go to <dir>/results.json.
 */
public final class Director {
    private static final String DIR = System.getProperty("beltlantern.demo");
    public static final boolean ACTIVE = DIR != null;
    static final Path OUT = Path.of(ACTIVE ? DIR : ".");
    static final String[] SCENES = System.getProperty("beltlantern.demo.scenes", "hang,walk").split(",");
    static final boolean FRAMES = !"false".equals(System.getProperty("beltlantern.demo.frames"));
    /** Camera: side (the lantern's side, square to the walk), front (a 3/4 view from ahead), back. */
    static final String CAM = System.getProperty("beltlantern.demo.cam", "side");
    static final ExecutorService WRITER = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "beltlantern-demo-writer");
        t.setDaemon(true);
        return t;
    });
    static final int G = -10;
    static int tick = -1, scene = -1, sceneTick, frame;
    static boolean recording, done, stopped, hudHidden;
    static final AtomicInteger pending = new AtomicInteger();
    static long drainUntil;
    static final List<String> timing = new ArrayList<>();
    static final List<String> results = Collections.synchronizedList(new ArrayList<>());
    /** Largest and last rod angle from straight down (degrees) and largest twist, per scene. */
    static double maxAngle, lastAngle, maxTwist, walkAngle, restLo, restHi, restSpread;
    static int drawnAtStart;
    static long busyAtStart, busyAtWalkEnd;

    private Director() {
    }

    public static void onTick(Minecraft mc) {
        if (done) {
            if (!stopped && (pending.get() == 0 || System.currentTimeMillis() > drainUntil)) {
                stopped = true;
                System.out.println("[bldemo] done" + (pending.get() > 0 ? ", " + pending.get() + " frames not written" : ""));
                mc.stop();
            }
            return;
        }
        if (mc.gui.screen() instanceof DeathScreen && mc.player != null) {
            mc.player.respawn();
            mc.gui.setScreen(null);
        }
        if (mc.level == null || mc.player == null) return;
        if (mc.getSingleplayerServer() == null) {
            MpDirector.onTick(mc);
            return;
        }
        tick++;
        if (!hudHidden) hideHud(mc);
        if (tick == 10) setup(mc);
        if (tick < 80) return;
        if (scene < 0 || sceneTick >= length(SCENES[scene])) {
            if (scene >= 0) {
                release(mc);
                finishScene(mc, SCENES[scene]);
                if (recording) stopRecording();
            }
            if (++scene >= SCENES.length) {
                finish();
                return;
            }
            sceneTick = -1;
            maxAngle = lastAngle = maxTwist = walkAngle = restSpread = 0;
            restLo = 999;
            restHi = -999;
            drawnAtStart = BeltLayer.drawnLocal;
            startScene(mc, SCENES[scene]);
            record();
            System.out.println("[bldemo] scene " + SCENES[scene]);
        }
        sceneTick++;
        tickScene(mc, SCENES[scene], sceneTick);
    }

    static void setup(Minecraft mc) {
        ModTestHook.audit();
        cmd(mc, ModTestHook.commands().toArray(String[]::new));
        String p = name(mc);
        cmd(mc, "gamerule advance_time false", "gamerule advance_weather false", "gamerule spawn_mobs false", "gamerule spawn_monsters false",
            "gamerule send_command_feedback false", "gamerule immediate_respawn true", "time set 6000", "weather clear", "difficulty peaceful",
            "gamemode survival " + p, "kill @e[type=!player]", "clear " + p, "give " + p + " lantern 2",
            "tp " + p + " 0.5 " + (G + 1) + " 0.5 -90 0");
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        mc.player.getInventory().setSelectedSlot(0);
    }

    static int length(String s) {
        return switch (s) {
            case "hang", "off" -> 40;
            case "idle", "turn", "sneak" -> 90;
            case "walk", "light" -> 110;
            case "sprint", "jump" -> 120;
            case "death", "keep" -> 90;
            default -> 60;
        };
    }

    static void startScene(Minecraft mc, String s) {
        String p = name(mc);
        // every scene starts from the same spot, facing east, standing still
        cmd(mc, "tp " + p + " 0.5 " + (G + 1) + " 0.5 -90 0", "time set " + ("light".equals(s) ? 18000 : 6000));
        mc.player.setYRot(-90);
        mc.player.setYHeadRot(-90);
        mc.player.yBodyRot = -90;
        if ("keep".equals(s) || "death".equals(s)) {
            // a known inventory: two lanterns in hand, nothing on the belt, no drops lying around
            cmd(mc, "gamerule keep_inventory " + "keep".equals(s), "kill @e[type=item]", "clear " + p, "item replace entity " + p + " armor.body with air",
                "give " + p + " lantern 2");
        }
    }

    static void tickScene(Minecraft mc, String s, int t) {
        var o = mc.options;
        switch (s) {
            case "hang", "off" -> {
                if (t == 2) KeyMapping.click(Keys.TOGGLE.getDefaultKey());
            }
            case "walk" -> hold(o.keyUp, t < 60);
            case "light" -> {
                hold(o.keyUp, t < 60);
                if (t == 0) busyAtStart = DynamicLight.busyTicks;
                // the lantern still settles for a moment after the stop; count from when it hangs still
                if (t == 95) busyAtWalkEnd = DynamicLight.busyTicks;
            }
            case "sprint" -> {
                hold(o.keyUp, t < 60);
                hold(o.keySprint, t < 60);
            }
            case "jump" -> {
                hold(o.keyUp, t < 70);
                hold(o.keySprint, t < 70);
                hold(o.keyJump, t < 70 && t % 14 < 2);
            }
            case "turn" -> {
                // a quick half turn, a pause, and a quick turn back
                float yaw = -90 + 180 * Mth.clamp((t - 10) / 5F, 0, 1) - 180 * Mth.clamp((t - 50) / 5F, 0, 1);
                mc.player.setYRot(yaw);
                mc.player.setYHeadRot(yaw);
            }
            case "sneak" -> {
                hold(o.keyShift, t < 70);
                hold(o.keyUp, t > 10 && t < 60);
            }
            case "death", "keep" -> {
                if (t == 2) KeyMapping.click(Keys.TOGGLE.getDefaultKey());
                if (t == 25) cmd(mc, "kill " + name(mc));
                // before the respawned player can pick the drops up again (40-tick pickup delay)
                if (t == 32 && s.equals("death")) onServer(mc, "dropped", sp -> droppedLanterns(sp) >= 2, sp -> droppedLanterns(sp) + " lanterns lying at the death spot (belt + hand)");
            }
            default -> {
            }
        }
        if ((s.equals("walk") || s.equals("sprint") || s.equals("jump")) && t > 10 && t < 60) walkAngle = Math.max(walkAngle, lastAngle);
    }

    static void hold(KeyMapping k, boolean down) {
        k.setDown(down);
    }

    static void release(Minecraft mc) {
        var o = mc.options;
        for (KeyMapping k : new KeyMapping[] {o.keyUp, o.keySprint, o.keyJump, o.keyShift}) k.setDown(false);
        mc.player.setSprinting(false);
    }

    static void finishScene(Minecraft mc, String s) {
        int drawn = BeltLayer.drawnLocal - drawnAtStart;
        switch (s) {
            case "hang" -> {
                onServer(mc, "belt", sp -> Lanterns.is(sp.getItemBySlot(Belt.SLOT)), sp -> "server belt: " + sp.getItemBySlot(Belt.SLOT));
                check("client_belt", !BeltClient.beltOf(mc.player).isEmpty(), "client sees " + BeltClient.beltOf(mc.player) + ", server mod " + BeltClient.serverHasMod());
                int left = count(mc.player.getInventory().getNonEquipmentItems());
                check("taken_one", left == 1, left + " lanterns left in the inventory (2 given)");
                check("drawn", drawn > 0, drawn + " lantern draws");
                boolean listed = java.util.Arrays.asList(mc.options.keyMappings).contains(Keys.TOGGLE);
                String label = net.minecraft.network.chat.Component.translatable(Keys.TOGGLE.getName()).getString();
                check("key", listed && label.startsWith("Belt lantern"), "in Controls " + listed + ", named '" + label + "', bound to " + Keys.TOGGLE.getTranslatedKeyMessage().getString());
                // this client draws the light itself: the server lights the cell but shows it no light block
                onServer(mc, "light", sp -> Lights.cellOf(sp.getUUID()) != null && Lights.smooth(sp.getUUID()) && Lights.shownTo(sp.getUUID()) == 0
                    && !sp.level().getBlockState(Lights.cellOf(sp.getUUID())).is(Blocks.LIGHT),
                    sp -> "cell " + Lights.cellOf(sp.getUUID()) + ", " + Lights.status());
                lightChecks(mc, "client_light");
            }
            case "idle" -> check("settled", restSpread < 1 && lastAngle < 15,
                String.format(Locale.ROOT, "rod %.1f deg from straight down at rest (moves %.2f deg over the last second), max %.1f", lastAngle, restSpread, maxAngle));
            case "walk", "sprint", "jump" -> {
                check("swings", walkAngle > 4, String.format(Locale.ROOT, "rod up to %.1f deg while moving, %.1f max overall", walkAngle, maxAngle));
                check("finite", Double.isFinite(maxAngle) && maxAngle < 120, String.format(Locale.ROOT, "max %.1f deg", maxAngle));
                check("drawn", drawn > 0, drawn + " lantern draws");
            }
            case "turn" -> check("twist", maxTwist > 8 && maxTwist < 111, String.format(Locale.ROOT, "lantern lagged the turn by up to %.1f deg", maxTwist));
            case "sneak" -> check("drawn", drawn > 0, drawn + " lantern draws");
            case "light" -> {
                lightChecks(mc, "lit");
                if (present("net.caffeinemc.mods.sodium.client.model.light.smooth.SmoothLightPipeline")) {
                    long q = io.github.profetgit.beltlantern.client.SodiumLight.quads;
                    check("sodium", q > 0, q + " quads lit through Sodium's pipeline");
                }
                if (shadersOn()) {
                    check("iris", BeltClient.irisHeld > 0, BeltClient.irisHeld + " updates where the shader's off-hand light was the belt lantern");
                }
                long moved = DynamicLight.busyTicks - busyAtWalkEnd;
                check("still_is_free", moved == 0, moved + " light rebuilds while standing still after the walk");
                check("walk_rebuilds", busyAtWalkEnd - busyAtStart > 10, (busyAtWalkEnd - busyAtStart) + " ticks rebuilt light while walking ("
                    + DynamicLight.rebuilt + " sections so far)");
            }
            case "off" -> {
                onServer(mc, "belt_empty", sp -> sp.getItemBySlot(Belt.SLOT).isEmpty(), sp -> "server belt: " + sp.getItemBySlot(Belt.SLOT));
                check("in_hand", Lanterns.is(mc.player.getMainHandItem()) || count(mc.player.getInventory().getNonEquipmentItems()) == 2,
                    "hand " + mc.player.getMainHandItem() + ", " + count(mc.player.getInventory().getNonEquipmentItems()) + " in the inventory");
                onServer(mc, "no_light", sp -> Lights.cellOf(sp.getUUID()) == null && lightsNear(sp) == 0,
                    sp -> Lights.status() + ", " + lightsNear(sp) + " light blocks near the player");
                check("client_off", BeltClient.beltOf(mc.player).isEmpty(), "client belt " + BeltClient.beltOf(mc.player));
                check("light_gone", !DynamicLight.active() && DynamicLight.at(mc.player.getX(), mc.player.getY() + 0.6, mc.player.getZ()) == 0,
                    "dynamic light " + DynamicLight.at(mc.player.getX(), mc.player.getY() + 0.6, mc.player.getZ()));
            }
            case "death" -> {
                onServer(mc, "belt_empty", sp -> sp.getItemBySlot(Belt.SLOT).isEmpty(), sp -> "respawned belt: " + sp.getItemBySlot(Belt.SLOT));
                onServer(mc, "no_light", sp -> Lights.cellOf(sp.getUUID()) == null && lightsNear(sp) == 0, sp -> Lights.status());
            }
            case "keep" -> {
                onServer(mc, "kept", sp -> Lanterns.is(sp.getItemBySlot(Belt.SLOT)), sp -> "respawned belt: " + sp.getItemBySlot(Belt.SLOT));
                check("client_kept", !BeltClient.beltOf(mc.player).isEmpty(), "client sees " + BeltClient.beltOf(mc.player));
            }
            default -> {
            }
        }
    }

    /**
     * The belt lantern lights the player (the entity light the renderer uses) and the ground beside it (the light a
     * chunk mesh is built with), and this client has no light block of its own there.
     */
    static void lightChecks(Minecraft mc, String name) {
        float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        int self = mc.getEntityRenderDispatcher().getPackedLightCoords(mc.player, partial);
        BlockPos ground = mc.player.blockPosition().relative(net.minecraft.core.Direction.NORTH, 2);
        int floor = net.minecraft.util.LightCoordsUtil.getLightCoords(mc.level, ground);
        int blocks = lightBlocks(mc);
        check(name, (self & 0xFFFF) >= 13 * 16 && (floor & 0xFFFF) >= 9 * 16 && blocks == 0,
            String.format(Locale.ROOT, "player lit %.2f, ground 2 blocks off %.2f (sixteenths of block light), %d light blocks on this client",
                (self & 0xFFFF) / 16F, (floor & 0xFFFF) / 16F, blocks));
    }

    static int lightBlocks(Minecraft mc) {
        int n = 0;
        for (BlockPos p : BlockPos.betweenClosed(mc.player.blockPosition().offset(-4, -3, -4), mc.player.blockPosition().offset(4, 4, 4))) {
            if (mc.level.getBlockState(p).is(Blocks.LIGHT)) n++;
        }
        return n;
    }

    /** Iris is installed and a shader pack is on (by reflection: Iris stays optional). */
    static boolean shadersOn() {
        try {
            Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi", false, Director.class.getClassLoader());
            Object inst = api.getMethod("getInstance").invoke(null);
            return (Boolean) api.getMethod("isShaderPackInUse").invoke(inst);
        } catch (ReflectiveOperationException | LinkageError e) {
            return false;
        }
    }

    static boolean present(String cls) {
        try {
            Class.forName(cls, false, Director.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    static int count(List<ItemStack> items) {
        int n = 0;
        for (ItemStack s : items) if (Lanterns.is(s)) n += s.getCount();
        return n;
    }

    static int lightsNear(ServerPlayer sp) {
        int n = 0;
        for (BlockPos p : BlockPos.betweenClosed(sp.blockPosition().offset(-3, -2, -3), sp.blockPosition().offset(3, 3, 3))) {
            if (sp.level().getBlockState(p).is(Blocks.LIGHT)) n++;
        }
        return n;
    }

    static int droppedLanterns(ServerPlayer sp) {
        int n = 0;
        for (ItemEntity e : sp.level().getEntitiesOfClass(ItemEntity.class, new AABB(new BlockPos(0, G + 1, 0)).inflate(8))) {
            if (Lanterns.is(e.getItem())) n += e.getItem().getCount();
        }
        return n;
    }

    static BlockPos serverCell(Minecraft mc) {
        MinecraftServer server = mc.getSingleplayerServer();
        try {
            return server.submit(() -> Lights.cellOf(mc.player.getUUID())).get(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            return null;
        }
    }

    interface Test {
        boolean ok(ServerPlayer sp);
    }

    interface Detail {
        String of(ServerPlayer sp);
    }

    /** A check that reads the server's side (on the server thread). */
    static void onServer(Minecraft mc, String name, Test test, Detail detail) {
        MinecraftServer server = mc.getSingleplayerServer();
        String sc = SCENES[scene];
        try {
            server.submit(() -> {
                ServerPlayer sp = server.getPlayerList().getPlayer(mc.player.getUUID());
                check(sc, name, sp != null && test.ok(sp), sp == null ? "no server player" : detail.of(sp));
            }).get(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            check(sc, name, false, "server check failed: " + e);
        }
    }

    /** Side-on follow camera (CameraMixin, after the game placed its third-person camera). */
    public static double[] camera(Entity e, float partial) {
        if (!recording || e != Minecraft.getInstance().player) return null;
        if ("fixed".equals(CAM)) {
            // still, above the walk's lantern side, looking down at the ground the light slides over
            double dx = 0, dy = -3.2, dz = 6.5;
            return new double[] {7.5, G + 4.2, -6, (float) Math.toDegrees(Math.atan2(-dx, dz)), (float) Math.toDegrees(-Math.atan2(dy, Math.hypot(dx, dz)))};
        }
        double x = Mth.lerp(partial, e.xo, e.getX()), y = Mth.lerp(partial, e.yo, e.getY()), z = Mth.lerp(partial, e.zo, e.getZ());
        // offsets from the player (who faces east, +x; the lantern hangs on the north side, -z), aimed at the hip
        double[] off = switch (CAM) {
            case "front" -> new double[] {2.6, 1.1, -1.9};
            case "back" -> new double[] {-2.6, 1.2, -1.6};
            case "close" -> new double[] {0.3, 0.8, -1.6};
            default -> new double[] {0, 0.95, -3.1};
        };
        double dx = -off[0], dy = 0.75 - off[1], dz = -off[2];
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) Math.toDegrees(-Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        return new double[] {x + off[0], y + off[1], z + off[2], yaw, pitch};
    }

    static void record() {
        recording = true;
        frame = 0;
        timing.clear();
    }

    static void stopRecording() {
        recording = false;
        Path dir = OUT.resolve(SCENES[scene]);
        List<String> lines = new ArrayList<>(timing);
        lines.add(0, "# frame nanos scene_tick partial x y z body_yaw angle fwd side twist");
        try {
            Files.createDirectories(dir);
            Files.write(dir.resolve("timing.txt"), lines);
        } catch (IOException e) {
            System.out.println("[bldemo] timing write failed: " + e);
        }
    }

    /** Called after every rendered frame. */
    public static void onFrame(Minecraft mc) {
        if (!recording || done || mc.player == null) return;
        Path dir = OUT.resolve(SCENES[scene]);
        int n = frame++;
        float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        double[] sw = Swing.debug(mc.player.getId());
        String line = n + " " + System.nanoTime() + " " + sceneTick + " " + f(partial);
        if (sw != null) {
            double dx = sw[0] - sw[3], dy = sw[1] - sw[4], dz = sw[2] - sw[5];
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double angle = Math.toDegrees(Math.acos(Mth.clamp(-dy / Math.max(len, 1e-9), -1, 1)));
            double yaw = Math.toRadians(sw[7]);
            // forward is (-sin yaw, cos yaw), the player's left is (cos yaw, sin yaw)
            double fwd = Math.toDegrees(Math.atan2(-Math.sin(yaw) * dx + Math.cos(yaw) * dz, -dy));
            double side = Math.toDegrees(Math.atan2(Math.cos(yaw) * dx + Math.sin(yaw) * dz, -dy));
            double twist = Math.toDegrees(sw[8]);
            lastAngle = angle;
            if (sceneTick >= length(SCENES[scene]) - 20) {
                restLo = Math.min(restLo, angle);
                restHi = Math.max(restHi, angle);
                restSpread = restHi - restLo;
            }
            maxAngle = Math.max(maxAngle, angle);
            maxTwist = Math.max(maxTwist, Math.abs(twist));
            line += " " + f(sw[3]) + " " + f(sw[4]) + " " + f(sw[5]) + " " + f(sw[7]) + " " + f(angle) + " " + f(fwd) + " " + f(side) + " " + f(twist);
        }
        timing.add(line);
        if (!FRAMES) return;
        Path out = dir.resolve(String.format("f%05d.png", n));
        pending.incrementAndGet();
        Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), (NativeImage img) -> WRITER.execute(() -> {
            try (img) {
                Files.createDirectories(dir);
                img.writeToFile(out);
            } catch (Exception e) {
                System.out.println("[bldemo] write failed " + out + ": " + e);
            } finally {
                pending.decrementAndGet();
            }
        }));
    }

    static void check(String name, boolean pass, String detail) {
        check(SCENES[scene], name, pass, detail);
    }

    static void check(String sc, String name, boolean pass, String detail) {
        String id = sc + "/" + name;
        results.add(String.format("{\"name\":\"%s\",\"pass\":%b,\"detail\":\"%s\"}", id, pass, detail.replace("\"", "'")));
        System.out.println("[bldemo] " + (pass ? "PASS " : "FAIL ") + id + "  " + detail);
    }

    static void finish() {
        done = true;
        try {
            Files.createDirectories(OUT);
            Files.writeString(OUT.resolve("results.json"), "{\"loader\":\"" + BeltLantern.loader() + "\",\"results\":[\n"
                + String.join(",\n", results) + "\n]}\n");
        } catch (IOException e) {
            System.out.println("[bldemo] results write failed: " + e);
        }
        drainUntil = System.currentTimeMillis() + 120_000;
    }

    static String name(Minecraft mc) {
        return mc.player.getGameProfile().name();
    }

    static void cmd(Minecraft mc, String... commands) {
        MinecraftServer server = mc.getSingleplayerServer();
        server.execute(() -> {
            for (String c : commands) server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
        });
    }

    static String f(double v) {
        return String.format(Locale.ROOT, "%.3f", v);
    }

    static void hideHud(Minecraft mc) {
        try {
            Field f = mc.gui.hud.getClass().getDeclaredField("isHidden");
            f.setAccessible(true);
            f.setBoolean(mc.gui.hud, true);
        } catch (ReflectiveOperationException e) {
            System.out.println("[bldemo] cannot hide HUD: " + e);
        }
        hudHidden = true;
    }
}
