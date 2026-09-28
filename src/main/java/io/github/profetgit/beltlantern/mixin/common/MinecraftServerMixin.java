package io.github.profetgit.beltlantern.mixin.common;

import io.github.profetgit.beltlantern.Config;
import io.github.profetgit.beltlantern.server.Belt;
import io.github.profetgit.beltlantern.server.Lights;
import java.util.function.BooleanSupplier;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Inject(method = "tickServer", at = @At("TAIL"))
    private void beltlantern$tick(BooleanSupplier hasTime, CallbackInfo ci) {
        MinecraftServer server = (MinecraftServer) (Object) this;
        Config.load(server.getServerDirectory());
        Belt.tick(server);
        Lights.tick(server);
    }

    /** Every light block goes back before the worlds are saved. */
    @Inject(method = "stopServer", at = @At("HEAD"))
    private void beltlantern$stop(CallbackInfo ci) {
        Lights.releaseAll();
        Belt.clear();
    }
}
