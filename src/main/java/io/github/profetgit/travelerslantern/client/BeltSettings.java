package io.github.profetgit.travelerslantern.client;

import io.github.profetgit.profetsettings.ModDef;
import io.github.profetgit.profetsettings.ProfetSettings;
import io.github.profetgit.profetsettings.Setting;
import io.github.profetgit.travelerslantern.Config;
import io.github.profetgit.travelerslantern.TravelersLantern;
//? if >=26.3 {
import io.github.profetgit.travelerslantern.ui.PoolScene;
import io.github.profetgit.travelerslantern.ui.WalkScene;
//?}
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.client.resources.language.I18n;

/**
 * The client keys of the config file for the shared Profet screen (tools/ProfetSettings). Every key is read live, so a
 * change applies at once.
 */
public final class BeltSettings {
    private BeltSettings() {
    }

    private static String t(String key) {
        return I18n.get(TravelersLantern.MOD_ID + "." + key);
    }

    private static Setting bool(String k, Function<Config, Boolean> get, BiConsumer<Config, Boolean> set) {
        return Setting.bool(k, t("setting." + k), t("setting." + k + ".desc"), () -> get.apply(Config.get()), v -> set.accept(Config.get(), v), get.apply(new Config()));
    }

    private static Setting range(String k, int min, int max, Function<Config, Integer> get, BiConsumer<Config, Integer> set) {
        return Setting.range(k, t("setting." + k), t("setting." + k + ".desc"), min, max, () -> get.apply(Config.get()), v -> set.accept(Config.get(), v),
            get.apply(new Config()));
    }

    public static ModDef create() {
        ModDef m = new ModDef(TravelersLantern.MOD_ID, "Traveler's Lantern", "Lantern", ProfetSettings.modVersion(TravelersLantern.MOD_ID),
            t("tagline"), TravelersLantern.MOD_ID + ":icon.png")
            .issues("ProfetGit/travelers-lantern");
        m.onChange = Config::save;
        m.group(t("group.belt"),
            Setting.choice("leftSide", t("setting.leftSide"), t("setting.leftSide.desc"), new String[] {t("hip.left"), t("hip.right")},
                () -> Config.get().leftSide ? 0 : 1, i -> Config.get().leftSide = i == 0, new Config().leftSide ? 0 : 1).preview("hip"));
        m.group(t("group.sound"),
            bool("clinks", c -> c.clinks, (c, v) -> c.clinks = v).preview("clink"),
            range("clinkVolume", 0, 100, c -> c.clinkVolume, (c, v) -> c.clinkVolume = v).format(v -> v + "%").preview("clink"));
        m.group(t("group.light"),
            bool("clientLight", c -> c.clientLight, (c, v) -> c.clientLight = v).preview("light"),
            bool("softInDaylight", c -> c.softInDaylight, (c, v) -> c.softInDaylight = v).preview("daylight"),
            bool("smoothLight", c -> c.smoothLight, (c, v) -> c.smoothLight = v).preview("smooth"),
            range("lightUpdatesPerSecond", 20, 60, c -> c.lightUpdatesPerSecond, (c, v) -> c.lightUpdatesPerSecond = v)
                .format(v -> I18n.get(TravelersLantern.MOD_ID + ".per_second", v)).preview("smooth"));
        m.preset(t("preset.default"));
        m.preset(t("preset.quiet"), "clinks", false);
        m.preset(t("preset.performance"), "smoothLight", false);
        //? if >=26.3 {
        m.scene("hip", d -> new WalkScene(false))
            .scene("clink", d -> new WalkScene(true))
            .scene("light", d -> new PoolScene(PoolScene.Show.LIGHT))
            .scene("daylight", d -> new PoolScene(PoolScene.Show.DAYLIGHT))
            .scene("smooth", d -> new PoolScene(PoolScene.Show.SMOOTH))
            .presetPreview(t("preset.default"), "light")
            .presetPreview(t("preset.quiet"), "clink")
            .presetPreview(t("preset.performance"), "smooth");
        //?}
        return m;
    }
}
