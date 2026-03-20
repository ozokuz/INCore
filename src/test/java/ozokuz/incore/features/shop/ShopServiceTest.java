package ozokuz.incore.features.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
}
