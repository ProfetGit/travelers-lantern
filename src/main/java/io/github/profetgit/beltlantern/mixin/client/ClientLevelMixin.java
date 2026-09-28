package io.github.profetgit.beltlantern.mixin.client;

import io.github.profetgit.beltlantern.client.GhostLight;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
    @Inject(method = "setServerVerifiedBlockState", at = @At("HEAD"))
    private void beltlantern$server(BlockPos pos, BlockState state, int flags, CallbackInfo ci) {
        GhostLight.onServerBlock(pos);
    }
}
