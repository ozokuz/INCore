package io.github.ozokuz.incore.features.surfaceore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class SurfaceStonePatchSavedData extends SavedData {
    private static final String DATA_NAME = "incore_surface_stone_patches";

    private final Map<Long, BlockPos> patchesByChunk = new HashMap<>();

    public static SurfaceStonePatchSavedData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(SurfaceStonePatchSavedData::new, SurfaceStonePatchSavedData::load, DataFixTypes.LEVEL),
                DATA_NAME
        );
    }

    public static SurfaceStonePatchSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        SurfaceStonePatchSavedData data = new SurfaceStonePatchSavedData();
        ListTag patches = tag.getList("patches", Tag.TAG_COMPOUND);
        for (Tag rowTag : patches) {
            CompoundTag row = (CompoundTag) rowTag;
            if (!row.contains("chunk", Tag.TAG_LONG) || !row.contains("pos", Tag.TAG_LONG)) {
                continue;
            }
            long chunkKey = row.getLong("chunk");
            BlockPos pos = BlockPos.of(row.getLong("pos"));
            data.patchesByChunk.put(chunkKey, pos);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag patches = new ListTag();
        for (Map.Entry<Long, BlockPos> entry : patchesByChunk.entrySet()) {
            CompoundTag row = new CompoundTag();
            row.putLong("chunk", entry.getKey());
            row.putLong("pos", entry.getValue().asLong());
            patches.add(row);
        }
        tag.put("patches", patches);
        return tag;
    }

    public void recordPatch(BlockPos patchPos) {
        ChunkPos chunkPos = new ChunkPos(patchPos);
        long chunkKey = ChunkPos.asLong(chunkPos.x, chunkPos.z);
        BlockPos previous = patchesByChunk.put(chunkKey, patchPos.immutable());
        if (previous == null || !previous.equals(patchPos)) {
            setDirty();
        }
    }

    public Optional<PatchTarget> findNearestUnfound(BlockPos from, Set<Long> foundChunkKeys) {
        double bestDistance = Double.MAX_VALUE;
        PatchTarget best = null;
        for (Map.Entry<Long, BlockPos> entry : patchesByChunk.entrySet()) {
            if (foundChunkKeys.contains(entry.getKey())) {
                continue;
            }

            double distance = from.distSqr(entry.getValue());
            if (distance >= bestDistance) {
                continue;
            }

            bestDistance = distance;
            best = new PatchTarget(entry.getKey(), entry.getValue());
        }
        return Optional.ofNullable(best);
    }

    public record PatchTarget(long chunkKey, BlockPos pos) {
    }
}
