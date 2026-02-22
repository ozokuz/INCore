package io.github.ozokuz.incore.features.shop;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Nullable;

public record ShopCategoryDefinition(
        ResourceLocation id,
        String displayName,
        int sortOrder,
        boolean enabled,
        ShopStockMode stockMode,
        int initialStock,
        ShopReplenishMode replenishMode,
        @Nullable ResourceLocation gachaCategoryId
) {
    public static ShopCategoryDefinition fromJson(ResourceLocation id, JsonObject object) {
        String displayName = GsonHelper.getAsString(object, "display_name", id.toString());
        int sortOrder = GsonHelper.getAsInt(object, "sort_order", 0);
        boolean enabled = GsonHelper.getAsBoolean(object, "enabled", true);

        ShopStockMode stockMode = ShopStockMode.fromString(GsonHelper.getAsString(object, "stock_mode", ShopStockMode.NONE.serialized()));
        int initialStock = Math.max(1, GsonHelper.getAsInt(object, "initial_stock", 1));
        if (stockMode == ShopStockMode.NONE) {
            initialStock = 0;
        }

        ShopReplenishMode replenishMode = ShopReplenishMode.fromString(
                GsonHelper.getAsString(object, "replenish_mode", ShopReplenishMode.NONE.serialized())
        );

        ResourceLocation gachaCategoryId = null;
        if (object.has("gacha_category")) {
            String raw = GsonHelper.getAsString(object, "gacha_category");
            gachaCategoryId = ResourceLocation.tryParse(raw);
        }

        return new ShopCategoryDefinition(
                id,
                displayName,
                sortOrder,
                enabled,
                stockMode,
                initialStock,
                replenishMode,
                gachaCategoryId
        );
    }
}
