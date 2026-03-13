package io.github.ozokuz.incore.client.features.status;

import io.github.ozokuz.incore.Registration;
import io.github.ozokuz.incore.client.INCoreKeyMappings;
import io.github.ozokuz.incore.client.features.battlepass.BattlePassScreen;
import io.github.ozokuz.incore.client.features.party.PartyManagementScreen;
import io.github.ozokuz.incore.client.features.tasks.TaskOverviewScreen;
import io.github.ozokuz.incore.client.ui.UIScreenTheme;
import io.github.ozokuz.incore.client.ui.render.ThemedUi;
import io.github.ozokuz.incore.features.arena.network.ArenaNetworking;
import io.github.ozokuz.incore.features.gacha.network.GachaNetworking;
import io.github.ozokuz.incore.features.market.network.MarketNetworking;
import io.github.ozokuz.incore.features.numismatics.network.NumismaticsNetworking;
import io.github.ozokuz.incore.features.playerlevel.PlayerFeatureUnlockIds;
import io.github.ozokuz.incore.features.playerlevel.network.PlayerLevelClientCache;
import io.github.ozokuz.incore.features.entropy.EntropyClientCache;
import io.github.ozokuz.incore.client.features.research.ResearchTreeScreen;
import io.github.ozokuz.incore.features.research.network.ResearchNetworking;
import io.github.ozokuz.incore.features.shop.network.ShopNetworking;
import io.github.ozokuz.incore.features.status.network.PlayerStatusCurrencyClientCache;
import io.github.ozokuz.incore.features.status.network.PlayerStatusNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PlayerStatusScreen extends Screen {
    private static final UIScreenTheme THEME = UIScreenTheme.INFO;
    private static final int TARGET_WINDOW_WIDTH = 560;
    private static final int TARGET_WINDOW_HEIGHT = 328;
    private static final int XP_BAR_HEIGHT = 5;
    private static final int BUTTON_HEIGHT = 20;
    private static final int QUICK_NAV_BUTTON_HEIGHT = 30;
    private static final int QUICK_NAV_BUTTON_GAP = 8;
    private static final float COST_SCALE = 0.75F;
    private static final ResourceLocation XP_BAR_BACKGROUND = ResourceLocation.parse("incore:hud/experience_bar_background_white");
    private static final ResourceLocation XP_BAR_PROGRESS = ResourceLocation.parse("incore:hud/experience_bar_progress_white");

    private Integer previousMenuBlur;
    private final List<QuickNavButton> quickNavButtons = new ArrayList<>();
    private Button combatCatalogButton;

    public PlayerStatusScreen() {
        super(Component.translatable("screen.incore.player_status.title"));
    }

    @Override
    protected void init() {
        if (this.previousMenuBlur == null) {
            this.previousMenuBlur = this.minecraft.options.getMenuBackgroundBlurriness();
            if (this.previousMenuBlur > 0) {
                this.minecraft.options.menuBackgroundBlurriness().set(0);
            }
        }

        this.clearWidgets();
        this.quickNavButtons.clear();

        Layout layout = layout();

        int buttonWidth = Math.max(126, Math.min(196, layout.entropyWidth() - 16));
        int buttonX = layout.entropyX() + (layout.entropyWidth() - buttonWidth) / 2;
        int rewardsButtonY = layout.levelY() + layout.levelHeight() - BUTTON_HEIGHT - 8;
        this.addRenderableWidget(Button.builder(
                        Component.translatable("screen.incore.player_status.open_rewards"),
                        button -> this.minecraft.setScreen(new PlayerLevelRewardsScreen(this))
                ).bounds(buttonX, rewardsButtonY, buttonWidth, BUTTON_HEIGHT)
                .build());

        int catalogButtonY = layout.entropyY() + layout.entropyHeight() - BUTTON_HEIGHT - 8;
        this.combatCatalogButton = null;
        if (isFeatureUnlocked(PlayerFeatureUnlockIds.ARENA_TIER_1.toString())) {
            this.combatCatalogButton = this.addRenderableWidget(Button.builder(
                            Component.translatable("screen.incore.player_status.open_combat_catalog"),
                            button -> {
                                StatusScreenReturnTracker.prepare(this);
                                ArenaNetworking.requestOpenCatalog();
                            }
                    ).bounds(buttonX, catalogButtonY, buttonWidth, BUTTON_HEIGHT)
                    .build());
        }
        this.addRenderableWidget(Button.builder(
                        Component.translatable("screen.incore.player_status.open_dungeon_settings"),
                        button -> this.minecraft.setScreen(new DungeonDifficultyScreen(this))
                ).bounds(buttonX, catalogButtonY - BUTTON_HEIGHT - 6, buttonWidth, BUTTON_HEIGHT)
                .build());

        addQuickNavButtons(layout);
        PlayerStatusNetworking.requestCurrencySync();
    }

    @Override
    public void removed() {
        if (this.minecraft != null && this.previousMenuBlur != null) {
            this.minecraft.options.menuBackgroundBlurriness().set(this.previousMenuBlur);
        }
        this.previousMenuBlur = null;
        super.removed();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        Layout layout = layout();
        ThemedUi ui = themed(guiGraphics);

        ui.drawBackdrop(this.width, this.height);
        ui.drawWindow(layout.windowLeft(), layout.windowTop(), layout.windowWidth(), layout.windowHeight());

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawString(this.font, this.title, layout.windowLeft() + 12, layout.windowTop() + 10, UIScreenTheme.Info.TITLE_TEXT, false);
        drawHeaderCurrencies(guiGraphics, layout);

        drawCard(guiGraphics, layout.levelX(), layout.levelY(), layout.levelWidth(), layout.levelHeight());
        drawCard(guiGraphics, layout.entropyX(), layout.entropyY(), layout.entropyWidth(), layout.entropyHeight());
        drawCard(guiGraphics, layout.quickNavX(), layout.quickNavY(), layout.quickNavWidth(), layout.quickNavHeight());

        drawLevelCard(guiGraphics, layout);
        drawEntropyCard(guiGraphics, layout);

        guiGraphics.drawString(
                this.font,
                Component.translatable("screen.incore.player_status.section_navigation"),
                layout.quickNavX() + 8,
                layout.quickNavY() + 8,
                UIScreenTheme.Info.PRIMARY_TEXT,
                false
        );

        QuickNavButton hovered = null;
        for (QuickNavButton navButton : this.quickNavButtons) {
            Button button = navButton.button();
            int iconX = button.getX() + 6;
            int iconY = button.getY() + (button.getHeight() - 16) / 2;
            guiGraphics.renderItem(navButton.icon(), iconX, iconY);

            int labelX = button.getX() + 26;
            int labelY = button.getY() + (button.getHeight() - this.font.lineHeight) / 2 + 1;
            int labelMaxWidth = Math.max(24, button.getWidth() - 30);
            Component label = Component.literal(this.font.plainSubstrByWidth(navButton.label().getString(), labelMaxWidth));
            guiGraphics.drawString(this.font, label, labelX, labelY, UIScreenTheme.Info.STATUS_SECTION_TEXT, false);

            if (mouseX >= button.getX()
                    && mouseX < button.getX() + button.getWidth()
                    && mouseY >= button.getY()
                    && mouseY < button.getY() + button.getHeight()) {
                hovered = navButton;
            }
        }

        if (hovered != null) {
            List<Component> lines = new ArrayList<>();
            lines.add(hovered.label());
            if (hovered.keyMapping() != null) {
                lines.add(Component.translatable("screen.incore.player_status.quick_nav_key", hovered.keyMapping().getTranslatedKeyMessage())
                        .withStyle(ChatFormatting.GRAY));
            }
            guiGraphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
        }
    }

    private void drawHeaderCurrencies(GuiGraphics guiGraphics, Layout layout) {
        PlayerStatusCurrencyClientCache.Snapshot snapshot = PlayerStatusCurrencyClientCache.snapshot();
        List<CostRenderLine> lines = snapshot.entries().stream()
                .map(entry -> new CostRenderLine(iconFromId(entry.iconItemId()), "x" + Math.max(0, entry.amount()), UIScreenTheme.Info.STATUS_BALANCE_TEXT))
                .toList();

        int rightX = layout.windowLeft() + layout.windowWidth() - 12;
        int minX = layout.windowLeft() + 140;
        int y = layout.windowTop() + 8;

        if (lines.isEmpty()) {
            Component text = snapshot.loaded()
                    ? Component.translatable("screen.incore.player_status.currencies_none")
                    : Component.translatable("screen.incore.player_status.currencies_loading");
            int textX = Math.max(minX, rightX - this.font.width(text));
            guiGraphics.drawString(this.font, text, textX, y + 1, UIScreenTheme.Info.STATUS_LINE_TEXT, false);
            return;
        }

        int cursorRight = rightX;
        int rendered = 0;
        for (int i = lines.size() - 1; i >= 0; i--) {
            CostRenderLine line = lines.get(i);
            int width = scaledCostLineWidth(line);
            int x = cursorRight - width;
            if (x < minX) {
                break;
            }
            renderCostLine(guiGraphics, x, y, line);
            cursorRight = x - 4;
            rendered++;
        }

        Component label = Component.translatable("screen.incore.player_status.section_currencies");
        int labelWidth = this.font.width(label);
        if (cursorRight - labelWidth >= minX) {
            guiGraphics.drawString(this.font, label, cursorRight - labelWidth, y + 2, UIScreenTheme.Info.PRIMARY_TEXT, false);
            cursorRight -= labelWidth + 4;
        }

        if (rendered < lines.size()) {
            Component overflow = Component.translatable("screen.incore.player_status.currencies_overflow", lines.size() - rendered);
            int overflowWidth = this.font.width(overflow);
            int overflowX = Math.max(minX, cursorRight - overflowWidth);
            guiGraphics.drawString(this.font, overflow, overflowX, y + 4, UIScreenTheme.Info.STATUS_OVERFLOW_TEXT, false);
        }
    }

    private void drawEntropyCard(GuiGraphics guiGraphics, Layout layout) {
        int cardX = layout.entropyX();
        int cardY = layout.entropyY();
        int cardWidth = layout.entropyWidth();

        int entropy = Math.max(0, EntropyClientCache.getCurrent());
        int cap = Math.max(1, EntropyClientCache.getCap());
        entropy = Math.min(entropy, cap);

        guiGraphics.drawString(this.font, Component.translatable("screen.incore.player_status.entropy"), cardX + 8, cardY + 8, UIScreenTheme.Info.PRIMARY_TEXT, false);

        int meterX = cardX + 8;
        int meterY = cardY + 24;
        int meterWidth = Math.max(70, cardWidth - 16);
        guiGraphics.blitSprite(XP_BAR_BACKGROUND, meterX, meterY, meterWidth, XP_BAR_HEIGHT);

        float ratio = (float) entropy / (float) cap;
        int fillWidth = Math.max(0, Math.min(meterWidth, Math.round(meterWidth * ratio)));
        if (fillWidth > 0) {
            guiGraphics.enableScissor(meterX, meterY, meterX + fillWidth, meterY + XP_BAR_HEIGHT);
            guiGraphics.blitSprite(XP_BAR_PROGRESS, meterX, meterY, meterWidth, XP_BAR_HEIGHT);
            guiGraphics.disableScissor();
        }

        int infoY = meterY + 10;
        guiGraphics.drawString(this.font, Component.literal(entropy + " / " + cap), cardX + 8, infoY, UIScreenTheme.Info.WHITE_TEXT, false);
        guiGraphics.drawString(
                this.font,
                Component.translatable("screen.incore.player_status.next_gain", formatCountdown(EntropyClientCache.getMillisUntilNextIncrease())),
                cardX + 8,
                infoY + 14,
                UIScreenTheme.Info.SECONDARY_TEXT,
                false
        );
        guiGraphics.drawString(
                this.font,
                Component.translatable("screen.incore.player_status.full_in", formatCountdown(EntropyClientCache.getMillisUntilFull())),
                cardX + 8,
                infoY + 28,
                UIScreenTheme.Info.STATUS_TRACK_TEXT,
                false
        );
    }

    private void drawLevelCard(GuiGraphics guiGraphics, Layout layout) {
        int level = PlayerLevelClientCache.getLevel();
        int levelExperience = PlayerLevelClientCache.getCurrentExperience();
        int nextLevelCost = PlayerLevelClientCache.getExperienceToNextLevel();

        int textX = layout.levelX() + 8;
        int top = layout.levelY();
        guiGraphics.drawString(this.font, Component.translatable("screen.incore.player_status.section_level"), textX, top + 8, UIScreenTheme.Info.PRIMARY_TEXT, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.incore.player_status.level", level), textX, top + 22, UIScreenTheme.Info.WHITE_TEXT, false);
        guiGraphics.drawString(
                this.font,
                Component.translatable("screen.incore.player_status.level_progress", levelExperience, nextLevelCost),
                textX,
                top + 36,
                UIScreenTheme.Info.SECONDARY_TEXT,
                false
        );
        guiGraphics.drawString(this.font, Component.translatable("screen.incore.player_status.hint"), textX, top + 50, UIScreenTheme.Info.MUTED_TEXT, false);
    }

    private void addQuickNavButtons(Layout layout) {
        List<QuickNavTarget> targets = quickNavTargets();

        int innerWidth = Math.max(110, layout.quickNavWidth() - 16);
        int columns = innerWidth >= 260 ? 2 : 1;
        int buttonWidth = (innerWidth - ((columns - 1) * QUICK_NAV_BUTTON_GAP)) / columns;
        int rows = (int) Math.ceil(targets.size() / (double) columns);
        int totalHeight = rows * QUICK_NAV_BUTTON_HEIGHT + (rows - 1) * QUICK_NAV_BUTTON_GAP;
        int startX = layout.quickNavX() + 8;
        int startY = layout.quickNavY() + 24 + Math.max(0, (layout.quickNavHeight() - 32 - totalHeight) / 2);

        for (int i = 0; i < targets.size(); i++) {
            QuickNavTarget target = targets.get(i);
            int col = i % columns;
            int row = i / columns;
            int x = startX + col * (buttonWidth + QUICK_NAV_BUTTON_GAP);
            int y = startY + row * (QUICK_NAV_BUTTON_HEIGHT + QUICK_NAV_BUTTON_GAP);

            Button button = this.addRenderableWidget(Button.builder(Component.empty(), ignored -> target.action().run())
                    .bounds(x, y, buttonWidth, QUICK_NAV_BUTTON_HEIGHT)
                    .build());
            if (target.featureId() != null) {
                button.active = isFeatureUnlocked(target.featureId());
            }
            this.quickNavButtons.add(new QuickNavButton(button, target.icon(), target.label(), target.keyMapping(), target.featureId()));
        }
    }

    private List<QuickNavTarget> quickNavTargets() {
        return List.of(
                new QuickNavTarget(
                        Component.translatable("screen.incore.player_status.nav.gacha"),
                        INCoreKeyMappings.OPEN_GACHA_BANNERS,
                        Registration.GACHA_RIFT_BLOCK_ITEM.get().getDefaultInstance(),
                        PlayerFeatureUnlockIds.GACHA_BASIC.toString(),
                        () -> {
                            StatusScreenReturnTracker.prepare(this);
                            GachaNetworking.requestOpenBannerScreen();
                        }
                ),
                new QuickNavTarget(
                        Component.translatable("screen.incore.player_status.nav.tasks"),
                        INCoreKeyMappings.OPEN_TASK_OVERVIEW,
                        new ItemStack(Items.WRITABLE_BOOK),
                        PlayerFeatureUnlockIds.TASKS_SCREEN.toString(),
                        () -> this.minecraft.setScreen(new TaskOverviewScreen(this))
                ),
                new QuickNavTarget(
                        Component.translatable("screen.incore.player_status.nav.research"),
                        INCoreKeyMappings.OPEN_RESEARCH_TREE,
                        Registration.RESEARCH_CONTROLLER_T1_BLOCK_ITEM.get().getDefaultInstance(),
                        null,
                        () -> {
                            StatusScreenReturnTracker.prepare(this);
                            this.minecraft.setScreen(new ResearchTreeScreen());
                            ResearchNetworking.requestSnapshot();
                        }
                ),
                new QuickNavTarget(
                        Component.translatable("screen.incore.player_status.nav.ftb_quests"),
                        null,
                        new ItemStack(Items.BOOK),
                        null,
                        this::openFtbQuests
                ),
                new QuickNavTarget(
                        Component.translatable("screen.incore.player_status.nav.battle_pass"),
                        INCoreKeyMappings.OPEN_BATTLE_PASS,
                        Registration.BATTLEPASS_LANE_UNLOCK_ITEM.get().getDefaultInstance(),
                        PlayerFeatureUnlockIds.BATTLEPASS_SCREEN.toString(),
                        () -> this.minecraft.setScreen(new BattlePassScreen(this))
                ),
                new QuickNavTarget(
                        Component.translatable("screen.incore.player_status.nav.market"),
                        INCoreKeyMappings.OPEN_MARKET,
                        Registration.MARKET_TERMINAL_BLOCK_ITEM.get().getDefaultInstance(),
                        PlayerFeatureUnlockIds.MARKET_BASIC.toString(),
                        () -> {
                            StatusScreenReturnTracker.prepare(this);
                            MarketNetworking.requestOpenMarketScreen();
                        }
                ),
                new QuickNavTarget(
                        Component.translatable("screen.incore.player_status.nav.shop"),
                        INCoreKeyMappings.OPEN_SHOP,
                        Registration.CARD_BOOSTER_BOX_ITEM.get().getDefaultInstance(),
                        PlayerFeatureUnlockIds.SHOP_SCREEN.toString(),
                        () -> {
                            StatusScreenReturnTracker.prepare(this);
                            ShopNetworking.requestOpenShopScreen();
                        }
                ),
                new QuickNavTarget(
                        Component.translatable("screen.incore.player_status.nav.ftb_teams"),
                        null,
                        new ItemStack(Items.NAME_TAG),
                        null,
                        this::openFtbTeams
                ),
                new QuickNavTarget(
                        Component.translatable("screen.incore.player_status.nav.numismatics_bank"),
                        INCoreKeyMappings.OPEN_NUMISMATICS_BANK,
                        iconOrDefault("numismatics:spur", new ItemStack(Items.GOLD_NUGGET)),
                        null,
                        () -> {
                            StatusScreenReturnTracker.prepareExternal(this);
                            NumismaticsNetworking.requestOpenBankScreen();
                        }
                ),
                new QuickNavTarget(
                        Component.translatable("screen.incore.player_status.nav.party"),
                        INCoreKeyMappings.OPEN_PARTY,
                        new ItemStack(Items.PLAYER_HEAD),
                        null,
                        () -> this.minecraft.setScreen(new PartyManagementScreen(this))
                )
        ).stream()
                .filter(Objects::nonNull)
                .filter(target -> target.featureId() == null || isFeatureUnlocked(target.featureId()))
                .toList();
    }

    private boolean isFeatureUnlocked(String featureId) {
        return PlayerLevelClientCache.isFeatureUnlocked(featureId);
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

    private static ItemStack iconOrDefault(String itemIdString, ItemStack fallback) {
        ItemStack stack = iconFromId(itemIdString);
        return stack.isEmpty() ? fallback : stack;
    }

    private void openFtbQuests() {
        invokeStaticNoArgs("dev.ftb.mods.ftbquests.client.FTBQuestsClient", "openGui");
    }

    private void openFtbTeams() {
        invokeStaticNoArgs("dev.ftb.mods.ftbteams.net.OpenGUIMessage", "sendToServer");
    }

    private void invokeStaticNoArgs(String className, String methodName) {
        try {
            Class.forName(className).getMethod(methodName).invoke(null);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private void renderCostLine(GuiGraphics guiGraphics, int x, int y, CostRenderLine line) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0.0F);
        guiGraphics.pose().scale(COST_SCALE, COST_SCALE, 1.0F);

        int textX = 0;
        if (!line.stack().isEmpty()) {
            guiGraphics.renderItem(line.stack(), 0, 0);
            textX = 20;
        }

        guiGraphics.drawString(this.font, Component.literal(line.text()), textX, 4, line.color(), false);
        guiGraphics.pose().popPose();
    }

    private int scaledCostLineWidth(CostRenderLine line) {
        int width = this.font.width(line.text());
        if (!line.stack().isEmpty()) {
            width += 20;
        }
        return (int) Math.ceil(width * COST_SCALE);
    }

    private void drawMainPanel(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        themed(guiGraphics).drawWindow(x, y, width, height);
    }

    private void drawCard(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        themed(guiGraphics).drawCard(x, y, width, height);
    }

    private Layout layout() {
        int windowWidth = this.windowWidth();
        int windowHeight = this.windowHeight();
        int windowLeft = this.windowLeft();
        int windowTop = this.windowTop();

        int contentX = windowLeft + 12;
        int contentY = windowTop + 28;
        int contentWidth = windowWidth - 24;
        int contentHeight = windowHeight - 40;

        int leftWidth = Math.max(170, Math.min(240, (contentWidth * 42) / 100));
        int rightWidth = contentWidth - leftWidth - 8;
        if (rightWidth < 150) {
            int needed = 150 - rightWidth;
            leftWidth = Math.max(150, leftWidth - needed);
            rightWidth = contentWidth - leftWidth - 8;
        }

        int leftX = contentX;
        int rightX = leftX + leftWidth + 8;

        int levelHeight = Math.max(90, Math.min(110, contentHeight / 3));
        int entropyHeight = contentHeight - levelHeight - 8;

        return new Layout(
                windowLeft,
                windowTop,
                windowWidth,
                windowHeight,
                leftX,
                contentY,
                leftWidth,
                levelHeight,
                entropyHeight,
                rightX,
                rightWidth,
                contentY,
                contentHeight
        );
    }

    private int windowLeft() {
        return (this.width - this.windowWidth()) / 2;
    }

    private int windowTop() {
        return (this.height - this.windowHeight()) / 2;
    }

    private int windowWidth() {
        return Math.min(TARGET_WINDOW_WIDTH, Math.max(360, this.width - 24));
    }

    private int windowHeight() {
        return Math.min(TARGET_WINDOW_HEIGHT, Math.max(260, this.height - 24));
    }

    private Component formatCountdown(long millis) {
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
            return Component.literal(String.format("%d:%02d:%02d", hours, minutes, seconds));
        }

        return Component.literal(String.format("%02d:%02d", minutes, seconds));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record CostRenderLine(ItemStack stack, String text, int color) {
    }

    private record QuickNavTarget(Component label, KeyMapping keyMapping, ItemStack icon, String featureId, Runnable action) {
    }

    private record QuickNavButton(Button button, ItemStack icon, Component label, KeyMapping keyMapping, String featureId) {
    }

    private record Layout(
            int windowLeft,
            int windowTop,
            int windowWidth,
            int windowHeight,
            int leftX,
            int leftY,
            int leftWidth,
            int leftLevelHeight,
            int leftEntropyHeight,
            int rightX,
            int rightWidth,
            int rightY,
            int rightHeight
    ) {
        int levelX() {
            return leftX;
        }

        int levelY() {
            return leftY;
        }

        int levelWidth() {
            return leftWidth;
        }

        int levelHeight() {
            return leftLevelHeight;
        }

        int entropyX() {
            return leftX;
        }

        int entropyY() {
            return leftY + leftLevelHeight + 8;
        }

        int entropyWidth() {
            return leftWidth;
        }

        int entropyHeight() {
            return leftEntropyHeight;
        }

        int quickNavX() {
            return rightX;
        }

        int quickNavY() {
            return rightY;
        }

        int quickNavWidth() {
            return rightWidth;
        }

        int quickNavHeight() {
            return rightHeight;
        }
    }

    private ThemedUi themed(GuiGraphics guiGraphics) {
        return new ThemedUi(guiGraphics, this.font, THEME.theme());
    }
}
