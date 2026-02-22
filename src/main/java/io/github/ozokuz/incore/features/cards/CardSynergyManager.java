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

public class CardSynergyManager extends SimpleJsonResourceReloadListener {
    private static volatile Map<ResourceLocation, CardSynergyData> synergies = Map.of();

    public CardSynergyManager() {
        super(new Gson(), "card_synergies");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        Map<ResourceLocation, CardSynergyData> next = new LinkedHashMap<>();
        jsons.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> {
                    if (!entry.getValue().isJsonObject()) {
                        return;
                    }

                    CardSynergyData data = CardSynergyData.fromJson(entry.getKey(), entry.getValue().getAsJsonObject());
                    if (data != null) {
                        next.put(entry.getKey(), data);
                    } else {
                        INCore.LOGGER.warn("Skipping invalid card synergy '{}'", entry.getKey());
                    }
                });

        synergies = Map.copyOf(next);
        INCore.LOGGER.info("Loaded {} card synergy definitions.", synergies.size());
    }

    public static List<CardSynergyData> all() {
        return synergies.values().stream().toList();
    }
}
