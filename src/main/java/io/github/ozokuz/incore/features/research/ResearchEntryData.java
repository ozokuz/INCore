package io.github.ozokuz.incore.features.research;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record ResearchEntryData(
        ResourceLocation id,
        String title,
        String description,
        int cost,
        ResourceLocation iconItem,
        int runDurationTicks,
        List<String> unlocks,
        List<ResearchMaterialData> researchMaterials,
        List<ResourceLocation> prerequisites,
        List<ResourceLocation> requiredTasks
) {
    public static ResearchEntryData fromJson(ResourceLocation id, JsonObject json) {
        String title = json.has("title") ? json.get("title").getAsString() : id.toString();
        String description = json.has("description") ? json.get("description").getAsString() : "";
        int cost = json.has("cost") ? Math.max(0, json.get("cost").getAsInt()) : 0;
        ResourceLocation iconItem = json.has("icon_item") ? ResourceLocation.tryParse(json.get("icon_item").getAsString()) : null;
        int runDurationTicks = json.has("run_duration_ticks") ? Math.max(20, json.get("run_duration_ticks").getAsInt()) : 200;
        List<String> unlocks = readStringList(json.getAsJsonArray("unlocks"));

        List<ResearchMaterialData> researchMaterials = readMaterialList(json.getAsJsonArray("research_materials"));
        List<ResourceLocation> prerequisites = readIdList(json.getAsJsonArray("prerequisites"));
        List<ResourceLocation> requiredTasks = readIdList(json.getAsJsonArray("required_tasks"));
        return new ResearchEntryData(
                id,
                title,
                description,
                cost,
                iconItem,
                runDurationTicks,
                List.copyOf(unlocks),
                List.copyOf(researchMaterials),
                List.copyOf(prerequisites),
                List.copyOf(requiredTasks)
        );
    }

    private static List<ResourceLocation> readIdList(JsonArray array) {
        if (array == null) {
            return List.of();
        }

        List<ResourceLocation> ids = new ArrayList<>();
        for (JsonElement element : array) {
            ResourceLocation parsed = ResourceLocation.tryParse(element.getAsString());
            if (parsed != null) {
                ids.add(parsed);
            }
        }
        return ids;
    }

    private static List<ResearchMaterialData> readMaterialList(JsonArray array) {
        if (array == null) {
            return List.of();
        }

        List<ResearchMaterialData> materials = new ArrayList<>();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                continue;
            }
            ResearchMaterialData material = ResearchMaterialData.fromJson(element.getAsJsonObject());
            if (material.itemId() != null) {
                materials.add(material);
            }
        }
        return materials;
    }

    private static List<String> readStringList(JsonArray array) {
        if (array == null) {
            return List.of();
        }

        List<String> values = new ArrayList<>();
        for (JsonElement element : array) {
            values.add(element.getAsString());
        }
        return values;
    }
}
