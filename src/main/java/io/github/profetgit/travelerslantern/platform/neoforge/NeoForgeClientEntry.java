package io.github.profetgit.travelerslantern.platform.neoforge;

//? neoforge {
/*import io.github.profetgit.travelerslantern.SettingsScreen;
import io.github.profetgit.travelerslantern.TravelersLantern;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

// The settings button in NeoForge's mod list; a second @Mod class for the client, so a dedicated server never loads it.
@Mod(value = TravelersLantern.MOD_ID, dist = Dist.CLIENT)
public final class NeoForgeClientEntry {
    public NeoForgeClientEntry(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> new SettingsScreen(parent));
    }
}
*///?}
