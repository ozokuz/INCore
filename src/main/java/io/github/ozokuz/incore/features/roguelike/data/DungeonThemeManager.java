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

public class DungeonThemeManager extends SimpleJsonResourceReloadListener {
    public static final Map<ResourceLocation, DungeonThemeData> THEMES = new LinkedHashMap<>();

    public DungeonThemeManager() {
        super(new Gson(), "roguelike/themes");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        THEMES.clear();

        jsons.forEach((id, json) -> {
            try {
                THEMES.put(id, DungeonThemeData.fromJson(json.getAsJsonObject()));
            } catch (Exception e) {
                INCore.LOGGER.warn("Failed to load roguelike theme {}", id, e);
            }
        });

        INCore.LOGGER.info("Loaded {} roguelike dungeon themes", THEMES.size());
    }

    public static Optional<PickedTheme> pickRandom(RandomSource random) {
        if (THEMES.isEmpty()) {
            return Optional.empty();
        }

        int totalWeight = THEMES.values().stream().mapToInt(DungeonThemeData::weight).sum();
        if (totalWeight <= 0) {
            return Optional.empty();
        }

        int roll = random.nextInt(totalWeight);
        for (Map.Entry<ResourceLocation, DungeonThemeData> entry : THEMES.entrySet()) {
            roll -= entry.getValue().weight();
            if (roll < 0) {
                return Optional.of(new PickedTheme(entry.getKey(), entry.getValue()));
            }
        }

        Map.Entry<ResourceLocation, DungeonThemeData> fallback = THEMES.entrySet().iterator().next();
        return Optional.of(new PickedTheme(fallback.getKey(), fallback.getValue()));
    }

    public record PickedTheme(ResourceLocation id, DungeonThemeData data) {
    }
}
