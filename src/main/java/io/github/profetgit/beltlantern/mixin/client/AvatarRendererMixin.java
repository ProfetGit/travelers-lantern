package io.github.profetgit.beltlantern.mixin.client;

import io.github.profetgit.beltlantern.Lanterns;
import io.github.profetgit.beltlantern.client.BeltClient;
import io.github.profetgit.beltlantern.client.BeltLayer;
import io.github.profetgit.beltlantern.client.BeltState;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    @SuppressWarnings("unchecked")
    private void beltlantern$layer(EntityRendererProvider.Context context, boolean slim, CallbackInfo ci) {
        AvatarRenderer<?> self = (AvatarRenderer<?>) (Object) this;
        ((LivingEntityRendererAccessor<AvatarRenderState, PlayerModel>) self).beltlantern$addLayer(new BeltLayer(self));
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
        at = @At("TAIL"))
    private void beltlantern$extract(Avatar entity, AvatarRenderState state, float partialTick, CallbackInfo ci) {
        ItemStack belt = BeltClient.beltOf(entity);
        ((BeltState) state).beltlantern$setLantern(belt.isEmpty() ? null : Lanterns.hanging(belt));
    }
}
