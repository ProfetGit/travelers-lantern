package io.github.profetgit.travelerslantern.mixin.common;

import io.github.profetgit.travelerslantern.server.Belt;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The belt slot isn't one of the inventory's own slots, so death drops and keepInventory respawns carry it here. */
@Mixin(Inventory.class)
public abstract class InventoryMixin {
    @Shadow
    @Final
    public Player player;

    @Inject(method = "dropAll", at = @At("HEAD"))
    private void travelerslantern$drop(CallbackInfo ci) {
        Belt.dropOnDeath(player);
    }

    @Inject(method = "replaceWith", at = @At("TAIL"))
    private void travelerslantern$keep(Inventory other, CallbackInfo ci) {
        Belt.carryOver(other.player, player);
    }
}
