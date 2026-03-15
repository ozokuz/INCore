package ozokuz.incore.integration.ldlib.ui.player;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import ozokuz.incore.Registration;
import ozokuz.incore.client.integration.ldlib.INCoreStatusUiClientActions;
import ozokuz.incore.features.entropy.EntropyManager;
import ozokuz.incore.features.playerlevel.PlayerFeatureUnlockIds;
import ozokuz.incore.features.playerlevel.PlayerFeatureUnlockService;
import ozokuz.incore.features.playerlevel.PlayerLevelManager;
import ozokuz.incore.features.playerlevel.network.PlayerLevelClientCache;
import ozokuz.incore.features.status.network.PlayerStatusNetworking;
import ozokuz.incore.integration.ldlib.ui.INCoreLdLibUiScaffold;
import ozokuz.incore.integration.ldlib.ui.INCorePlayerUiNavigator;
import ozokuz.incore.integration.ldlib.ui.INCoreUiIds;

public final class PlayerStatusUiHolder implements PlayerUIMenuType.PlayerUIHolder {
    private static final int TARGET_WINDOW_WIDTH = 560;
    private static final int TARGET_WINDOW_HEIGHT = 328;
    private static final int MIN_WINDOW_WIDTH = 360;
    private static final int MIN_WINDOW_HEIGHT = 260;
    private static final float LEFT_COLUMN_WIDTH_PERCENT = 42.0F;

    @Override
    public ModularUI createUI(Player player) {
        if (player.level().isClientSide()) {
            PlayerStatusNetworking.requestCurrencySync();
        }

        var window = INCoreLdLibUiScaffold.createWindowShell(TARGET_WINDOW_WIDTH, TARGET_WINDOW_HEIGHT);
        window.window().getLayout().widthPercent(94);
        window.window().getLayout().heightPercent(94);
        window.window().getLayout().maxWidth(TARGET_WINDOW_WIDTH);
        window.window().getLayout().maxHeight(TARGET_WINDOW_HEIGHT);
        window.window().getLayout().minWidth(MIN_WINDOW_WIDTH);
        window.window().getLayout().minHeight(MIN_WINDOW_HEIGHT);

        Label titleLabel = INCoreLdLibUiScaffold.titleLabel(Component.translatable("screen.incore.player_status.title"));
        titleLabel.getLayout().flex(1);

        PlayerStatusCurrencyStripElement balances = new PlayerStatusCurrencyStripElement();
        balances.getLayout().flex(1);
        balances.getLayout().height(16);
        balances.getLayout().minWidth(132);

        window.header().addChildren(titleLabel, balances);

        UIElement content = INCoreLdLibUiScaffold.row();
        content.getLayout().flex(1);
        content.getLayout().gapAll(8);
        content.getLayout().alignItems(AlignItems.STRETCH);

        UIElement leftColumn = INCoreLdLibUiScaffold.column();
        leftColumn.getLayout().flexBasisPercent(LEFT_COLUMN_WIDTH_PERCENT);
        leftColumn.getLayout().minWidth(170);
        leftColumn.getLayout().maxWidth(240);
        leftColumn.getLayout().heightPercent(100);
        leftColumn.getLayout().gapAll(8);

        UIElement rightColumn = INCoreLdLibUiScaffold.column();
        rightColumn.getLayout().flex(1);
        rightColumn.getLayout().heightPercent(100);

        var levelSection = createLevelSection(player);
        levelSection.root().getLayout().minHeight(94);

        var entropySection = createEntropySection(player);
        entropySection.root().getLayout().flex(1);

        var navSection = createQuickNavSection(player);
        navSection.root().getLayout().flex(1);

        leftColumn.addChildren(levelSection.root(), entropySection.root());
        rightColumn.addChild(navSection.root());
        content.addChildren(leftColumn, rightColumn);
        window.body().addChild(content);
        return INCoreLdLibUiScaffold.build(player, window.root());
    }

