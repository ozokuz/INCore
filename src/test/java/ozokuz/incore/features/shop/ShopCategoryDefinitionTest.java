package ozokuz.incore.features.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class ShopCategoryDefinitionTest {
    @Test
    void fromJsonParsesCurrencySortAndRotationMetadata() {
        JsonObject json = new JsonObject();
        json.addProperty("display_name", "Daily Exchange");
        json.addProperty("stock_mode", "per_item");
        json.addProperty("initial_stock", 12);
        json.addProperty("replenish_mode", "shop_rotation");
        json.addProperty("offer_sort", "rotation_time_remaining");

        JsonObject currency = new JsonObject();
        currency.addProperty("type", "incore:item");
        currency.addProperty("item", "minecraft:emerald");
        json.add("currency", currency);

        JsonObject rotation = new JsonObject();
        rotation.addProperty("duration_hours", 24);
        rotation.addProperty("visible_count", 2);
        json.add("rotation", rotation);

        ShopCategoryDefinition definition = ShopCategoryDefinition.fromJson(ResourceLocation.parse("incore:daily_exchange"), json);

        assertEquals(ShopOfferSortMode.ROTATION_TIME_REMAINING, definition.offerSortMode());
        assertInstanceOf(ItemShopCurrencyType.Spec.class, definition.defaultCurrency());
        assertNotNull(definition.rotation());
        assertEquals(24, definition.rotation().durationHours());
        assertEquals(2, definition.rotation().visibleCount());
    }

    @Test
    void fromJsonRejectsRotationSortWithoutRotationConfig() {
        JsonObject json = new JsonObject();
        json.addProperty("display_name", "Basic Supplies");
        json.addProperty("stock_mode", "none");
        json.addProperty("replenish_mode", "none");
        json.addProperty("offer_sort", "rotation_time_remaining");

        JsonObject currency = new JsonObject();
        currency.addProperty("type", "incore:bank_spur");
        json.add("currency", currency);

        assertThrows(
                IllegalArgumentException.class,
                () -> ShopCategoryDefinition.fromJson(ResourceLocation.parse("incore:basic_supplies"), json)
        );
    }
}
