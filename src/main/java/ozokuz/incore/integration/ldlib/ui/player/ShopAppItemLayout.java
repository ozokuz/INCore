package ozokuz.incore.integration.ldlib.ui.player;

import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import ozokuz.incore.features.shop.ShopService;

interface ShopAppItemLayout {
    void addOfferChildren(
            Button card,
            ShopService.OfferView offer,
            ShopAppLayoutContext context,
            ShopAppUiSupport.TabTheme theme,
            int iconSize
    );
}
