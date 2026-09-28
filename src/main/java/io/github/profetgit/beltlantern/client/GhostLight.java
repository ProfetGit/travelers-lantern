package io.github.profetgit.beltlantern.client;

import io.github.profetgit.beltlantern.Config;
import io.github.profetgit.beltlantern.Lanterns;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Light on a server without the mod: a light block that only this client's copy of the world has, in the lantern's
 * cell. The game's own light engine lights around it, so it works with any renderer and shader pack, but nobody else
 * sees it and the server never knows. It only replaces air or still water, and it is put back the moment the lantern
 * moves on. When the server changes that cell, the server's block wins and the light moves on the next tick.
 */
public final class GhostLight {
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static ClientLevel level;
    private static BlockPos pos;
    private static BlockState real;
    private static BlockState ghost;

    private GhostLight() {
    }

    public static void tick(Minecraft mc, boolean clientOnly) {
        LocalPlayer p = mc.player;
        ClientLevel lv = mc.level;
        if (lv != level) {
            // a new world or dimension: the old light went with the old copy
            level = lv;
            pos = null;
        }
        int want = 0;
        BlockPos target = null;
        if (lv != null && p != null && clientOnly && Config.get().clientLight && p.isAlive() && !p.isSpectator()) {
            ItemStack s = VirtualBelt.lantern(p);
            if (!s.isEmpty()) {
                want = Lanterns.light(s);
                if (want > 0) target = choose(p, lv);
            }
        }
        if (pos != null && !lv.getBlockState(pos).equals(ghost)) pos = null; // replaced by the server or a prediction
        if (pos != null && (target == null || !target.equals(pos) || ghost.getValue(LightBlock.LEVEL) != want)) remove();
        if (target != null && pos == null) place(lv, target, want);
    }

    private static BlockPos choose(LocalPlayer p, ClientLevel lv) {
        Vec3 bob = Swing.bobOf(p.getId());
        if (bob == null) {
            double yaw = Math.toRadians(p.yBodyRot);
            bob = p.position().add(-Math.sin(yaw) * 0.15, p.getBbHeight() * 0.32, Math.cos(yaw) * 0.15);
        }
        BlockPos[] tries = {BlockPos.containing(bob), p.blockPosition(), BlockPos.containing(p.getEyePosition())};
        for (BlockPos t : tries) {
            if (t.equals(pos)) return t;
            if (usable(lv, t)) return t;
        }
        return null;
    }

    private static boolean usable(ClientLevel lv, BlockPos t) {
        if (!lv.isInWorldBounds(t) || !lv.isLoaded(t)) return false;
        BlockState s = lv.getBlockState(t);
        return s.isAir() || s.is(Blocks.WATER) && s.getFluidState().isSource();
    }

    private static void place(ClientLevel lv, BlockPos t, int lvl) {
        real = lv.getBlockState(t);
        ghost = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, lvl).setValue(LightBlock.WATERLOGGED, real.is(Blocks.WATER));
        pos = t.immutable();
        lv.setBlock(pos, ghost, FLAGS);
    }

    private static void remove() {
        if (level != null && level.getBlockState(pos).equals(ghost)) level.setBlock(pos, real, FLAGS);
        pos = null;
    }

    /** The server set this block (ClientLevel.setServerVerifiedBlockState): the ghost there is gone. */
    public static void onServerBlock(BlockPos at) {
        if (pos != null && pos.equals(at)) pos = null;
    }

    /**
     * A block prediction (placing or breaking) keeps the state it replaced, to roll back to if the server refuses; it
     * must roll back to the real block, not to the ghost light.
     */
    public static BlockState knownServerState(BlockPos at, BlockState state) {
        return pos != null && pos.equals(at) && state.equals(ghost) ? real : state;
    }

    /** The cell the ghost light is in (tests). */
    public static BlockPos cell() {
        return pos;
    }
}
