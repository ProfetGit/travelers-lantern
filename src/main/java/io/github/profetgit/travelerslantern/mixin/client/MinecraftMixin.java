package io.github.profetgit.travelerslantern.mixin.client;

import io.github.profetgit.travelerslantern.client.BeltClient;
import io.github.profetgit.travelerslantern.demo.Director;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void travelerslantern$boot(CallbackInfo ci) {
        BeltClient.init();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void travelerslantern$tick(CallbackInfo ci) {
        BeltClient.tick((Minecraft) (Object) this);
    }

    /** Smooth light can follow a lantern more often than once a tick. */
    @Inject(method = "runTick", at = @At("HEAD"))
    private void travelerslantern$frame(boolean advanceGameTime, CallbackInfo ci) {
        io.github.profetgit.travelerslantern.client.DynamicLight.frame((Minecraft) (Object) this);
    }

    /** Drives the dev demo (ModTest); inert unless the game runs with -Dtravelers_lantern.demo. */
    @Inject(method = "tick", at = @At("TAIL"))
    private void travelerslantern$demoTick(CallbackInfo ci) {
        if (Director.ACTIVE) Director.onTick((Minecraft) (Object) this);
    }

    @Inject(method = "runTick", at = @At("TAIL"))
    private void travelerslantern$demoFrame(boolean advanceGameTime, CallbackInfo ci) {
        if (Director.ACTIVE) Director.onFrame((Minecraft) (Object) this);
    }
}
