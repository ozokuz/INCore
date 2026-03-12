package io.github.ozokuz.incore.features.assembly.unlock;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.ozokuz.incore.INCore;
import io.github.ozokuz.incore.features.researchv2.registry.ResearchRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

public final class AssemblyRecipeUnlockManager extends SimpleJsonResourceReloadListener {
    private static volatile Map<ResourceLocation, ResourceLocation> recipeToResearchNode = Map.of();

    public AssemblyRecipeUnlockManager() {
        super(new Gson(), "assembly_recipe_unlocks");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        Map<ResourceLocation, ResourceLocation> next = new LinkedHashMap<>();
        jsons.forEach((id, json) -> {
            if (!json.isJsonObject()) {
                return;
            }
            JsonObject root = json.getAsJsonObject();
            if (!root.has("requiredResearchNodeId")) {
                INCore.LOGGER.warn("Ignoring assembly unlock set {} due to missing requiredResearchNodeId", id);
                return;
            }
            ResourceLocation nodeId = ResourceLocation.tryParse(root.get("requiredResearchNodeId").getAsString());
            if (nodeId == null) {
                INCore.LOGGER.warn("Ignoring assembly unlock set {} due to invalid requiredResearchNodeId", id);
                return;
            }
            if (!ResearchRegistry.nodes().containsKey(nodeId)) {
                INCore.LOGGER.warn("Ignoring assembly unlock set {} because research node {} is not registered", id, nodeId);
                return;
            }
            JsonArray recipes = root.getAsJsonArray("recipes");
            if (recipes == null) {
                INCore.LOGGER.warn("Ignoring assembly unlock set {} due to missing recipes array", id);
                return;
            }
            for (JsonElement element : recipes) {
                ResourceLocation recipeId = ResourceLocation.tryParse(element.getAsString());
                if (recipeId == null) {
                    continue;
                }
                if (next.containsKey(recipeId)) {
                    INCore.LOGGER.warn("Duplicate assembly unlock mapping for recipe {} in {}; first mapping wins", recipeId, id);
                    continue;
                }
                next.put(recipeId, nodeId);
            }
        });
        recipeToResearchNode = Map.copyOf(next);
        INCore.LOGGER.info("Loaded {} assembly recipe unlock mapping(s).", recipeToResearchNode.size());
    }

    public static ResourceLocation requiredResearchNode(ResourceLocation recipeId) {
        return recipeId == null ? null : recipeToResearchNode.get(recipeId);
    }

    public static boolean hasUnlock(ResourceLocation recipeId) {
        return recipeToResearchNode.containsKey(recipeId);
    }

    public static Map<ResourceLocation, ResourceLocation> all() {
        return recipeToResearchNode;
    }
}
