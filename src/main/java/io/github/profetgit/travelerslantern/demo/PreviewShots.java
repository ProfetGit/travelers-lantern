package io.github.profetgit.travelerslantern.demo;

//? if >=26.3 {
import io.github.profetgit.profetsettings.Preview;
import io.github.profetgit.profetsettings.ProfetSettingsScreen;
import io.github.profetgit.travelerslantern.Config;
import io.github.profetgit.travelerslantern.ConfigHook;
import io.github.profetgit.travelerslantern.ui.WalkScene;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;

// Scene "previews": every settings preview at fixed moments (Preview.debugTime), with its options on and off.
final class PreviewShots {
    // time -1: just after the loop's first clink
    private record Shot(String name, String key, String kind, Runnable set, int time) {
    }

    private static final List<Shot> SHOTS = new ArrayList<>();
    private static final int EACH = 8;
    private static Config before;

    // with -Ddense, every frame of a stretch of each moving thing instead (flicker and motion checks)
    private static void dense() {
        frames("hip", "leftSide", "hip", () -> {}, 0, 1100);
        frames("knock", "clinks", "clink", () -> {}, 1300, 1700);
        frames("smooth", "smoothLight", "smooth", () -> {}, 500, 1000);
        frames("smooth-off", "smoothLight", "smooth", () -> Config.get().smoothLight = false, 500, 1000);
        frames("smooth-rate", "lightUpdatesPerSecond", "smooth", () -> Config.get().lightUpdatesPerSecond = 20, 500, 1000);
    }

    private static void frames(String name, String key, String kind, Runnable set, int from, int length) {
        for (int i = 0; i < length * 60 / 1000; i++) add(name, key, kind, set, from + Math.round(i * 1000F / 60));
    }

    static {
        if (!System.getProperty("travelers_lantern.demo.dense", "").isEmpty()) dense();
        else stills();
    }

    private static void stills() {
        add("hip-left", "leftSide", "hip", () -> {}, 0, 600, 1900);
        add("hip-right", "leftSide", "hip", () -> Config.get().leftSide = false, 600);
        add("clink", "clinks", "clink", () -> {}, 600, 1500, -1, 3200);
        add("clink-quiet", "clinkVolume", "clink", () -> Config.get().clinkVolume = 20, -1);
        add("clink-off", "clinks", "clink", () -> Config.get().clinks = false, -1);
        add("light", "clientLight", "light", () -> {}, 0, 1100, 2200, 3300);
        add("light-off", "clientLight", "light", () -> Config.get().clientLight = false, 1100);
        add("day-soft", "softInDaylight", "daylight", () -> Config.get().softInDaylight = true, 800, 3300, 5300, 7900);
        add("day-full", "softInDaylight", "daylight", () -> {}, 800, 5300);
        add("smooth", "smoothLight", "smooth", () -> {}, 700, 1100);
        add("smooth-off", "smoothLight", "smooth", () -> Config.get().smoothLight = false, 700, 1100);
        add("smooth-rate", "lightUpdatesPerSecond", "smooth", () -> Config.get().lightUpdatesPerSecond = 20, 700);
    }

    private static void add(String name, String key, String kind, Runnable set, int... times) {
        for (int t : times) SHOTS.add(new Shot(name, key, kind, set, t));
    }

    static int length() {
        return 10 + SHOTS.size() * EACH + 4;
    }

    private static void reset() {
        Config c = Config.get();
        c.leftSide = true;
        c.clinks = true;
        c.clinkVolume = 100;
        c.clientLight = true;
        c.softInDaylight = false;
        c.smoothLight = true;
        c.lightUpdatesPerSecond = 60;
    }

    // the shared screen drops its preview pane in a small window
    private static boolean tooSmall(Minecraft mc) {
        return mc.getWindow().getWidth() < 1600;
    }

