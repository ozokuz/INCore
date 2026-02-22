package io.github.ozokuz.incore.features.shop;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Comparator;

public final class ShopSavedData extends SavedData {
    private static final String DATA_NAME = "incore_shop";

    private final Map<UUID, PlayerState> playerStates = new HashMap<>();
    private final Set<ResourceLocation> globalLockedCategories = new HashSet<>();
    private final Set<ResourceLocation> globalLockedOffers = new HashSet<>();

    public static ShopSavedData get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(ShopSavedData::new, ShopSavedData::load, DataFixTypes.LEVEL),
                DATA_NAME
        );
    }

    public static ShopSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        ShopSavedData data = new ShopSavedData();

        ListTag players = tag.getList("players", Tag.TAG_COMPOUND);
        for (Tag entryTag : players) {
            if (!(entryTag instanceof CompoundTag entry) || !entry.hasUUID("player")) {
                continue;
            }

            UUID playerId = entry.getUUID("player");
            PlayerState state = PlayerState.fromTag(entry);
            data.playerStates.put(playerId, state);
        }

        data.globalLockedCategories.addAll(readResourceSet(tag.getList("globalLockedCategories", Tag.TAG_STRING)));
        data.globalLockedOffers.addAll(readResourceSet(tag.getList("globalLockedOffers", Tag.TAG_STRING)));

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag players = new ListTag();
        for (Map.Entry<UUID, PlayerState> entry : playerStates.entrySet()) {
            CompoundTag row = entry.getValue().toTag();
            row.putUUID("player", entry.getKey());
            players.add(row);
        }
        tag.put("players", players);

        tag.put("globalLockedCategories", writeResourceSet(globalLockedCategories));
        tag.put("globalLockedOffers", writeResourceSet(globalLockedOffers));
        return tag;
    }

    public PlayerState stateFor(UUID playerId) {
        return playerStates.computeIfAbsent(playerId, ignored -> {
            setDirty();
            return new PlayerState();
        });
    }

    public boolean isCategoryLocked(UUID playerId, ResourceLocation categoryId) {
        PlayerState state = stateFor(playerId);
        if (state.unlockedCategories().contains(categoryId)) {
            return false;
        }
        return globalLockedCategories.contains(categoryId) || state.lockedCategories().contains(categoryId);
    }

    public boolean isOfferLocked(UUID playerId, ResourceLocation offerId) {
        PlayerState state = stateFor(playerId);
        if (state.unlockedOffers().contains(offerId)) {
            return false;
        }
        return globalLockedOffers.contains(offerId) || state.lockedOffers().contains(offerId);
    }

    public boolean setPlayerCategoryLock(UUID playerId, ResourceLocation categoryId, boolean locked) {
        PlayerState state = stateFor(playerId);
        boolean changed;
        if (locked) {
            changed = state.lockedCategories().add(categoryId);
            changed |= state.unlockedCategories().remove(categoryId);
        } else {
            changed = state.unlockedCategories().add(categoryId);
            changed |= state.lockedCategories().remove(categoryId);
        }
        if (changed) {
            setDirty();
        }
        return changed;
    }

    public boolean setPlayerOfferLock(UUID playerId, ResourceLocation offerId, boolean locked) {
        PlayerState state = stateFor(playerId);
        boolean changed;
        if (locked) {
            changed = state.lockedOffers().add(offerId);
            changed |= state.unlockedOffers().remove(offerId);
        } else {
            changed = state.unlockedOffers().add(offerId);
            changed |= state.lockedOffers().remove(offerId);
        }
        if (changed) {
            setDirty();
        }
        return changed;
    }

    public boolean setGlobalCategoryLock(ResourceLocation categoryId, boolean locked) {
        boolean changed = locked
                ? globalLockedCategories.add(categoryId)
                : globalLockedCategories.remove(categoryId);
        if (changed) {
            setDirty();
        }
        return changed;
    }

    public boolean setGlobalOfferLock(ResourceLocation offerId, boolean locked) {
        boolean changed = locked
                ? globalLockedOffers.add(offerId)
                : globalLockedOffers.remove(offerId);
        if (changed) {
            setDirty();
        }
        return changed;
    }

    public Set<ResourceLocation> globalLockedCategories() {
        return Set.copyOf(globalLockedCategories);
    }

    public Set<ResourceLocation> globalLockedOffers() {
        return Set.copyOf(globalLockedOffers);
    }

    private static Set<ResourceLocation> readResourceSet(ListTag listTag) {
        Set<ResourceLocation> values = new HashSet<>();
        for (Tag tag : listTag) {
            if (!(tag instanceof StringTag stringTag)) {
                continue;
            }
            ResourceLocation id = ResourceLocation.tryParse(stringTag.getAsString());
            if (id != null) {
                values.add(id);
            }
        }
        return values;
    }

    private static ListTag writeResourceSet(Set<ResourceLocation> values) {
        ListTag listTag = new ListTag();
        values.stream()
                .map(ResourceLocation::toString)
                .sorted()
                .forEach(value -> listTag.add(StringTag.valueOf(value)));
        return listTag;
    }

    public static final class PlayerState {
        private final Map<ResourceLocation, Integer> itemStocks = new HashMap<>();
        private final Map<ResourceLocation, Integer> categoryBucketStocks = new HashMap<>();
        private final Map<ResourceLocation, String> replenishTokens = new HashMap<>();

        private final Set<ResourceLocation> lockedCategories = new HashSet<>();
        private final Set<ResourceLocation> unlockedCategories = new HashSet<>();
        private final Set<ResourceLocation> lockedOffers = new HashSet<>();
        private final Set<ResourceLocation> unlockedOffers = new HashSet<>();

        private static PlayerState fromTag(CompoundTag tag) {
            PlayerState state = new PlayerState();
            readIntMap(tag.getList("itemStocks", Tag.TAG_COMPOUND), state.itemStocks);
            readIntMap(tag.getList("categoryBucketStocks", Tag.TAG_COMPOUND), state.categoryBucketStocks);
            readStringMap(tag.getList("replenishTokens", Tag.TAG_COMPOUND), state.replenishTokens);

            state.lockedCategories.addAll(readResourceSet(tag.getList("lockedCategories", Tag.TAG_STRING)));
            state.unlockedCategories.addAll(readResourceSet(tag.getList("unlockedCategories", Tag.TAG_STRING)));
            state.lockedOffers.addAll(readResourceSet(tag.getList("lockedOffers", Tag.TAG_STRING)));
            state.unlockedOffers.addAll(readResourceSet(tag.getList("unlockedOffers", Tag.TAG_STRING)));
            return state;
        }

        private CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.put("itemStocks", writeIntMap(itemStocks));
            tag.put("categoryBucketStocks", writeIntMap(categoryBucketStocks));
            tag.put("replenishTokens", writeStringMap(replenishTokens));

            tag.put("lockedCategories", writeResourceSet(lockedCategories));
            tag.put("unlockedCategories", writeResourceSet(unlockedCategories));
            tag.put("lockedOffers", writeResourceSet(lockedOffers));
            tag.put("unlockedOffers", writeResourceSet(unlockedOffers));
            return tag;
        }

        public Map<ResourceLocation, Integer> itemStocks() {
            return itemStocks;
        }

        public Map<ResourceLocation, Integer> categoryBucketStocks() {
            return categoryBucketStocks;
        }

        public Map<ResourceLocation, String> replenishTokens() {
            return replenishTokens;
        }

        public Set<ResourceLocation> lockedCategories() {
            return lockedCategories;
        }

        public Set<ResourceLocation> unlockedCategories() {
            return unlockedCategories;
        }

        public Set<ResourceLocation> lockedOffers() {
            return lockedOffers;
        }

        public Set<ResourceLocation> unlockedOffers() {
            return unlockedOffers;
        }

        private static void readIntMap(ListTag listTag, Map<ResourceLocation, Integer> target) {
            for (Tag entryTag : listTag) {
                if (!(entryTag instanceof CompoundTag entry)) {
                    continue;
                }
                ResourceLocation id = ResourceLocation.tryParse(entry.getString("id"));
                if (id == null) {
                    continue;
                }
                target.put(id, Math.max(0, entry.getInt("value")));
            }
        }

        private static ListTag writeIntMap(Map<ResourceLocation, Integer> source) {
            ListTag listTag = new ListTag();
            source.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                    .forEach(entry -> {
                        CompoundTag row = new CompoundTag();
                        row.putString("id", entry.getKey().toString());
                        row.putInt("value", Math.max(0, entry.getValue()));
                        listTag.add(row);
                    });
            return listTag;
        }

        private static void readStringMap(ListTag listTag, Map<ResourceLocation, String> target) {
            for (Tag entryTag : listTag) {
                if (!(entryTag instanceof CompoundTag entry)) {
                    continue;
                }
                ResourceLocation id = ResourceLocation.tryParse(entry.getString("id"));
                if (id == null) {
                    continue;
                }
                target.put(id, entry.getString("value"));
            }
        }

        private static ListTag writeStringMap(Map<ResourceLocation, String> source) {
            ListTag listTag = new ListTag();
            source.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                    .forEach(entry -> {
                        CompoundTag row = new CompoundTag();
                        row.putString("id", entry.getKey().toString());
                        row.putString("value", entry.getValue());
                        listTag.add(row);
                    });
            return listTag;
        }
    }
}
