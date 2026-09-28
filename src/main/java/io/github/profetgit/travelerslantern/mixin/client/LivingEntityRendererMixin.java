package io.github.profetgit.travelerslantern.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.profetgit.travelerslantern.client.BeltState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps the pose an avatar's render starts from (camera offset, before the body's rotations) for the belt layer. */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
        at = @At("HEAD"))
    private void travelerslantern$root(LivingEntityRenderState state, PoseStack ps, SubmitNodeCollector c, CameraRenderState camera, CallbackInfo ci) {
        if (state instanceof BeltState b && b.travelerslantern$lantern() != null) b.travelerslantern$root().set(ps.last().pose());
    }
}
