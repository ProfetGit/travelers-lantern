package io.github.profetgit.travelerslantern.mixin.client.compat;

//? if >=26.2 {
import io.github.profetgit.travelerslantern.client.DynamicLight;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndLightGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Fabric API's Indigo renderer builds chunk meshes with its own light lookup (it never calls the game's, which the
// LightCoordsUtilMixin hooks), so with Fabric API and no Sodium only entities and foliage-like flat models took the
// belt-lantern light. Every Indigo mesh light goes through this one method.
@Pseudo
@Mixin(targets = "net.fabricmc.fabric.impl.client.indigo.renderer.aocalc.AoCalculator", remap = false)
public abstract class IndigoLightMixin {
    @Inject(method = "getLightmapCoordinates", at = @At("RETURN"), cancellable = true, require = 0, remap = false)
    private static void travelerslantern$dynamic(BlockAndLightGetter level, BlockState state, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        if (!DynamicLight.active() || !(level instanceof ClientLevel || level instanceof RenderSectionRegion)) return;
        int packed = cir.getReturnValueI();
        int lit = DynamicLight.apply(packed, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        if (lit != packed) cir.setReturnValue(lit);
    }
}
//?} else {
/*import io.github.profetgit.travelerslantern.client.DynamicLight;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Fabric API's Indigo renderer builds chunk meshes with its own light lookup (it never calls the game's, which the
// LightCoordsUtilMixin hooks), so with Fabric API and no Sodium only entities and foliage-like flat models took the
// belt-lantern light. Every Indigo mesh light goes through this one method.
@Pseudo
@Mixin(targets = "net.fabricmc.fabric.impl.client.indigo.renderer.aocalc.AoCalculator", remap = false)
public abstract class IndigoLightMixin {
    @Inject(method = "getLightmapCoordinates", at = @At("RETURN"), cancellable = true, require = 0, remap = false)
    private static void travelerslantern$dynamic(BlockAndTintGetter level, BlockState state, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        if (!DynamicLight.active() || !(level instanceof ClientLevel || level instanceof RenderSectionRegion)) return;
        int packed = cir.getReturnValueI();
        int lit = DynamicLight.apply(packed, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        if (lit != packed) cir.setReturnValue(lit);
    }
}
*///?}
