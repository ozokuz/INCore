package ozokuz.incore.integration.ldlib.ui.player;

import ozokuz.incore.features.shop.ShopService;

interface ShopAppLayoutContext {
    ShopService.ScreenData data();

    ShopAppUiState state();

    void rebuild();
}
