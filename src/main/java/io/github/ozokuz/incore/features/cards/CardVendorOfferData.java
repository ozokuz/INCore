package io.github.ozokuz.incore.features.cards;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Nullable;

public record CardVendorOfferData(
        ResourceLocation id,
        String name,
        ProductType productType,
        ResourceLocation productId,
        int count,
        int tokenCost,
        int weight,
        int stockMin,
        int stockMax
) {
    public enum ProductType {
        BOOSTER,
        BOOSTER_BOX
    }

    public static @Nullable CardVendorOfferData fromJson(ResourceLocation id, JsonObject json) {
        if (!json.has("name") || !json.has("product_type") || !json.has("product_id")) {
            return null;
        }

        ProductType productType = switch (GsonHelper.getAsString(json, "product_type").toLowerCase()) {
            case "booster_box", "box" -> ProductType.BOOSTER_BOX;
            default -> ProductType.BOOSTER;
        };

        ResourceLocation productId = ResourceLocation.tryParse(GsonHelper.getAsString(json, "product_id", ""));
        if (productId == null) {
            return null;
        }

        int stockMin = Math.max(0, GsonHelper.getAsInt(json, "stock_min", 5));
        int stockMax = Math.max(stockMin, GsonHelper.getAsInt(json, "stock_max", 10));

        return new CardVendorOfferData(
                id,
                GsonHelper.getAsString(json, "name"),
                productType,
                productId,
                Math.max(1, GsonHelper.getAsInt(json, "count", 1)),
                Math.max(0, GsonHelper.getAsInt(json, "token_cost", 0)),
                Math.max(1, GsonHelper.getAsInt(json, "weight", 1)),
                stockMin,
                stockMax
        );
    }
}
