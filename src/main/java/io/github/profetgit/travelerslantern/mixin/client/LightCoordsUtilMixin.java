package io.github.profetgit.travelerslantern.mixin.client;

import io.github.profetgit.travelerslantern.client.DynamicLight;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.BlockAndLightGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Belt-lantern light wherever vanilla looks up a block's light in the world: chunk meshes (vanilla's renderer), fluids,
 * particles, block entities. Only for the real world and its chunk-building copies, not for GUI renders of blocks.
 */
@Mixin(LightCoordsUtil.class)
public abstract class LightCoordsUtilMixin {
    @Inject(method = "getLightCoords(Lnet/minecraft/util/LightCoordsUtil$BrightnessGetter;Lnet/minecraft/world/level/BlockAndLightGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)I",
        at = @At("RETURN"), cancellable = true)
    private static void travelerslantern$dynamic(LightCoordsUtil.BrightnessGetter getter, BlockAndLightGetter level, BlockState state, BlockPos pos,
                                            CallbackInfoReturnable<Integer> cir) {
        if (!DynamicLight.active() || !(level instanceof ClientLevel || level instanceof RenderSectionRegion)) return;
        int packed = cir.getReturnValueI();
        int lit = DynamicLight.apply(packed, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        if (lit != packed) cir.setReturnValue(lit);
    }
}
