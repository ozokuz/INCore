package io.github.ozokuz.incore.features.gacha;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public record GachaEventCategoryData(
        ResourceLocation id,
        int durationHours,
        List<ResourceLocation> bannerOrder
) {
    @Nullable
    public static GachaEventCategoryData fromJson(ResourceLocation id, JsonObject json) {
        if (!json.has("duration_hours") || !json.has("banners")) {
            return null;
        }

        int durationHours = Math.max(1, json.get("duration_hours").getAsInt());
        List<ResourceLocation> banners = new ArrayList<>();
        for (JsonElement element : json.getAsJsonArray("banners")) {
            ResourceLocation bannerId = ResourceLocation.tryParse(element.getAsString());
            if (bannerId != null && !banners.contains(bannerId)) {
                banners.add(bannerId);
            }
        }

        if (banners.isEmpty()) {
            return null;
        }

        return new GachaEventCategoryData(id, durationHours, List.copyOf(banners));
    }
}
