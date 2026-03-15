package ozokuz.incore.features.cards;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public record CardModuleData(
        ResourceLocation id,
        String name,
        ResourceLocation setId,
        int rarity,
        int deckPoints,
        CardModuleType moduleType,
        List<String> tags,
        List<CardAttributeEffect> effects,
        List<CardAttributeEffect> downsides,
        int corruptedIntegrityCost,
        double chaoticMin,
        double chaoticMax
) {
    public static @Nullable CardModuleData fromJson(ResourceLocation id, JsonObject json) {
        if (!json.has("name") || !json.has("set")) {
            return null;
        }

        String name = GsonHelper.getAsString(json, "name");
        ResourceLocation setId = ResourceLocation.tryParse(GsonHelper.getAsString(json, "set", ""));
        if (setId == null) {
            return null;
        }

        int rarity = Math.clamp(GsonHelper.getAsInt(json, "rarity", 1), 1, 6);
        int deckPoints = Math.max(1, GsonHelper.getAsInt(json, "deck_points", 1));
        CardModuleType moduleType = CardModuleType.fromString(GsonHelper.getAsString(json, "module_type", "regular"));

        List<String> tags = new ArrayList<>();
        if (json.has("tags")) {
            for (JsonElement element : GsonHelper.getAsJsonArray(json, "tags")) {
                String tag = element.getAsString().trim().toLowerCase();
                if (!tag.isEmpty()) {
                    tags.add(tag);
                }
            }
        }

        List<CardAttributeEffect> effects = parseEffects(json, "effects");
        List<CardAttributeEffect> downsides = parseEffects(json, "downsides");

        int corruptedIntegrityCost = Math.max(1, GsonHelper.getAsInt(json, "corrupted_integrity_cost", 1));
        double chaoticMin = GsonHelper.getAsDouble(json, "chaotic_multiplier_min", 0.7D);
        double chaoticMax = GsonHelper.getAsDouble(json, "chaotic_multiplier_max", 1.3D);
        if (chaoticMax < chaoticMin) {
            double swap = chaoticMin;
            chaoticMin = chaoticMax;
            chaoticMax = swap;
        }

        return new CardModuleData(
                id,
                name,
                setId,
                rarity,
                deckPoints,
                moduleType,
                List.copyOf(tags),
                List.copyOf(effects),
                List.copyOf(downsides),
                corruptedIntegrityCost,
                chaoticMin,
                chaoticMax
        );
    }

    private static List<CardAttributeEffect> parseEffects(JsonObject json, String key) {
        List<CardAttributeEffect> effects = new ArrayList<>();
        if (!json.has(key)) {
            return effects;
        }

        for (JsonElement element : GsonHelper.getAsJsonArray(json, key)) {
            if (!element.isJsonObject()) {
                continue;
            }
            CardAttributeEffect effect = CardAttributeEffect.fromJson(element.getAsJsonObject());
            if (effect != null && effect.amount() != 0.0D) {
                effects.add(effect);
            }
        }

        return effects;
    }
}
