package io.github.profetgit.travelerslantern;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Where the belt lantern is stored: the BODY equipment slot (1.20.5 and later), or before that a field of the mod's own on
 * the player (PlayerBeltMixin), which BeltNet sends to the clients that have the mod.
 */
public final class BeltSlot {
    private BeltSlot() {
    }

    //? if >=1.20.5 {
    public static ItemStack get(LivingEntity e) {
        return e.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.BODY);
    }

    public static void set(LivingEntity e, ItemStack s) {
        e.setItemSlot(net.minecraft.world.entity.EquipmentSlot.BODY, s);
    }
    //?} else {
    /*// Implemented by Player through PlayerBeltMixin.
    public interface Holder {
        ItemStack travelerslantern$getBelt();

        void travelerslantern$setBelt(ItemStack stack);
    }

    public static ItemStack get(LivingEntity e) {
        return e instanceof Holder h ? h.travelerslantern$getBelt() : ItemStack.EMPTY;
    }

    public static void set(LivingEntity e, ItemStack s) {
        if (e instanceof Holder h) h.travelerslantern$setBelt(s);
    }
    *///?}
}
