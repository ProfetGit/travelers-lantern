package io.github.profetgit.travelerslantern.mixin.client.compat;

import io.github.profetgit.travelerslantern.client.SodiumLight;
import net.caffeinemc.mods.sodium.client.model.light.data.QuadLightData;
import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadView;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Sodium's SmoothLightPipeline: belt-lantern light per vertex (Sodium 0.9.2 for 26.2: calculate takes two directions and two booleans; only when Sodium is installed). */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.model.light.smooth.SmoothLightPipeline", remap = false)
public abstract class SodiumSmoothLightPipelineMixinB {
    @Inject(method = "calculate", at = @At("RETURN"), require = 0, remap = false)
    private void travelerslantern$dynamic(ModelQuadView quad, BlockPos pos, QuadLightData out, Direction a, Direction b, boolean shade, boolean offset,
                                      CallbackInfo ci) {
        SodiumLight.apply(quad, pos, out);
    }
}
