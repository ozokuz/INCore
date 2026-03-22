package ozokuz.incore.integration.ldlib.ui.player;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;

interface ShopAppLayout {
    int visibleOfferRows();

    UIElement createContentRow(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme);
}
