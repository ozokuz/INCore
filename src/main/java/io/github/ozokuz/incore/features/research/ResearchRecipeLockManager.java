package io.github.ozokuz.incore.features.research;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.github.ozokuz.incore.INCore;
import io.github.ozokuz.incore.features.research.client.ResearchRecipeLockClientCache;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

public class ResearchRecipeLockManager extends SimpleJsonResourceReloadListener {
    private static volatile Map<ResourceLocation, ResearchRecipeLockSetData> lockSets = Map.of();

    public ResearchRecipeLockManager() {
        super(new Gson(), "research_recipe_locks");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        Map<ResourceLocation, ResearchRecipeLockSetData> next = new LinkedHashMap<>();
        jsons.forEach((id, json) -> {
            if (json.isJsonObject()) {
                next.put(id, ResearchRecipeLockSetData.fromJson(id, json.getAsJsonObject()));
            }
        });
        lockSets = Map.copyOf(next);
        ResearchRecipeLockClientCache.markDataReloaded();
        INCore.LOGGER.info("Loaded {} research recipe lock sets.", lockSets.size());
    }

    public static Map<ResourceLocation, ResearchRecipeLockSetData> all() {
        return lockSets;
    }

    public static ResearchRecipeLockSetData get(ResourceLocation id) {
        return id == null ? null : lockSets.get(id);
    }
}
