package ozokuz.incore.features.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import ozokuz.incore.integration.ldlib.ui.INCoreUiRouteContext;
import ozokuz.incore.integration.ldlib.ui.ShopUiRouteContext;

class ShopServiceTest {
    @Test
    void requestedSelectionFallsBackToShopRouteContext() {
        ResourceLocation categoryId = ResourceLocation.parse("incore:daily_exchange");
        ResourceLocation offerId = ResourceLocation.parse("incore:daily_redstone");

        ShopService.RequestedSelection selection = ShopService.requestedSelectionForContext(
                null,
                null,
                new ShopUiRouteContext(categoryId, offerId)
        );

        assertEquals(categoryId, selection.categoryId());
        assertEquals(offerId, selection.offerId());
    }

    @Test
    void requestedSelectionPrefersExplicitArgumentsOverContext() {
        ResourceLocation categoryId = ResourceLocation.parse("incore:basic_supplies");
        ResourceLocation offerId = ResourceLocation.parse("incore:basic_bread");

        ShopService.RequestedSelection selection = ShopService.requestedSelectionForContext(
                categoryId,
                offerId,
                INCoreUiRouteContext.Empty.INSTANCE
        );

        assertEquals(categoryId, selection.categoryId());
        assertEquals(offerId, selection.offerId());
    }

    @Test
    void requestedSelectionReturnsEmptyWhenNoFocusExists() {
        ShopService.RequestedSelection selection = ShopService.requestedSelectionForContext(
                null,
                null,
                INCoreUiRouteContext.Empty.INSTANCE
        );

        assertNull(selection.categoryId());
        assertNull(selection.offerId());
    }

    @Test
    void orderedCategoriesComeFromTabDefinition() {
        ShopService.ScreenData data = screenData();

        assertEquals(
                List.of("incore:vendor_daily_deals", "incore:salvage_exchange"),
                ShopService.orderedCategoriesForTab(data, ShopTabId.ARCADE_VENDOR).stream()
                        .map(ShopService.CategoryView::categoryId)
                        .toList()
        );
    }

    @Test
    void buildTabFeedRemovesShowcaseOfferFromRemainder() {
        ShopService.TabFeedView feed = ShopService.buildTabFeed(screenData(), ShopTabId.LUXURY_BOUTIQUE, "incore:boutique_premium_gear");

        assertEquals(List.of("incore:chartered_diamond"), feed.showcaseOffers().stream().map(ShopService.OfferView::offerId).toList());
        assertEquals(List.of("incore:premium_ghast_tears", "incore:premium_ender_pearls"), feed.remainingOffers().stream().map(ShopService.OfferView::offerId).toList());
    }

    @Test
    void rotatingFirstShowcasePrefersRotatingCategoryEvenWhenAnotherCategoryIsActive() {
        ShopService.TabFeedView feed = ShopService.buildTabFeed(screenData(), ShopTabId.LUXURY_BOUTIQUE, "incore:boutique_premium_gear");

        assertEquals("incore:boutique_premium_gear", feed.activeCategoryId());
        assertEquals("incore:chartered_diamond", feed.showcaseOffers().getFirst().offerId());
    }

    @Test
    void categoryPinnedShowcaseUsesPinnedCategory() {
        ShopService.TabFeedView feed = ShopService.buildTabFeed(screenData(), ShopTabId.ARCHIVE_EDITORIAL, "incore:expedition_cache");

        assertEquals("incore:archive_relic", feed.showcaseOffers().getFirst().offerId());
        assertEquals(List.of("incore:expedition_obsidian"), feed.remainingOffers().stream().map(ShopService.OfferView::offerId).toList());
    }

    @Test
    void currencyFixturesPreserveBankSpurIcon() {
        ShopService.ScreenData data = screenData();

        assertEquals("numismatics:spur", data.categories().getFirst().currency().iconItemId());
    }

    @Test
    void currencyFixturesPreserveItemCurrencyIcon() {
        ShopService.ScreenData data = screenData();

        assertEquals("minecraft:emerald", data.offers().getFirst().currency().iconItemId());
    }

    @Test
    void visibleOfferIdsForCategoryRollWindowAdvancesOneOfferAtATime() {
        ShopCategoryRotationDefinition rotation = new ShopCategoryRotationDefinition(24, 3);
        long durationMillis = 24L * 60L * 60L * 1000L;
        List<ResourceLocation> orderedOfferIds = List.of(
                ResourceLocation.parse("incore:a"),
                ResourceLocation.parse("incore:b"),
                ResourceLocation.parse("incore:c"),
                ResourceLocation.parse("incore:d"),
                ResourceLocation.parse("incore:e")
        );
        List<ResourceLocation> step0 = ShopService.visibleOfferIdsForOrderedOffers(orderedOfferIds, rotation, 0L);
        List<ResourceLocation> step1 = ShopService.visibleOfferIdsForOrderedOffers(orderedOfferIds, rotation, durationMillis);
        List<ResourceLocation> step2 = ShopService.visibleOfferIdsForOrderedOffers(orderedOfferIds, rotation, durationMillis * 2L);

        assertEquals(List.of(
                ResourceLocation.parse("incore:a"),
                ResourceLocation.parse("incore:b"),
                ResourceLocation.parse("incore:c")
        ), step0);
        assertEquals(List.of(
                ResourceLocation.parse("incore:b"),
                ResourceLocation.parse("incore:c"),
                ResourceLocation.parse("incore:d")
        ), step1);
        assertEquals(List.of(
                ResourceLocation.parse("incore:c"),
                ResourceLocation.parse("incore:d"),
                ResourceLocation.parse("incore:e")
        ), step2);
    }

