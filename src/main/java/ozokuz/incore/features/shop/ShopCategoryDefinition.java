package ozokuz.incore.features.shop;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Nullable;

public record ShopCategoryDefinition(
        ResourceLocation id,
        String displayName,
        ShopTabId tab,
        int sortOrder,
        boolean enabled,
        ShopStockMode stockMode,
        int initialStock,
        ShopReplenishMode replenishMode,
        ShopCurrencySpec defaultCurrency,
        ShopOfferSortMode offerSortMode,
        @Nullable ShopCategoryRotationDefinition rotation
) {
    public static ShopCategoryDefinition fromJson(ResourceLocation id, JsonObject object) {
        String displayName = GsonHelper.getAsString(object, "display_name", id.toString());
        ShopTabId tab = ShopTabId.fromString(GsonHelper.getAsString(object, "tab", ShopTabId.SUPPLIES.serialized()));
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

        ShopCurrencyRegistry.ParsedCurrency parsedCurrency = ShopCurrencyRegistry.parse(GsonHelper.getAsJsonObject(object, "currency"));
        if (parsedCurrency == null) {
            throw new IllegalArgumentException("Missing or invalid shop category currency for " + id);
        }

        ShopOfferSortMode offerSortMode = ShopOfferSortMode.fromString(GsonHelper.getAsString(object, "offer_sort", ShopOfferSortMode.ID.serialized()));
        ShopCategoryRotationDefinition rotation = null;
        if (object.has("rotation")) {
            rotation = ShopCategoryRotationDefinition.fromJson(GsonHelper.getAsJsonObject(object, "rotation"));
        }

        if (replenishMode == ShopReplenishMode.SHOP_ROTATION && rotation == null) {
            throw new IllegalArgumentException("Shop category " + id + " uses shop_rotation without rotation config");
        }
        if (offerSortMode == ShopOfferSortMode.ROTATION_TIME_REMAINING && rotation == null) {
            throw new IllegalArgumentException("Shop category " + id + " uses rotation_time_remaining without rotation config");
        }

        return new ShopCategoryDefinition(
                id,
                displayName,
                tab,
                sortOrder,
                enabled,
                stockMode,
                initialStock,
                replenishMode,
                parsedCurrency.spec(),
                offerSortMode,
                rotation
        );
    }
}
