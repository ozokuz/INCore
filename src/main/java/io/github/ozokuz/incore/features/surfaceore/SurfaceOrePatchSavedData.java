package io.github.ozokuz.incore.features.surfaceore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class SurfaceOrePatchSavedData extends SavedData {
    private static final String DATA_NAME = "incore_surface_ore_patches";

    private final Map<String, BlockPos> patchesByKey = new HashMap<>();

    public static SurfaceOrePatchSavedData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(SurfaceOrePatchSavedData::new, SurfaceOrePatchSavedData::load, DataFixTypes.LEVEL),
                DATA_NAME
        );
    }

    public static SurfaceOrePatchSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        SurfaceOrePatchSavedData data = new SurfaceOrePatchSavedData();
        ListTag patches = tag.getList("patches", Tag.TAG_COMPOUND);
        for (Tag rowTag : patches) {
            CompoundTag row = (CompoundTag) rowTag;
            if (!row.contains("key", Tag.TAG_STRING) || !row.contains("pos", Tag.TAG_LONG)) {
                continue;
            }
            String key = row.getString("key");
            BlockPos pos = BlockPos.of(row.getLong("pos"));
            data.patchesByKey.put(key, pos);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag patches = new ListTag();
        for (Map.Entry<String, BlockPos> entry : patchesByKey.entrySet()) {
            CompoundTag row = new CompoundTag();
            row.putString("key", entry.getKey());
            row.putLong("pos", entry.getValue().asLong());
            patches.add(row);
        }
        tag.put("patches", patches);
        return tag;
    }

    public void recordPatch(BlockPos patchPos, ResourceKey<Level> dimension, SurfaceOreType type) {
        String key = makeKey(patchPos, dimension, type);
        BlockPos previous = patchesByKey.put(key, patchPos.immutable());
        if (previous == null || !previous.equals(patchPos)) {
            setDirty();
        }
    }

    public Optional<PatchTarget> findNearestUnfound(BlockPos from, ResourceKey<Level> dimension, Set<String> foundKeys) {
        String dimensionPrefix = dimension.location().toString() + ":";
        double bestDistance = Double.MAX_VALUE;
        PatchTarget best = null;
        for (Map.Entry<String, BlockPos> entry : patchesByKey.entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith(dimensionPrefix)) {
                continue;
            }
            if (foundKeys.contains(key)) {
                continue;
            }

            double distance = from.distSqr(entry.getValue());
            if (distance >= bestDistance) {
                continue;
            }

            bestDistance = distance;
            best = new PatchTarget(key, entry.getValue());
        }
        return Optional.ofNullable(best);
    }

    public Optional<PatchTarget> findNearestUnfoundByType(BlockPos from, ResourceKey<Level> dimension, SurfaceOreType type, Set<String> foundKeys) {
        String typePrefix = type.getSerializedName();
        String dimensionPrefix = dimension.location().toString() + ":" + typePrefix + ":";
        double bestDistance = Double.MAX_VALUE;
        PatchTarget best = null;
        for (Map.Entry<String, BlockPos> entry : patchesByKey.entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith(dimensionPrefix)) {
                continue;
            }
            if (foundKeys.contains(key)) {
                continue;
            }

            double distance = from.distSqr(entry.getValue());
            if (distance >= bestDistance) {
                continue;
            }

            bestDistance = distance;
            best = new PatchTarget(key, entry.getValue());
        }
        return Optional.ofNullable(best);
    }

    private static String makeKey(BlockPos pos, ResourceKey<Level> dimension, SurfaceOreType type) {
        ChunkPos chunkPos = new ChunkPos(pos);
        return dimension.location().toString() + ":" + type.getSerializedName() + ":" + chunkPos.x + ":" + chunkPos.z;
    }

    public record PatchTarget(String key, BlockPos pos) {
    }
}
