package io.github.profetgit.travelerslantern.platform.fabric;

//? fabric {
import io.github.profetgit.travelerslantern.TravelersLantern;
import net.fabricmc.api.ModInitializer;

public final class FabricEntry implements ModInitializer {
    @Override
    public void onInitialize() {
        TravelersLantern.init("fabric");
    }
}
//?}
