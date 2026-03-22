package ozokuz.incore.features.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class ShopOfferDefinitionTest {
    @Test
    void fromJsonParsesSingleItemOffer() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "single_item");
        json.addProperty("category", "incore:basic_supplies");
        json.addProperty("display_name", "Field Rations");
        json.addProperty("price", 18);

        JsonObject reward = new JsonObject();
        reward.addProperty("stack", "minecraft:bread");
        reward.addProperty("count", 8);
        json.add("reward", reward);

        ShopOfferDefinition definition = ShopOfferDefinition.fromJson(ResourceLocation.parse("incore:basic_bread"), json);

        assertEquals(18, definition.price());
        ShopSingleItemPurchaseableDefinition purchaseable = assertInstanceOf(
                ShopSingleItemPurchaseableDefinition.class,
                definition.purchaseable()
        );
        assertEquals("minecraft:bread", purchaseable.stackSpec());
        assertEquals(8, purchaseable.count());
    }

    @Test
    void fromJsonParsesBundleOffer() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "bundle");
        json.addProperty("category", "incore:basic_supplies");
        json.addProperty("display_name", "Field Pack");
        json.addProperty("price", 75);

        JsonArray reward = new JsonArray();
        JsonObject bread = new JsonObject();
        bread.addProperty("stack", "minecraft:bread");
        bread.addProperty("count", 8);
        reward.add(bread);
        JsonObject torch = new JsonObject();
        torch.addProperty("stack", "minecraft:torch");
        torch.addProperty("count", 16);
        reward.add(torch);
        json.add("reward", reward);

        ShopOfferDefinition definition = ShopOfferDefinition.fromJson(ResourceLocation.parse("incore:field_pack"), json);

        ShopBundlePurchaseableDefinition purchaseable = assertInstanceOf(
                ShopBundlePurchaseableDefinition.class,
                definition.purchaseable()
        );
        assertEquals(2, purchaseable.items().size());
        assertEquals("minecraft:torch", purchaseable.items().get(1).stackSpec());
    }

    @Test
    void fromJsonRejectsEmptyBundleReward() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "bundle");
        json.addProperty("category", "incore:basic_supplies");
        json.addProperty("display_name", "Empty Bundle");
        json.addProperty("price", 10);
        json.add("reward", new JsonArray());

        assertThrows(
                IllegalArgumentException.class,
                () -> ShopOfferDefinition.fromJson(ResourceLocation.parse("incore:empty_bundle"), json)
        );
    }
}
