package ozokuz.incore.features.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class ShopTabDefinitionTest {
    @Test
    void shopTabIdParsesAllSixValues() {
        assertEquals(ShopTabId.INDUSTRIAL_MARKET, ShopTabId.fromString("industrial_market"));
        assertEquals(ShopTabId.COMMODITY_EXCHANGE, ShopTabId.fromString("commodity_exchange"));
        assertEquals(ShopTabId.LUXURY_BOUTIQUE, ShopTabId.fromString("luxury_boutique"));
        assertEquals(ShopTabId.ARCADE_VENDOR, ShopTabId.fromString("arcade_vendor"));
        assertEquals(ShopTabId.ARCHIVE_EDITORIAL, ShopTabId.fromString("archive_editorial"));
        assertEquals(ShopTabId.ABYSSAL_TERMINAL, ShopTabId.fromString("abyssal_terminal"));
    }

    @Test
    void fromJsonParsesTabDefinitionAndShowcase() {
        JsonObject json = new JsonObject();
        json.addProperty("display_name", "Archive Editorial");
        json.addProperty("palette", "blood_protocol");
        json.addProperty("layout", "archive_editorial");
        json.addProperty("category_navigation", "sidebar");
        json.addProperty("details_mode", "modal");

        JsonArray categories = new JsonArray();
        categories.add("incore:archive_artifacts");
        categories.add("incore:expedition_cache");
        json.add("categories", categories);

        JsonObject showcase = new JsonObject();
        showcase.addProperty("enabled", true);
        showcase.addProperty("slots", 1);
        showcase.addProperty("source", "category_pinned");
        JsonArray scope = new JsonArray();
        scope.add("incore:archive_artifacts");
        showcase.add("category_scope", scope);
        json.add("showcase", showcase);

        ShopTabDefinition definition = ShopTabDefinition.fromJson(
                ShopTabId.ARCHIVE_EDITORIAL,
                json,
                categoryId -> Set.of(
                        ResourceLocation.parse("incore:archive_artifacts"),
                        ResourceLocation.parse("incore:expedition_cache")
                ).contains(categoryId)
        );

        assertEquals(ShopPaletteId.BLOOD_PROTOCOL, definition.paletteId());
        assertEquals(ShopLayoutId.ARCHIVE_EDITORIAL, definition.layoutId());
        assertEquals(ShopCategoryNavigationMode.SIDEBAR, definition.categoryNavigationMode());
        assertEquals(ShopDetailsPresentationMode.MODAL_OVERLAY, definition.detailsMode());
        assertEquals(
                Set.of(ResourceLocation.parse("incore:archive_artifacts"), ResourceLocation.parse("incore:expedition_cache")),
                Set.copyOf(definition.categoryIds())
        );
        assertEquals(ShopShowcaseSource.CATEGORY_PINNED, definition.showcase().source());
    }

    @Test
    void fromJsonRejectsUnknownCategory() {
        JsonObject json = new JsonObject();
        json.addProperty("display_name", "Industrial Market");
        json.addProperty("palette", "tactical_archive");
        json.addProperty("layout", "industrial_market");
        json.addProperty("category_navigation", "sidebar");
        json.addProperty("details_mode", "inline");
        JsonArray categories = new JsonArray();
        categories.add("incore:missing");
        json.add("categories", categories);

        assertThrows(
                IllegalArgumentException.class,
                () -> ShopTabDefinition.fromJson(ShopTabId.INDUSTRIAL_MARKET, json, categoryId -> false)
        );
    }
}
