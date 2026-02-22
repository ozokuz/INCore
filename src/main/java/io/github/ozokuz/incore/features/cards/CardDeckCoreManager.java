package io.github.ozokuz.incore.features.cards;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.github.ozokuz.incore.INCore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CardDeckCoreManager extends SimpleJsonResourceReloadListener {
    private static volatile Map<ResourceLocation, CardDeckCoreData> cores = Map.of();

    public CardDeckCoreManager() {
        super(new Gson(), "deck_cores");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        Map<ResourceLocation, CardDeckCoreData> next = new LinkedHashMap<>();
        jsons.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> {
                    if (!entry.getValue().isJsonObject()) {
                        return;
                    }

                    CardDeckCoreData data = CardDeckCoreData.fromJson(entry.getKey(), entry.getValue().getAsJsonObject());
                    if (data != null) {
                        next.put(entry.getKey(), data);
                    } else {
                        INCore.LOGGER.warn("Skipping invalid deck core '{}'", entry.getKey());
                    }
                });
        cores = Map.copyOf(next);
        INCore.LOGGER.info("Loaded {} deck core definitions.", cores.size());
    }

    public static CardDeckCoreData get(ResourceLocation id) {
        return cores.get(id);
    }

    public static List<CardDeckCoreData> all() {
        return cores.values().stream().toList();
    }

    public static ResourceLocation getDefaultCoreId() {
        return cores.keySet().stream().findFirst().orElse(ResourceLocation.parse("incore:starter_core"));
    }
}
