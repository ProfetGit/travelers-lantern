package io.github.profetgit.beltlantern.mixin.client;

import io.github.profetgit.beltlantern.client.BeltClient;
import io.github.profetgit.beltlantern.demo.Director;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void beltlantern$boot(CallbackInfo ci) {
        BeltClient.init();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void beltlantern$tick(CallbackInfo ci) {
        BeltClient.tick((Minecraft) (Object) this);
    }

    /** Drives the dev demo (ModTest); inert unless the game runs with -Dbeltlantern.demo. */
    @Inject(method = "tick", at = @At("TAIL"))
    private void beltlantern$demoTick(CallbackInfo ci) {
        if (Director.ACTIVE) Director.onTick((Minecraft) (Object) this);
    }

    @Inject(method = "runTick", at = @At("TAIL"))
    private void beltlantern$demoFrame(boolean advanceGameTime, CallbackInfo ci) {
        if (Director.ACTIVE) Director.onFrame((Minecraft) (Object) this);
    }
}
