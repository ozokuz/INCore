package io.github.ozokuz.incore.features.research;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

public record LabProcessData(ResourceLocation id, ResourceLocation itemId, int itemCount, int durationTicks, int rewardPoints) {
    public static LabProcessData fromJson(ResourceLocation id, JsonObject json) {
        ResourceLocation itemId = json.has("item") ? ResourceLocation.tryParse(json.get("item").getAsString()) : null;
        int itemCount = json.has("count") ? Math.max(1, json.get("count").getAsInt()) : 1;
        int durationTicks = json.has("duration_ticks") ? Math.max(20, json.get("duration_ticks").getAsInt()) : 200;
        int rewardPoints = json.has("reward_points") ? Math.max(0, json.get("reward_points").getAsInt()) : 0;
        return new LabProcessData(id, itemId, itemCount, durationTicks, rewardPoints);
    }
}
