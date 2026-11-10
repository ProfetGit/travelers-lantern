package io.github.profetgit.travelerslantern.mixin.client;

import org.spongepowered.asm.mixin.Mixin;

/** Before 1.20.5 the belt arrives as a custom payload on travelers_lantern:belt (BeltNet) and is put on the entity here. */
//? if >=1.20.5 {
@Mixin(net.minecraft.client.Minecraft.class)
public abstract class ClientPacketListenerMixin {
}
//?} else {
/*@Mixin(net.minecraft.client.multiplayer.ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @org.spongepowered.asm.mixin.injection.Inject(method = "handleCustomPayload", at = @org.spongepowered.asm.mixin.injection.At("HEAD"), cancellable = true)
    private void travelerslantern$belt(net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket packet, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (!io.github.profetgit.travelerslantern.server.BeltNet.ID.equals(packet.getIdentifier())) return;
        ci.cancel();
        net.minecraft.network.FriendlyByteBuf buf = packet.getData();
        try {
            int id = buf.readVarInt();
            net.minecraft.world.item.ItemStack stack = buf.readItem();
            net.minecraft.client.Minecraft.getInstance().execute(() -> io.github.profetgit.travelerslantern.client.BeltClient.receive(id, stack));
        } finally {
            buf.release();
        }
    }
}
*///?}
