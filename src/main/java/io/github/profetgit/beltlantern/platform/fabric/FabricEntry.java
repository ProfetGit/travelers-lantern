package io.github.profetgit.beltlantern.platform.fabric;

//? fabric {
import io.github.profetgit.beltlantern.BeltLantern;
import net.fabricmc.api.ModInitializer;

public final class FabricEntry implements ModInitializer {
    @Override
    public void onInitialize() {
        BeltLantern.init("fabric");
    }
}
//?}
