package ozokuz.incore.integration.ldlib.ui.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import ozokuz.incore.features.shop.ShopService;
import ozokuz.incore.features.shop.ShopTabId;

class ShopAppUiStateTest {
    @Test
    void reconcileUsesSnapshotTabAndCategoryFallbacks() {
        ShopAppUiState state = new ShopAppUiState();
        state.setVisibleOfferRows(2);

        state.reconcile(screenData());

        assertEquals(ShopTabId.ROTATIONS, state.activeTab());
        assertEquals("incore:daily_exchange", state.selectedCategoryId());
        assertEquals("incore:daily_redstone", state.selectedOfferId());
    }

    @Test
    void openPurchaseAndConsumeEscapeClosesWorkspace() {
        ShopAppUiState state = new ShopAppUiState();
        ShopService.ScreenData data = screenData();
        state.reconcile(data);

        state.openPurchase("incore:daily_redstone", data);

        assertTrue(state.purchaseWorkspaceOpen());
        assertTrue(state.consumeEscape());
        assertFalse(state.purchaseWorkspaceOpen());
    }

    @Test
    void quantityAndScrollClampAgainstVisibleRowsAndStock() {
        ShopAppUiState state = new ShopAppUiState();
        state.setVisibleOfferRows(1);
        ShopService.ScreenData data = screenData();
        state.reconcile(data);

        state.scrollBy(1, data);
        state.openPurchase("incore:daily_redstone", data);
        state.increaseQuantity(data);
        state.increaseQuantity(data);
        state.increaseQuantity(data);

        assertEquals(1, state.offerScrollRow());
        assertEquals(3, state.quantity());
        assertFalse(state.canScrollNext(data));
    }

    private static ShopService.ScreenData screenData() {
        return new ShopService.ScreenData(
                120,
                "incore:daily_exchange",
                "incore:daily_redstone",
                List.of(
                        new ShopService.CategoryView("incore:basic_supplies", "Basic Supplies", "supplies", 10, "none", "none", -1, false),
                        new ShopService.CategoryView("incore:daily_exchange", "Daily Exchange", "rotations", 20, "per_item", "daily_noon", 12, false)
                ),
                List.of(
                        new ShopService.OfferView("incore:basic_bread", "incore:basic_supplies", "minecraft:bread", "Bread", 0, 4, 4, -1, false),
                        new ShopService.OfferView("incore:daily_redstone", "incore:daily_exchange", "minecraft:redstone", "Redstone", 0, 8, 2, 3, false),
                        new ShopService.OfferView("incore:daily_iron_ingot", "incore:daily_exchange", "minecraft:iron_ingot", "Iron Ingot", 1, 12, 1, 1, false)
                )
        );
    }
}
