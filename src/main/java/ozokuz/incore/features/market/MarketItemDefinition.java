package ozokuz.incore.features.market;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

public record MarketItemDefinition(
        ResourceLocation id,
        ResourceLocation itemId,
        int basePriceSpur,
        String displayName,
        int sortOrder,
        boolean enabled,
        double volatilityWeight
) {
    public static MarketItemDefinition fromJson(ResourceLocation id, JsonObject object) {
        ResourceLocation itemId = ResourceLocation.parse(GsonHelper.getAsString(object, "item"));
        int basePriceSpur = Math.max(1, GsonHelper.getAsInt(object, "base_price_spur", 1));
        String displayName = GsonHelper.getAsString(object, "display_name", itemId.toString());
        int sortOrder = GsonHelper.getAsInt(object, "sort_order", 0);
        boolean enabled = GsonHelper.getAsBoolean(object, "enabled", true);
        double volatilityWeight = Math.max(0.01D, GsonHelper.getAsDouble(object, "volatility_weight", 1.0D));
        return new MarketItemDefinition(id, itemId, basePriceSpur, displayName, sortOrder, enabled, volatilityWeight);
    }
}
