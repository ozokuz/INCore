package io.github.ozokuz.incore.features.roguelike.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.github.ozokuz.incore.INCore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class DungeonObjectiveManager extends SimpleJsonResourceReloadListener {
    public static final Map<ResourceLocation, DungeonObjectiveData> OBJECTIVES = new LinkedHashMap<>();

    public DungeonObjectiveManager() {
        super(new Gson(), "roguelike/objectives");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        OBJECTIVES.clear();

        jsons.forEach((id, json) -> {
            try {
                OBJECTIVES.put(id, DungeonObjectiveData.fromJson(json.getAsJsonObject()));
            } catch (Exception e) {
                INCore.LOGGER.warn("Failed to load roguelike objective {}", id, e);
            }
        });

        INCore.LOGGER.info("Loaded {} roguelike dungeon objectives", OBJECTIVES.size());
    }

    public static Optional<PickedObjective> pickRandom(RandomSource random) {
        if (OBJECTIVES.isEmpty()) {
            return Optional.empty();
        }

        int totalWeight = OBJECTIVES.values().stream().mapToInt(DungeonObjectiveData::weight).sum();
        if (totalWeight <= 0) {
            return Optional.empty();
        }

        int roll = random.nextInt(totalWeight);
        for (Map.Entry<ResourceLocation, DungeonObjectiveData> entry : OBJECTIVES.entrySet()) {
            roll -= entry.getValue().weight();
            if (roll < 0) {
                return Optional.of(new PickedObjective(entry.getKey(), entry.getValue()));
            }
        }

        Map.Entry<ResourceLocation, DungeonObjectiveData> fallback = OBJECTIVES.entrySet().iterator().next();
        return Optional.of(new PickedObjective(fallback.getKey(), fallback.getValue()));
    }

    public record PickedObjective(ResourceLocation id, DungeonObjectiveData data) {
    }
}