    private static INCoreLdLibUiScaffold.SectionScaffold createLevelSection(Player player) {
        var section = INCoreLdLibUiScaffold.createSection(Component.translatable("screen.incore.player_status.section_level"));
        section.body().getLayout().flex(1);

        Label levelLabel = statusValueLabel(Component.literal("..."));
        levelLabel.bind(DataBindingBuilder.componentS2C(() -> levelLine(player)).build());

        Label xpLabel = statusSecondaryLabel(Component.literal("..."));
        xpLabel.bind(DataBindingBuilder.componentS2C(() -> xpLine(player)).build());

        Label hintLabel = statusMutedLabel(Component.translatable("screen.incore.player_status.hint"));

        Button rewardsButton = INCoreLdLibUiScaffold.actionButton(
                Component.translatable("screen.incore.player_status.open_rewards")
        );
        rewardsButton.setOnServerClick(event -> {
            if (player instanceof ServerPlayer serverPlayer) {
                INCorePlayerUiNavigator.pushAndOpen(serverPlayer, INCoreUiIds.PLAYER_LEVEL_REWARDS);
            }
        });

        section.body().addChildren(levelLabel, xpLabel, hintLabel, INCoreLdLibUiScaffold.spacer(), rewardsButton);
        return section;
    }

    private static INCoreLdLibUiScaffold.SectionScaffold createEntropySection(Player player) {
        var section = INCoreLdLibUiScaffold.createSection(Component.translatable("screen.incore.player_status.entropy"));
        section.body().getLayout().flex(1);

        ProgressBar entropyBar = INCoreLdLibUiScaffold.slimProgressBar();
        entropyBar.bind(DataBindingBuilder.floatValS2C(() -> entropyRatio(player)).build());

        Label amountLabel = statusValueLabel(Component.literal("..."));
        amountLabel.bind(DataBindingBuilder.componentS2C(() -> entropyAmountLine(player)).build());

        Label nextGainLabel = statusSecondaryLabel(Component.literal("..."));
        nextGainLabel.bind(DataBindingBuilder.componentS2C(() -> nextGainLine(player)).build());

        Label fullLabel = statusTrackLabel(Component.literal("..."));
        fullLabel.bind(DataBindingBuilder.componentS2C(() -> fullEntropyLine(player)).build());

        Button difficultyButton = INCoreLdLibUiScaffold.actionButton(
                Component.translatable("screen.incore.player_status.open_dungeon_settings")
        );
        difficultyButton.setOnServerClick(event -> {
            if (player instanceof ServerPlayer serverPlayer) {
                INCorePlayerUiNavigator.pushAndOpen(serverPlayer, INCoreUiIds.DUNGEON_DIFFICULTY);
            }
        });

        Button combatCatalogButton = INCoreLdLibUiScaffold.actionButton(
                Component.translatable("screen.incore.player_status.open_combat_catalog")
        );
        applyFeatureVisibility(player, combatCatalogButton, PlayerFeatureUnlockIds.ARENA_TIER_1);
        if (player.level().isClientSide()) {
            INCoreStatusUiClientActions.bindAction(
                    combatCatalogButton,
                    PlayerStatusAction.COMBAT_CATALOG,
                    Component.translatable("screen.incore.player_status.open_combat_catalog")
            );
        }

        section.body().addChildren(
                entropyBar,
                amountLabel,
                nextGainLabel,
                fullLabel,
                INCoreLdLibUiScaffold.spacer(),
                difficultyButton,
                combatCatalogButton
        );
        return section;
    }

    private static INCoreLdLibUiScaffold.SectionScaffold createQuickNavSection(Player player) {
        var section = INCoreLdLibUiScaffold.createSection(Component.translatable("screen.incore.player_status.section_navigation"));

        UIElement buttonGrid = new UIElement();
        buttonGrid.getLayout().widthPercent(100);
        buttonGrid.getLayout().flexDirection(FlexDirection.ROW);
        buttonGrid.getLayout().flexWrap(FlexWrap.WRAP);
        buttonGrid.getLayout().gapAll(8);
        buttonGrid.getLayout().alignItems(AlignItems.STRETCH);
        buttonGrid.getLayout().alignContent(AlignContent.FLEX_START);

        for (QuickNavTarget target : quickNavTargets()) {
            Button button = quickNavButton(target);
            applyFeatureVisibility(player, button, target.featureId());
            if (player.level().isClientSide()) {
                INCoreStatusUiClientActions.bindAction(button, target.action(), target.label());
            }
            buttonGrid.addChild(button);
        }

        section.body().addChild(buttonGrid);
        return section;
    }

