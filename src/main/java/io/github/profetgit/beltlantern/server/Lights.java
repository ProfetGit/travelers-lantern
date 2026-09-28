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
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
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
 * Belt-lantern light for players whose client doesn't draw it: the server tells their client there is a light block in
 * the lantern's cell (or the wearer's feet or head cell), and takes it back when the lantern moves on. Nothing is placed
 * in the world, so nothing is left behind and nothing else sees it. Only air and still water cells are used. Clients
 * with the mod light every lantern themselves, smoothly (DynamicLight); they say so with /beltlantern client smooth.
 * A client says blocks (or nothing: a client without the mod) and gets these.
 */
public final class Lights {
    static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    /** Every how many ticks a player's fake lights are sent again (a chunk that reloaded on their client lost them). */
    static final int RESEND = 40;
    /** Players whose client lights belt lanterns itself. */
    private static final Set<UUID> SMOOTH = new HashSet<>();
    /** Per wearer: the cell their lantern lights. */
    private static final Map<UUID, Cell> CELLS = new HashMap<>();
    /** Per viewer: the fake light blocks their client has now, and the dimension they were sent in. */
    private static final Map<UUID, Sent> SENT = new HashMap<>();
    /** Dimensions whose saved record of 0.1.0's world light blocks was checked since the server started. */
    private static final Set<ResourceKey<Level>> SWEPT = new HashSet<>();
    private static int ticks;

    private record Cell(ResourceKey<Level> dim, long pos, int level) {
    }

    private static final class Sent {
        ResourceKey<Level> dim;
        final Map<Long, BlockState> blocks = new HashMap<>();
        final Map<Long, Integer> at = new HashMap<>();
    }

    private Lights() {
    }

