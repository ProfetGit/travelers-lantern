package io.github.profetgit.travelerslantern.platform.forge;

import io.github.profetgit.travelerslantern.platform.Platform;
import java.nio.file.Path;
import java.util.List;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod("travelers_lantern")
public final class ForgeEntry implements Platform {
    public ForgeEntry() {
        Platform.set(this);
    }

    @Override
    public String loader() {
        return "forge";
    }

    @Override
    public Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public Path gameDir() {
        return FMLPaths.GAMEDIR.get();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public List<ModInfo> mods() {
        return ModList.get().getMods().stream().map(m -> new ModInfo(m.getModId(), m.getDisplayName())).toList();
    }

    @Override
    public boolean isClient() {
        return FMLEnvironment.dist == Dist.CLIENT;
    }
}
