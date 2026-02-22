package io.github.ozokuz.incore.features.research;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class ResearchProgressSavedData extends SavedData {
    private static final String DATA_NAME = "incore_research_progress";

    private final Map<String, CompoundTag> stateByOwner = new HashMap<>();

    public static ResearchProgressSavedData get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(ResearchProgressSavedData::new, ResearchProgressSavedData::load, DataFixTypes.LEVEL),
                DATA_NAME
        );
    }

    public static ResearchProgressSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        ResearchProgressSavedData data = new ResearchProgressSavedData();
        ListTag owners = tag.getList("owners", Tag.TAG_COMPOUND);
        for (Tag ownerTag : owners) {
            if (!(ownerTag instanceof CompoundTag ownerCompound)) {
                continue;
            }
            String ownerKey = ownerCompound.getString("owner");
            if (ownerKey == null || ownerKey.isBlank()) {
                continue;
            }
            CompoundTag root = ownerCompound.getCompound("root");
            data.stateByOwner.put(ownerKey, root.copy());
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag owners = new ListTag();
        for (Map.Entry<String, CompoundTag> entry : stateByOwner.entrySet()) {
            CompoundTag ownerTag = new CompoundTag();
            ownerTag.putString("owner", entry.getKey());
            ownerTag.put("root", entry.getValue().copy());
            owners.add(ownerTag);
        }
        tag.put("owners", owners);
        return tag;
    }

    public CompoundTag getOrCreateRoot(String ownerKey) {
        return stateByOwner.computeIfAbsent(ownerKey, ignored -> new CompoundTag());
    }

    public @Nullable CompoundTag getRoot(String ownerKey) {
        return stateByOwner.get(ownerKey);
    }

    public boolean removeOwner(String ownerKey) {
        return stateByOwner.remove(ownerKey) != null;
    }
}

