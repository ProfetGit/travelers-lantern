package io.github.profetgit.travelerslantern.mixin.common;

import io.github.profetgit.travelerslantern.server.Belt;
import io.github.profetgit.travelerslantern.server.Lights;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A player leaving takes their light with them at once (before their chunks can unload). */
@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
    @Inject(method = "remove", at = @At("HEAD"))
    private void travelerslantern$leave(ServerPlayer player, CallbackInfo ci) {
        Lights.release(player.getUUID());
        Belt.forget(player);
    }
}
