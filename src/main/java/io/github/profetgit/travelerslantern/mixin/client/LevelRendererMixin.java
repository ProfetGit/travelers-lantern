package io.github.profetgit.travelerslantern.mixin.client;

//? if >=1.21.9 {
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
//?}
//? if >=1.21.2 <1.21.9 {
/*import com.mojang.blaze3d.vertex.PoseStack;
import io.github.profetgit.travelerslantern.client.BeltClient;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// The lantern swings only from the world's entity pass; other renders of a player reuse that frame's swing.
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Inject(method = "renderEntities", at = @At("HEAD"))
    private void travelerslantern$start(PoseStack ps, MultiBufferSource.BufferSource buffer, Camera camera, DeltaTracker delta, java.util.List<Entity> entities, CallbackInfo ci) {
        BeltClient.setWorldPass(true);
    }

    @Inject(method = "renderEntities", at = @At("TAIL"))
    private void travelerslantern$end(PoseStack ps, MultiBufferSource.BufferSource buffer, Camera camera, DeltaTracker delta, java.util.List<Entity> entities, CallbackInfo ci) {
        BeltClient.setWorldPass(false);
    }
}
*///?}
//? if <1.21.2 {
/*import com.mojang.blaze3d.vertex.PoseStack;
import io.github.profetgit.travelerslantern.client.BeltClient;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// The lantern swings only from the world's render; other renders of a player reuse that frame's swing.
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void travelerslantern$start(DeltaTracker delta, boolean b, Camera camera, GameRenderer gr, LightTexture lt, Matrix4f m1, Matrix4f m2, CallbackInfo ci) {
        BeltClient.setWorldPass(true);
    }

    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void travelerslantern$end(DeltaTracker delta, boolean b, Camera camera, GameRenderer gr, LightTexture lt, Matrix4f m1, Matrix4f m2, CallbackInfo ci) {
        BeltClient.setWorldPass(false);
    }
}
*///?}
