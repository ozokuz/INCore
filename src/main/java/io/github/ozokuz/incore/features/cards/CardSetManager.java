package io.github.ozokuz.incore.features.cards;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.github.ozokuz.incore.INCore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CardSetManager extends SimpleJsonResourceReloadListener {
    private static volatile Map<ResourceLocation, CardSetData> sets = Map.of();

    public CardSetManager() {
        super(new Gson(), "card_sets");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        Map<ResourceLocation, CardSetData> next = new LinkedHashMap<>();
        jsons.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> {
                    if (!entry.getValue().isJsonObject()) {
                        return;
                    }
                    CardSetData data = CardSetData.fromJson(entry.getKey(), entry.getValue().getAsJsonObject());
                    if (data != null) {
                        next.put(entry.getKey(), data);
                    } else {
                        INCore.LOGGER.warn("Skipping invalid card set '{}'", entry.getKey());
                    }
                });

        sets = Map.copyOf(next);
        INCore.LOGGER.info("Loaded {} card set definitions.", sets.size());
    }

    public static CardSetData get(ResourceLocation id) {
        return sets.get(id);
    }

    public static List<CardSetData> all() {
        return sets.values().stream().toList();
    }

    public static boolean isSetActive(ResourceLocation id) {
        CardSetData set = sets.get(id);
        if (set == null) {
            return false;
        }
        return set.isActiveAt(Instant.now());
    }
}
