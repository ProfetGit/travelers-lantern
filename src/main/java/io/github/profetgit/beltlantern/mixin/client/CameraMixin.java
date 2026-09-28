package io.github.profetgit.beltlantern.mixin.client;

import io.github.profetgit.beltlantern.demo.Director;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Dev demo only: a follow camera beside the player (inert unless -Dbeltlantern.demo). */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    private Entity entity;

    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Inject(method = "alignWithEntity", at = @At("TAIL"))
    private void beltlantern$demoCam(float partial, CallbackInfo ci) {
        if (!Director.ACTIVE || entity == null) return;
        double[] c = Director.camera(entity, partial);
        if (c == null) return;
        setPosition(c[0], c[1], c[2]);
        setRotation((float) c[3], (float) c[4]);
    }
}
