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
}
