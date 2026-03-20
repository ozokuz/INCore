package ozokuz.incore.features.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class ShopCategoryDefinitionTest {
    @Test
    void fromJsonParsesTabMetadata() {
        JsonObject json = new JsonObject();
        json.addProperty("display_name", "Daily Exchange");
        json.addProperty("tab", "rotations");
        json.addProperty("stock_mode", "per_item");
        json.addProperty("initial_stock", 12);
        json.addProperty("replenish_mode", "daily_noon");

        ShopCategoryDefinition definition = ShopCategoryDefinition.fromJson(ResourceLocation.parse("incore:daily_exchange"), json);

        assertEquals(ShopTabId.ROTATIONS, definition.tab());
    }
}
