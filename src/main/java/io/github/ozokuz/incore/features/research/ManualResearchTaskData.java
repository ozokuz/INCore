package io.github.ozokuz.incore.features.research;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

public record ManualResearchTaskData(ResourceLocation id, String title, String description, ResourceLocation itemId, int itemCount, int rewardPoints, boolean repeatable) {
    public static ManualResearchTaskData fromJson(ResourceLocation id, JsonObject json) {
        String title = json.has("title") ? json.get("title").getAsString() : id.toString();
        String description = json.has("description") ? json.get("description").getAsString() : "";
        ResourceLocation itemId = json.has("item") ? ResourceLocation.tryParse(json.get("item").getAsString()) : null;
        int itemCount = json.has("count") ? Math.max(1, json.get("count").getAsInt()) : 1;
        int rewardPoints = json.has("reward_points") ? Math.max(0, json.get("reward_points").getAsInt()) : 0;
        boolean repeatable = json.has("repeatable") && json.get("repeatable").getAsBoolean();
        return new ManualResearchTaskData(id, title, description, itemId, itemCount, rewardPoints, repeatable);
    }
}
