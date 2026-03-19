package ozokuz.incore.integration.ldlib.ui.player;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.RectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import ozokuz.incore.client.ui.UIScreenTheme;
import ozokuz.incore.features.battlepass.network.BattlePassClientCache;
import ozokuz.incore.features.battlepass.network.BattlePassNetworking;
import ozokuz.incore.integration.ldlib.ui.INCoreLdLibUiScaffold;
import ozokuz.incore.integration.ldlib.ui.RequestBackIncoreUiPayload;
import ozokuz.incore.integration.ldlib.ui.texture.BeveledRectTexture;
import ozokuz.incore.integration.ldlib.ui.texture.BottomBorderTexture;

final class BattlePassScreenElement extends UIElement {
    private static final IGuiTexture TAB_IDLE_TEXTURE = new BottomBorderTexture(
            UIScreenTheme.BattlepassTasks.TAB_FILL_DEFAULT,
            UIScreenTheme.BattlepassTasks.TAB_UNDERLINE_DEFAULT,
            1
    );
    private static final IGuiTexture TAB_HOVER_TEXTURE = new BottomBorderTexture(
            0x88414854,
            UIScreenTheme.BattlepassTasks.ACCENT_GOLD,
            1
    );
    private static final IGuiTexture TAB_SELECTED_TEXTURE = new BottomBorderTexture(
            UIScreenTheme.BattlepassTasks.TAB_FILL_SELECTED,
            UIScreenTheme.BattlepassTasks.TAB_UNDERLINE_SELECTED,
            1
    );
    private static final IGuiTexture CLAIM_IDLE_TEXTURE = new BeveledRectTexture(
            0xFF24272D,
            UIScreenTheme.BattlepassTasks.BORDER_DARK,
            0xFF5A6270,
            0xFF15181E,
            1,
            1
    );
    private static final IGuiTexture CLAIM_HOVER_TEXTURE = new BeveledRectTexture(
            0xFF30343C,
            UIScreenTheme.BattlepassTasks.BORDER_DARK,
            0xFF8A95A7,
            0xFF191D23,
            1,
            1
    );
    private static final IGuiTexture CLAIM_PRESSED_TEXTURE = new BeveledRectTexture(
            0xFF1D2026,
            UIScreenTheme.BattlepassTasks.BORDER_DARK,
            0xFF444A56,
            0xFFC2C8D2,
            1,
            1
    );

    private final BattlePassUiSupport.BattlePassUiState state = new BattlePassUiSupport.BattlePassUiState();
    private final Button rewardsTabButton;
    private final Button missionsTabButton;
    private Button claimAllButton;
    private BattlePassUiSupport.BattlePassTab styledTab = null;

