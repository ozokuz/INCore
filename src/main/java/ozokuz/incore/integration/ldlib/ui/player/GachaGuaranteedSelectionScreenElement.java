package ozokuz.incore.integration.ldlib.ui.player;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.IBindable;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import ozokuz.incore.client.ui.UIScreenTheme;
import ozokuz.incore.features.gacha.GachaService;
import ozokuz.incore.features.gacha.network.GachaNetworking;
import ozokuz.incore.integration.ldlib.ui.RequestBackIncoreUiPayload;
import ozokuz.incore.integration.ldlib.ui.texture.BeveledRectTexture;

final class GachaGuaranteedSelectionScreenElement extends UIElement implements IBindable<String> {
    private String currentJson = "";
    private GachaService.ScreenData data = new GachaService.ScreenData("", List.of());
    private @Nullable ResourceLocation selectedItemId;

    GachaGuaranteedSelectionScreenElement() {
        layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
        });
        internalSetup();
        rebuild();
    }

    @Override
    public String getValue() {
        return currentJson;
    }

    @Override
    public GachaGuaranteedSelectionScreenElement setValue(@Nullable String value) {
        String nextJson = value == null ? "" : value;
        GachaService.ScreenData nextData = GachaAppUiSupport.parseScreenData(nextJson);
        currentJson = nextJson;
        if (GachaAppUiSupport.isUiEquivalent(data, nextData) && !getChildren().isEmpty()) {
            data = nextData;
            ensureValidSelection();
            return this;
        }
        data = nextData;
        ensureValidSelection();
        rebuild();
        return this;
    }

    private void rebuild() {
        clearAllChildren();
        GachaService.BannerView banner = selectedBanner();
        UIElement cardGrid = new UIElement()
                .layout(layout -> {
                    layout.flex(1);
                    layout.widthPercent(100);
                    layout.minHeight(0);
                    layout.flexDirection(FlexDirection.ROW);
                    layout.flexWrap(FlexWrap.WRAP);
                    layout.justifyContent(AlignContent.CENTER);
                    layout.alignItems(AlignItems.FLEX_START);
                    layout.gapAll(8);
                });

        if (banner != null) {
            for (ResourceLocation itemId : GachaAppUiSupport.selectableSixItems(banner)) {
                cardGrid.addChild(selectionCard(itemId));
            }
        }

        Button confirmButton = GachaViewSupport.footerButton(
                Component.translatable("screen.incore.gacha_guaranteed_six.confirm"),
                140,
                banner != null && selectedItemId != null
        );
        confirmButton.setOnClick(event -> {
            if (banner == null || selectedItemId == null) {
                return;
            }
            ResourceLocation bannerId = ResourceLocation.tryParse(banner.id());
            if (bannerId != null) {
                GachaNetworking.sendBasicGuaranteedSixClaim(bannerId, selectedItemId);
            }
        });

        addChild(
                new UIElement()
                        .layout(layout -> {
                            layout.widthPercent(100);
                            layout.heightPercent(100);
                            layout.flexDirection(FlexDirection.COLUMN);
                            layout.alignItems(AlignItems.CENTER);
                            layout.gapAll(4);
                        })
                        .addChildren(
                                GachaViewSupport.titleLabel(Component.translatable("screen.incore.gacha_guaranteed_six.title", banner == null ? "" : banner.name())),
                                GachaViewSupport.centeredLabel(
                                        banner == null
                                                ? Component.empty()
                                                : Component.translatable(
                                                        "screen.incore.gacha_banners.basic_guaranteed_pity",
                                                        banner.basicSelectedSixPity(),
                                                        GachaService.BASIC_SELECTED_SIX_THRESHOLD
                                                ),
                                        UIScreenTheme.OtherContent.INFO_RATE_LABEL_TEXT
                                ),
                                GachaViewSupport.centeredLabel(
                                        Component.translatable("screen.incore.gacha_guaranteed_six.select_hint"),
                                        UIScreenTheme.OtherContent.CATALOG_TEXT_META
                                ),
                                cardGrid,
                                new UIElement()
                                        .layout(layout -> {
                                            layout.widthPercent(100);
                                            layout.flexDirection(FlexDirection.ROW);
                                            layout.justifyContent(AlignContent.CENTER);
                                            layout.alignItems(AlignItems.CENTER);
                                            layout.gapAll(16);
                                        })
                                        .addChildren(
                                                GachaViewSupport.footerButton(Component.translatable("gui.back"), 120, true)
                                                        .setOnClick(event -> net.neoforged.neoforge.network.PacketDistributor.sendToServer(RequestBackIncoreUiPayload.INSTANCE)),
                                                confirmButton
                                        )
                        )
        );
    }

    private Button selectionCard(ResourceLocation itemId) {
        boolean selected = itemId.equals(selectedItemId);
        Button card = GachaViewSupport.rowButton(Component.empty(), GachaAppUiSupport.GUARANTEE_CARD_HEIGHT, selected
                ? UIScreenTheme.OtherContent.GUARANTEE_ROW_FILL_SELECTED
                : UIScreenTheme.OtherContent.GUARANTEE_ROW_FILL);
        card.layout(layout -> {
            layout.width(GachaAppUiSupport.GUARANTEE_CARD_WIDTH);
            layout.height(GachaAppUiSupport.GUARANTEE_CARD_HEIGHT);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.alignItems(AlignItems.CENTER);
            layout.justifyContent(AlignContent.FLEX_START);
            layout.paddingTop(10);
            layout.paddingHorizontal(8);
            layout.paddingBottom(6);
            layout.gapAll(2);
        });
        int fillColor = UIScreenTheme.OtherContent.GUARANTEE_ROW_FILL;
        int borderColor = selected
                ? UIScreenTheme.OtherContent.GUARANTEE_ROW_BORDER_SELECTED
                : UIScreenTheme.OtherContent.GUARANTEE_ROW_BORDER;
        card.buttonStyle(style -> style
                .baseTexture(new BeveledRectTexture(fillColor, borderColor, borderColor, borderColor, 1, 0))
                .hoverTexture(new BeveledRectTexture(
                        fillColor,
                        selected ? UIScreenTheme.OtherContent.GUARANTEE_ROW_BORDER_SELECTED : UIScreenTheme.OtherContent.GUARANTEE_ROW_BORDER_HOVER,
                        selected ? UIScreenTheme.OtherContent.GUARANTEE_ROW_BORDER_SELECTED : UIScreenTheme.OtherContent.GUARANTEE_ROW_BORDER_HOVER,
                        selected ? UIScreenTheme.OtherContent.GUARANTEE_ROW_BORDER_SELECTED : UIScreenTheme.OtherContent.GUARANTEE_ROW_BORDER_HOVER,
                        1,
                        0
                ))
                .pressedTexture(new BeveledRectTexture(fillColor, borderColor, borderColor, borderColor, 1, 0))
        );
        card.text.setDisplay(false);

        ItemStack stack = GachaAppUiSupport.stackForId(itemId);
        card.style(style -> style.tooltips(GachaViewSupport.stackTooltip(
                stack,
                Component.literal(itemId.toString())
        )));
        UIElement iconRow = new UIElement()
                .layout(layout -> {
                    layout.widthPercent(100);
                    layout.height(20);
                    layout.justifyContent(AlignContent.CENTER);
                    layout.alignItems(AlignItems.CENTER);
                });
        if (!stack.isEmpty()) {
            iconRow.addChild(GachaViewSupport.icon(stack, 16));
        }

        var nameLabel = GachaViewSupport.centeredLabel(
                stack.isEmpty() ? Component.literal(itemId.toString()) : stack.getHoverName(),
                UIScreenTheme.OtherContent.CATALOG_TEXT_HEADING
        );
        nameLabel.layout(layout -> {
            layout.widthPercent(100);
            layout.height(20);
        });
        nameLabel.textStyle(style -> style
                .adaptiveWidth(false)
                .adaptiveHeight(false)
                .textWrap(TextWrap.HIDE)
                .textAlignHorizontal(Horizontal.CENTER)
        );

        var idLabel = GachaViewSupport.centeredLabel(Component.literal(itemId.toString()), UIScreenTheme.OtherContent.GUARANTEE_ID_TEXT);
        idLabel.layout(layout -> {
            layout.widthPercent(100);
            layout.height(16);
        });
        idLabel.textStyle(style -> style
                .adaptiveWidth(false)
                .adaptiveHeight(false)
                .textWrap(TextWrap.HIDE)
                .textAlignHorizontal(Horizontal.CENTER)
        );

        var rarityLabel = GachaViewSupport.centeredLabel(Component.literal("6★"), UIScreenTheme.OtherContent.GACHA_SHOWCASE_SIX_TEXT);
        rarityLabel.layout(layout -> {
            layout.widthPercent(100);
            layout.height(12);
        });
        rarityLabel.textStyle(style -> style
                .adaptiveWidth(false)
                .adaptiveHeight(false)
                .textWrap(TextWrap.HIDE)
                .textAlignHorizontal(Horizontal.CENTER)
        );

        card.addChildren(iconRow, nameLabel, idLabel, rarityLabel);
        card.setOnClick(event -> {
            selectedItemId = itemId;
            rebuild();
        });
        return card;
    }

    private void ensureValidSelection() {
        if (selectedItemId == null) {
            return;
        }
        GachaService.BannerView banner = selectedBanner();
        if (banner == null || !GachaAppUiSupport.selectableSixItems(banner).contains(selectedItemId)) {
            selectedItemId = null;
        }
    }

    private @Nullable GachaService.BannerView selectedBanner() {
        return GachaAppUiSupport.findBanner(data, null);
    }
}
