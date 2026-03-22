package ozokuz.incore.features.shop;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public record ShopOfferDefinition(
        ResourceLocation id,
        ResourceLocation categoryId,
        String displayName,
        int price,
        @Nullable ShopCurrencySpec currencyOverride,
        ShopPurchaseableDefinition purchaseable
) {
    public static ShopOfferDefinition fromJson(ResourceLocation id, JsonObject object) {
        String type = GsonHelper.getAsString(object, "type");
        ResourceLocation categoryId = ResourceLocation.parse(GsonHelper.getAsString(object, "category"));
        String displayName = GsonHelper.getAsString(object, "display_name", id.toString());
        int price = Math.max(1, GsonHelper.getAsInt(object, "price", 1));

        ShopCurrencySpec currencyOverride = null;
        if (object.has("currency")) {
            ShopCurrencyRegistry.ParsedCurrency parsedCurrency = ShopCurrencyRegistry.parse(GsonHelper.getAsJsonObject(object, "currency"));
            if (parsedCurrency == null) {
                throw new IllegalArgumentException("Invalid currency override for offer " + id);
            }
            currencyOverride = parsedCurrency.spec();
        }

        ShopPurchaseableDefinition purchaseable = switch (type) {
            case "single_item" -> new ShopSingleItemPurchaseableDefinition(
                    GsonHelper.getAsString(GsonHelper.getAsJsonObject(object, "reward"), "stack"),
                    Math.max(1, GsonHelper.getAsInt(GsonHelper.getAsJsonObject(object, "reward"), "count", 1))
            );
            case "bundle" -> new ShopBundlePurchaseableDefinition(parseBundleRewards(id, object));
            default -> throw new IllegalArgumentException("Unknown shop purchaseable type '" + type + "' for " + id);
        };

        return new ShopOfferDefinition(id, categoryId, displayName, price, currencyOverride, purchaseable);
    }

    private static List<ShopRewardStackDefinition> parseBundleRewards(ResourceLocation id, JsonObject object) {
        List<ShopRewardStackDefinition> rewards = new ArrayList<>();
        for (var element : GsonHelper.getAsJsonArray(object, "reward")) {
            if (!element.isJsonObject()) {
                throw new IllegalArgumentException("Non-object reward entry for " + id);
            }
            JsonObject reward = element.getAsJsonObject();
            rewards.add(new ShopRewardStackDefinition(
                    GsonHelper.getAsString(reward, "stack"),
                    Math.max(1, GsonHelper.getAsInt(reward, "count", 1))
            ));
        }
        if (rewards.isEmpty()) {
            throw new IllegalArgumentException("Empty bundle reward list for " + id);
        }
        return rewards;
    }
}
