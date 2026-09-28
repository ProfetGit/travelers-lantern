package io.github.profetgit.travelerslantern.mixin.client;

import io.github.profetgit.travelerslantern.client.DynamicLight;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Entities near a belt lantern are lit by it (at their exact position, so the light slides smoothly over them). */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
    @Inject(method = "getPackedLightCoords", at = @At("RETURN"), cancellable = true)
    private void travelerslantern$dynamic(Entity entity, float partial, CallbackInfoReturnable<Integer> cir) {
        if (!DynamicLight.active()) return;
        Vec3 p = entity.getLightProbePosition(partial);
        int packed = cir.getReturnValueI();
        int lit = DynamicLight.apply(packed, p.x, p.y, p.z);
        if (lit != packed) cir.setReturnValue(lit);
    }
}
