package io.github.profetgit.beltlantern.server;

import io.github.profetgit.beltlantern.Config;
import io.github.profetgit.beltlantern.Lanterns;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Real light for belt lanterns: an invisible light block in the cell the lantern is in (or the wearer's feet or head
 * cell), moved along as they walk. Only air and still water are used, and only ever-our-own light blocks are taken
 * back: anything a player or the world put in the cell since wins. Two wearers in one cell share it (the brighter
 * lantern counts). The blocks are set without neighbour or shape updates, so observers, redstone and water don't
 * react to them.
 */
public final class Lights {
    static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    /** Per dimension: our light cells by position. */
    private static final Map<ResourceKey<Level>, Map<Long, Cell>> CELLS = new HashMap<>();
    /** Per wearer: the cell they light. */
    private static final Map<UUID, Claim> CLAIMS = new HashMap<>();
    /** Dimensions whose saved record was checked since the server started. */
    private static final Set<ResourceKey<Level>> SWEPT = new HashSet<>();
    private static int ticks;

    private static final class Cell {
        int level;
        final Map<UUID, Integer> owners = new HashMap<>();
    }

    private record Claim(ServerLevel level, long pos) {
    }

    private Lights() {
    }

    public static void tick(MinecraftServer server) {
        ticks++;
        boolean on = Config.get().light;
        Set<UUID> online = new HashSet<>();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            online.add(p.getUUID());
            ServerLevel level = p.level();
            if (SWEPT.add(level.dimension())) sweep(level);
            ItemStack belt = Belt.get(p);
            int lv = on && !belt.isEmpty() && p.isAlive() && !p.isSpectator() ? Lanterns.light(belt) : 0;
            Claim c = CLAIMS.get(p.getUUID());
            if (lv == 0) {
                if (c != null) release(p.getUUID());
                continue;
            }
            BlockPos target = choose(p, level, c);
            if (c != null && valid(c) && c.level() == level && target != null && c.pos() == target.asLong()) {
                Cell cell = cell(level, c.pos());
                if (cell.owners.getOrDefault(p.getUUID(), 0) != lv) {
                    cell.owners.put(p.getUUID(), lv);
                    refresh(level, BlockPos.of(c.pos()), cell);
                }
                continue;
            }
            // the new cell lights up before the old one goes dark, in the same tick
            Claim old = CLAIMS.remove(p.getUUID());
            if (target != null) claim(p.getUUID(), level, target, lv);
            if (old != null) unclaim(p.getUUID(), old);
        }
        for (Iterator<UUID> it = CLAIMS.keySet().iterator(); it.hasNext(); ) {
            UUID id = it.next();
            if (online.contains(id)) continue;
            Claim c = CLAIMS.get(id);
            it.remove();
            unclaim(id, c);
        }
        if (ticks % 20 == 0) for (ServerLevel level : server.getAllLevels()) if (SWEPT.contains(level.dimension())) sweep(level);
    }

    /** The first usable cell of: the lantern's cell, the feet cell, the head cell. The current cell counts as usable. */
    private static BlockPos choose(ServerPlayer p, ServerLevel level, Claim current) {
        double yaw = Math.toRadians(p.yBodyRot);
        Vec3 lantern = p.position().add(-Math.sin(yaw) * 0.15, p.getBbHeight() * 0.32, Math.cos(yaw) * 0.15);
        BlockPos[] tries = {BlockPos.containing(lantern), p.blockPosition(), BlockPos.containing(p.getEyePosition())};
        for (BlockPos pos : tries) {
            if (current != null && current.level() == level && current.pos() == pos.asLong() && valid(current)) return pos;
            if (usable(level, pos)) return pos;
        }
        return null;
    }

    private static boolean usable(ServerLevel level, BlockPos pos) {
        if (!level.isInWorldBounds(pos) || !level.isLoaded(pos)) return false;
        BlockState s = level.getBlockState(pos);
        if (s.isAir()) return true;
        if (s.is(Blocks.LIGHT)) return cell(level, pos.asLong()) != null;
        return s.is(Blocks.WATER) && s.getFluidState().isSource();
    }

    /** Still our light block (nothing replaced it). */
    private static boolean valid(Claim c) {
        Cell cell = cell(c.level(), c.pos());
        if (cell == null) return false;
        BlockPos pos = BlockPos.of(c.pos());
        if (!c.level().isLoaded(pos)) return false;
        if (c.level().getBlockState(pos).is(Blocks.LIGHT)) return true;
        // replaced (a placed block, a piston, flowing water): forget the cell without touching it
        CELLS.get(c.level().dimension()).remove(c.pos());
        LightRecord.of(c.level()).remove(c.pos());
        return false;
    }

    private static Cell cell(ServerLevel level, long pos) {
        Map<Long, Cell> cells = CELLS.get(level.dimension());
        return cells == null ? null : cells.get(pos);
    }

    private static void claim(UUID who, ServerLevel level, BlockPos pos, int lv) {
        Map<Long, Cell> cells = CELLS.computeIfAbsent(level.dimension(), k -> new HashMap<>());
        Cell cell = cells.get(pos.asLong());
        if (cell == null) {
            cell = new Cell();
            cell.owners.put(who, lv);
            cell.level = lv;
            cells.put(pos.asLong(), cell);
            boolean water = level.getBlockState(pos).is(Blocks.WATER);
            level.setBlock(pos, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, lv).setValue(LightBlock.WATERLOGGED, water), FLAGS);
            LightRecord.of(level).add(pos.asLong());
        } else {
            cell.owners.put(who, lv);
            refresh(level, pos, cell);
        }
        CLAIMS.put(who, new Claim(level, pos.asLong()));
    }

    /** The cell shows its brightest owner's lantern. */
    private static void refresh(ServerLevel level, BlockPos pos, Cell cell) {
        int max = 0;
        for (int v : cell.owners.values()) max = Math.max(max, v);
        if (max == cell.level) return;
        cell.level = max;
        BlockState s = level.getBlockState(pos);
        if (s.is(Blocks.LIGHT)) level.setBlock(pos, s.setValue(LightBlock.LEVEL, max), FLAGS);
    }

    public static void release(UUID who) {
        Claim c = CLAIMS.remove(who);
        if (c != null) unclaim(who, c);
    }

    private static void unclaim(UUID who, Claim c) {
        Map<Long, Cell> cells = CELLS.get(c.level().dimension());
        Cell cell = cells == null ? null : cells.get(c.pos());
        if (cell == null) return;
        cell.owners.remove(who);
        BlockPos pos = BlockPos.of(c.pos());
        if (!cell.owners.isEmpty()) {
            refresh(c.level(), pos, cell);
            return;
        }
        cells.remove(c.pos());
        // an unloaded cell stays in the record; the sweep puts it back once it loads
        if (c.level().isLoaded(pos)) putBack(c.level(), pos);
    }

    /** Our light block back to what it replaced: water if it's (still) waterlogged, else air. */
    private static void putBack(ServerLevel level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        if (s.is(Blocks.LIGHT)) {
            level.setBlock(pos, s.getValue(LightBlock.WATERLOGGED) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(), FLAGS);
        }
        LightRecord.of(level).remove(pos.asLong());
    }

    /** Recorded cells that are no longer in use (left by a crash, or unloaded when released) go back once loaded. */
    private static void sweep(ServerLevel level) {
        LightRecord rec = LightRecord.of(level);
        if (rec.positions.isEmpty()) return;
        Map<Long, Cell> cells = CELLS.get(level.dimension());
        long[] todo = rec.positions.toLongArray();
        for (long p : todo) {
            if (cells != null && cells.containsKey(p)) continue;
            BlockPos pos = BlockPos.of(p);
            if (level.isLoaded(pos)) putBack(level, pos);
        }
    }

    /** Server stopping: every light goes back before the world is saved. */
    public static void releaseAll() {
        for (UUID id : new HashSet<>(CLAIMS.keySet())) release(id);
        CELLS.clear();
        CLAIMS.clear();
        SWEPT.clear();
    }

    /** /beltlantern light false: every light goes back now. */
    public static void releaseAllNow() {
        for (UUID id : new HashSet<>(CLAIMS.keySet())) release(id);
    }

    /** Test hook: how many cells are lit in a dimension. */
    public static int count(ServerLevel level) {
        Map<Long, Cell> cells = CELLS.get(level.dimension());
        return cells == null ? 0 : cells.size();
    }

    /** Test hook: the cell a wearer lights, or null. */
    public static BlockPos cellOf(UUID who) {
        Claim c = CLAIMS.get(who);
        return c == null ? null : BlockPos.of(c.pos());
    }
}
