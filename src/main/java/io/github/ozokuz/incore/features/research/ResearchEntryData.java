package io.github.ozokuz.incore.features.research;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record ResearchEntryData(ResourceLocation id, String title, String description, int cost, List<ResourceLocation> prerequisites, List<ResourceLocation> requiredTasks) {
    public static ResearchEntryData fromJson(ResourceLocation id, JsonObject json) {
        String title = json.has("title") ? json.get("title").getAsString() : id.toString();
        String description = json.has("description") ? json.get("description").getAsString() : "";
        int cost = json.has("cost") ? Math.max(0, json.get("cost").getAsInt()) : 0;

        List<ResourceLocation> prerequisites = readIdList(json.getAsJsonArray("prerequisites"));
        List<ResourceLocation> requiredTasks = readIdList(json.getAsJsonArray("required_tasks"));
        return new ResearchEntryData(id, title, description, cost, List.copyOf(prerequisites), List.copyOf(requiredTasks));
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
}
