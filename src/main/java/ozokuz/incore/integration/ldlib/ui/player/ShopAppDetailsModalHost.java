package ozokuz.incore.integration.ldlib.ui.player;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;

final class ShopAppDetailsModalHost {
    private ShopAppDetailsModalHost() {
    }

    static UIElement create(
            ShopAppLayoutContext context,
            ShopAppUiSupport.TabTheme theme
    ) {
        var offer = context.state().selectedOffer(context.data());
        if (offer == null) {
            return new UIElement();
        }

        UIElement overlay = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
            layout.justifyContent(AlignContent.CENTER);
            layout.alignItems(AlignItems.CENTER);
            layout.paddingAll(20);
        });
        overlay.style(style -> style.backgroundTexture(ShopAppUiSupport.buttonTexture(0xA0000000, 0x00000000, 0x00000000, 0)));

        UIElement panel = ShopAppDetailsView.create(context, theme, offer, true);
        panel.layout(layout -> {
            layout.widthPercent(66);
            layout.maxWidth(420);
        });
        overlay.addChild(panel);
        return overlay;
    }
}