    BattlePassScreenElement() {
        layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
            layout.justifyContent(AlignContent.CENTER);
            layout.alignItems(AlignItems.CENTER);
        });
        style(style -> style.backgroundTexture(RectTexture.of(0x72000000)));
        internalSetup();

        UIElement window = BattlePassSurfaceElement.window().layout(layout -> {
            layout.widthPercent(96);
            layout.heightPercent(94);
            layout.maxWidth(BattlePassUiSupport.TARGET_WINDOW_WIDTH);
            layout.maxHeight(BattlePassUiSupport.TARGET_WINDOW_HEIGHT);
            layout.minWidth(BattlePassUiSupport.MIN_WINDOW_WIDTH);
            layout.minHeight(BattlePassUiSupport.MIN_WINDOW_HEIGHT);
            layout.flexDirection(dev.vfyjxf.taffy.style.FlexDirection.COLUMN);
            layout.gapAll(8);
            layout.paddingAll(10);
        });
        UIElement header = INCoreLdLibUiScaffold.column().layout(layout -> {
            layout.widthPercent(100);
            layout.gapAll(2);
        });
        UIElement headerRow = INCoreLdLibUiScaffold.row().layout(layout -> {
            layout.alignItems(AlignItems.CENTER);
            layout.gapAll(6);
        });
        UIElement headerDivider = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.height(1);
        }).style(style -> style.backgroundTexture(RectTexture.of(UIScreenTheme.BattlepassTasks.TAB_DIVIDER)));

        UIElement body = INCoreLdLibUiScaffold.column().layout(layout -> {
            layout.flex(1);
            layout.widthPercent(100);
            layout.minHeight(0);
            layout.gapAll(8);
        });

        UIElement tabRow = INCoreLdLibUiScaffold.row().layout(layout -> {
            layout.gapAll(2);
            layout.alignItems(AlignItems.CENTER);
        });
        this.rewardsTabButton = createTabButton(
                Component.translatable("screen.incore.battle_pass.tab_rewards"),
                BattlePassUiSupport.BattlePassTab.REWARDS,
                126
        );
        this.missionsTabButton = createTabButton(
                Component.translatable("screen.incore.battle_pass.tab_missions"),
                BattlePassUiSupport.BattlePassTab.MISSIONS,
                126
        );
        tabRow.addChildren(this.rewardsTabButton, this.missionsTabButton);

        headerRow.addChildren(
                tabRow,
                INCoreLdLibUiScaffold.spacer()
        );
        header.addChildren(headerRow, headerDivider);

        body.addChildren(
                new BattlePassContentElement(this.state).layout(layout -> {
                    layout.flex(1);
                    layout.widthPercent(100);
                    layout.minHeight(0);
                }),
                footerRow()
        );
        window.addChildren(header, body);
        addChild(window);
        refreshControls();
        addEventListener(UIEvents.TICK, event -> refreshControls());
    }

    private Button createTabButton(Component text, BattlePassUiSupport.BattlePassTab tab, float width) {
        Button button = new Button().setText(text).setOnClick(event -> this.state.setActiveTab(tab));
        button.layout(layout -> {
            layout.width(width);
            layout.height(18);
            layout.alignItems(AlignItems.CENTER);
            layout.justifyContent(AlignContent.CENTER);
        });
        button.text.getLayout().flex(1);
        button.text.getLayout().heightPercent(100);
        button.textStyle(style -> {
            style.adaptiveWidth(false);
            style.textWrap(TextWrap.HIDE);
            style.textAlignHorizontal(Horizontal.CENTER);
        });
        return button;
    }

    private UIElement footerRow() {
        UIElement footer = INCoreLdLibUiScaffold.row().layout(layout -> {
            layout.widthPercent(100);
            layout.justifyContent(AlignContent.SPACE_BETWEEN);
            layout.alignItems(AlignItems.CENTER);
        });

        Button closeButton = INCoreLdLibUiScaffold.actionButton(Component.translatable("gui.close"));
        closeButton.layout(layout -> {
            layout.width(88);
            layout.height(20);
        });
        closeButton.buttonStyle(style -> style
                .baseTexture(CLAIM_IDLE_TEXTURE)
                .hoverTexture(CLAIM_HOVER_TEXTURE)
                .pressedTexture(CLAIM_PRESSED_TEXTURE)
        );
        closeButton.textStyle(style -> style
                .adaptiveWidth(false)
                .textWrap(TextWrap.HIDE)
                .textAlignHorizontal(Horizontal.CENTER)
                .textColor(UIScreenTheme.BattlepassTasks.TEXT_PRIMARY)
        );
        closeButton.setOnClick(event -> PacketDistributor.sendToServer(RequestBackIncoreUiPayload.INSTANCE));

        this.claimAllButton = INCoreLdLibUiScaffold.actionButton(Component.translatable("screen.incore.battle_pass.claim_all"));
        this.claimAllButton.layout(layout -> {
            layout.width(176);
            layout.height(20);
        });
        this.claimAllButton.buttonStyle(style -> style
                .baseTexture(CLAIM_IDLE_TEXTURE)
                .hoverTexture(CLAIM_HOVER_TEXTURE)
                .pressedTexture(CLAIM_PRESSED_TEXTURE)
        );
        this.claimAllButton.textStyle(style -> style
                .adaptiveWidth(false)
                .textWrap(TextWrap.HIDE)
                .textAlignHorizontal(Horizontal.CENTER)
                .textColor(UIScreenTheme.BattlepassTasks.TEXT_PRIMARY)
        );
        this.claimAllButton.setOnClick(event -> BattlePassNetworking.requestClaimAllRewards());
        footer.addChildren(closeButton, this.claimAllButton);
        return footer;
    }

    private void refreshControls() {
        BattlePassUiSupport.BattlePassTab activeTab = this.state.activeTab();
        if (this.styledTab != activeTab) {
            styleTabButton(this.rewardsTabButton, activeTab == BattlePassUiSupport.BattlePassTab.REWARDS);
            styleTabButton(this.missionsTabButton, activeTab == BattlePassUiSupport.BattlePassTab.MISSIONS);
            this.styledTab = activeTab;
        }

        int unclaimedRewardLevels = BattlePassClientCache.getUnclaimedRewardLevels();
        this.claimAllButton.setActive(BattlePassClientCache.hasActiveSet() && unclaimedRewardLevels > 0);
        this.claimAllButton.setText(unclaimedRewardLevels > 0
                ? Component.translatable("screen.incore.battle_pass.claim_all_count", unclaimedRewardLevels)
                : Component.translatable("screen.incore.battle_pass.claim_all"));
    }

    private void styleTabButton(Button button, boolean selected) {
        button.buttonStyle(style -> {
            if (selected) {
                style.baseTexture(TAB_SELECTED_TEXTURE)
                        .hoverTexture(TAB_SELECTED_TEXTURE)
                        .pressedTexture(TAB_SELECTED_TEXTURE);
            } else {
                style.baseTexture(TAB_IDLE_TEXTURE)
                        .hoverTexture(TAB_HOVER_TEXTURE)
                        .pressedTexture(TAB_HOVER_TEXTURE);
            }
        });
        button.textStyle(style -> {
            style.adaptiveWidth(false);
            style.textWrap(TextWrap.HIDE);
            style.textAlignHorizontal(Horizontal.CENTER);
            style.textColor(selected
                    ? UIScreenTheme.BattlepassTasks.TAB_TEXT_SELECTED
                    : UIScreenTheme.BattlepassTasks.TAB_TEXT_DEFAULT);
        });
    }
}
