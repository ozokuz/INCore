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

public class ManualResearchTaskManager extends SimpleJsonResourceReloadListener {
    private static volatile Map<ResourceLocation, ManualResearchTaskData> tasks = Map.of();

    public ManualResearchTaskManager() {
        super(new Gson(), "manual_research_tasks");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        Map<ResourceLocation, ManualResearchTaskData> next = new LinkedHashMap<>();
        jsons.forEach((id, json) -> {
            if (json.isJsonObject()) {
                next.put(id, ManualResearchTaskData.fromJson(id, json.getAsJsonObject()));
            }
        });
        tasks = Map.copyOf(next);
        INCore.LOGGER.info("Loaded {} manual research tasks.", tasks.size());
    }

    public static Map<ResourceLocation, ManualResearchTaskData> all() {
        return tasks;
    }
}
