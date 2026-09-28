package io.github.profetgit.travelerslantern.mixin.common;

import io.github.profetgit.travelerslantern.Config;
import io.github.profetgit.travelerslantern.server.Belt;
import io.github.profetgit.travelerslantern.server.Lights;
import java.util.function.BooleanSupplier;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Inject(method = "tickServer", at = @At("TAIL"))
    private void travelerslantern$tick(BooleanSupplier hasTime, CallbackInfo ci) {
        MinecraftServer server = (MinecraftServer) (Object) this;
        Config.load(server.getServerDirectory());
        Belt.tick(server);
        Lights.tick(server);
    }

    /** A stopped server forgets its players (singleplayer starts the next world in the same game). */
    @Inject(method = "stopServer", at = @At("HEAD"))
    private void travelerslantern$stop(CallbackInfo ci) {
        Lights.clear();
        Belt.clear();
    }
}
