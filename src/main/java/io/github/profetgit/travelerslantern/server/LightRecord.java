package io.github.profetgit.travelerslantern.server;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Where this dimension has belt-lantern light blocks, saved with the world, so a crash can't leave them behind: on the
 * next start every recorded one that isn't in use goes back to air (or water).
 */
public final class LightRecord extends SavedData {
    public static final Codec<LightRecord> CODEC = Codec.LONG.listOf().xmap(LightRecord::new, LightRecord::list);
    private static final Map<String, SavedDataType<LightRecord>> TYPES = new ConcurrentHashMap<>();

    final LongOpenHashSet positions = new LongOpenHashSet();

    public LightRecord() {
    }

    private LightRecord(List<Long> list) {
        positions.addAll(list);
    }

    private List<Long> list() {
        return List.copyOf(positions);
    }

    /** One type per dimension, cached: the storage keys by the type record. */
    private static SavedDataType<LightRecord> type(ServerLevel level) {
        String dim = level.dimension().identifier().toString().replace(':', '_').replace('/', '_');
        return TYPES.computeIfAbsent(dim, d -> new SavedDataType<>(Identifier.fromNamespaceAndPath("travelers_lantern", "lights_" + d),
            LightRecord::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE));
    }

    public static LightRecord of(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(type(level));
    }

    void add(long pos) {
        if (positions.add(pos)) setDirty();
    }

    void remove(long pos) {
        if (positions.remove(pos)) setDirty();
    }
}
