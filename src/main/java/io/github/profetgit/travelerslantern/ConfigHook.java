package io.github.profetgit.travelerslantern;

import net.minecraft.client.gui.screens.Screen;

/** What a mod list's config button opens: the shared Profet screen with this mod selected. Client only. */
public final class ConfigHook {
    private ConfigHook() {
    }

    public static Screen create(Screen parent) {
        return io.github.profetgit.profetsettings.ProfetSettings.screen(parent, TravelersLantern.MOD_ID);
    }
}
