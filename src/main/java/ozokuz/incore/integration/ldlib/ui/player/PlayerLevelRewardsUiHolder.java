package ozokuz.incore.integration.ldlib.ui.player;

import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import ozokuz.incore.client.ui.UIScreenTheme;
import ozokuz.incore.integration.ldlib.ui.INCoreLdLibUiScaffold;
import ozokuz.incore.integration.ldlib.ui.INCorePlayerUiNavigator;
import ozokuz.incore.integration.ldlib.ui.elements.INCoreInfoSurfaceElement;
import ozokuz.incore.integration.ldlib.ui.texture.BeveledRectTexture;

public final class PlayerLevelRewardsUiHolder implements PlayerUIMenuType.PlayerUIHolder {
    private static final float LEFT_COLUMN_WIDTH_PERCENT = 34.0F;
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
        PlayerLevelRewardsUiState state = new PlayerLevelRewardsUiState();
        var window = INCoreLdLibUiScaffold.createWindowShell(
                PlayerLevelRewardsUiSupport.TARGET_WINDOW_WIDTH,
                PlayerLevelRewardsUiSupport.TARGET_WINDOW_HEIGHT
        );
        window.window().layout(layout -> {
            layout.widthPercent(96);
            layout.heightPercent(94);
            layout.maxWidth(PlayerLevelRewardsUiSupport.TARGET_WINDOW_WIDTH);
            layout.maxHeight(PlayerLevelRewardsUiSupport.TARGET_WINDOW_HEIGHT);
            layout.minWidth(PlayerLevelRewardsUiSupport.MIN_WINDOW_WIDTH);
            layout.minHeight(PlayerLevelRewardsUiSupport.MIN_WINDOW_HEIGHT);
        });

        window.header().addChild(
                INCoreLdLibUiScaffold.titleLabel(Component.translatable("screen.incore.player_level_rewards.title"))
                        .layout(layout -> layout.flex(1))
        );

        window.body().addChild(
                INCoreLdLibUiScaffold.row()
                        .layout(layout -> {
                            layout.flex(1);
                            layout.gapAll(8);
                            layout.alignItems(AlignItems.STRETCH);
                        })
                        .addChildren(
                                INCoreLdLibUiScaffold.column()
                                        .layout(layout -> {
                                            layout.flexBasisPercent(LEFT_COLUMN_WIDTH_PERCENT);
                                            layout.minWidth(170);
                                            layout.maxWidth(PlayerLevelRewardsUiSupport.SIDEBAR_TARGET_WIDTH);
                                            layout.heightPercent(100);
                                            layout.gapAll(8);
                                        })
                                        .addChildren(createRailCard(state), createBackButtonRow(player)),
                                INCoreLdLibUiScaffold.column()
                                        .layout(layout -> {
                                            layout.flex(1);
                                            layout.heightPercent(100);
                                            layout.gapAll(8);
                                        })
                                        .addChildren(createHeroCard(state), createDetailsCard(state))
                        )
        );
        return window.root();
    }

    private static UIElement createRailCard(PlayerLevelRewardsUiState state) {
        var scrollerView = new ScrollerView()
                .scrollerStyle(style -> style
                        .mode(ScrollerMode.VERTICAL)
                        .horizontalScrollDisplay(ScrollDisplay.NEVER)
                        .verticalScrollDisplay(ScrollDisplay.AUTO)
                        .minScrollPixel(0.0F)
                        .maxScrollPixel(PlayerLevelRewardsUiSupport.LEVEL_CARD_HEIGHT)
                );
        scrollerView.layout(layout -> {
            layout.flex(1);
            layout.widthPercent(100);
        });
        scrollerView.viewPort
                .style(style -> style.backgroundTexture(IGuiTexture.EMPTY))
                .layout(layout -> layout.paddingAll(0));
        scrollerView.verticalContainer.layout(layout -> layout.gapColumn(PlayerLevelRewardsUiSupport.SCROLLBAR_GAP));
        scrollerView.verticalScroller.layout(layout -> layout.width(PlayerLevelRewardsUiSupport.SCROLLBAR_WIDTH));
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
            layout.flexDirection(FlexDirection.COLUMN);
            layout.widthPercent(100);
        });
        scrollerView.addScrollViewChild(
                new PlayerLevelRewardsRailElement(state, scrollerView)
                        .layout(layout -> layout.widthPercent(100))
        );

        return INCoreInfoSurfaceElement.card()
                .layout(layout -> {
                    layout.flex(1);
                    layout.paddingAll(8);
                    layout.gapAll(6);
                })
                .addChildren(
                        INCoreLdLibUiScaffold.sectionTitle(Component.translatable("screen.incore.player_level_rewards.sidebar_title"))
                                .layout(layout -> layout.widthPercent(100)),
                        scrollerView
                );
    }

    private static UIElement createHeroCard(PlayerLevelRewardsUiState state) {
        return INCoreInfoSurfaceElement.card()
                .layout(layout -> {
                    layout.height(PlayerLevelRewardsUiSupport.HERO_HEIGHT);
                    layout.paddingAll(0);
                })
                .addChild(
                        new PlayerLevelRewardsHeroElement(state).layout(layout -> {
                            layout.widthPercent(100);
                            layout.heightPercent(100);
                        })
                );
    }

    private static UIElement createDetailsCard(PlayerLevelRewardsUiState state) {
        return INCoreInfoSurfaceElement.card()
                .layout(layout -> {
                    layout.flex(1);
                    layout.paddingAll(0);
                })
                .addChild(
                        new PlayerLevelRewardsDetailsElement(state).layout(layout -> {
                            layout.widthPercent(100);
                            layout.heightPercent(100);
                        })
                );
    }

    private static UIElement createBackButtonRow(Player player) {
        return INCoreLdLibUiScaffold.row()
                .layout(layout -> {
                    layout.widthPercent(100);
                    layout.justifyContent(AlignContent.FLEX_START);
                })
                .addChild(
                        INCoreLdLibUiScaffold.actionButton(Component.translatable("gui.back"))
                                .setOnServerClick(event -> {
                                    if (player instanceof ServerPlayer serverPlayer) {
                                        INCorePlayerUiNavigator.goBack(serverPlayer);
                                    }
                                })
                                .layout(layout -> {
                                    layout.width(96);
                                    layout.height(20);
                                })
                );
    }
}
