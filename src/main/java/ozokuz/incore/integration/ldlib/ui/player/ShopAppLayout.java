package ozokuz.incore.integration.ldlib.ui.player;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;

interface ShopAppLayout {
    int visibleOfferRows();

    int offerCardHeight();

    int offerIconSize();

    int heroIconSize();

    UIElement createContentRow(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme);

    UIElement createStandbyAccent(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme);

    ShopAppItemLayout item();
}
