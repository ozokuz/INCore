package ozokuz.incore.integration.ldlib.ui.player;

import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
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

public final class PlayerLevelRewardsUiHolder implements PlayerUIMenuType.PlayerUIHolder {
    private static final float LEFT_COLUMN_WIDTH_PERCENT = 34.0F;

    @Override
    public ModularUI createUI(Player player) {
        PlayerLevelRewardsUiState state = new PlayerLevelRewardsUiState();
        var window = INCoreLdLibUiScaffold.createWindowShell(
                PlayerLevelRewardsUiSupport.TARGET_WINDOW_WIDTH,
                PlayerLevelRewardsUiSupport.TARGET_WINDOW_HEIGHT
        );
        window.window().getLayout().widthPercent(96);
        window.window().getLayout().heightPercent(94);
        window.window().getLayout().maxWidth(PlayerLevelRewardsUiSupport.TARGET_WINDOW_WIDTH);
        window.window().getLayout().maxHeight(PlayerLevelRewardsUiSupport.TARGET_WINDOW_HEIGHT);
        window.window().getLayout().minWidth(PlayerLevelRewardsUiSupport.MIN_WINDOW_WIDTH);
        window.window().getLayout().minHeight(PlayerLevelRewardsUiSupport.MIN_WINDOW_HEIGHT);

        var titleLabel = INCoreLdLibUiScaffold.titleLabel(Component.translatable("screen.incore.player_level_rewards.title"));
        titleLabel.getLayout().flex(1);
        window.header().addChild(titleLabel);

        UIElement content = INCoreLdLibUiScaffold.row();
        content.getLayout().flex(1);
        content.getLayout().gapAll(8);
        content.getLayout().alignItems(AlignItems.STRETCH);

        UIElement leftColumn = INCoreLdLibUiScaffold.column();
        leftColumn.getLayout().flexBasisPercent(LEFT_COLUMN_WIDTH_PERCENT);
        leftColumn.getLayout().minWidth(170);
        leftColumn.getLayout().maxWidth(PlayerLevelRewardsUiSupport.SIDEBAR_TARGET_WIDTH);
        leftColumn.getLayout().heightPercent(100);
        leftColumn.getLayout().gapAll(8);

        UIElement rightColumn = INCoreLdLibUiScaffold.column();
        rightColumn.getLayout().flex(1);
        rightColumn.getLayout().heightPercent(100);
        rightColumn.getLayout().gapAll(8);

        leftColumn.addChildren(createRailCard(state), createBackButtonRow(player));
        rightColumn.addChildren(createHeroCard(state), createDetailsCard(state));
        content.addChildren(leftColumn, rightColumn);
        window.body().addChild(content);
        return INCoreLdLibUiScaffold.build(player, window.root());
    }

    private static UIElement createRailCard(PlayerLevelRewardsUiState state) {
        UIElement card = INCoreInfoSurfaceElement.card();
        card.getLayout().flex(1);
        card.getLayout().paddingAll(8);
        card.getLayout().gapAll(6);

        var title = INCoreLdLibUiScaffold.sectionTitle(Component.translatable("screen.incore.player_level_rewards.sidebar_title"));
        title.getLayout().widthPercent(100);

        ScrollerView scrollerView = new ScrollerView();
        scrollerView.getLayout().flex(1);
        scrollerView.getLayout().widthPercent(100);
        scrollerView.scrollerStyle(style -> style
                .mode(ScrollerMode.VERTICAL)
                .horizontalScrollDisplay(ScrollDisplay.NEVER)
                .verticalScrollDisplay(ScrollDisplay.AUTO)
                .minScrollPixel(0.0F)
                .maxScrollPixel(PlayerLevelRewardsUiSupport.LEVEL_CARD_HEIGHT)
        );
        scrollerView.viewPort.style(style -> style.backgroundTexture(IGuiTexture.EMPTY));
        scrollerView.viewPort.layout(layout -> layout.paddingAll(0));
        scrollerView.verticalContainer.getLayout().gapColumn(PlayerLevelRewardsUiSupport.SCROLLBAR_GAP);
        scrollerView.verticalScroller.getLayout().width(PlayerLevelRewardsUiSupport.SCROLLBAR_WIDTH);
        scrollerView.verticalScroller.headButton.setDisplay(false);
        scrollerView.verticalScroller.tailButton.setDisplay(false);
        scrollerView.horizontalScroller.headButton.setDisplay(false);
        scrollerView.horizontalScroller.tailButton.setDisplay(false);
        scrollerView.verticalScroller.scrollContainer.style(style -> style.backgroundTexture(new ColorRectTexture(UIScreenTheme.Info.PLR_SCROLL_TRACK_FILL)));
        scrollerView.verticalScroller.scrollBar.buttonStyle(style -> style
                .baseTexture(new ColorRectTexture(UIScreenTheme.Info.PLR_SCROLL_THUMB_FILL))
                .hoverTexture(new ColorRectTexture(UIScreenTheme.Info.PLR_SCROLL_THUMB_TOP))
                .pressedTexture(new ColorRectTexture(UIScreenTheme.Info.PLR_SCROLL_THUMB_BOTTOM))
        );
        scrollerView.viewContainer.layout(layout -> {
            layout.flexDirection(FlexDirection.COLUMN);
            layout.widthPercent(100);
        });

        PlayerLevelRewardsRailElement railElement = new PlayerLevelRewardsRailElement(state, scrollerView);
        railElement.getLayout().widthPercent(100);
        scrollerView.addScrollViewChild(railElement);

        card.addChildren(title, scrollerView);
        return card;
    }

    private static UIElement createHeroCard(PlayerLevelRewardsUiState state) {
        UIElement card = INCoreInfoSurfaceElement.card();
        card.getLayout().height(PlayerLevelRewardsUiSupport.HERO_HEIGHT);
        card.getLayout().paddingAll(0);

        PlayerLevelRewardsHeroElement heroElement = new PlayerLevelRewardsHeroElement(state);
        heroElement.getLayout().widthPercent(100);
        heroElement.getLayout().heightPercent(100);
        card.addChild(heroElement);
        return card;
    }

    private static UIElement createDetailsCard(PlayerLevelRewardsUiState state) {
        UIElement card = INCoreInfoSurfaceElement.card();
        card.getLayout().flex(1);
        card.getLayout().paddingAll(0);

        PlayerLevelRewardsDetailsElement detailsElement = new PlayerLevelRewardsDetailsElement(state);
        detailsElement.getLayout().widthPercent(100);
        detailsElement.getLayout().heightPercent(100);
        card.addChild(detailsElement);
        return card;
    }

    private static UIElement createBackButtonRow(Player player) {
        UIElement row = INCoreLdLibUiScaffold.row();
        row.getLayout().widthPercent(100);
        row.getLayout().justifyContent(AlignContent.FLEX_START);

        Button backButton = INCoreLdLibUiScaffold.actionButton(Component.translatable("gui.back"));
        backButton.getLayout().width(96);
        backButton.getLayout().height(20);
        backButton.setOnServerClick(event -> {
            if (player instanceof ServerPlayer serverPlayer) {
                INCorePlayerUiNavigator.goBack(serverPlayer);
            }
        });

        row.addChild(backButton);
        return row;
    }
}
