package io.github.profetgit.beltlantern.mixin.client;

import io.github.profetgit.beltlantern.client.GhostLight;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** A refused placement must roll back to the real block, not to the client-side light. */
@Mixin(BlockStatePredictionHandler.class)
public abstract class BlockStatePredictionHandlerMixin {
    @ModifyVariable(method = "retainKnownServerState", at = @At("HEAD"), argsOnly = true)
    private BlockState beltlantern$real(BlockState state, BlockPos pos, BlockState same, LocalPlayer player) {
        return GhostLight.knownServerState(pos, state);
    }
}
