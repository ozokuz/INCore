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
        ShopService.CurrencyView spur = new ShopService.CurrencyView("incore:bank_spur", "numismatics:spur", "SPUR", 1, 120);
        return new ShopService.ScreenData(
                "incore:daily_exchange",
                "incore:daily_redstone",
                List.of(
                        new ShopService.CategoryView("incore:basic_supplies", "Basic Supplies", "supplies", 10, "none", "none", -1, false, spur, false, -1, 1),
                        new ShopService.CategoryView("incore:daily_exchange", "Daily Exchange", "rotations", 20, "per_item", "daily_noon", 12, false, spur, false, -1, 2)
                ),
                List.of(
                        new ShopService.OfferView(
                                "incore:basic_bread",
                                "incore:basic_supplies",
                                "Bread",
                                18,
                                "single_item",
                                new ShopService.CurrencyView("incore:bank_spur", "numismatics:spur", "SPUR", 18, 120),
                                "minecraft:bread",
                                8,
                                1,
                                List.of(new ShopService.RewardEntryView("minecraft:bread", 8)),
                                -1,
                                false,
                                -1
                        ),
                        new ShopService.OfferView(
                                "incore:daily_redstone",
                                "incore:daily_exchange",
                                "Redstone",
                                28,
                                "single_item",
                                new ShopService.CurrencyView("incore:bank_spur", "numismatics:spur", "SPUR", 28, 120),
                                "minecraft:redstone",
                                8,
                                1,
                                List.of(new ShopService.RewardEntryView("minecraft:redstone", 8)),
                                3,
                                false,
                                -1
                        ),
                        new ShopService.OfferView(
                                "incore:daily_iron_ingot",
                                "incore:daily_exchange",
                                "Iron Ingot",
                                40,
                                "single_item",
                                new ShopService.CurrencyView("incore:bank_spur", "numismatics:spur", "SPUR", 40, 120),
                                "minecraft:iron_ingot",
                                4,
                                1,
                                List.of(new ShopService.RewardEntryView("minecraft:iron_ingot", 4)),
                                1,
                                false,
                                -1
                        )
                )
        );
    }
}
