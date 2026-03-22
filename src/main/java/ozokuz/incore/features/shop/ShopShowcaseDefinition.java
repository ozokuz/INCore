package ozokuz.incore.features.shop;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

public record ShopShowcaseDefinition(
        boolean enabled,
        int slots,
        ShopShowcaseSource source,
        List<ResourceLocation> categoryScope
) {
    public static ShopShowcaseDefinition disabled() {
        return new ShopShowcaseDefinition(false, 0, ShopShowcaseSource.TOP_OF_FEED, List.of());
    }

    public static ShopShowcaseDefinition fromJson(JsonObject object) {
        boolean enabled = GsonHelper.getAsBoolean(object, "enabled", false);
        int slots = enabled ? Math.max(1, GsonHelper.getAsInt(object, "slots", 1)) : 0;
        ShopShowcaseSource source = ShopShowcaseSource.fromString(GsonHelper.getAsString(object, "source", ShopShowcaseSource.TOP_OF_FEED.serialized()));
        List<ResourceLocation> categoryScope = new ArrayList<>();
        if (object.has("category_scope")) {
            for (var entry : GsonHelper.getAsJsonArray(object, "category_scope")) {
                categoryScope.add(ResourceLocation.parse(entry.getAsString()));
            }
        }
        return new ShopShowcaseDefinition(enabled, slots, source, List.copyOf(categoryScope));
    }
}
