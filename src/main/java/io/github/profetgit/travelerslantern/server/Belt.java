package io.github.profetgit.travelerslantern.server;

import com.mojang.datafixers.util.Pair;
import io.github.profetgit.travelerslantern.Lanterns;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The belt, server side. The lantern is stored in the player's BODY equipment slot, which players don't otherwise use:
 * it's saved with the player, and vanilla already sends it to every client that sees the player (a client without the
 * mod ignores it). The player's own client isn't sent its equipment, so that one goes out here.
 */
public final class Belt {
    public static final EquipmentSlot SLOT = EquipmentSlot.BODY;
    /**
     * Per player: what their own client was last sent, and to which player object in which dimension. A respawn makes a
     * new player object and a dimension change a new client player, and either starts with an empty belt on the client.
     */
    private static final Map<UUID, Sent> SENT = new HashMap<>();

    private record Sent(ServerPlayer player, ServerLevel level, ItemStack stack) {
    }

    private Belt() {
    }

    /** The lantern on the player's belt, or empty. */
    public static ItemStack get(Player p) {
        ItemStack s = p.getItemBySlot(SLOT);
        return Lanterns.is(s) ? s : ItemStack.EMPTY;
    }

    /** Hangs a lantern (the held one first) or takes the one on the belt off. Returns 1 when something changed. */
    public static int toggle(ServerPlayer p) {
        if (p.isSpectator()) {
            say(p, "Spectators can't carry a lantern");
            return 0;
        }
        ItemStack on = p.getItemBySlot(SLOT);
        if (!on.isEmpty()) {
            if (!Lanterns.is(on)) {
                say(p, "Something else is worn there");
                return 0;
            }
            p.setItemSlot(SLOT, ItemStack.EMPTY);
            giveBack(p, on);
            sound(p, false);
            say(p, "Lantern off your belt");
            sync(p);
            return 1;
        }
        ItemStack src = find(p);
        if (src.isEmpty()) {
            say(p, "No lantern to hang on your belt");
            return 0;
        }
        p.setItemSlot(SLOT, src.split(1));
        sound(p, true);
        say(p, "Lantern on your belt");
        sync(p);
        return 1;
    }

    /** The lantern to hang: the main hand, the off hand, then the first one in the inventory. */
    private static ItemStack find(ServerPlayer p) {
        for (InteractionHand h : InteractionHand.values()) {
            ItemStack s = p.getItemInHand(h);
            if (Lanterns.is(s)) return s;
        }
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getNonEquipmentItems().size(); i++) {
            ItemStack s = inv.getNonEquipmentItems().get(i);
            if (Lanterns.is(s)) return s;
        }
        return ItemStack.EMPTY;
    }

    /** Into the empty main hand, else the inventory; what doesn't fit drops at the player's feet. */
    private static void giveBack(ServerPlayer p, ItemStack s) {
        if (p.getMainHandItem().isEmpty()) {
            p.setItemInHand(InteractionHand.MAIN_HAND, s);
            return;
        }
        p.getInventory().add(s);
        if (!s.isEmpty()) drop(p, s, false);
    }

    private static void drop(ServerPlayer p, ItemStack s, boolean scatter) {
        ServerLevel level = p.level();
        ItemEntity e = new ItemEntity(level, p.getX(), p.getEyeY() - 0.3, p.getZ(), s);
        if (scatter) {
            float speed = p.getRandom().nextFloat() * 0.5F, angle = p.getRandom().nextFloat() * Mth.TWO_PI;
            e.setDeltaMovement(-Mth.sin(angle) * speed, 0.2, Mth.cos(angle) * speed);
            e.setDefaultPickUpDelay();
        } else {
            e.setDeltaMovement(0, 0, 0);
            e.setNoPickUpDelay();
        }
        level.addFreshEntity(e);
    }

    private static void sound(ServerPlayer p, boolean hang) {
        p.level().playSound(null, p.getX(), p.getY() + 0.8, p.getZ(), hang ? SoundEvents.LANTERN_PLACE : SoundEvents.LANTERN_HIT,
            SoundSource.PLAYERS, 0.6F, hang ? 1.25F : 1.05F);
    }

    private static void say(ServerPlayer p, String msg) {
        p.sendOverlayMessage(Component.literal(msg));
    }

    /** Sends the player's own belt to their client. */
    static void sync(ServerPlayer p) {
        ItemStack s = p.getItemBySlot(SLOT).copy();
        p.connection.send(new ClientboundSetEquipmentPacket(p.getId(), List.of(Pair.of(SLOT, s))));
        SENT.put(p.getUUID(), new Sent(p, p.level(), s));
    }

    /** Every tick: a belt that changed some other way (a command, a respawn, a new dimension) reaches its own client. */
    public static void tick(MinecraftServer server) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            Sent s = SENT.get(p.getUUID());
            ItemStack cur = p.getItemBySlot(SLOT);
            if (s == null ? !cur.isEmpty() : s.player() != p || s.level() != p.level() || !ItemStack.matches(s.stack(), cur)) sync(p);
        }
    }

    public static void forget(ServerPlayer p) {
        SENT.remove(p.getUUID());
    }

    public static void clear() {
        SENT.clear();
    }

    /** Death without keepInventory (Inventory.dropAll): the lantern drops with the rest. */
    public static void dropOnDeath(Player p) {
        if (!(p instanceof ServerPlayer sp)) return;
        ItemStack s = sp.getItemBySlot(SLOT);
        if (!Lanterns.is(s)) return;
        sp.setItemSlot(SLOT, ItemStack.EMPTY);
        drop(sp, s, true);
    }

    /** Respawn with keepInventory (or back from the End): Inventory.replaceWith copies only the inventory's own slots. */
    public static void carryOver(Player from, Player to) {
        ItemStack s = from.getItemBySlot(SLOT);
        if (Lanterns.is(s) && to.getItemBySlot(SLOT).isEmpty()) to.setItemSlot(SLOT, s.copy());
    }
}
