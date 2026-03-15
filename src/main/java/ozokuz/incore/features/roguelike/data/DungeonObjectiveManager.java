package ozokuz.incore.features.roguelike.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import ozokuz.incore.INCore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
                ResourceLocation resolvedId = DungeonObjectiveIds.resolve(id);
                OBJECTIVES.put(resolvedId, DungeonObjectiveData.fromJson(json.getAsJsonObject()));
            } catch (Exception e) {
                INCore.LOGGER.warn("Failed to load roguelike objective {}", id, e);
            }
        });

        for (Map.Entry<ResourceLocation, ResourceLocation> aliasEntry : DungeonObjectiveIds.legacyToCanonical().entrySet()) {
            DungeonObjectiveData canonical = OBJECTIVES.get(aliasEntry.getValue());
            if (canonical != null) {
                OBJECTIVES.put(aliasEntry.getKey(), canonical);
            }
        }

        long canonicalCount = OBJECTIVES.entrySet().stream()
                .filter(entry -> DungeonObjectiveIds.resolve(entry.getKey()).equals(entry.getKey()))
                .count();
        INCore.LOGGER.info("Loaded {} roguelike dungeon objectives", canonicalCount);
    }

    public static Optional<PickedObjective> pickRandom(RandomSource random) {
        Map<ResourceLocation, DungeonObjectiveData> canonicalObjectives = OBJECTIVES.entrySet().stream()
                .filter(entry -> DungeonObjectiveIds.isCanonical(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left, LinkedHashMap::new));
        if (canonicalObjectives.isEmpty()) {
            return Optional.empty();
        }

        int totalWeight = canonicalObjectives.values().stream().mapToInt(DungeonObjectiveData::weight).sum();
        if (totalWeight <= 0) {
            return Optional.empty();
        }

        int roll = random.nextInt(totalWeight);
        for (Map.Entry<ResourceLocation, DungeonObjectiveData> entry : canonicalObjectives.entrySet()) {
            roll -= entry.getValue().weight();
            if (roll < 0) {
                return Optional.of(new PickedObjective(entry.getKey(), entry.getValue()));
            }
        }

        Map.Entry<ResourceLocation, DungeonObjectiveData> fallback = canonicalObjectives.entrySet().iterator().next();
        return Optional.of(new PickedObjective(fallback.getKey(), fallback.getValue()));
    }

    public static ResourceLocation resolveObjectiveId(ResourceLocation id) {
        return DungeonObjectiveIds.resolve(id);
    }

    public static DungeonObjectiveData getObjective(ResourceLocation id) {
        return OBJECTIVES.get(resolveObjectiveId(id));
    }

    public record PickedObjective(ResourceLocation id, DungeonObjectiveData data) {
    }
}
