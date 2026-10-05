package io.github.profetgit.travelerslantern;

/** Vanilla API differences between the Minecraft versions this mod supports. */
public final class Compat {
    private Compat() {
    }

    public static net.minecraft.client.DeltaTracker delta(net.minecraft.client.Minecraft mc) {
        //? if >=1.21.2 {
        return mc.getDeltaTracker();
        //?} else {
        /*return mc.getTimer();
        *///?}
    }

    public static void screenshot(com.mojang.blaze3d.pipeline.RenderTarget target, java.util.function.Consumer<com.mojang.blaze3d.platform.NativeImage> then) {
        //? if >=1.21.5 {
        net.minecraft.client.Screenshot.takeScreenshot(target, then);
        //?} else {
        /*then.accept(net.minecraft.client.Screenshot.takeScreenshot(target));
        *///?}
    }
}
