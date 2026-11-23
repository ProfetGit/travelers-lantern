package io.github.profetgit.travelerslantern.platform.fabric;

//? fabric {
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import io.github.profetgit.travelerslantern.SettingsScreen;

/** Mod Menu's settings button (only loaded when Mod Menu is installed). */
public final class ModMenuEntry implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return SettingsScreen::new;
    }
}
//?}
