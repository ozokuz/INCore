package ozokuz.incore.integration.ldlib.ui.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import ozokuz.incore.features.shop.ShopDetailsPresentationMode;
import ozokuz.incore.features.shop.ShopService;
import ozokuz.incore.features.shop.ShopTabId;

class ShopAppUiStateTest {
    @Test
    void reconcileUsesSnapshotTabAndCategoryFallbacks() {
        ShopAppUiState state = new ShopAppUiState();
        state.setVisibleOfferRows(2);

        state.reconcile(screenData());

        assertEquals(ShopTabId.LUXURY_BOUTIQUE, state.activeTab());
        assertEquals("incore:boutique_premium_gear", state.selectedCategoryId());
        assertEquals("incore:chartered_diamond", state.selectedOfferId());
    }

    @Test
    void openDetailsAndConsumeEscapeClosesModal() {
        ShopAppUiState state = new ShopAppUiState();
        ShopService.ScreenData data = screenData();
        state.reconcile(data);

        state.openDetails("incore:archive_relic", data);

        assertTrue(state.detailsModalOpen());
        assertTrue(state.consumeEscape());
        assertFalse(state.detailsModalOpen());
    }

    @Test
    void quantityAndScrollClampAgainstVisibleRowsAndStock() {
        ShopAppUiState state = new ShopAppUiState();
        state.setVisibleOfferRows(1);
        ShopService.ScreenData data = screenData();
        state.reconcile(data);

        state.selectTab(ShopTabId.COMMODITY_EXCHANGE, data);
        state.selectCategory("incore:exchange_coolants", data);
        state.scrollBy(1, data);
        state.selectOffer("incore:coolant_beta", data);
        state.increaseQuantity(data);
        state.increaseQuantity(data);
        state.increaseQuantity(data);

        assertEquals(1, state.offerScrollRow());
        assertEquals(1, state.quantity());
        assertFalse(state.canScrollNext(data));
    }

    @Test
    void detailsModeSelectionMatchesTabDefinitions() {
        ShopService.ScreenData data = screenData();

        assertEquals(ShopDetailsPresentationMode.INLINE_DOCK, ShopAppUiSupport.detailsModeFor(data, ShopTabId.LUXURY_BOUTIQUE));
        assertEquals(ShopDetailsPresentationMode.MODAL_OVERLAY, ShopAppUiSupport.detailsModeFor(data, ShopTabId.ARCHIVE_EDITORIAL));
    }

    private static ShopService.ScreenData screenData() {
        ShopService.CurrencyView spur = new ShopService.CurrencyView("SPUR", 1, 120);
        ShopService.CurrencyView emerald = new ShopService.CurrencyView("Emerald", 1, 24);
        return new ShopService.ScreenData(
                "incore:boutique_premium_gear",
                "incore:chartered_diamond",
                List.of(
                        new ShopService.TabView("commodity_exchange", "Commodity Exchange", "steel_aegis", "commodity_exchange", "inline_header_strip", "inline", List.of("incore:daily_exchange", "incore:exchange_coolants"), new ShopService.ShowcaseView(false, 0, "top_of_feed", List.of())),
                        new ShopService.TabView("luxury_boutique", "Luxury Boutique", "obsidian_ember", "luxury_boutique", "inline_segmented_selector", "inline", List.of("incore:chartered_rotation", "incore:boutique_premium_gear"), new ShopService.ShowcaseView(true, 1, "rotating_first", List.of())),
                        new ShopService.TabView("archive_editorial", "Archive Editorial", "blood_protocol", "archive_editorial", "sidebar", "modal", List.of("incore:archive_artifacts", "incore:expedition_cache"), new ShopService.ShowcaseView(true, 1, "category_pinned", List.of("incore:archive_artifacts")))
                ),
                List.of(
                        new ShopService.CategoryView("incore:daily_exchange", "Daily Exchange", "per_item", "daily_noon", 12, false, spur, false, -1, 1),
                        new ShopService.CategoryView("incore:exchange_coolants", "Exchange Coolants", "per_item", "daily_noon", 12, false, spur, false, -1, 2),
                        new ShopService.CategoryView("incore:chartered_rotation", "Chartered Rotation", "category_bucket", "shop_rotation", 8, false, emerald, true, 7_200_000L, 1),
                        new ShopService.CategoryView("incore:boutique_premium_gear", "Boutique Premium Gear", "category_bucket", "none", 12, false, emerald, false, -1, 2),
                        new ShopService.CategoryView("incore:archive_artifacts", "Archive Artifacts", "category_bucket", "none", 8, false, emerald, false, -1, 1),
                        new ShopService.CategoryView("incore:expedition_cache", "Expedition Cache", "category_bucket", "none", 12, false, spur, false, -1, 1)
                ),
                List.of(
                        new ShopService.OfferView(
                                "incore:daily_alpha",
                                "incore:daily_exchange",
                                "Alpha",
                                18,
                                new ShopService.CurrencyView("SPUR", 18, 120),
                                List.of(new ShopService.RewardEntryView("minecraft:redstone", 8)),
                                3,
                                false,
                                -1
                        ),
                        new ShopService.OfferView(
                                "incore:coolant_alpha",
                                "incore:exchange_coolants",
                                "Coolant Alpha",
                                28,
                                new ShopService.CurrencyView("SPUR", 28, 120),
                                List.of(new ShopService.RewardEntryView("minecraft:slime_ball", 8)),
                                3,
                                false,
                                -1
                        ),
                        new ShopService.OfferView(
                                "incore:coolant_beta",
                                "incore:exchange_coolants",
                                "Coolant Beta",
                                40,
                                new ShopService.CurrencyView("SPUR", 40, 120),
                                List.of(new ShopService.RewardEntryView("minecraft:honeycomb", 4)),
                                1,
                                false,
                                -1
                        ),
                        new ShopService.OfferView(
                                "incore:chartered_diamond",
                                "incore:chartered_rotation",
                                "Chartered Diamond",
                                9,
                                new ShopService.CurrencyView("Emerald", 9, 24),
                                List.of(new ShopService.RewardEntryView("minecraft:diamond", 1)),
                                3,
                                false,
                                7_200_000L
                        ),
                        new ShopService.OfferView(
                                "incore:premium_glass",
                                "incore:boutique_premium_gear",
                                "Premium Glass",
                                6,
                                new ShopService.CurrencyView("Emerald", 6, 24),
                                List.of(new ShopService.RewardEntryView("minecraft:tinted_glass", 4)),
                                8,
                                false,
                                -1
                        ),
                        new ShopService.OfferView(
                                "incore:premium_signal",
                                "incore:boutique_premium_gear",
                                "Premium Signal",
                                8,
                                new ShopService.CurrencyView("Emerald", 8, 24),
                                List.of(new ShopService.RewardEntryView("minecraft:ender_pearl", 2)),
                                8,
                                false,
                                -1
                        ),
                        new ShopService.OfferView(
                                "incore:archive_relic",
                                "incore:archive_artifacts",
                                "Archive Relic",
                                5,
                                new ShopService.CurrencyView("Emerald", 5, 24),
                                List.of(new ShopService.RewardEntryView("minecraft:experience_bottle", 4)),
                                3,
                                false,
                                -1
                        )
                )
        );
    }
}
