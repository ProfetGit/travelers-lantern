package io.github.profetgit.travelerslantern.server;

/**
 * Before 1.20.5 vanilla has no slot for the belt, so the belt goes out as a custom payload on travelers_lantern:belt: to the
 * owner and to every player tracking the owner. Only clients that said they have the mod (the /travelerslantern client
 * command, which every client with the mod sends on a server that has it) are sent anything; the client side is
 * ClientPacketListenerMixin.
 */
public final class BeltNet {
    private BeltNet() {
    }

    //? if <1.20.5 {
    /*public static final net.minecraft.resources.ResourceLocation ID = new net.minecraft.resources.ResourceLocation("travelers_lantern", "belt");
    private static final java.util.Set<java.util.UUID> MODDED = new java.util.HashSet<>();
    // Entity equality is by id and a respawned player keeps theirs: these have to compare by identity
    private static final java.util.Map<net.minecraft.server.level.ServerPlayer, java.util.Set<net.minecraft.server.level.ServerPlayer>> VIEWERS =
        new java.util.IdentityHashMap<>();

    public static void hello(net.minecraft.server.level.ServerPlayer p) {
        MODDED.add(p.getUUID());
        net.minecraft.world.item.ItemStack own = io.github.profetgit.travelerslantern.BeltSlot.get(p);
        if (!own.isEmpty()) send(p, p, own);
        for (java.util.Map.Entry<net.minecraft.server.level.ServerPlayer, java.util.Set<net.minecraft.server.level.ServerPlayer>> e : VIEWERS.entrySet()) {
            if (e.getKey() == p || !e.getValue().contains(p)) continue;
            net.minecraft.world.item.ItemStack s = io.github.profetgit.travelerslantern.BeltSlot.get(e.getKey());
            if (!s.isEmpty()) send(p, e.getKey(), s);
        }
    }

    // The viewer started tracking the owner: they get the owner's belt.
    public static void track(net.minecraft.server.level.ServerPlayer owner, net.minecraft.server.level.ServerPlayer viewer) {
        VIEWERS.computeIfAbsent(owner, k -> java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>())).add(viewer);
        net.minecraft.world.item.ItemStack s = io.github.profetgit.travelerslantern.BeltSlot.get(owner);
        if (!s.isEmpty()) send(viewer, owner, s);
    }

    public static void untrack(net.minecraft.server.level.ServerPlayer owner, net.minecraft.server.level.ServerPlayer viewer) {
        java.util.Set<net.minecraft.server.level.ServerPlayer> v = VIEWERS.get(owner);
        if (v == null) return;
        v.remove(viewer);
        if (v.isEmpty()) VIEWERS.remove(owner);
    }

    // The owner's belt changed: to the owner and everyone tracking them.
    public static void broadcast(net.minecraft.server.level.ServerPlayer owner, net.minecraft.world.item.ItemStack s) {
        send(owner, owner, s);
        java.util.Set<net.minecraft.server.level.ServerPlayer> v = VIEWERS.get(owner);
        if (v != null) for (net.minecraft.server.level.ServerPlayer w : v) send(w, owner, s);
    }

    private static void send(net.minecraft.server.level.ServerPlayer to, net.minecraft.server.level.ServerPlayer owner, net.minecraft.world.item.ItemStack s) {
        if (!MODDED.contains(to.getUUID())) return;
        net.minecraft.network.FriendlyByteBuf buf = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        buf.writeVarInt(owner.getId());
        buf.writeItem(s);
        to.connection.send(new net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket(ID, buf));
    }

    public static void forget(net.minecraft.server.level.ServerPlayer p) {
        MODDED.remove(p.getUUID());
        VIEWERS.remove(p);
        for (java.util.Iterator<java.util.Set<net.minecraft.server.level.ServerPlayer>> it = VIEWERS.values().iterator(); it.hasNext(); ) {
            java.util.Set<net.minecraft.server.level.ServerPlayer> v = it.next();
            v.remove(p);
            if (v.isEmpty()) it.remove();
        }
    }

    public static void clear() {
        MODDED.clear();
        VIEWERS.clear();
    }
    *///?}
}
