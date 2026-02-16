package io.github.ozokuz.incore.features.research;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

public record ResearchMaterialData(ResourceLocation itemId, int itemCount) {
    public static ResearchMaterialData fromJson(JsonObject json) {
        ResourceLocation itemId = json.has("item") ? ResourceLocation.tryParse(json.get("item").getAsString()) : null;
        int itemCount = json.has("count") ? Math.max(1, json.get("count").getAsInt()) : 1;
        return new ResearchMaterialData(itemId, itemCount);
    }
}
