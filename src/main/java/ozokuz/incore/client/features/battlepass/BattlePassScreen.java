package ozokuz.incore.client.features.battlepass;

import ozokuz.incore.client.ui.UIScreenTheme;
import ozokuz.incore.client.ui.render.ThemedUi;
import ozokuz.incore.features.battlepass.network.BattlePassClientCache;
import ozokuz.incore.features.battlepass.network.BattlePassNetworking;
import ozokuz.incore.features.battlepass.network.BattlePassSyncPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
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
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class BattlePassScreen extends Screen {
    private static final UIScreenTheme THEME = UIScreenTheme.BATTLEPASS_TASKS;
    private static final int TARGET_WINDOW_WIDTH = 700;
    private static final int TARGET_WINDOW_HEIGHT = 360;
    private static final int TAB_HEIGHT = 18;
    private static final int HEADER_HEIGHT = 74;
    private static final int REWARD_CARD_WIDTH = 48;
    private static final int REWARD_CARD_HEIGHT = 42;
    private static final int REWARD_CARD_GAP = 5;
    private static final int MISSION_CATEGORY_WIDTH = 112;
    private static final int MISSION_CATEGORY_ROW_HEIGHT = 32;
    private static final int MISSION_CATEGORY_GAP = 4;
    private static final int MISSION_ROW_HEIGHT = 44;
    private static final int XP_BAR_HEIGHT = 5;
    private static final int XP_BAR_TEXTURE_WIDTH = 182;
    private static final int XP_BAR_TEXTURE_HEIGHT = 5;
    private static final int XP_BAR_CAP_WIDTH = 2;
    private static final ResourceLocation XP_BAR_BACKGROUND = ResourceLocation.parse("incore:hud/experience_bar_background_white");
    private static final ResourceLocation XP_BAR_PROGRESS = ResourceLocation.parse("incore:hud/experience_bar_progress_white");

    private final Screen parent;
    private Integer previousMenuBlur;
    private Tab activeTab = Tab.REWARDS;
    private int missionScroll;
    private int rewardLevelScroll;
    private int selectedMissionCategoryIndex;
    private String selectedMissionTaskId;
    private int selectedRewardLevel = -1;
    private int selectedRewardTrack = -1;
    private boolean rewardAutoFocus = true;
    private Button claimAllButton;

    public BattlePassScreen(Screen parent) {
        super(Component.translatable("screen.incore.battle_pass.title"));
        this.parent = parent;
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
        this.addRenderableWidget(Button.builder(Component.literal("X"), button -> this.onClose())
                .bounds(this.windowLeft() + this.windowWidth() - 24, this.windowTop() + 8, 16, 16)
                .build());
        this.claimAllButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("screen.incore.battle_pass.claim_all"),
                        button -> BattlePassNetworking.requestClaimAllRewards()
                ).bounds(this.windowLeft() + this.windowWidth() - 166, this.windowTop() + this.windowHeight() - 28, 156, 20)
                .build());
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
        themed(guiGraphics).drawBackdrop(this.width, this.height);

        int panelX = this.windowLeft();
        int panelY = this.windowTop();
        int panelWidth = this.windowWidth();
        int panelHeight = this.windowHeight();
        int unclaimedRewardLevels = BattlePassClientCache.getUnclaimedRewardLevels();

        if (this.claimAllButton != null) {
            this.claimAllButton.active = BattlePassClientCache.hasActiveSet() && unclaimedRewardLevels > 0;
            this.claimAllButton.setMessage(unclaimedRewardLevels > 0
                    ? Component.translatable("screen.incore.battle_pass.claim_all_count", unclaimedRewardLevels)
                    : Component.translatable("screen.incore.battle_pass.claim_all"));
        }

        drawMainPanel(guiGraphics, panelX, panelY, panelWidth, panelHeight);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        drawTabs(guiGraphics, panelX, panelY, panelWidth);

        if (!BattlePassClientCache.hasActiveSet()) {
            guiGraphics.drawCenteredString(this.font, Component.translatable("screen.incore.battle_pass.none"), panelX + panelWidth / 2, panelY + panelHeight / 2, UIScreenTheme.BattlepassTasks.NONE_TEXT);
            return;
        }

        drawHeader(guiGraphics, panelX, panelY, panelWidth);

        int contentX = panelX + 12;
        int contentY = panelY + 12 + TAB_HEIGHT + 10 + HEADER_HEIGHT + 8;
        int contentWidth = panelWidth - 24;
        int contentHeight = panelHeight - (contentY - panelY) - 10;

        if (this.activeTab == Tab.REWARDS) {
            renderRewardsTab(guiGraphics, mouseX, mouseY, contentX, contentY, contentWidth, contentHeight);
        } else {
            renderMissionsTab(guiGraphics, mouseX, mouseY, contentX, contentY, contentWidth, contentHeight);
        }
    }

    private void drawMainPanel(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        themed(guiGraphics).drawWindow(x, y, width, height);
    }

    private void drawTabs(GuiGraphics guiGraphics, int x, int y, int width) {
        int tabY = y + 8;
        int tabX = x + 12;
        int rewardsWidth = 126;
        int missionsWidth = 126;
        drawTab(guiGraphics, tabX, tabY, rewardsWidth, Component.translatable("screen.incore.battle_pass.tab_rewards"), this.activeTab == Tab.REWARDS);
        drawTab(guiGraphics, tabX + rewardsWidth + 2, tabY, missionsWidth, Component.translatable("screen.incore.battle_pass.tab_missions"), this.activeTab == Tab.MISSIONS);
        guiGraphics.fill(tabX, tabY + TAB_HEIGHT + 2, x + width - 12, tabY + TAB_HEIGHT + 3, UIScreenTheme.BattlepassTasks.TAB_DIVIDER);
    }

    private void drawTab(GuiGraphics guiGraphics, int x, int y, int width, Component text, boolean selected) {
        int bg = selected ? UIScreenTheme.BattlepassTasks.TAB_FILL_SELECTED : UIScreenTheme.BattlepassTasks.TAB_FILL_DEFAULT;
        int fg = selected ? UIScreenTheme.BattlepassTasks.TAB_TEXT_SELECTED : UIScreenTheme.BattlepassTasks.TAB_TEXT_DEFAULT;
        guiGraphics.fill(x, y, x + width, y + TAB_HEIGHT, bg);
        guiGraphics.fill(x, y + TAB_HEIGHT - 1, x + width, y + TAB_HEIGHT, selected ? UIScreenTheme.BattlepassTasks.TAB_UNDERLINE_SELECTED : UIScreenTheme.BattlepassTasks.TAB_UNDERLINE_DEFAULT);
        guiGraphics.drawString(this.font, text, x + 8, y + 5, fg, false);
    }

    private void drawHeader(GuiGraphics guiGraphics, int panelX, int panelY, int panelWidth) {
        int headerX = panelX + 12;
        int headerY = panelY + 12 + TAB_HEIGHT + 10;
        int headerWidth = panelWidth - 24;

        guiGraphics.fill(headerX, headerY, headerX + headerWidth, headerY + HEADER_HEIGHT, UIScreenTheme.BattlepassTasks.PANEL_FILL);
        guiGraphics.fill(headerX, headerY, headerX + headerWidth, headerY + 1, UIScreenTheme.BattlepassTasks.HEADER_BORDER_TOP);
        guiGraphics.fill(headerX, headerY + HEADER_HEIGHT - 1, headerX + headerWidth, headerY + HEADER_HEIGHT, UIScreenTheme.BattlepassTasks.HEADER_BORDER_BOTTOM);

        int level = BattlePassClientCache.getLevel();
        int xpPerLevel = BattlePassClientCache.getXpPerLevel();
        int xpIntoLevel = BattlePassClientCache.getXpIntoCurrentLevel();
        int xpTotal = BattlePassClientCache.getXp();

        Component setDisplay = localizeSetId(BattlePassClientCache.getSetId());
        guiGraphics.fill(headerX + 8, headerY + 8, headerX + 145, headerY + HEADER_HEIGHT - 8, UIScreenTheme.BattlepassTasks.HEADER_CHIP_FILL);
        guiGraphics.drawString(this.font, Component.translatable("screen.incore.battle_pass.set", setDisplay), headerX + 14, headerY + 14, UIScreenTheme.BattlepassTasks.HEADER_TITLE_TEXT);
        guiGraphics.drawString(this.font, Component.translatable("screen.incore.battle_pass.week", BattlePassClientCache.getCurrentWeek(), BattlePassClientCache.getTotalWeeks()), headerX + 14, headerY + 28, UIScreenTheme.BattlepassTasks.HEADER_META_TEXT);
        guiGraphics.drawString(this.font, Component.translatable("screen.incore.battle_pass.time_left", formatTimeLeft(BattlePassClientCache.getEndsAtMillis())), headerX + 14, headerY + 42, UIScreenTheme.BattlepassTasks.HEADER_META_TEXT);

        int progressX = headerX + 164;
        int progressWidth = headerWidth - 174;
        Component levelTitle = Component.literal(level + " " + Component.translatable("screen.incore.battle_pass.level_short").getString());
        long now = BattlePassClientCache.getServerNowMillis();
        long startsAtMillis = BattlePassClientCache.getStartsAtMillis();
        long endsAtMillis = BattlePassClientCache.getEndsAtMillis();
        Component seasonWindow = Component.translatable(
                "screen.incore.battle_pass.window",
                formatDuration(Math.max(0L, now - startsAtMillis), false),
                formatDuration(Math.max(0L, endsAtMillis - startsAtMillis), false)
        );
        guiGraphics.drawString(this.font, levelTitle, progressX, headerY + 12, UIScreenTheme.BattlepassTasks.TEXT_WHITE);
        int seasonWindowX = progressX + Math.max(96, progressWidth - this.font.width(seasonWindow));
        guiGraphics.drawString(this.font, seasonWindow, seasonWindowX, headerY + 12, UIScreenTheme.BattlepassTasks.HEADER_SEASON_TEXT);

        drawProgressBar(guiGraphics, progressX, headerY + 30, progressWidth, xpIntoLevel, xpPerLevel);
        Component xpLine = Component.translatable("screen.incore.battle_pass.xp", xpIntoLevel, xpPerLevel);
        Component totalXpLine = Component.translatable("screen.incore.battle_pass.total_xp", xpTotal);
        guiGraphics.drawString(this.font, xpLine, progressX, headerY + 40, UIScreenTheme.BattlepassTasks.HEADER_XP_TEXT);
        int totalXpX = progressX + Math.max(126, progressWidth - this.font.width(totalXpLine));
        guiGraphics.drawString(this.font, totalXpLine, totalXpX, headerY + 40, UIScreenTheme.BattlepassTasks.HEADER_TOTAL_XP_TEXT);

        Component capsLine = Component.translatable(
                "screen.incore.battle_pass.caps",
                BattlePassClientCache.getWeeklyCompleted(),
                BattlePassClientCache.getWeeklyCap(),
                BattlePassClientCache.getPermanentCompleted()
        );
        Component unclaimedLine = Component.translatable("screen.incore.battle_pass.unclaimed", BattlePassClientCache.getUnclaimedRewardLevels());
        int capsY = headerY + 54;
        int unclaimedX = progressX + progressWidth - this.font.width(unclaimedLine);
        int unclaimedY = capsY;
        int capsEndX = progressX + this.font.width(capsLine);
        if (unclaimedX <= capsEndX + 8) {
            unclaimedX = progressX;
            unclaimedY = capsY + 10;
        }

        guiGraphics.drawString(this.font, capsLine, progressX, capsY, UIScreenTheme.BattlepassTasks.TEXT_SECONDARY);
        guiGraphics.drawString(this.font, unclaimedLine, unclaimedX, unclaimedY, UIScreenTheme.BattlepassTasks.TEXT_WARNING, false);
    }

    private void renderRewardsTab(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y, int width, int height) {
        guiGraphics.fill(x, y, x + width, y + height, UIScreenTheme.BattlepassTasks.PANEL_FILL);

        List<BattlePassClientCache.RewardLevelEntry> levels = BattlePassClientCache.getRewardLevels().stream()
                .sorted(Comparator.comparingInt(BattlePassClientCache.RewardLevelEntry::level))
                .toList();
        List<BattlePassClientCache.LaneEntry> lanes = rewardLanes();

        if (levels.isEmpty()) {
            guiGraphics.drawCenteredString(this.font, Component.translatable("screen.incore.battle_pass.rewards_empty"), x + width / 2, y + height / 2 - 4, UIScreenTheme.BattlepassTasks.EMPTY_TEXT);
            return;
        }
        int highestConfiguredLevel = levels.get(levels.size() - 1).level();

        int trackCount = Math.max(1, Math.min(3, lanes.size()));

        int trackLabelWidth = 126;
        int cardsX = x + trackLabelWidth + 8;
        int cardsWidth = width - trackLabelWidth - 14;
        int columns = Math.max(1, cardsWidth / (REWARD_CARD_WIDTH + REWARD_CARD_GAP));
        RewardTrackLayout trackLayout = buildRewardTrackLayout(levels, columns, this.rewardLevelScroll, this.rewardAutoFocus);
        this.rewardLevelScroll = trackLayout.scroll();
        this.rewardAutoFocus = false;
        List<Integer> visibleLevelIndices = trackLayout.visibleLevelIndices();

        int levelHeaderY = y + 10;
        int gridY = y + 28;

        for (int col = 0; col < visibleLevelIndices.size(); col++) {
            int levelIndex = visibleLevelIndices.get(col);
            BattlePassClientCache.RewardLevelEntry entry = levels.get(levelIndex);
            int cardX = cardsX + col * (REWARD_CARD_WIDTH + REWARD_CARD_GAP);
            Component levelText = Component.literal("Lv " + entry.level());
            int textX = cardX + (REWARD_CARD_WIDTH - this.font.width(levelText)) / 2;
            guiGraphics.drawString(this.font, levelText, textX, levelHeaderY, UIScreenTheme.BattlepassTasks.REWARD_LEVEL_TEXT, false);
        }

        HoveredReward hovered = null;
        for (int track = 0; track < trackCount; track++) {
            int rowY = gridY + track * (REWARD_CARD_HEIGHT + 8);
            BattlePassClientCache.LaneEntry lane = lanes.get(track);
            drawTrackLabel(guiGraphics, x + 6, rowY, trackLabelWidth - 10, REWARD_CARD_HEIGHT, lane);

            for (int col = 0; col < visibleLevelIndices.size(); col++) {
                int levelIndex = visibleLevelIndices.get(col);
                BattlePassClientCache.RewardLevelEntry levelEntry = levels.get(levelIndex);
                int cardX = cardsX + col * (REWARD_CARD_WIDTH + REWARD_CARD_GAP);
                int rewardLevel = levelEntry.level();
                boolean laneUnlocked = lane.unlocked();
                boolean levelUnlocked = rewardLevel <= BattlePassClientCache.getLevel();
                boolean claimed = laneUnlocked && rewardLevel <= lane.highestClaimedLevel();
                boolean unclaimed = laneUnlocked && !claimed && levelUnlocked;
                boolean unlocked = laneUnlocked && levelUnlocked;
                boolean selected = rewardLevel == this.selectedRewardLevel && track == this.selectedRewardTrack;
                boolean highestLevel = rewardLevel == highestConfiguredLevel;

                int bg = unlocked ? UIScreenTheme.BattlepassTasks.REWARD_FILL_UNLOCKED : UIScreenTheme.BattlepassTasks.REWARD_FILL_LOCKED;
                int border;
                if (selected && highestLevel) {
                    border = UIScreenTheme.BattlepassTasks.REWARD_BORDER_MYTHIC;
                } else if (selected) {
                    border = UIScreenTheme.BattlepassTasks.ACCENT_GOLD;
                } else if (highestLevel) {
                    border = UIScreenTheme.BattlepassTasks.REWARD_BORDER_EPIC;
                } else if (claimed) {
                    border = UIScreenTheme.BattlepassTasks.PROGRESS_FILL_COMPLETE;
                } else if (unclaimed) {
                    border = UIScreenTheme.BattlepassTasks.REWARD_BORDER_RARE;
                } else if (laneUnlocked) {
                    border = UIScreenTheme.BattlepassTasks.REWARD_BORDER_COMMON;
                } else {
                    border = UIScreenTheme.BattlepassTasks.REWARD_BORDER_LOCKED;
                }
                guiGraphics.fill(cardX, rowY, cardX + REWARD_CARD_WIDTH, rowY + REWARD_CARD_HEIGHT, bg);
                drawBoxBorder(guiGraphics, cardX, rowY, REWARD_CARD_WIDTH, REWARD_CARD_HEIGHT, border);

                BattlePassClientCache.RewardEntry reward = track < levelEntry.rewards().size() ? levelEntry.rewards().get(track) : null;
                if (reward != null && reward.kind() == BattlePassSyncPayload.REWARD_KIND_NONE) {
                    reward = null;
                }
                if (reward == null) {
                    guiGraphics.drawCenteredString(this.font, Component.literal("-"), cardX + REWARD_CARD_WIDTH / 2, rowY + 16, UIScreenTheme.BattlepassTasks.REWARD_PLACEHOLDER_TEXT);
                    continue;
                }

                ItemStack icon = iconForReward(reward);
                int iconX = cardX + (REWARD_CARD_WIDTH - 16) / 2;
                int iconY = rowY + 5;
                guiGraphics.renderItem(icon, iconX, iconY);
                if (reward.amount() > 1) {
                    String quantity = "x" + reward.amount();
                    int quantityWidth = this.font.width(quantity);
                    int quantityX = cardX + (REWARD_CARD_WIDTH - quantityWidth) / 2;
                    int quantityY = rowY + REWARD_CARD_HEIGHT - this.font.lineHeight - 3;
                    guiGraphics.fill(quantityX - 2, quantityY - 1, quantityX + quantityWidth + 2, quantityY + this.font.lineHeight, UIScreenTheme.BattlepassTasks.QUANTITY_CHIP_FILL);
                    guiGraphics.drawString(this.font, quantity, quantityX, quantityY, UIScreenTheme.BattlepassTasks.QUANTITY_TEXT, false);
                }

                if (!laneUnlocked) {
                    guiGraphics.fill(cardX, rowY, cardX + REWARD_CARD_WIDTH, rowY + REWARD_CARD_HEIGHT, UIScreenTheme.BattlepassTasks.REWARD_OVERLAY_CLAIMED);
                    drawBoxBorder(guiGraphics, cardX, rowY, REWARD_CARD_WIDTH, REWARD_CARD_HEIGHT, border);
                } else if (!levelUnlocked) {
                    guiGraphics.fill(cardX, rowY, cardX + REWARD_CARD_WIDTH, rowY + REWARD_CARD_HEIGHT, UIScreenTheme.BattlepassTasks.REWARD_OVERLAY_LOCKED);
                    drawBoxBorder(guiGraphics, cardX, rowY, REWARD_CARD_WIDTH, REWARD_CARD_HEIGHT, border);
                }

                boolean isHovered = mouseX >= cardX && mouseX < cardX + REWARD_CARD_WIDTH && mouseY >= rowY && mouseY < rowY + REWARD_CARD_HEIGHT;
                if (isHovered) {
                    hovered = new HoveredReward(reward, icon);
                }
            }
        }

        if (hovered != null) {
            if (hovered.reward().kind() == 0) {
                guiGraphics.renderTooltip(this.font, hovered.icon(), mouseX, mouseY);
            } else {
                guiGraphics.renderTooltip(this.font, Component.literal(hovered.reward().text()).withStyle(ChatFormatting.YELLOW), mouseX, mouseY);
            }
        }

        if (this.selectedRewardLevel >= 0) {
            BattlePassClientCache.RewardLevelEntry selectedLevel = levels.stream()
                    .filter(entry -> entry.level() == this.selectedRewardLevel)
                    .findFirst()
                    .orElse(null);
            if (selectedLevel != null) {
                int infoY = y + height - 34;
                guiGraphics.drawString(this.font, Component.translatable("screen.incore.battle_pass.rewards_selected", selectedLevel.level()), x + 8, infoY, UIScreenTheme.BattlepassTasks.DETAILS_TEXT);
                guiGraphics.drawString(this.font, Component.translatable("screen.incore.battle_pass.selected_xp_to_reach", selectedLevel.requiredXp()), x + 8, infoY + 10, UIScreenTheme.BattlepassTasks.HEADER_META_TEXT);
                guiGraphics.drawString(this.font, Component.translatable("screen.incore.battle_pass.selected_xp_for_level", selectedLevel.xpForLevel()), x + 8, infoY + 20, UIScreenTheme.BattlepassTasks.HEADER_META_TEXT, false);
            }
        }
    }

    private void drawTrackLabel(GuiGraphics guiGraphics, int x, int y, int width, int height, BattlePassClientCache.LaneEntry lane) {
        String laneId = lane.id();
        int color;
        Component text;
        if ("basic".equals(laneId)) {
            color = UIScreenTheme.BattlepassTasks.BADGE_FILL_DEFAULT;
        } else if ("originium".equals(laneId)) {
            color = UIScreenTheme.BattlepassTasks.REQUIREMENT_FILL_WARNING;
        } else {
            color = UIScreenTheme.BattlepassTasks.REQUIREMENT_FILL_DANGER;
        }
        text = Component.literal(lane.displayName());
        if (!lane.unlocked()) {
            color = UIScreenTheme.BattlepassTasks.REQUIREMENT_FILL_BLOCKED;
            text = Component.literal("\uD83D\uDD12 ").append(text);
        }

        guiGraphics.fill(x, y, x + width, y + height, color);
        drawBoxBorder(guiGraphics, x, y, width, height, UIScreenTheme.BattlepassTasks.BORDER_DARK);
        guiGraphics.drawString(this.font, text, x + 8, y + 12, UIScreenTheme.BattlepassTasks.HEADER_TITLE_TEXT, false);
    }

    private void renderMissionsTab(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y, int width, int height) {
        guiGraphics.fill(x, y, x + width, y + height, UIScreenTheme.BattlepassTasks.PANEL_FILL);

        List<BattlePassClientCache.TaskEntry> allTasks = new ArrayList<>(BattlePassClientCache.getTasks());
        if (allTasks.isEmpty()) {
            guiGraphics.drawCenteredString(this.font, Component.translatable("screen.incore.battle_pass.tasks_empty"), x + width / 2, y + height / 2 - 4, UIScreenTheme.BattlepassTasks.EMPTY_TEXT);
            return;
        }

        List<MissionCategory> categories = buildMissionCategories(allTasks, BattlePassClientCache.getCurrentWeek());
        if (categories.isEmpty()) {
            guiGraphics.drawCenteredString(this.font, Component.translatable("screen.incore.battle_pass.tasks_empty"), x + width / 2, y + height / 2 - 4, UIScreenTheme.BattlepassTasks.EMPTY_TEXT);
            return;
        }

        this.selectedMissionCategoryIndex = clamp(this.selectedMissionCategoryIndex, 0, categories.size() - 1);
        int categoryX = x + 6;
        int categoryY = y + 10;
        for (int i = 0; i < categories.size(); i++) {
            int rowY = categoryY + i * (MISSION_CATEGORY_ROW_HEIGHT + MISSION_CATEGORY_GAP);
            if (rowY + MISSION_CATEGORY_ROW_HEIGHT > y + height - 22) {
                break;
            }

            boolean selectedCategory = i == this.selectedMissionCategoryIndex;
            int bg = selectedCategory ? UIScreenTheme.BattlepassTasks.REQUIREMENT_FILL_WARNING : UIScreenTheme.BattlepassTasks.LIST_FILL_DEFAULT;
            int border = selectedCategory ? UIScreenTheme.BattlepassTasks.ACCENT_GOLD : UIScreenTheme.BattlepassTasks.BORDER_MUTED;
            guiGraphics.fill(categoryX, rowY, categoryX + MISSION_CATEGORY_WIDTH, rowY + MISSION_CATEGORY_ROW_HEIGHT, bg);
            drawBoxBorder(guiGraphics, categoryX, rowY, MISSION_CATEGORY_WIDTH, MISSION_CATEGORY_ROW_HEIGHT, border);
            MissionCategory category = categories.get(i);
            guiGraphics.drawString(this.font, category.label(), categoryX + 8, rowY + 7, UIScreenTheme.BattlepassTasks.TEXT_SOFT, false);
            guiGraphics.drawString(this.font, category.progressLabel(), categoryX + 8, rowY + 18, UIScreenTheme.BattlepassTasks.TEXT_MUTED, false);
        }

        MissionCategory selectedCategory = categories.get(this.selectedMissionCategoryIndex);
        List<BattlePassClientCache.TaskEntry> tasks = tasksForCategory(allTasks, selectedCategory);

        int listX = x + MISSION_CATEGORY_WIDTH + 12;
        int listWidth = width - MISSION_CATEGORY_WIDTH - 20;
        int listTop = y + 10;
        int listBottom = y + height - 22;
        int listHeight = listBottom - listTop;
        int visibleRows = Math.max(1, listHeight / MISSION_ROW_HEIGHT);
        int maxScroll = Math.max(0, tasks.size() - visibleRows);
        this.missionScroll = clamp(this.missionScroll, 0, maxScroll);
        BattlePassClientCache.TaskEntry selectedTask = null;

        for (int i = 0; i < visibleRows; i++) {
            int index = this.missionScroll + i;
            if (index >= tasks.size()) {
                break;
            }

            BattlePassClientCache.TaskEntry task = tasks.get(index);
            int rowY = listTop + i * MISSION_ROW_HEIGHT;
            boolean selected = task.id().equals(this.selectedMissionTaskId);
            if (selected) {
                selectedTask = task;
            }
            int rowBg = selected ? UIScreenTheme.BattlepassTasks.TASK_ROW_FILL_SELECTED : UIScreenTheme.BattlepassTasks.LIST_FILL_DEFAULT;
            guiGraphics.fill(listX, rowY, listX + listWidth, rowY + MISSION_ROW_HEIGHT - 4, rowBg);
            drawBoxBorder(guiGraphics, listX, rowY, listWidth, MISSION_ROW_HEIGHT - 4, selected ? UIScreenTheme.BattlepassTasks.ACCENT_GOLD : UIScreenTheme.BattlepassTasks.BORDER_MUTED);

            Component type = task.weekly()
                    ? Component.translatable("screen.incore.battle_pass.task_weekly", task.week())
                    : Component.translatable("screen.incore.battle_pass.task_permanent");
            guiGraphics.drawString(this.font, type, listX + 6, rowY + 5, UIScreenTheme.BattlepassTasks.TASK_TYPE_TEXT);
            guiGraphics.drawString(this.font, Component.literal("+" + task.xpReward() + " XP"), listX + listWidth - 72, rowY + 5, UIScreenTheme.BattlepassTasks.TASK_XP_TEXT);

            int tierColor = tierColor(task.tier());
            String line = truncate(task.description(), listWidth - 228);
            guiGraphics.drawString(this.font, Component.literal(line), listX + 6, rowY + 18, UIScreenTheme.BattlepassTasks.TEXT_SOFT);
            guiGraphics.drawString(this.font, Component.literal(capitalize(task.tier())), listX + listWidth - 132, rowY + 18, tierColor);

            int statusColor = task.completed() ? UIScreenTheme.BattlepassTasks.TASK_STATUS_COMPLETE : (task.completableNow() ? UIScreenTheme.BattlepassTasks.DETAILS_TEXT : UIScreenTheme.BattlepassTasks.TASK_STATUS_LOCKED);
            int progressCurrent = Math.min(task.progressCurrent(), task.progressGoal());
            int progressGoal = Math.max(1, task.progressGoal());
            int progressBarWidth = 152;
            int progressBarX = listX + listWidth - progressBarWidth - 10;
            int progressBarY = rowY + 30;
            drawTaskProgressBar(guiGraphics, progressBarX, progressBarY, progressBarWidth, 6, progressCurrent, progressGoal, task.completed(), task.completableNow());

            String progressLabel = progressCurrent + "/" + progressGoal;
            guiGraphics.drawString(this.font, Component.literal(progressLabel), progressBarX + progressBarWidth - this.font.width(progressLabel), rowY + 20, UIScreenTheme.BattlepassTasks.DETAILS_TEXT, false);

            int statusMaxWidth = Math.max(30, progressBarX - (listX + 10));
            guiGraphics.drawString(this.font, Component.literal(truncate(task.status(), statusMaxWidth)), listX + 6, rowY + 31, statusColor);
        }

        if (selectedTask != null) {
            guiGraphics.drawString(this.font, Component.translatable("screen.incore.battle_pass.task_id", selectedTask.id()), listX, y + height - 12, UIScreenTheme.BattlepassTasks.FOOTER_TASK_ID_TEXT);
        } else {
            guiGraphics.drawString(this.font, Component.translatable("screen.incore.battle_pass.task_select_hint"), listX, y + height - 12, UIScreenTheme.BattlepassTasks.FOOTER_HINT_TEXT);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int panelX = this.windowLeft();
            int panelY = this.windowTop();
            int panelWidth = this.windowWidth();

            int tabY = panelY + 8;
            int tabX = panelX + 12;
            if (mouseY >= tabY && mouseY <= tabY + TAB_HEIGHT) {
                if (mouseX >= tabX && mouseX <= tabX + 126) {
                    this.activeTab = Tab.REWARDS;
                    return true;
                }
                if (mouseX >= tabX + 128 && mouseX <= tabX + 254) {
                    this.activeTab = Tab.MISSIONS;
                    return true;
                }
            }

            if (!BattlePassClientCache.hasActiveSet()) {
                return super.mouseClicked(mouseX, mouseY, button);
            }

            int contentX = panelX + 12;
            int contentY = panelY + 12 + TAB_HEIGHT + 10 + HEADER_HEIGHT + 8;
            int contentWidth = panelWidth - 24;
            int contentHeight = this.windowHeight() - (contentY - panelY) - 10;

            if (this.activeTab == Tab.MISSIONS) {
                List<BattlePassClientCache.TaskEntry> allTasks = new ArrayList<>(BattlePassClientCache.getTasks());
                List<MissionCategory> categories = buildMissionCategories(allTasks, BattlePassClientCache.getCurrentWeek());
                if (categories.isEmpty()) {
                    return true;
                }

                this.selectedMissionCategoryIndex = clamp(this.selectedMissionCategoryIndex, 0, categories.size() - 1);
                int categoryX = contentX + 6;
                int categoryY = contentY + 10;
                for (int i = 0; i < categories.size(); i++) {
                    int rowY = categoryY + i * (MISSION_CATEGORY_ROW_HEIGHT + MISSION_CATEGORY_GAP);
                    if (rowY + MISSION_CATEGORY_ROW_HEIGHT > contentY + contentHeight - 22) {
                        break;
                    }

                    if (mouseX >= categoryX && mouseX < categoryX + MISSION_CATEGORY_WIDTH
                            && mouseY >= rowY && mouseY < rowY + MISSION_CATEGORY_ROW_HEIGHT) {
                        this.selectedMissionCategoryIndex = i;
                        this.missionScroll = 0;
                        this.selectedMissionTaskId = null;
                        return true;
                    }
                }

                MissionCategory selectedCategory = categories.get(this.selectedMissionCategoryIndex);
                List<BattlePassClientCache.TaskEntry> tasks = tasksForCategory(allTasks, selectedCategory);
                int listX = contentX + MISSION_CATEGORY_WIDTH + 12;
                int listTop = contentY + 10;
                int listBottom = contentY + contentHeight - 22;
                int listWidth = contentWidth - MISSION_CATEGORY_WIDTH - 20;
                if (mouseX >= listX && mouseX <= listX + listWidth && mouseY >= listTop && mouseY <= listBottom) {
                    int row = (int) ((mouseY - listTop) / MISSION_ROW_HEIGHT);
                    int listHeight = listBottom - listTop;
                    int visibleRows = Math.max(1, listHeight / MISSION_ROW_HEIGHT);
                    int maxScroll = Math.max(0, tasks.size() - visibleRows);
                    this.missionScroll = clamp(this.missionScroll, 0, maxScroll);
                    int index = this.missionScroll + row;
                    if (index >= 0 && index < tasks.size()) {
                        this.selectedMissionTaskId = tasks.get(index).id();
                        return true;
                    }
                }
            } else {
                List<BattlePassClientCache.RewardLevelEntry> levels = BattlePassClientCache.getRewardLevels().stream()
                        .sorted(Comparator.comparingInt(BattlePassClientCache.RewardLevelEntry::level))
                        .toList();
                List<BattlePassClientCache.LaneEntry> lanes = rewardLanes();
                if (!levels.isEmpty()) {
                    int trackCount = Math.max(1, Math.min(3, lanes.size()));
                    int trackLabelWidth = 126;
                    int cardsX = contentX + trackLabelWidth + 8;
                    int cardsWidth = contentWidth - trackLabelWidth - 14;
                    int columns = Math.max(1, cardsWidth / (REWARD_CARD_WIDTH + REWARD_CARD_GAP));
                    RewardTrackLayout trackLayout = buildRewardTrackLayout(levels, columns, this.rewardLevelScroll, false);
                    this.rewardLevelScroll = trackLayout.scroll();
                    List<Integer> visibleLevelIndices = trackLayout.visibleLevelIndices();
                    int gridY = contentY + 28;

                    for (int track = 0; track < trackCount; track++) {
                        int rowY = gridY + track * (REWARD_CARD_HEIGHT + 8);
                        for (int col = 0; col < visibleLevelIndices.size(); col++) {
                            int levelIndex = visibleLevelIndices.get(col);
                            int cardX = cardsX + col * (REWARD_CARD_WIDTH + REWARD_CARD_GAP);
                            if (mouseX >= cardX && mouseX < cardX + REWARD_CARD_WIDTH && mouseY >= rowY && mouseY < rowY + REWARD_CARD_HEIGHT) {
                                this.selectedRewardLevel = levels.get(levelIndex).level();
                                this.selectedRewardTrack = track;
                                return true;
                            }
                        }
                    }
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY == 0.0D) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        int direction = scrollY > 0.0D ? -1 : 1;
        if (this.activeTab == Tab.MISSIONS) {
            List<BattlePassClientCache.TaskEntry> allTasks = new ArrayList<>(BattlePassClientCache.getTasks());
            List<MissionCategory> categories = buildMissionCategories(allTasks, BattlePassClientCache.getCurrentWeek());
            if (categories.isEmpty()) {
                return true;
            }

            this.selectedMissionCategoryIndex = clamp(this.selectedMissionCategoryIndex, 0, categories.size() - 1);
            MissionCategory selectedCategory = categories.get(this.selectedMissionCategoryIndex);
            List<BattlePassClientCache.TaskEntry> tasks = tasksForCategory(allTasks, selectedCategory);
            int contentHeight = this.windowHeight() - (12 + TAB_HEIGHT + 10 + HEADER_HEIGHT + 8) - 10;
            int listHeight = contentHeight - 32;
            int visibleRows = Math.max(1, listHeight / MISSION_ROW_HEIGHT);
            int maxScroll = Math.max(0, tasks.size() - visibleRows);
            this.missionScroll = clamp(this.missionScroll + direction, 0, maxScroll);
            return true;
        }

        int contentWidth = this.windowWidth() - 24;
        int trackLabelWidth = 126;
        int cardsWidth = contentWidth - trackLabelWidth - 14;
        List<BattlePassClientCache.RewardLevelEntry> levels = BattlePassClientCache.getRewardLevels().stream()
                .sorted(Comparator.comparingInt(BattlePassClientCache.RewardLevelEntry::level))
                .toList();
        int columns = Math.max(1, cardsWidth / (REWARD_CARD_WIDTH + REWARD_CARD_GAP));
        RewardTrackLayout trackLayout = buildRewardTrackLayout(levels, columns, this.rewardLevelScroll, false);
        this.rewardLevelScroll = clamp(trackLayout.scroll() + direction, 0, trackLayout.maxScroll());
        return true;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int windowLeft() {
        return (this.width - this.windowWidth()) / 2;
    }

    private int windowTop() {
        return (this.height - this.windowHeight()) / 2;
    }

    private int windowWidth() {
        return Math.min(TARGET_WINDOW_WIDTH, Math.max(420, this.width - 18));
    }

    private int windowHeight() {
        return Math.min(TARGET_WINDOW_HEIGHT, Math.max(240, this.height - 22));
    }

    private void drawProgressBar(GuiGraphics guiGraphics, int x, int y, int width, int current, int max) {
        int clampedMax = Math.max(1, max);
        int clampedCurrent = Math.max(0, Math.min(current, clampedMax));
        drawSlicedXpBarSprite(guiGraphics, XP_BAR_BACKGROUND, x, y, width);
        int filled = Math.max(0, Math.min(width, Math.round(width * (clampedCurrent / (float) clampedMax))));
        if (filled > 0) {
            guiGraphics.enableScissor(x, y, x + filled, y + XP_BAR_HEIGHT);
            drawSlicedXpBarSprite(guiGraphics, XP_BAR_PROGRESS, x, y, width);
            guiGraphics.disableScissor();
        }
    }

    private void drawSlicedXpBarSprite(GuiGraphics guiGraphics, ResourceLocation sprite, int x, int y, int width) {
        int barWidth = Math.max(1, width);
        if (barWidth <= XP_BAR_CAP_WIDTH * 2) {
            guiGraphics.blitSprite(sprite, XP_BAR_TEXTURE_WIDTH, XP_BAR_TEXTURE_HEIGHT, 0, 0, x, y, barWidth, XP_BAR_HEIGHT);
            return;
        }

        int centerWidth = XP_BAR_TEXTURE_WIDTH - (XP_BAR_CAP_WIDTH * 2);
        int leftX = x;
        int rightX = x + barWidth - XP_BAR_CAP_WIDTH;

        guiGraphics.blitSprite(sprite, XP_BAR_TEXTURE_WIDTH, XP_BAR_TEXTURE_HEIGHT, 0, 0, leftX, y, XP_BAR_CAP_WIDTH, XP_BAR_HEIGHT);
        guiGraphics.blitSprite(
                sprite,
                XP_BAR_TEXTURE_WIDTH,
                XP_BAR_TEXTURE_HEIGHT,
                XP_BAR_TEXTURE_WIDTH - XP_BAR_CAP_WIDTH,
                0,
                rightX,
                y,
                XP_BAR_CAP_WIDTH,
                XP_BAR_HEIGHT
        );

        int drawX = x + XP_BAR_CAP_WIDTH;
        int remaining = barWidth - (XP_BAR_CAP_WIDTH * 2);
        while (remaining > 0) {
            int chunk = Math.min(centerWidth, remaining);
            guiGraphics.blitSprite(sprite, XP_BAR_TEXTURE_WIDTH, XP_BAR_TEXTURE_HEIGHT, XP_BAR_CAP_WIDTH, 0, drawX, y, chunk, XP_BAR_HEIGHT);
            drawX += chunk;
            remaining -= chunk;
        }
    }

    private void drawTaskProgressBar(GuiGraphics guiGraphics, int x, int y, int width, int height, int current, int goal, boolean completed, boolean available) {
        int background = available ? UIScreenTheme.BattlepassTasks.PROGRESS_BG_AVAILABLE : UIScreenTheme.BattlepassTasks.PROGRESS_BG_LOCKED;
        int fill = completed ? UIScreenTheme.BattlepassTasks.PROGRESS_FILL_CLAIMED : (available ? UIScreenTheme.BattlepassTasks.PROGRESS_FILL_AVAILABLE : UIScreenTheme.BattlepassTasks.PROGRESS_FILL_LOCKED);
        int border = completed ? UIScreenTheme.BattlepassTasks.PROGRESS_BORDER_CLAIMED : UIScreenTheme.BattlepassTasks.PROGRESS_BORDER;
        guiGraphics.fill(x, y, x + width, y + height, background);
        int clampedGoal = Math.max(1, goal);
        int clampedCurrent = Math.max(0, Math.min(current, clampedGoal));
        int fillWidth = Math.max(0, Math.min(width, Math.round(width * (clampedCurrent / (float) clampedGoal))));
        if (fillWidth > 0) {
            guiGraphics.fill(x, y, x + fillWidth, y + height, fill);
        }
        drawBoxBorder(guiGraphics, x, y, width, height, border);
    }

    private static void drawBoxBorder(GuiGraphics guiGraphics, int x, int y, int width, int height, int color) {
        themed(guiGraphics).drawBorder(x, y, x + width, y + height, color);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static RewardTrackLayout buildRewardTrackLayout(
            List<BattlePassClientCache.RewardLevelEntry> levels,
            int columns,
            int requestedScroll,
            boolean autoFocus
    ) {
        int visibleColumns = Math.max(1, columns);
        if (levels.isEmpty()) {
            return new RewardTrackLayout(List.of(), 0, 0);
        }

        int currentLevel = Math.max(0, BattlePassClientCache.getLevel());
        int currentIndex = resolveLevelIndex(levels, currentLevel);
        int highestIndex = levels.size() - 1;
        if (visibleColumns == 1) {
            return new RewardTrackLayout(List.of(highestIndex), 0, 0);
        }

        List<Integer> nonHighestIndices = new ArrayList<>();
        for (int i = 0; i < levels.size(); i++) {
            if (i != highestIndex) {
                nonHighestIndices.add(i);
            }
        }

        int baseSlots = Math.max(0, visibleColumns - 1);
        int baseMaxScroll = Math.max(0, nonHighestIndices.size() - baseSlots);
        int baseScroll = clamp(requestedScroll, 0, baseMaxScroll);

        if (autoFocus && baseSlots > 0 && !nonHighestIndices.isEmpty()) {
            int targetPos = baseMaxScroll;
            for (int i = 0; i < nonHighestIndices.size(); i++) {
                int level = levels.get(nonHighestIndices.get(i)).level();
                if (level > currentLevel) {
                    targetPos = i;
                    break;
                }
            }
            baseScroll = clamp(targetPos, 0, baseMaxScroll);
        }

        List<Integer> baseVisibleIndices = new ArrayList<>(baseSlots);
        for (int i = 0; i < baseSlots; i++) {
            int sourceIndex = baseScroll + i;
            if (sourceIndex >= nonHighestIndices.size()) {
                break;
            }
            baseVisibleIndices.add(nonHighestIndices.get(sourceIndex));
        }

        boolean hasSmallerVisible = baseVisibleIndices.stream().anyMatch(index -> index < currentIndex);
        boolean stickyCurrentLeft = currentIndex != highestIndex && !hasSmallerVisible && visibleColumns > 1;
        if (stickyCurrentLeft) {
            int dynamicSlots = Math.max(0, visibleColumns - 2);
            List<Integer> dynamicIndices = new ArrayList<>();
            for (int index : nonHighestIndices) {
                if (index != currentIndex) {
                    dynamicIndices.add(index);
                }
            }

            int maxScroll = Math.max(0, dynamicIndices.size() - dynamicSlots);
            int scroll = clamp(requestedScroll, 0, maxScroll);
            if (autoFocus && dynamicSlots > 0 && !dynamicIndices.isEmpty()) {
                int targetPos = maxScroll;
                for (int i = 0; i < dynamicIndices.size(); i++) {
                    int level = levels.get(dynamicIndices.get(i)).level();
                    if (level > currentLevel) {
                        targetPos = i;
                        break;
                    }
                }
                scroll = clamp(targetPos, 0, maxScroll);
            }

            List<Integer> visibleLevelIndices = new ArrayList<>(visibleColumns);
            visibleLevelIndices.add(currentIndex);
            for (int i = 0; i < dynamicSlots; i++) {
                int sourceIndex = scroll + i;
                if (sourceIndex >= dynamicIndices.size()) {
                    break;
                }
                visibleLevelIndices.add(dynamicIndices.get(sourceIndex));
            }
            visibleLevelIndices.add(highestIndex);
            return new RewardTrackLayout(visibleLevelIndices, scroll, maxScroll);
        }

        List<Integer> visibleLevelIndices = new ArrayList<>(visibleColumns);
        visibleLevelIndices.addAll(baseVisibleIndices);
        visibleLevelIndices.add(highestIndex);
        return new RewardTrackLayout(visibleLevelIndices, baseScroll, baseMaxScroll);
    }

    private static int resolveLevelIndex(List<BattlePassClientCache.RewardLevelEntry> levels, int targetLevel) {
        if (levels.isEmpty()) {
            return 0;
        }

        int fallback = 0;
        for (int i = 0; i < levels.size(); i++) {
            int level = levels.get(i).level();
            if (level == targetLevel) {
                return i;
            }
            if (level < targetLevel) {
                fallback = i;
            } else if (level > targetLevel) {
                return i;
            }
        }

        return fallback;
    }

    private static List<MissionCategory> buildMissionCategories(List<BattlePassClientCache.TaskEntry> tasks, int currentWeek) {
        List<MissionCategory> categories = new ArrayList<>();
        tasks.stream()
                .filter(BattlePassClientCache.TaskEntry::weekly)
                .map(BattlePassClientCache.TaskEntry::week)
                .distinct()
                .sorted()
                .forEach(week -> categories.add(MissionCategory.forWeek(week, categoryProgress(tasks, true, week, currentWeek))));

        if (tasks.stream().anyMatch(task -> !task.weekly())) {
            categories.add(MissionCategory.permanent(categoryProgress(tasks, false, 0, currentWeek)));
        }

        return categories;
    }

    private static CategoryProgress categoryProgress(
            List<BattlePassClientCache.TaskEntry> tasks,
            boolean weekly,
            int week,
            int currentWeek
    ) {
        int available = 0;
        int completed = 0;
        for (BattlePassClientCache.TaskEntry task : tasks) {
            if (weekly != task.weekly()) {
                continue;
            }
            if (weekly && task.week() != week) {
                continue;
            }
            if (!isTaskAvailable(task, currentWeek)) {
                continue;
            }

            available++;
            if (task.completed()) {
                completed++;
            }
        }

        return new CategoryProgress(completed, completionCapFromAvailable(available));
    }

    private static boolean isTaskAvailable(BattlePassClientCache.TaskEntry task, int currentWeek) {
        return !task.weekly() || task.week() <= currentWeek;
    }

    private static int completionCapFromAvailable(int availableTaskCount) {
        if (availableTaskCount <= 0) {
            return 0;
        }

        return Math.max(1, availableTaskCount / 2);
    }

    private static List<BattlePassClientCache.TaskEntry> tasksForCategory(
            List<BattlePassClientCache.TaskEntry> tasks,
            MissionCategory category
    ) {
        return tasks.stream()
                .filter(category::matches)
                .sorted(Comparator
                        .comparingInt(BattlePassClientCache.TaskEntry::week)
                        .thenComparing(BattlePassClientCache.TaskEntry::id))
                .toList();
    }

    private String truncate(String text, int maxWidth) {
        if (this.font.width(text) <= maxWidth) {
            return text;
        }

        String ellipsis = "...";
        int available = Math.max(0, maxWidth - this.font.width(ellipsis));
        String trimmed = this.font.plainSubstrByWidth(text, available);
        return trimmed + ellipsis;
    }

    private static int tierColor(String tier) {
        if (tier == null) {
            return UIScreenTheme.BattlepassTasks.LEAGUE_DEFAULT;
        }

        return switch (tier.toLowerCase(Locale.ROOT)) {
            case "bronze" -> UIScreenTheme.BattlepassTasks.LEAGUE_BRONZE;
            case "silver" -> UIScreenTheme.BattlepassTasks.LEAGUE_SILVER;
            case "gold" -> UIScreenTheme.BattlepassTasks.LEAGUE_GOLD;
            case "platinum" -> UIScreenTheme.BattlepassTasks.LEAGUE_PLATINUM;
            case "diamond" -> UIScreenTheme.BattlepassTasks.LEAGUE_DIAMOND;
            default -> UIScreenTheme.BattlepassTasks.LEAGUE_DEFAULT;
        };
    }

    private static String capitalize(String text) {
        if (text == null || text.isBlank()) {
            return "Unknown";
        }

        String lower = text.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private static String simplifySetId(String setId) {
        int separator = setId.indexOf(':');
        return separator >= 0 && separator + 1 < setId.length() ? setId.substring(separator + 1) : setId;
    }

    private static Component localizeSetId(String setId) {
        ResourceLocation id = ResourceLocation.tryParse(setId);
        if (id == null) {
            return Component.literal(simplifySetId(setId));
        }

        String translationKey = "battlepass_set." + id.getNamespace() + "." + id.getPath().replace('/', '.');
        if (I18n.exists(translationKey)) {
            return Component.translatable(translationKey);
        }

        return Component.literal(simplifySetId(setId));
    }

    private static String formatTimeLeft(long endsAtMillis) {
        long remainingMillis = endsAtMillis - BattlePassClientCache.getServerNowMillis();
        if (remainingMillis <= 0L) {
            return Component.translatable("screen.incore.battle_pass.ended").getString();
        }

        return formatDuration(remainingMillis, true);
    }

    private static String formatDuration(long millis, boolean roundUp) {
        long totalSeconds = roundUp
                ? Math.max(0L, (millis + 999L) / 1000L)
                : Math.max(0L, millis / 1000L);
        long weeks = totalSeconds / 604800L;
        long days = (totalSeconds % 604800L) / 86400L;
        long hours = (totalSeconds % 86400L) / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;

        if (weeks > 0L) {
            return days > 0L
                    ? String.format(Locale.ROOT, "%dw %dd", weeks, days)
                    : String.format(Locale.ROOT, "%dw", weeks);
        }
        if (days > 0L) {
            return hours > 0L
                    ? String.format(Locale.ROOT, "%dd %02dh", days, hours)
                    : String.format(Locale.ROOT, "%dd", days);
        }
        if (hours > 0L) {
            return minutes > 0L
                    ? String.format(Locale.ROOT, "%02dh %02dm", hours, minutes)
                    : String.format(Locale.ROOT, "%02dh", hours);
        }

        long displayMinutes = roundUp
                ? Math.max(1L, (millis + 59999L) / 60000L)
                : millis / 60000L;
        return displayMinutes + "m";
    }

    private static ItemStack iconForReward(BattlePassClientCache.RewardEntry reward) {
        ResourceLocation id = ResourceLocation.tryParse(reward.iconItemId());
        Item item = id == null ? Items.BARRIER : BuiltInRegistries.ITEM.get(id);
        if (item == Items.AIR) {
            item = Items.BARRIER;
        }

        ItemStack stack = new ItemStack(item, 1);
        return stack;
    }

    private static List<BattlePassClientCache.LaneEntry> rewardLanes() {
        List<BattlePassClientCache.LaneEntry> lanes = BattlePassClientCache.getLanes();
        if (!lanes.isEmpty()) {
            return lanes;
        }

        return List.of(
                new BattlePassClientCache.LaneEntry("basic", "Basic Core", true, -1),
                new BattlePassClientCache.LaneEntry("northium", "Northium Core", true, -1),
                new BattlePassClientCache.LaneEntry("integrated", "Integrated Core", true, -1)
        );
    }

    private record HoveredReward(BattlePassClientCache.RewardEntry reward, ItemStack icon) {
    }

    private record RewardTrackLayout(List<Integer> visibleLevelIndices, int scroll, int maxScroll) {
    }

    private static ThemedUi themed(GuiGraphics guiGraphics) {
        return new ThemedUi(guiGraphics, THEME.theme());
    }

    private record MissionCategory(boolean weekly, int week, Component label, Component progressLabel) {
        private static MissionCategory forWeek(int week, CategoryProgress progress) {
            return new MissionCategory(
                    true,
                    week,
                    Component.literal("Week " + week),
                    Component.literal(progress.completed() + "/" + progress.cap() + " completions")
            );
        }

        private static MissionCategory permanent(CategoryProgress progress) {
            return new MissionCategory(
                    false,
                    0,
                    Component.translatable("screen.incore.battle_pass.task_permanent"),
                    Component.literal(progress.completed() + "/" + progress.cap() + " completions")
            );
        }

        private boolean matches(BattlePassClientCache.TaskEntry task) {
            return this.weekly ? task.weekly() && task.week() == this.week : !task.weekly();
        }
    }

    private record CategoryProgress(int completed, int cap) {
    }

    private enum Tab {
        REWARDS,
        MISSIONS
    }
}
