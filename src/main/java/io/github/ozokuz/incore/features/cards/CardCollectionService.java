package io.github.ozokuz.incore.features.cards;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class CardCollectionService {
    private static final String KEY_ROOT = "incore:cards_collection";
    private static final String KEY_TOTAL = "total";
    private static final String KEY_FOILS = "foils";

    private CardCollectionService() {
    }

    public static void addCollected(ServerPlayer player, ResourceLocation cardId, boolean foil, int amount) {
        if (amount <= 0) {
            return;
        }

        CompoundTag root = player.getPersistentData().getCompound(KEY_ROOT);
        CompoundTag totals = root.getCompound(KEY_TOTAL);

        String key = cardId.toString();
        totals.putInt(key, Math.max(0, totals.getInt(key)) + amount);
        root.put(KEY_TOTAL, totals);

        if (foil) {
            CompoundTag foils = root.getCompound(KEY_FOILS);
            foils.putInt(key, Math.max(0, foils.getInt(key)) + amount);
            root.put(KEY_FOILS, foils);
        }

        player.getPersistentData().put(KEY_ROOT, root);
    }

    public static CollectionSummary summary(ServerPlayer player) {
        CompoundTag root = player.getPersistentData().getCompound(KEY_ROOT);
        CompoundTag totals = root.getCompound(KEY_TOTAL);
        CompoundTag foils = root.getCompound(KEY_FOILS);

        Map<String, Integer> counts = new TreeMap<>();
        List<String> keys = new ArrayList<>(totals.getAllKeys());
        for (String key : keys) {
            counts.put(key, Math.max(0, totals.getInt(key)));
        }

        int totalCards = counts.values().stream().mapToInt(Integer::intValue).sum();
        int totalFoils = foils.getAllKeys().stream().mapToInt(key -> Math.max(0, foils.getInt(key))).sum();
        return new CollectionSummary(totalCards, totalFoils, counts);
    }

    public static void copyData(ServerPlayer from, ServerPlayer to) {
        CompoundTag fromRoot = from.getPersistentData().getCompound(KEY_ROOT);
        if (!fromRoot.isEmpty()) {
            to.getPersistentData().put(KEY_ROOT, fromRoot.copy());
        }
    }

    public record CollectionSummary(int totalCards, int totalFoils, Map<String, Integer> counts) {
    }
}
