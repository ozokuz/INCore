package ozokuz.incore.integration.ldlib.ui.player;

import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import ozokuz.incore.client.ui.UIScreenTheme;
import ozokuz.incore.features.party.network.PartyActionPayload;
import ozokuz.incore.integration.ldlib.ui.INCoreLdLibUiScaffold;
import ozokuz.incore.integration.ldlib.ui.INCorePlayerUiNavigator;
import ozokuz.incore.integration.ldlib.ui.elements.INCoreInfoSurfaceElement;
import ozokuz.incore.integration.ldlib.ui.texture.BeveledRectTexture;

public final class PartyManagementUiHolder implements PlayerUIMenuType.PlayerUIHolder {
    private static final int TARGET_WINDOW_WIDTH = 360;
    private static final int TARGET_WINDOW_HEIGHT = 400;
    private static final int MIN_WINDOW_WIDTH = 300;
    private static final int MIN_WINDOW_HEIGHT = 280;
    private static final IGuiTexture SCROLL_TRACK_TEXTURE = new BeveledRectTexture(
            UIScreenTheme.Info.PLR_SCROLL_TRACK_FILL,
            0x99243A4A,
            UIScreenTheme.Info.PLR_SCROLL_TRACK_TOP,
            UIScreenTheme.Info.PLR_SCROLL_TRACK_BOTTOM,
            1,
            1
    );
    private static final IGuiTexture SCROLL_THUMB_IDLE_TEXTURE = new BeveledRectTexture(
            UIScreenTheme.Info.PLR_SCROLL_THUMB_FILL,
            0xAA1B394B,
            UIScreenTheme.Info.PLR_SCROLL_THUMB_TOP,
            UIScreenTheme.Info.PLR_SCROLL_THUMB_BOTTOM,
            1,
            1
    );
    private static final IGuiTexture SCROLL_THUMB_HOVER_TEXTURE = new BeveledRectTexture(
            0xD093EEFF,
            0xB2234960,
            0xFFF2FCFF,
            0xCC2A5D73,
            1,
            1
    );
    private static final IGuiTexture SCROLL_THUMB_PRESSED_TEXTURE = new BeveledRectTexture(
            0xC06BC8E4,
            0xAA17384A,
            0xAA2A576B,
            0xFFE0F8FF,
            1,
            1
    );

    @Override
    public ModularUI createUI(Player player) {
        return INCoreLdLibUiScaffold.build(player, createView(player));
    }

    static UIElement createView(Player player) {
        if (player.level().isClientSide()) {
            PacketDistributor.sendToServer(new PartyActionPayload(PartyActionPayload.ActionType.REQUEST_SYNC, null));
        }

        var window = INCoreLdLibUiScaffold.createWindowShell(TARGET_WINDOW_WIDTH, TARGET_WINDOW_HEIGHT);
        window.window().layout(layout -> {
            layout.widthPercent(88);
            layout.heightPercent(92);
            layout.maxWidth(TARGET_WINDOW_WIDTH);
            layout.maxHeight(TARGET_WINDOW_HEIGHT);
            layout.minWidth(MIN_WINDOW_WIDTH);
            layout.minHeight(MIN_WINDOW_HEIGHT);
        });

        window.header().addChild(
                INCoreLdLibUiScaffold.titleLabel(Component.translatable("screen.incore.party.title"))
                        .layout(layout -> layout.flex(1))
        );

        window.body().addChildren(
                createContentCard(),
                createFooterRow(player)
        );

        return window.root();
    }

    private static UIElement createContentCard() {
        ScrollerView scrollerView = new ScrollerView()
                .scrollerStyle(style -> style
                        .mode(ScrollerMode.VERTICAL)
                        .horizontalScrollDisplay(ScrollDisplay.NEVER)
                        .verticalScrollDisplay(ScrollDisplay.AUTO)
                        .minScrollPixel(0.0F)
                        .maxScrollPixel(18.0F)
                );
        scrollerView.layout(layout -> {
            layout.flex(1);
            layout.widthPercent(100);
        });
        scrollerView.viewPort
                .style(style -> style.backgroundTexture(IGuiTexture.EMPTY))
                .layout(layout -> {
                    layout.flex(1);
                    layout.paddingAll(0);
                });
        scrollerView.verticalContainer.layout(layout -> layout.gapColumn(3));
        scrollerView.verticalScroller.layout(layout -> layout.width(6));
        scrollerView.verticalScroller.headButton.setDisplay(false);
        scrollerView.verticalScroller.tailButton.setDisplay(false);
        scrollerView.horizontalScroller.headButton.setDisplay(false);
        scrollerView.horizontalScroller.tailButton.setDisplay(false);
        scrollerView.verticalScroller.scrollContainer.style(style -> style.backgroundTexture(SCROLL_TRACK_TEXTURE));
        scrollerView.verticalScroller.scrollBar.buttonStyle(style -> style
                .baseTexture(SCROLL_THUMB_IDLE_TEXTURE)
                .hoverTexture(SCROLL_THUMB_HOVER_TEXTURE)
                .pressedTexture(SCROLL_THUMB_PRESSED_TEXTURE)
        );
        scrollerView.viewContainer.layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(8);
        });
        scrollerView.addScrollViewChild(
                new PartyManagementContentElement().layout(layout -> layout.widthPercent(100))
        );

        return INCoreInfoSurfaceElement.card()
                .layout(layout -> {
                    layout.flex(1);
                    layout.paddingAll(8);
                })
                .addChild(scrollerView);
    }

    private static UIElement createFooterRow(Player player) {
        Button backButton = INCoreLdLibUiScaffold.actionButton(Component.translatable("gui.back"));
        backButton.layout(layout -> {
            layout.width(96);
            layout.height(20);
        });
        backButton.setOnServerClick(event -> {
            if (player instanceof ServerPlayer serverPlayer) {
                INCorePlayerUiNavigator.goBack(serverPlayer);
            }
        });

        PartyManagementPrimaryActionButton primaryActionButton = new PartyManagementPrimaryActionButton();
        primaryActionButton.layout(layout -> {
            layout.width(112);
            layout.height(20);
        });

        return INCoreLdLibUiScaffold.row()
                .layout(layout -> {
                    layout.widthPercent(100);
                    layout.justifyContent(AlignContent.SPACE_BETWEEN);
                })
                .addChildren(backButton, primaryActionButton);
    }
}
