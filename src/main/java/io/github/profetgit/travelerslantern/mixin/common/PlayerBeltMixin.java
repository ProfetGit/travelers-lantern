package io.github.profetgit.travelerslantern.mixin.common;

import org.spongepowered.asm.mixin.Mixin;

/**
 * Before 1.21.5 a player has no BODY equipment slot at all: this gives it one (kept in a field, saved with the player, and
 * sent by vanilla's equipment packets like any other slot, which the client applies through the same field).
 */
//? if >=1.21.5 {
@Mixin(net.minecraft.client.Minecraft.class)
public abstract class PlayerBeltMixin {
}
//?} else {
/*@Mixin(net.minecraft.world.entity.player.Player.class)
public abstract class PlayerBeltMixin {
    @org.spongepowered.asm.mixin.Unique
    private net.minecraft.world.item.ItemStack travelerslantern$belt = net.minecraft.world.item.ItemStack.EMPTY;

    @org.spongepowered.asm.mixin.injection.Inject(method = "getItemBySlot", at = @org.spongepowered.asm.mixin.injection.At("HEAD"), cancellable = true)
    private void travelerslantern$get(net.minecraft.world.entity.EquipmentSlot slot, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<net.minecraft.world.item.ItemStack> cir) {
        if (slot == net.minecraft.world.entity.EquipmentSlot.BODY) cir.setReturnValue(travelerslantern$belt);
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = "setItemSlot", at = @org.spongepowered.asm.mixin.injection.At("HEAD"), cancellable = true)
    private void travelerslantern$set(net.minecraft.world.entity.EquipmentSlot slot, net.minecraft.world.item.ItemStack stack, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (slot == net.minecraft.world.entity.EquipmentSlot.BODY) {
            travelerslantern$belt = stack;
            ci.cancel();
        }
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = "addAdditionalSaveData", at = @org.spongepowered.asm.mixin.injection.At("TAIL"))
    private void travelerslantern$save(net.minecraft.nbt.CompoundTag tag, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (!travelerslantern$belt.isEmpty()) {
            tag.put("TravelersLanternBelt", travelerslantern$belt.save(((net.minecraft.world.entity.Entity) (Object) this).registryAccess()));
        }
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = "readAdditionalSaveData", at = @org.spongepowered.asm.mixin.injection.At("TAIL"))
    private void travelerslantern$load(net.minecraft.nbt.CompoundTag tag, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (tag.contains("TravelersLanternBelt")) {
            travelerslantern$belt = net.minecraft.world.item.ItemStack.parse(((net.minecraft.world.entity.Entity) (Object) this).registryAccess(),
                tag.get("TravelersLanternBelt")).orElse(net.minecraft.world.item.ItemStack.EMPTY);
        }
    }
}
*///?}