    private static Button quickNavButton(QuickNavTarget target) {
        Button button = INCoreLdLibUiScaffold.actionButton(target.label(), 30);
        button.addClass("incore-quick-nav-button");
        button.getLayout().widthAuto();
        button.getLayout().flexGrow(1);
        button.getLayout().flexBasis(132);
        button.getLayout().minWidth(122);
        button.getLayout().justifyContent(AlignContent.FLEX_START);
        button.getLayout().alignItems(AlignItems.CENTER);
        button.getLayout().paddingHorizontal(6);
        button.getLayout().gapAll(4);
        button.text.getLayout().flex(1);
        button.textStyle(style -> {
            style.textWrap(TextWrap.HIDE);
            style.adaptiveWidth(false);
            style.textAlignHorizontal(Horizontal.LEFT);
        });

        UIElement icon = new UIElement();
        icon.getLayout().width(16);
        icon.getLayout().height(16);
        icon.getLayout().marginRight(4);
        icon.style(style -> style.backgroundTexture(new ItemStackTexture(target.icon())));
        icon.setAllowHitTest(false);
        button.addChildAt(icon, 0);
        return button;
    }

    private static Label statusValueLabel(Component text) {
        Label label = INCoreLdLibUiScaffold.wrappedLabel(text);
        label.addClass("incore-status-value");
        label.textStyle(style -> style.fontSize(9).textWrap(TextWrap.HIDE));
        return label;
    }

    private static Label statusSecondaryLabel(Component text) {
        Label label = INCoreLdLibUiScaffold.wrappedLabel(text);
        label.addClass("incore-status-secondary");
        label.textStyle(style -> style.textWrap(TextWrap.HIDE));
        return label;
    }

    private static Label statusMutedLabel(Component text) {
        Label label = INCoreLdLibUiScaffold.wrappedLabel(text);
        label.addClass("incore-status-muted");
        label.textStyle(style -> style.textWrap(TextWrap.HIDE));
        return label;
    }

    private static Label statusTrackLabel(Component text) {
        Label label = INCoreLdLibUiScaffold.wrappedLabel(text);
        label.addClass("incore-status-track");
        label.textStyle(style -> style.textWrap(TextWrap.HIDE));
        return label;
    }

    private static void applyFeatureVisibility(Player player, UIElement element, ResourceLocation featureId) {
        if (featureId == null) {
            return;
        }
        element.setDisplay(isFeatureUnlocked(player, featureId));
    }

    private static boolean isFeatureUnlocked(Player player, ResourceLocation featureId) {
        if (player instanceof ServerPlayer serverPlayer) {
            return PlayerFeatureUnlockService.hasUnlocked(serverPlayer, featureId);
        }
        return PlayerLevelClientCache.isFeatureUnlocked(featureId.toString());
    }

