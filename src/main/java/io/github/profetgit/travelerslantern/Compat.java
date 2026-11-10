package io.github.profetgit.travelerslantern;

/** Vanilla API differences between the Minecraft versions this mod supports. */
public final class Compat {
    private Compat() {
    }

    /** Real-time ticks since the last frame (20 a second). */
    public static float realtimeDeltaTicks(net.minecraft.client.Minecraft mc) {
        //? if >=1.21.2 {
        return mc.getDeltaTracker().getRealtimeDeltaTicks();
        //?} else if >=1.21 {
        /*return mc.getTimer().getRealtimeDeltaTicks();
        *///?} else {
        /*return mc.getDeltaFrameTime();
        *///?}
    }

    /** The partial tick of the game time (the frame's position between two ticks). */
    public static float partialTick(net.minecraft.client.Minecraft mc) {
        //? if >=1.21.2 {
        return mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        //?} else if >=1.21 {
        /*return mc.getTimer().getGameTimeDeltaPartialTick(false);
        *///?} else {
        /*return mc.getFrameTime();
        *///?}
    }

    /** A full opaque cube: its light cell is dark in vanilla and stays so. */
    public static boolean solid(net.minecraft.world.level.block.state.BlockState state, Object level, net.minecraft.core.BlockPos pos) {
        //? if >=1.21.2 {
        return state.isSolidRender();
        //?} else {
        /*return state.isSolidRender((net.minecraft.world.level.BlockGetter) level, pos);
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
