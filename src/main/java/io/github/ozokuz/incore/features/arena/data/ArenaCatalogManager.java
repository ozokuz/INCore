package io.github.ozokuz.incore.features.arena.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.github.ozokuz.incore.INCore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ArenaCatalogManager extends SimpleJsonResourceReloadListener {
    private static volatile Map<ResourceLocation, ArenaCatalogEntry> entries = Map.of();

    public ArenaCatalogManager() {
        super(new Gson(), "arena/catalog");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        Map<ResourceLocation, ArenaCatalogEntry> next = new LinkedHashMap<>();
        jsons.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> {
                    if (!entry.getValue().isJsonObject()) {
                        return;
                    }

                    ArenaCatalogEntry parsed = ArenaCatalogEntry.fromJson(entry.getKey(), entry.getValue().getAsJsonObject());
                    if (parsed == null) {
                        INCore.LOGGER.warn("Skipping invalid arena catalog entry {}", entry.getKey());
                        return;
                    }

                    next.put(entry.getKey(), parsed);
                });

        entries = Map.copyOf(next);
        INCore.LOGGER.info("Loaded {} arena catalog entries", entries.size());
    }

    public static List<ArenaCatalogEntry> all() {
        return entries.values().stream()
                .sorted(Comparator.comparingInt(ArenaCatalogEntry::sortOrder)
                        .thenComparing(ArenaCatalogEntry::categoryName)
                        .thenComparing(ArenaCatalogEntry::difficultyName)
                        .thenComparing(entry -> entry.id().toString()))
                .toList();
    }

    @Nullable
    public static ArenaCatalogEntry get(ResourceLocation id) {
        return entries.get(id);
    }
}