    public static void tick(MinecraftServer server) {
        ticks++;
        boolean on = Config.get().light;
        Set<UUID> online = new HashSet<>();
        // each wearer's cell
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            online.add(p.getUUID());
            ServerLevel level = p.level();
            if (SWEPT.add(level.dimension()) || ticks % 20 == 0) sweep(level);
            ItemStack belt = Belt.get(p);
            int lv = on && !belt.isEmpty() && p.isAlive() && !p.isSpectator() ? Lanterns.light(belt) : 0;
            BlockPos target = lv > 0 ? choose(p, level, CELLS.get(p.getUUID())) : null;
            if (target == null) CELLS.remove(p.getUUID());
            else CELLS.put(p.getUUID(), new Cell(level.dimension(), target.asLong(), lv));
        }
        CELLS.keySet().retainAll(online);
        SMOOTH.retainAll(online);
        SENT.keySet().retainAll(online);
        // the cells per dimension, the brighter lantern winning a shared cell
        Map<ResourceKey<Level>, Map<Long, Integer>> want = new HashMap<>();
        for (Cell c : CELLS.values()) want.computeIfAbsent(c.dim(), k -> new HashMap<>()).merge(c.pos(), c.level(), Math::max);
        // what each viewer without the mod's own light is shown
        for (ServerPlayer viewer : server.getPlayerList().getPlayers()) {
            boolean shows = !SMOOTH.contains(viewer.getUUID());
            Sent sent = SENT.computeIfAbsent(viewer.getUUID(), k -> new Sent());
            ServerLevel level = viewer.level();
            if (sent.dim != level.dimension()) {
                // a new dimension: the client dropped the old world, fakes and all
                sent.blocks.clear();
                sent.at.clear();
                sent.dim = level.dimension();
            }
            Map<Long, Integer> cells = shows ? want.getOrDefault(level.dimension(), Map.of()) : Map.of();
            for (Map.Entry<Long, Integer> e : cells.entrySet()) {
                BlockPos pos = BlockPos.of(e.getKey());
                if (!tracks(viewer, pos)) continue;
                BlockState real = level.getBlockState(pos);
                BlockState fake = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, e.getValue())
                    .setValue(LightBlock.WATERLOGGED, real.is(Blocks.WATER));
                Integer when = sent.at.get(e.getKey());
                if (fake.equals(sent.blocks.get(e.getKey())) && when != null && ticks - when < RESEND) continue;
                viewer.connection.send(new ClientboundBlockUpdatePacket(pos, fake));
                sent.blocks.put(e.getKey(), fake);
                sent.at.put(e.getKey(), ticks);
            }
            for (Iterator<Long> it = sent.blocks.keySet().iterator(); it.hasNext(); ) {
                long p = it.next();
                if (cells.containsKey(p)) continue;
                BlockPos pos = BlockPos.of(p);
                // the real block again (air, water, or whatever took the cell)
                if (tracks(viewer, pos)) viewer.connection.send(new ClientboundBlockUpdatePacket(level, pos));
                it.remove();
                sent.at.remove(p);
            }
        }
    }

    private static boolean tracks(ServerPlayer viewer, BlockPos pos) {
        return viewer.getChunkTrackingView().contains(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
    }

    /** The first usable cell of: the lantern's cell, the feet cell, the head cell; the current cell while it's one of them. */
    private static BlockPos choose(ServerPlayer p, ServerLevel level, Cell current) {
        double yaw = Math.toRadians(p.yBodyRot);
        Vec3 lantern = p.position().add(Math.cos(yaw) * 0.3, p.getBbHeight() * 0.32, Math.sin(yaw) * 0.3);
        BlockPos[] tries = {BlockPos.containing(lantern), p.blockPosition(), BlockPos.containing(p.getEyePosition())};
        for (BlockPos pos : tries) {
            if (current != null && current.dim() == level.dimension() && current.pos() == pos.asLong() && usable(level, pos)) return pos;
        }
        for (BlockPos pos : tries) if (usable(level, pos)) return pos;
        return null;
    }

    private static boolean usable(ServerLevel level, BlockPos pos) {
        if (!level.isInWorldBounds(pos) || !level.isLoaded(pos)) return false;
        BlockState s = level.getBlockState(pos);
        return s.isAir() || s.is(Blocks.WATER) && s.getFluidState().isSource();
    }

    /** /beltlantern client smooth|blocks: whether this player's client draws the light itself. */
    public static void setSmooth(ServerPlayer p, boolean smooth) {
        if (smooth) {
            SMOOTH.add(p.getUUID());
            // take back what was already sent (the next tick sees nothing to show and restores the real blocks)
        } else {
            SMOOTH.remove(p.getUUID());
        }
    }

    public static void release(UUID who) {
        CELLS.remove(who);
        SENT.remove(who);
        SMOOTH.remove(who);
    }

    /** 0.1.0 placed real light blocks and recorded them; any left (a crash) go back to air or water once loaded. */
    private static void sweep(ServerLevel level) {
        LightRecord rec = LightRecord.of(level);
        if (rec.positions.isEmpty()) return;
        for (long p : rec.positions.toLongArray()) {
            BlockPos pos = BlockPos.of(p);
            if (!level.isLoaded(pos)) continue;
            BlockState s = level.getBlockState(pos);
            if (s.is(Blocks.LIGHT)) {
                level.setBlock(pos, s.getValue(LightBlock.WATERLOGGED) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(), FLAGS);
            }
            rec.remove(p);
        }
    }

    public static void clear() {
        CELLS.clear();
        SENT.clear();
        SMOOTH.clear();
        SWEPT.clear();
    }

    /** For /beltlantern light and the tests: lit cells, and fake light blocks sent to players now. */
    public static String status() {
        int fakes = 0, viewers = 0;
        for (Sent s : SENT.values()) {
            fakes += s.blocks.size();
            if (!s.blocks.isEmpty()) viewers++;
        }
        return CELLS.size() + " lanterns lit, " + fakes + " light blocks shown to " + viewers + " players without the mod's own light, "
            + SMOOTH.size() + " players light them themselves";
    }

    /** Test hook: the cell a wearer's lantern lights, or null. */
    public static BlockPos cellOf(UUID who) {
        Cell c = CELLS.get(who);
        return c == null ? null : BlockPos.of(c.pos());
    }

    /** Test hook: fake light blocks a viewer's client has now. */
    public static int shownTo(UUID viewer) {
        Sent s = SENT.get(viewer);
        return s == null ? 0 : s.blocks.size();
    }

    public static boolean smooth(UUID viewer) {
        return SMOOTH.contains(viewer);
    }
}
