package io.github.ozokuz.incore.features.research;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.github.ozokuz.incore.INCore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

public class ResearchEntryManager extends SimpleJsonResourceReloadListener {
    private static volatile Map<ResourceLocation, ResearchEntryData> entries = Map.of();

    public ResearchEntryManager() {
        super(new Gson(), "research_entries");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        Map<ResourceLocation, ResearchEntryData> next = new LinkedHashMap<>();
        jsons.forEach((id, json) -> {
            if (json.isJsonObject()) {
                next.put(id, ResearchEntryData.fromJson(id, json.getAsJsonObject()));
            }
        });
        entries = Map.copyOf(next);
        INCore.LOGGER.info("Loaded {} research entries.", entries.size());
    }

    public static Map<ResourceLocation, ResearchEntryData> all() {
        return entries;
    }
}
