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
//? if >=1.21.5 {
import net.minecraft.world.level.saveddata.SavedDataType;
//?}

/**
 * Where this dimension has belt-lantern light blocks, saved with the world, so a crash can't leave them behind: on the
 * next start every recorded one that isn't in use goes back to air (or water).
 */
public final class LightRecord extends SavedData {
    //? if >=1.21.5 {
    public static final Codec<LightRecord> CODEC = Codec.LONG.listOf().xmap(LightRecord::new, LightRecord::list);
    private static final Map<String, SavedDataType<LightRecord>> TYPES = new ConcurrentHashMap<>();
    //?}

    final LongOpenHashSet positions = new LongOpenHashSet();

    public LightRecord() {
    }

    private LightRecord(List<Long> list) {
        positions.addAll(list);
    }

    private List<Long> list() {
        return List.copyOf(positions);
    }

    //? if >=26.2 {
    /** One type per dimension, cached: the storage keys by the type record. */
    private static SavedDataType<LightRecord> type(ServerLevel level) {
        String dim = level.dimension().identifier().toString().replace(':', '_').replace('/', '_');
        return TYPES.computeIfAbsent(dim, d -> new SavedDataType<>(Identifier.fromNamespaceAndPath("travelers_lantern", "lights_" + d),
            LightRecord::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE));
    }

    public static LightRecord of(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(type(level));
    }
    //?}
    //? if >=1.21.5 <26.2 {
    /*// One type per dimension, cached: the storage keys by the type record (its id is a string before 26.2).
    private static SavedDataType<LightRecord> type(ServerLevel level) {
        String dim = level.dimension().identifier().toString().replace(':', '_').replace('/', '_');
        return TYPES.computeIfAbsent(dim, d -> new SavedDataType<>("travelers_lantern_lights_" + d,
            LightRecord::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE));
    }

    public static LightRecord of(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(type(level));
    }
    *///?}
    //? if <1.21.5 {
    /*// before 1.21.5 the data is NBT: a factory and a file name per dimension
    private static final String KEY = "travelers_lantern_lights_";

    @Override
    public net.minecraft.nbt.CompoundTag save(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        tag.putLongArray("positions", positions.toLongArray());
        return tag;
    }

    private static LightRecord load(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        LightRecord r = new LightRecord();
        for (long p : tag.getLongArray("positions")) r.positions.add(p);
        return r;
    }

    public static LightRecord of(ServerLevel level) {
        String dim = level.dimension().identifier().toString().replace(':', '_').replace('/', '_');
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(LightRecord::new, LightRecord::load, DataFixTypes.SAVED_DATA_COMMAND_STORAGE), KEY + dim);
    }
    *///?}

    void add(long pos) {
        if (positions.add(pos)) setDirty();
    }

    void remove(long pos) {
        if (positions.remove(pos)) setDirty();
    }
}
