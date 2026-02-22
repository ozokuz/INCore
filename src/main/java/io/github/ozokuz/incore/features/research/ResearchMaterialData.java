package io.github.ozokuz.incore.features.research;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

public record ResearchMaterialData(ResourceLocation materialId, ResourceLocation itemId, int itemCount, int color) {
    public static ResearchMaterialData fromJson(JsonObject json) {
        ResourceLocation materialId = json.has("material") ? ResourceLocation.tryParse(json.get("material").getAsString()) : null;
        ResourceLocation itemId = json.has("item") ? ResourceLocation.tryParse(json.get("item").getAsString()) : null;
        int itemCount = json.has("count") ? Math.max(1, json.get("count").getAsInt()) : 1;
        int color = parseColor(json);

        if (materialId != null) {
            ResearchMaterialDefinition definition = ResearchMaterialManager.get(materialId);
            if (definition != null) {
                if (itemId == null) {
                    itemId = definition.itemId();
                }
                if (!json.has("color")) {
                    color = definition.color();
                }
            }
        }

        return new ResearchMaterialData(materialId, itemId, itemCount, color);
    }

    private static int parseColor(JsonObject json) {
        if (!json.has("color")) {
            return 0xFFFFFFFF;
        }
        try {
            String value = json.get("color").getAsString().trim();
            if (value.startsWith("#")) {
                value = value.substring(1);
            }
            int parsed = (int) Long.parseLong(value, 16);
            if (value.length() <= 6) {
                parsed |= 0xFF000000;
            }
            return parsed;
        } catch (Throwable ignored) {
            return 0xFFFFFFFF;
        }
    }
}
