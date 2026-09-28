package io.github.profetgit.travelerslantern.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.profetgit.travelerslantern.client.BeltClient;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The lantern swings only from the world's entity pass; other renders of a player reuse that frame's swing. */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Inject(method = "submitEntities", at = @At("HEAD"))
    private void travelerslantern$start(PoseStack ps, LevelRenderState state, SubmitNodeCollector collector, CallbackInfo ci) {
        BeltClient.setWorldPass(true);
    }

    @Inject(method = "submitEntities", at = @At("TAIL"))
    private void travelerslantern$end(PoseStack ps, LevelRenderState state, SubmitNodeCollector collector, CallbackInfo ci) {
        BeltClient.setWorldPass(false);
    }
}
