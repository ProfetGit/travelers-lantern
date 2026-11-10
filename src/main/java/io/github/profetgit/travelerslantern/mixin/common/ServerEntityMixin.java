package io.github.profetgit.travelerslantern.mixin.common;

import org.spongepowered.asm.mixin.Mixin;

/** Before 1.20.5 the belt isn't an equipment slot, so who is tracking whom is noted here (BeltNet). */
//? if >=1.20.5 {
@Mixin(net.minecraft.server.MinecraftServer.class)
public abstract class ServerEntityMixin {
}
//?} else {
/*@Mixin(net.minecraft.server.level.ServerEntity.class)
public abstract class ServerEntityMixin {
    @org.spongepowered.asm.mixin.Shadow
    @org.spongepowered.asm.mixin.Final
    private net.minecraft.world.entity.Entity entity;

    @org.spongepowered.asm.mixin.injection.Inject(method = "addPairing", at = @org.spongepowered.asm.mixin.injection.At("TAIL"))
    private void travelerslantern$add(net.minecraft.server.level.ServerPlayer viewer, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (entity instanceof net.minecraft.server.level.ServerPlayer owner) io.github.profetgit.travelerslantern.server.BeltNet.track(owner, viewer);
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = "removePairing", at = @org.spongepowered.asm.mixin.injection.At("TAIL"))
    private void travelerslantern$remove(net.minecraft.server.level.ServerPlayer viewer, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (entity instanceof net.minecraft.server.level.ServerPlayer owner) io.github.profetgit.travelerslantern.server.BeltNet.untrack(owner, viewer);
    }
}
*///?}
