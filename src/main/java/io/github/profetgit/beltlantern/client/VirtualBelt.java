package io.github.profetgit.beltlantern.client;

import io.github.profetgit.beltlantern.Config;
import io.github.profetgit.beltlantern.Lanterns;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.ContainerInput;

/**
 * The belt on a server without the mod. The server has nowhere to keep it, so the lantern stays a normal item in the
 * inventory (it drops, stacks and moves as usual) and the belt only shows it: while the belt is on, the first lantern
 * that isn't held in a hand hangs there. Hanging the held lantern moves it out of the hand with the same inventory
 * click a player would make (shift-click), and taking it off swaps it back into an empty hand.
 */
public final class VirtualBelt {
    /** Inventory menu slots: hotbar slot i is menu slot 36 + i, the off hand 45; the main inventory keeps its index. */
    private static final int OFFHAND = 45;

    private VirtualBelt() {
    }

    public static ItemStack lantern(LocalPlayer p) {
        return Config.get().clientBelt ? find(p) : ItemStack.EMPTY;
    }

    /** Inventory index of the lantern the belt shows, or -1 (-2: the off hand). */
    private static int index(LocalPlayer p) {
        Inventory inv = p.getInventory();
        int sel = inv.getSelectedSlot();
        for (int i = 0; i < inv.getNonEquipmentItems().size(); i++) {
            if (i != sel && Lanterns.is(inv.getNonEquipmentItems().get(i))) return i;
        }
        // a stack in a hand hangs one of its lanterns; a single one is shown in the hand instead
        if (Lanterns.is(inv.getSelectedItem()) && inv.getSelectedItem().getCount() > 1) return sel;
        if (Lanterns.is(p.getOffhandItem()) && p.getOffhandItem().getCount() > 1) return -2;
        return -1;
    }

    private static ItemStack find(LocalPlayer p) {
        int i = index(p);
        return i == -1 ? ItemStack.EMPTY : i == -2 ? p.getOffhandItem() : p.getInventory().getNonEquipmentItems().get(i);
    }

    private static int menuSlot(int index) {
        return index < Inventory.SELECTION_SIZE ? 36 + index : index;
    }

    public static void toggle(Minecraft mc, LocalPlayer p) {
        Config c = Config.get();
        int menu = p.inventoryMenu.containerId;
        if (c.clientBelt && !find(p).isEmpty()) {
            c.clientBelt = false;
            Config.save();
            int i = index(p);
            if (i >= 0 && i != p.getInventory().getSelectedSlot() && p.getMainHandItem().isEmpty()) {
                mc.gameMode.handleContainerInput(menu, menuSlot(i), p.getInventory().getSelectedSlot(), ContainerInput.SWAP, p);
            }
            feedback(p, false, "Lantern off your belt");
            return;
        }
        // hang: a held lantern leaves the hand for the inventory (a shift-click), then the belt shows it
        if (Lanterns.is(p.getMainHandItem())) {
            mc.gameMode.handleContainerInput(menu, menuSlot(p.getInventory().getSelectedSlot()), 0, ContainerInput.QUICK_MOVE, p);
        } else if (Lanterns.is(p.getOffhandItem())) {
            mc.gameMode.handleContainerInput(menu, OFFHAND, 0, ContainerInput.QUICK_MOVE, p);
        }
        c.clientBelt = true;
        if (find(p).isEmpty()) {
            c.clientBelt = false;
            Config.save();
            p.sendOverlayMessage(Component.literal(hasAny(p) ? "No room to move the lantern off your hand" : "No lantern in your inventory"));
            return;
        }
        Config.save();
        feedback(p, true, "Lantern on your belt");
    }

    private static boolean hasAny(LocalPlayer p) {
        for (ItemStack s : p.getInventory().getNonEquipmentItems()) if (Lanterns.is(s)) return true;
        return Lanterns.is(p.getOffhandItem());
    }

    private static void feedback(LocalPlayer p, boolean hang, String msg) {
        p.level().playLocalSound(p.getX(), p.getY() + 0.8, p.getZ(), hang ? SoundEvents.LANTERN_PLACE : SoundEvents.LANTERN_HIT,
            SoundSource.PLAYERS, 0.6F, hang ? 1.25F : 1.05F, false);
        p.sendOverlayMessage(Component.literal(msg));
    }
}
