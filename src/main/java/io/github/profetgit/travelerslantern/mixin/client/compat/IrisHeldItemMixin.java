package io.github.profetgit.travelerslantern.mixin.client.compat;

import io.github.profetgit.travelerslantern.Config;
import io.github.profetgit.travelerslantern.Lanterns;
import io.github.profetgit.travelerslantern.client.BeltClient;
import it.unimi.dsi.fastutil.objects.Object2IntFunction;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Iris: shader packs light the area around you from what you hold (heldBlockLightValue, heldItemId and their off-hand
 * twins), every frame and at the exact position, so it's perfectly smooth. The belt lantern counts as held in the off
 * hand when it's brighter than what the off hand holds. Only when Iris is installed; it only shows with a shader pack
 * that has hand-held light (Complementary does).
 */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.uniforms.IdMapUniforms$HeldItemSupplier", remap = false)
public abstract class IrisHeldItemMixin {
    @Shadow(remap = false)
    @Final
    private InteractionHand hand;
    @Shadow(remap = false)
    @Final
    private Object2IntFunction<NamespacedId> itemIdMap;
    @Shadow(remap = false)
    private int intID;
    @Shadow(remap = false)
    private int lightValue;
    @Shadow(remap = false)
    private Vector3f lightColor;

    @Inject(method = "update", at = @At("RETURN"), require = 0, remap = false)
    private void travelerslantern$belt(CallbackInfo ci) {
        if (hand != InteractionHand.OFF_HAND || !Config.get().clientLight) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        ItemStack belt = BeltClient.beltOf(mc.player);
        if (belt.isEmpty()) return;
        int light = Lanterns.light(belt);
        if (light <= lightValue) return;
        Identifier id = BuiltInRegistries.ITEM.getKey(belt.getItem());
        intID = itemIdMap.applyAsInt(new NamespacedId(id.getNamespace(), id.getPath()));
        lightValue = light;
        lightColor = new Vector3f(1, 1, 1);
        io.github.profetgit.travelerslantern.client.BeltClient.irisHeld++;
    }
}