    static void tick(Minecraft mc, int t) {
        if (tooSmall(mc)) {
            if (t == 2) Director.check("skipped", true, "window too small for the preview pane: run with --var width=1920 --var height=1080");
            return;
        }
        if (t == 2) {
            Config c = Config.get();
            before = new Config();
            before.leftSide = c.leftSide;
            before.clinks = c.clinks;
            before.clinkVolume = c.clinkVolume;
            before.clientLight = c.clientLight;
            before.softInDaylight = c.softInDaylight;
            before.smoothLight = c.smoothLight;
            before.lightUpdatesPerSecond = c.lightUpdatesPerSecond;
            reset();
            Preview.debugTime = 0;
            mc.gui.setScreen(ConfigHook.create(null));
        }
        int i = (t - 10) / EACH, at = (t - 10) % EACH;
        if (t < 10 || i >= SHOTS.size()) {
            if (t >= 10 && i >= SHOTS.size() && at == 0) {
                Preview.debugTime = -1;
                Config c = Config.get();
                c.leftSide = before.leftSide;
                c.clinks = before.clinks;
                c.clinkVolume = before.clinkVolume;
                c.clientLight = before.clientLight;
                c.softInDaylight = before.softInDaylight;
                c.smoothLight = before.smoothLight;
                c.lightUpdatesPerSecond = before.lightUpdatesPerSecond;
                mc.gui.setScreen(null);
            }
            return;
        }
        if (!(mc.gui.screen() instanceof ProfetSettingsScreen hub)) {
            if (at == 0) Director.check(shotName(i), false, "settings screen not open: " + mc.gui.screen());
            return;
        }
        Shot s = SHOTS.get(i);
        if (at == 0) {
            reset();
            s.set().run();
            hub.debugRevealRow(s.key());
        }
        if (at == 2) {
            int[] p = hub.debugPoint(s.key(), s.key().equals("clinkVolume") || s.key().equals("lightUpdatesPerSecond") ? 0.5F : -1F);
            if (p != null) hub.debugHover(p[0], p[1]);
        }
        if (at == 3) Preview.debugTime = Math.max(0, s.time());
        // the clink's moment is known once the loop is baked
        if (at == 5 && s.time() < 0) Preview.debugTime = WalkScene.firstBumpMs + 100;
        if (at == 7) {
            boolean right = s.kind().equals(hub.debugPreviewKind());
            if (!right || i == 0 || !SHOTS.get(i - 1).name().equals(s.name())) {
                Director.check("kind_" + s.name(), right, "row " + s.key() + " shows preview " + hub.debugPreviewKind());
            }
            if (s.name().equals("clink") && s.time() < 0) {
                Director.check("clinks", WalkScene.bakedBumps > 0, WalkScene.bakedBumps + " clinks in the loop, first at " + WalkScene.firstBumpMs + " ms");
            }
            Director.still(mc, shotName(i));
        }
    }

    // scene "title" (the run's last): the previews with no world and no player, as a mod list's config button opens them
    private static final String[][] TITLE = {{"leftSide", "hip"}, {"clinks", "clink"}, {"clientLight", "light"}, {"softInDaylight", "daylight"},
        {"smoothLight", "smooth"}};
    private static int titleTicks = -1;

    static void leave(Minecraft mc) {
        if (tooSmall(mc)) {
            Director.check("skipped", true, "window too small for the preview pane: run with --var width=1920 --var height=1080");
            Director.finish();
            return;
        }
        titleTicks = 0;
        mc.execute(() -> mc.disconnectFromWorld(net.minecraft.network.chat.Component.literal("title")));
    }

    // true while the title screen part runs
    static boolean titleTick(Minecraft mc) {
        if (titleTicks < 0 || mc.level != null) return false;
        int t = ++titleTicks;
        if (t == 20) {
            Preview.debugTime = 600;
            mc.gui.setScreen(ConfigHook.create(mc.gui.screen()));
        }
        int i = (t - 30) / EACH, at = (t - 30) % EACH;
        if (t >= 30 && i < TITLE.length) {
            ProfetSettingsScreen hub = mc.gui.screen() instanceof ProfetSettingsScreen h ? h : null;
            if (hub == null) {
                if (at == 0) Director.check("title_" + TITLE[i][1], false, "settings screen not open: " + mc.gui.screen());
                return true;
            }
            if (at == 0) hub.debugRevealRow(TITLE[i][0]);
            if (at == 2) {
                int[] p = hub.debugPoint(TITLE[i][0], -1F);
                if (p != null) hub.debugHover(p[0], p[1]);
            }
            if (at == 7) {
                Director.check("title_" + TITLE[i][1], TITLE[i][1].equals(hub.debugPreviewKind()) && mc.player == null,
                    "no world: row " + TITLE[i][0] + " shows preview " + hub.debugPreviewKind());
                Director.still(mc, "title-" + TITLE[i][1]);
            }
        } else if (i >= TITLE.length) {
            titleTicks = -1;
            Preview.debugTime = -1;
            mc.gui.setScreen(null);
            Director.finish();
        }
        return true;
    }

    private static String shotName(int i) {
        Shot s = SHOTS.get(i);
        return s.name() + "-" + (s.time() < 0 ? "bump" : String.format(java.util.Locale.ROOT, "%05d", s.time()));
    }
}
//?}
