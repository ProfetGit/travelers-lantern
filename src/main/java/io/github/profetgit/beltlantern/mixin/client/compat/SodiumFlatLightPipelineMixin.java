package io.github.profetgit.beltlantern.mixin.client.compat;

import io.github.profetgit.beltlantern.client.SodiumLight;
import net.caffeinemc.mods.sodium.client.model.light.data.QuadLightData;
import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadView;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Sodium's FlatLightPipeline: belt-lantern light per vertex (only when Sodium is installed). */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.model.light.flat.FlatLightPipeline", remap = false)
public abstract class SodiumFlatLightPipelineMixin {
    @Inject(method = "calculate", at = @At("RETURN"), require = 0, remap = false)
    private void beltlantern$dynamic(ModelQuadView quad, BlockPos pos, QuadLightData out, Direction a, Direction b, Direction c, boolean shade,
                                      CallbackInfo ci) {
        SodiumLight.apply(quad, pos, out);
    }
}