    @Test
    void visibleOfferIdsWrapAroundCategoryOrder() {
        ShopCategoryRotationDefinition rotation = new ShopCategoryRotationDefinition(24, 2);
        long durationMillis = 24L * 60L * 60L * 1000L;
        assertEquals(
                List.of(ResourceLocation.parse("incore:d"), ResourceLocation.parse("incore:a")),
                ShopService.visibleOfferIdsForOrderedOffers(
                        List.of(
                                ResourceLocation.parse("incore:a"),
                                ResourceLocation.parse("incore:b"),
                                ResourceLocation.parse("incore:c"),
                                ResourceLocation.parse("incore:d")
                        ),
                        rotation,
                        durationMillis * 3L
                )
        );
    }

    private static ShopService.ScreenData screenData() {
        ShopService.CurrencyView spur = new ShopService.CurrencyView("numismatics:spur", "SPUR", 1, 120);
        ShopService.CurrencyView emerald = new ShopService.CurrencyView("minecraft:emerald", "Emerald", 1, 12);
        return new ShopService.ScreenData(
                "incore:boutique_premium_gear",
                "incore:premium_ghast_tears",
                List.of(
                        new ShopService.TabView(
                                "luxury_boutique",
                                "Luxury Boutique",
                                "obsidian_ember",
                                "luxury_boutique",
                                "inline_segmented_selector",
                                "inline",
                                List.of("incore:chartered_rotation", "incore:boutique_premium_gear"),
                                new ShopService.ShowcaseView(true, 1, "rotating_first", List.of())
                        ),
                        new ShopService.TabView(
                                "arcade_vendor",
                                "Arcade Vendor",
                                "neon_shadow",
                                "arcade_vendor",
                                "inline_chips",
                                "modal",
                                List.of("incore:vendor_daily_deals", "incore:salvage_exchange"),
                                new ShopService.ShowcaseView(true, 1, "top_of_feed", List.of())
                        ),
                        new ShopService.TabView(
                                "archive_editorial",
                                "Archive Editorial",
                                "blood_protocol",
                                "archive_editorial",
                                "sidebar",
                                "modal",
                                List.of("incore:archive_artifacts", "incore:expedition_cache"),
                                new ShopService.ShowcaseView(true, 1, "category_pinned", List.of("incore:archive_artifacts"))
                        )
                ),
                List.of(
                        new ShopService.CategoryView("incore:vendor_daily_deals", "Vendor Daily Deals", "category_bucket", "daily_noon", 24, false, spur, false, -1, 1, 0),
                        new ShopService.CategoryView("incore:salvage_exchange", "Salvage Exchange", "category_bucket", "shop_rotation", 18, false, emerald, true, 3_600_000L, 5, 0),
                        new ShopService.CategoryView("incore:chartered_rotation", "Chartered Rotation", "category_bucket", "shop_rotation", 12, false, emerald, true, 3_600_000L, 1, 0),
                        new ShopService.CategoryView("incore:boutique_premium_gear", "Boutique Premium Gear", "category_bucket", "none", 16, false, emerald, false, -1, 2, 0),
                        new ShopService.CategoryView("incore:archive_artifacts", "Archive Artifacts", "category_bucket", "none", 18, false, emerald, false, -1, 1, 0),
                        new ShopService.CategoryView("incore:expedition_cache", "Expedition Cache", "category_bucket", "none", 40, false, spur, false, -1, 1, 0)
                ),
                List.of(
                        new ShopService.OfferView("incore:chartered_diamond", "incore:chartered_rotation", "Chartered Diamond", 48, emerald, List.of(new ShopService.RewardEntryView("minecraft:diamond", 1)), 2, false, 3_600_000L),
                        new ShopService.OfferView("incore:premium_ghast_tears", "incore:boutique_premium_gear", "Ghast Tears", 9, emerald, List.of(new ShopService.RewardEntryView("minecraft:ghast_tear", 2)), 8, false, -1),
                        new ShopService.OfferView("incore:premium_ender_pearls", "incore:boutique_premium_gear", "Ender Pearls", 6, emerald, List.of(new ShopService.RewardEntryView("minecraft:ender_pearl", 2)), 8, false, -1),
                        new ShopService.OfferView("incore:vendor_bundle", "incore:vendor_daily_deals", "Vendor Bundle", 24, spur, List.of(new ShopService.RewardEntryView("minecraft:amethyst_shard", 4), new ShopService.RewardEntryView("minecraft:gold_nugget", 5)), 12, false, -1),
                        new ShopService.OfferView("incore:archive_relic", "incore:archive_artifacts", "Archive Relic", 16, emerald, List.of(new ShopService.RewardEntryView("minecraft:experience_bottle", 4)), 4, false, -1),
                        new ShopService.OfferView("incore:expedition_obsidian", "incore:expedition_cache", "Expedition Obsidian", 32, spur, List.of(new ShopService.RewardEntryView("minecraft:obsidian", 8)), 6, false, -1)
                )
        );
    }
}
