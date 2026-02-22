package io.github.ozokuz.incore.features.shop;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

public record ShopOfferDefinition(
        ResourceLocation id,
        ResourceLocation categoryId,
        ResourceLocation itemId,
        String displayName,
        int sortOrder,
        boolean enabled,
        int priceSpur,
        int itemCount
) {
    public static ShopOfferDefinition fromJson(ResourceLocation id, JsonObject object) {
        ResourceLocation categoryId = ResourceLocation.parse(GsonHelper.getAsString(object, "category"));
        ResourceLocation itemId = ResourceLocation.parse(GsonHelper.getAsString(object, "item"));
        String displayName = GsonHelper.getAsString(object, "display_name", itemId.toString());
        int sortOrder = GsonHelper.getAsInt(object, "sort_order", 0);
        boolean enabled = GsonHelper.getAsBoolean(object, "enabled", true);
        int priceSpur = Math.max(1, GsonHelper.getAsInt(object, "price_spur", 1));
        int itemCount = Math.max(1, GsonHelper.getAsInt(object, "item_count", 1));

        return new ShopOfferDefinition(id, categoryId, itemId, displayName, sortOrder, enabled, priceSpur, itemCount);
    }
}
