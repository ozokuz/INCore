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

public class CardBoosterManager extends SimpleJsonResourceReloadListener {
    private static volatile Map<ResourceLocation, CardBoosterData> boostersBySet = Map.of();
    private static volatile Map<ResourceLocation, ResourceLocation> boosterAliases = Map.of();

    public CardBoosterManager() {
        super(new Gson(), "boosters");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        Map<ResourceLocation, CardBoosterData> nextBySet = new LinkedHashMap<>();
        Map<ResourceLocation, ResourceLocation> nextAliases = new LinkedHashMap<>();
        jsons.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> {
                    if (!entry.getValue().isJsonObject()) {
                        return;
                    }

                    CardBoosterData data = CardBoosterData.fromJson(entry.getKey(), entry.getValue().getAsJsonObject());
                    if (data != null) {
                        nextAliases.put(entry.getKey(), data.setId());
                        if (nextBySet.containsKey(data.setId())) {
                            INCore.LOGGER.warn(
                                    "Skipping duplicate booster '{}' because set '{}' already has a booster. Booster identity is set-scoped.",
                                    entry.getKey(),
                                    data.setId()
                            );
                            return;
                        }

                        CardBoosterData canonicalData = new CardBoosterData(
                                data.setId(),
                                data.name(),
                                data.setId(),
                                data.cardsPerPack(),
                                data.foilChance()
                        );
                        nextBySet.put(data.setId(), canonicalData);
                    } else {
                        INCore.LOGGER.warn("Skipping invalid booster '{}'", entry.getKey());
                    }
                });

        boostersBySet = Map.copyOf(nextBySet);
        boosterAliases = Map.copyOf(nextAliases);
        INCore.LOGGER.info("Loaded {} booster definitions (set-scoped).", boostersBySet.size());
    }

    public static CardBoosterData get(ResourceLocation setOrBoosterId) {
        ResourceLocation setId = resolveSetId(setOrBoosterId);
        if (setId == null) {
            return null;
        }
        return boostersBySet.get(setId);
    }

    public static List<CardBoosterData> all() {
        return boostersBySet.values().stream().toList();
    }

    public static ResourceLocation getDefaultSetId() {
        return boostersBySet.keySet().stream().findFirst().orElse(ResourceLocation.parse("incore:base_protocol"));
    }

    public static ResourceLocation resolveSetId(ResourceLocation setOrBoosterId) {
        if (boostersBySet.containsKey(setOrBoosterId)) {
            return setOrBoosterId;
        }
        return boosterAliases.get(setOrBoosterId);
    }
}
