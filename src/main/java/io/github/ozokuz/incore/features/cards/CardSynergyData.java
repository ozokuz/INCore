package io.github.ozokuz.incore.features.cards;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public record CardSynergyData(
        ResourceLocation id,
        String name,
        List<String> requiredTags,
        Map<CardModuleType, Integer> requiredTypeCounts,
        int minCards,
        List<CardAttributeEffect> effects
) {
    public static @Nullable CardSynergyData fromJson(ResourceLocation id, JsonObject json) {
        if (!json.has("name")) {
            return null;
        }

        List<String> requiredTags = new ArrayList<>();
        if (json.has("required_tags")) {
            for (JsonElement element : GsonHelper.getAsJsonArray(json, "required_tags")) {
                String value = element.getAsString().trim().toLowerCase();
                if (!value.isEmpty()) {
                    requiredTags.add(value);
                }
            }
        }

        Map<CardModuleType, Integer> requiredTypeCounts = new EnumMap<>(CardModuleType.class);
        if (json.has("required_type_counts") && json.get("required_type_counts").isJsonObject()) {
            JsonObject counts = json.getAsJsonObject("required_type_counts");
            for (String key : counts.keySet()) {
                CardModuleType type = CardModuleType.fromString(key);
                requiredTypeCounts.put(type, Math.max(1, counts.get(key).getAsInt()));
            }
        }

        List<CardAttributeEffect> effects = new ArrayList<>();
        if (json.has("effects")) {
            for (JsonElement element : GsonHelper.getAsJsonArray(json, "effects")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                CardAttributeEffect effect = CardAttributeEffect.fromJson(element.getAsJsonObject());
                if (effect != null) {
                    effects.add(effect);
                }
            }
        }

        if (effects.isEmpty()) {
            return null;
        }

        return new CardSynergyData(
                id,
                GsonHelper.getAsString(json, "name"),
                List.copyOf(requiredTags),
                Map.copyOf(requiredTypeCounts),
                Math.max(1, GsonHelper.getAsInt(json, "min_cards", 1)),
                List.copyOf(effects)
        );
    }
}
