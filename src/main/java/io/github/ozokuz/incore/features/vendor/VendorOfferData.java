package io.github.ozokuz.incore.features.vendor;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Nullable;

public record VendorOfferData(
        ResourceLocation id,
        String name,
        ResourceLocation category,
        VendorProductType productType,
        VendorProductSpec productSpec,
        VendorCurrencyType currencyType,
        VendorCurrencySpec currencySpec,
        int weight,
        int stockMin,
        int stockMax
) {
    public static @Nullable VendorOfferData fromJson(ResourceLocation id, JsonObject json) {
        if (!json.has("name") || !json.has("category") || !json.has("product") || !json.has("currency")) {
            return null;
        }

        ResourceLocation categoryId = ResourceLocation.tryParse(GsonHelper.getAsString(json, "category", ""));
        if (categoryId == null) {
            return null;
        }

        JsonObject productJson = GsonHelper.getAsJsonObject(json, "product");
        JsonObject currencyJson = GsonHelper.getAsJsonObject(json, "currency");

        VendorProductRegistry.ParsedProduct parsedProduct = VendorProductRegistry.parse(productJson);
        VendorCurrencyRegistry.ParsedCurrency parsedCurrency = VendorCurrencyRegistry.parse(currencyJson);
        if (parsedProduct == null || parsedCurrency == null) {
            return null;
        }

        int stockMin = Math.max(0, GsonHelper.getAsInt(json, "stock_min", 5));
        int stockMax = Math.max(stockMin, GsonHelper.getAsInt(json, "stock_max", 10));

        return new VendorOfferData(
                id,
                GsonHelper.getAsString(json, "name"),
                categoryId,
                parsedProduct.type(),
                parsedProduct.spec(),
                parsedCurrency.type(),
                parsedCurrency.spec(),
                Math.max(1, GsonHelper.getAsInt(json, "weight", 1)),
                stockMin,
                stockMax
        );
    }
}