    private static Component levelLine(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return Component.literal("...");
        }
        return Component.translatable("screen.incore.player_status.level", PlayerLevelManager.getLevel(serverPlayer));
    }

    private static Component xpLine(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return Component.literal("...");
        }
        return Component.translatable(
                "screen.incore.player_status.level_progress",
                PlayerLevelManager.getCurrentExperience(serverPlayer),
                PlayerLevelManager.getExperienceToNextLevel(serverPlayer)
        );
    }

    private static float entropyRatio(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return 0.0F;
        }
        int cap = Math.max(1, EntropyManager.getEntropyCap(serverPlayer));
        int current = Math.max(0, Math.min(cap, EntropyManager.getCurrentEntropy(serverPlayer)));
        return current / (float) cap;
    }

    private static Component entropyAmountLine(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return Component.literal("...");
        }
        int cap = Math.max(1, EntropyManager.getEntropyCap(serverPlayer));
        int current = Math.max(0, Math.min(cap, EntropyManager.getCurrentEntropy(serverPlayer)));
        return Component.literal(current + " / " + cap);
    }

    private static Component nextGainLine(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return Component.literal("...");
        }
        return Component.translatable(
                "screen.incore.player_status.next_gain",
                formatCountdown(EntropyManager.getMillisUntilNextIncrease(serverPlayer))
        );
    }

    private static Component fullEntropyLine(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return Component.literal("...");
        }
        return Component.translatable(
                "screen.incore.player_status.full_in",
                formatCountdown(EntropyManager.getMillisUntilFull(serverPlayer))
        );
    }

    private static Component formatCountdown(long millis) {
        if (millis < 0L) {
            return Component.translatable("screen.incore.player_status.timer.paused");
        }
        if (millis == 0L) {
            return Component.translatable("screen.incore.player_status.timer.full");
        }

        long totalSeconds = Math.max(1L, (millis + 999L) / 1000L);
        long seconds = totalSeconds % 60L;
        long minutes = (totalSeconds / 60L) % 60L;
        long hours = totalSeconds / 3600L;
        if (hours > 0L) {
            return Component.literal("%d:%02d:%02d".formatted(hours, minutes, seconds));
        }
        return Component.literal("%02d:%02d".formatted(minutes, seconds));
    }

    private static List<QuickNavTarget> quickNavTargets() {
        return List.of(
                new QuickNavTarget(
                        Component.translatable("screen.incore.player_status.nav.gacha"),
                        Registration.GACHA_RIFT_BLOCK_ITEM.get().getDefaultInstance(),
                        PlayerFeatureUnlockIds.GACHA_BASIC,
                        PlayerStatusAction.GACHA
                ),
                new QuickNavTarget(
                        Component.translatable("screen.incore.player_status.nav.tasks"),
                        new ItemStack(Items.WRITABLE_BOOK),
                        PlayerFeatureUnlockIds.TASKS_SCREEN,
                        PlayerStatusAction.TASKS
                ),
                new QuickNavTarget(
                        Component.translatable("screen.incore.player_status.nav.research"),
                        Registration.RESEARCH_CONTROLLER_T1_BLOCK_ITEM.get().getDefaultInstance(),
                        null,
                        PlayerStatusAction.RESEARCH
                ),
                new QuickNavTarget(
                        Component.translatable("screen.incore.player_status.nav.ftb_quests"),
                        new ItemStack(Items.BOOK),
                        null,
                        PlayerStatusAction.FTB_QUESTS
                ),
                new QuickNavTarget(
                        Component.translatable("screen.incore.player_status.nav.battle_pass"),
                        Registration.BATTLEPASS_LANE_UNLOCK_ITEM.get().getDefaultInstance(),
                        PlayerFeatureUnlockIds.BATTLEPASS_SCREEN,
                        PlayerStatusAction.BATTLE_PASS
                ),
                new QuickNavTarget(
                        Component.translatable("screen.incore.player_status.nav.market"),
                        Registration.MARKET_TERMINAL_BLOCK_ITEM.get().getDefaultInstance(),
                        PlayerFeatureUnlockIds.MARKET_BASIC,
                        PlayerStatusAction.MARKET
                ),
                new QuickNavTarget(
                        Component.translatable("screen.incore.player_status.nav.shop"),
                        Registration.CARD_BOOSTER_BOX_ITEM.get().getDefaultInstance(),
                        PlayerFeatureUnlockIds.SHOP_SCREEN,
                        PlayerStatusAction.SHOP
                ),
                new QuickNavTarget(
                        Component.translatable("screen.incore.player_status.nav.ftb_teams"),
                        new ItemStack(Items.NAME_TAG),
                        null,
                        PlayerStatusAction.FTB_TEAMS
                ),
                new QuickNavTarget(
                        Component.translatable("screen.incore.player_status.nav.numismatics_bank"),
                        iconOrDefault("numismatics:spur", new ItemStack(Items.GOLD_NUGGET)),
                        null,
                        PlayerStatusAction.NUMISMATICS
                ),
                new QuickNavTarget(
                        Component.translatable("screen.incore.player_status.nav.party"),
                        new ItemStack(Items.PLAYER_HEAD),
                        null,
                        PlayerStatusAction.PARTY
                )
        );
    }

    private static ItemStack iconOrDefault(String itemIdString, ItemStack fallback) {
        ItemStack stack = iconFromId(itemIdString);
        return stack.isEmpty() ? fallback : stack;
    }

    private static ItemStack iconFromId(String itemIdString) {
        if (itemIdString == null || itemIdString.isBlank()) {
            return ItemStack.EMPTY;
        }
        ResourceLocation itemId = ResourceLocation.tryParse(itemIdString);
        if (itemId == null) {
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.get(itemId);
        return item == Items.AIR ? ItemStack.EMPTY : item.getDefaultInstance();
    }

    private record QuickNavTarget(
            Component label,
            ItemStack icon,
            ResourceLocation featureId,
            PlayerStatusAction action
    ) {
    }
}
