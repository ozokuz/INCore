package ozokuz.incore.integration.ldlib.ui.player;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import ozokuz.incore.client.ui.UIScreenTheme;
import ozokuz.incore.features.battlepass.network.BattlePassClientCache;
import ozokuz.incore.features.battlepass.network.BattlePassSyncPayload;

final class BattlePassContentElement extends UIElement {
    private final BattlePassUiSupport.BattlePassUiState state;
    private BattlePassClientCache.RewardEntry hoveredReward;
    private ItemStack hoveredStack = ItemStack.EMPTY;

    BattlePassContentElement(BattlePassUiSupport.BattlePassUiState state) {
        this.state = state;
        layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
            layout.minHeight(0);
        });
        internalSetup();
        addEventListener(UIEvents.CLICK, event -> handleClick(event.x, event.y, event.button));
        addEventListener(UIEvents.MOUSE_WHEEL, event -> handleScroll(event.deltaY));
        addEventListener(UIEvents.HOVER_TOOLTIPS, event -> {
            if (this.hoveredReward == null) {
                return;
            }
            event.hoverTooltips = new HoverTooltips(
                    BattlePassUiSupport.tooltipForReward(this.hoveredReward, this.hoveredStack),
                    null,
                    BattlePassUiSupport.font(),
                    this.hoveredStack
            );
        });
    }

    @Override
    public void drawBackgroundAdditional(GUIContext guiContext) {
        this.hoveredReward = null;
        this.hoveredStack = ItemStack.EMPTY;

        int left = Math.round(getPositionX());
        int top = Math.round(getPositionY());
        int width = Math.round(getSizeWidth());
        int height = Math.round(getSizeHeight());
        if (width <= 0 || height <= 0) {
            return;
        }

        int headerX = left;
        int headerY = top;
        int headerWidth = width;
        BattlePassUiSupport.themed(guiContext.graphics).drawPanel(headerX, headerY, headerWidth, BattlePassUiSupport.HEADER_HEIGHT);
        drawHeader(guiContext, headerX, headerY, headerWidth);

        int contentX = left;
        int contentY = top + BattlePassUiSupport.HEADER_HEIGHT + 8;
        int contentWidth = width;
        int contentHeight = Math.max(0, height - BattlePassUiSupport.HEADER_HEIGHT - 8);

        guiContext.graphics.fill(contentX, contentY, contentX + contentWidth, contentY + contentHeight, UIScreenTheme.BattlepassTasks.PANEL_FILL);

        if (!BattlePassClientCache.hasActiveSet()) {
            guiContext.graphics.drawCenteredString(
                    BattlePassUiSupport.font(),
                    Component.translatable("screen.incore.battle_pass.none"),
                    contentX + contentWidth / 2,
                    contentY + Math.max(0, (contentHeight / 2) - 4),
                    UIScreenTheme.BattlepassTasks.NONE_TEXT
            );
            return;
        }

        if (this.state.activeTab() == BattlePassUiSupport.BattlePassTab.REWARDS) {
            renderRewardsTab(guiContext, contentX, contentY, contentWidth, contentHeight);
        } else {
            renderMissionsTab(guiContext, contentX, contentY, contentWidth, contentHeight);
        }
    }

    private void drawHeader(GUIContext guiContext, int x, int y, int width) {
        int level = BattlePassClientCache.getLevel();
        int xpPerLevel = BattlePassClientCache.getXpPerLevel();
        int xpIntoLevel = BattlePassClientCache.getXpIntoCurrentLevel();
        int xpTotal = BattlePassClientCache.getXp();

        Component setDisplay = BattlePassUiSupport.localizeSetId(BattlePassClientCache.getSetId());
        guiContext.graphics.fill(x + 8, y + 8, x + 145, y + BattlePassUiSupport.HEADER_HEIGHT - 8, UIScreenTheme.BattlepassTasks.HEADER_CHIP_FILL);
        guiContext.graphics.drawString(BattlePassUiSupport.font(), Component.translatable("screen.incore.battle_pass.set", setDisplay), x + 14, y + 14, UIScreenTheme.BattlepassTasks.HEADER_TITLE_TEXT, false);
        guiContext.graphics.drawString(BattlePassUiSupport.font(), Component.translatable("screen.incore.battle_pass.week", BattlePassClientCache.getCurrentWeek(), BattlePassClientCache.getTotalWeeks()), x + 14, y + 28, UIScreenTheme.BattlepassTasks.HEADER_META_TEXT, false);
        guiContext.graphics.drawString(BattlePassUiSupport.font(), Component.translatable("screen.incore.battle_pass.time_left", BattlePassUiSupport.formatTimeLeft(BattlePassClientCache.getEndsAtMillis())), x + 14, y + 42, UIScreenTheme.BattlepassTasks.HEADER_META_TEXT, false);

        int progressX = x + 164;
        int progressWidth = Math.max(40, width - 174);
        Component levelTitle = Component.literal(level + " " + Component.translatable("screen.incore.battle_pass.level_short").getString());
        long now = BattlePassClientCache.getServerNowMillis();
        long startsAtMillis = BattlePassClientCache.getStartsAtMillis();
        long endsAtMillis = BattlePassClientCache.getEndsAtMillis();
        Component seasonWindow = Component.translatable(
                "screen.incore.battle_pass.window",
                BattlePassUiSupport.formatDuration(Math.max(0L, now - startsAtMillis), false),
                BattlePassUiSupport.formatDuration(Math.max(0L, endsAtMillis - startsAtMillis), false)
        );
        guiContext.graphics.drawString(BattlePassUiSupport.font(), levelTitle, progressX, y + 12, UIScreenTheme.BattlepassTasks.TEXT_WHITE, false);
        int seasonWindowX = progressX + Math.max(96, progressWidth - BattlePassUiSupport.font().width(seasonWindow));
        guiContext.graphics.drawString(BattlePassUiSupport.font(), seasonWindow, seasonWindowX, y + 12, UIScreenTheme.BattlepassTasks.HEADER_SEASON_TEXT, false);

        drawProgressBar(guiContext, progressX, y + 30, progressWidth, xpIntoLevel, xpPerLevel);
        Component xpLine = Component.translatable("screen.incore.battle_pass.xp", xpIntoLevel, xpPerLevel);
        Component totalXpLine = Component.translatable("screen.incore.battle_pass.total_xp", xpTotal);
        guiContext.graphics.drawString(BattlePassUiSupport.font(), xpLine, progressX, y + 40, UIScreenTheme.BattlepassTasks.HEADER_XP_TEXT, false);
        int totalXpX = progressX + Math.max(126, progressWidth - BattlePassUiSupport.font().width(totalXpLine));
        guiContext.graphics.drawString(BattlePassUiSupport.font(), totalXpLine, totalXpX, y + 40, UIScreenTheme.BattlepassTasks.HEADER_TOTAL_XP_TEXT, false);

        Component capsLine = Component.translatable(
                "screen.incore.battle_pass.caps",
                BattlePassClientCache.getWeeklyCompleted(),
                BattlePassClientCache.getWeeklyCap(),
                BattlePassClientCache.getPermanentCompleted()
        );
        Component unclaimedLine = Component.translatable("screen.incore.battle_pass.unclaimed", BattlePassClientCache.getUnclaimedRewardLevels());
        int capsY = y + 54;
        int unclaimedX = progressX + progressWidth - BattlePassUiSupport.font().width(unclaimedLine);
        int unclaimedY = capsY;
        int capsEndX = progressX + BattlePassUiSupport.font().width(capsLine);
        if (unclaimedX <= capsEndX + 8) {
            unclaimedX = progressX;
            unclaimedY = capsY + 10;
        }

        guiContext.graphics.drawString(BattlePassUiSupport.font(), capsLine, progressX, capsY, UIScreenTheme.BattlepassTasks.TEXT_SECONDARY, false);
        guiContext.graphics.drawString(BattlePassUiSupport.font(), unclaimedLine, unclaimedX, unclaimedY, UIScreenTheme.BattlepassTasks.TEXT_WARNING, false);
    }

    private void renderRewardsTab(GUIContext guiContext, int x, int y, int width, int height) {
        List<BattlePassClientCache.RewardLevelEntry> levels = BattlePassUiSupport.orderedRewardLevels();
        List<BattlePassClientCache.LaneEntry> lanes = BattlePassUiSupport.rewardLanes();
        if (levels.isEmpty()) {
            guiContext.graphics.drawCenteredString(
                    BattlePassUiSupport.font(),
                    Component.translatable("screen.incore.battle_pass.rewards_empty"),
                    x + width / 2,
                    y + height / 2 - 4,
                    UIScreenTheme.BattlepassTasks.EMPTY_TEXT
            );
            return;
        }

        int highestConfiguredLevel = levels.getLast().level();
        int trackCount = Math.max(1, Math.min(3, lanes.size()));
        int trackLabelWidth = 126;
        int cardsX = x + trackLabelWidth + 8;
        int cardsWidth = Math.max(1, width - trackLabelWidth - 14);
        int columns = Math.max(1, cardsWidth / (BattlePassUiSupport.REWARD_CARD_WIDTH + BattlePassUiSupport.REWARD_CARD_GAP));
        BattlePassUiSupport.RewardTrackLayout trackLayout = BattlePassUiSupport.buildRewardTrackLayout(
                levels,
                columns,
                this.state.rewardLevelScroll(),
                this.state.rewardAutoFocus()
        );
        this.state.setRewardLevelScroll(trackLayout.scroll());
        this.state.clearRewardAutoFocus();

        int levelHeaderY = y + 10;
        int gridY = y + 28;
        for (int col = 0; col < trackLayout.visibleLevelIndices().size(); col++) {
            int levelIndex = trackLayout.visibleLevelIndices().get(col);
            int cardX = cardsX + col * (BattlePassUiSupport.REWARD_CARD_WIDTH + BattlePassUiSupport.REWARD_CARD_GAP);
            Component levelText = Component.literal("Lv " + levels.get(levelIndex).level());
            int textX = cardX + (BattlePassUiSupport.REWARD_CARD_WIDTH - BattlePassUiSupport.font().width(levelText)) / 2;
            guiContext.graphics.drawString(BattlePassUiSupport.font(), levelText, textX, levelHeaderY, UIScreenTheme.BattlepassTasks.REWARD_LEVEL_TEXT, false);
        }

        int mouseX = guiContext.mouseX;
        int mouseY = guiContext.mouseY;
        for (int track = 0; track < trackCount; track++) {
            int rowY = gridY + track * (BattlePassUiSupport.REWARD_CARD_HEIGHT + 8);
            BattlePassClientCache.LaneEntry lane = lanes.get(track);
            drawTrackLabel(guiContext, x + 6, rowY, trackLabelWidth - 10, BattlePassUiSupport.REWARD_CARD_HEIGHT, lane);

            for (int col = 0; col < trackLayout.visibleLevelIndices().size(); col++) {
                int levelIndex = trackLayout.visibleLevelIndices().get(col);
                BattlePassClientCache.RewardLevelEntry levelEntry = levels.get(levelIndex);
                int cardX = cardsX + col * (BattlePassUiSupport.REWARD_CARD_WIDTH + BattlePassUiSupport.REWARD_CARD_GAP);
                int rewardLevel = levelEntry.level();
                boolean laneUnlocked = lane.unlocked();
                boolean levelUnlocked = rewardLevel <= BattlePassClientCache.getLevel();
                boolean claimed = laneUnlocked && rewardLevel <= lane.highestClaimedLevel();
                boolean unclaimed = laneUnlocked && !claimed && levelUnlocked;
                boolean unlocked = laneUnlocked && levelUnlocked;
                boolean selected = rewardLevel == this.state.selectedRewardLevel() && track == this.state.selectedRewardTrack();
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

                guiContext.graphics.fill(cardX, rowY, cardX + BattlePassUiSupport.REWARD_CARD_WIDTH, rowY + BattlePassUiSupport.REWARD_CARD_HEIGHT, bg);
                drawBoxBorder(guiContext, cardX, rowY, BattlePassUiSupport.REWARD_CARD_WIDTH, BattlePassUiSupport.REWARD_CARD_HEIGHT, border);

                BattlePassClientCache.RewardEntry reward = track < levelEntry.rewards().size() ? levelEntry.rewards().get(track) : null;
                if (reward != null && reward.kind() == BattlePassSyncPayload.REWARD_KIND_NONE) {
                    reward = null;
                }
                if (reward == null) {
                    guiContext.graphics.drawCenteredString(
                            BattlePassUiSupport.font(),
                            Component.literal("-"),
                            cardX + BattlePassUiSupport.REWARD_CARD_WIDTH / 2,
                            rowY + 16,
                            UIScreenTheme.BattlepassTasks.REWARD_PLACEHOLDER_TEXT
                    );
                    continue;
                }

                ItemStack icon = BattlePassUiSupport.iconForReward(reward);
                int iconX = cardX + (BattlePassUiSupport.REWARD_CARD_WIDTH - 16) / 2;
                int iconY = rowY + 5;
                guiContext.graphics.renderItem(icon, iconX, iconY);
                if (reward.amount() > 1) {
                    String quantity = "x" + reward.amount();
                    int quantityWidth = BattlePassUiSupport.font().width(quantity);
                    int quantityX = cardX + (BattlePassUiSupport.REWARD_CARD_WIDTH - quantityWidth) / 2;
                    int quantityY = rowY + BattlePassUiSupport.REWARD_CARD_HEIGHT - BattlePassUiSupport.font().lineHeight - 3;
                    guiContext.graphics.fill(quantityX - 2, quantityY - 1, quantityX + quantityWidth + 2, quantityY + BattlePassUiSupport.font().lineHeight, UIScreenTheme.BattlepassTasks.QUANTITY_CHIP_FILL);
                    guiContext.graphics.drawString(BattlePassUiSupport.font(), quantity, quantityX, quantityY, UIScreenTheme.BattlepassTasks.QUANTITY_TEXT, false);
                }

                if (!laneUnlocked) {
                    guiContext.graphics.fill(cardX, rowY, cardX + BattlePassUiSupport.REWARD_CARD_WIDTH, rowY + BattlePassUiSupport.REWARD_CARD_HEIGHT, UIScreenTheme.BattlepassTasks.REWARD_OVERLAY_CLAIMED);
                    drawBoxBorder(guiContext, cardX, rowY, BattlePassUiSupport.REWARD_CARD_WIDTH, BattlePassUiSupport.REWARD_CARD_HEIGHT, border);
                } else if (!levelUnlocked) {
                    guiContext.graphics.fill(cardX, rowY, cardX + BattlePassUiSupport.REWARD_CARD_WIDTH, rowY + BattlePassUiSupport.REWARD_CARD_HEIGHT, UIScreenTheme.BattlepassTasks.REWARD_OVERLAY_LOCKED);
                    drawBoxBorder(guiContext, cardX, rowY, BattlePassUiSupport.REWARD_CARD_WIDTH, BattlePassUiSupport.REWARD_CARD_HEIGHT, border);
                }

                boolean hovered = mouseX >= cardX && mouseX < cardX + BattlePassUiSupport.REWARD_CARD_WIDTH
                        && mouseY >= rowY && mouseY < rowY + BattlePassUiSupport.REWARD_CARD_HEIGHT;
                if (hovered) {
                    this.hoveredReward = reward;
                    this.hoveredStack = icon;
                }
            }
        }

        if (this.state.selectedRewardLevel() >= 0) {
            BattlePassClientCache.RewardLevelEntry selectedLevel = levels.stream()
                    .filter(entry -> entry.level() == this.state.selectedRewardLevel())
                    .findFirst()
                    .orElse(null);
            if (selectedLevel != null) {
                int infoY = y + Math.max(0, height - 34);
                guiContext.graphics.drawString(BattlePassUiSupport.font(), Component.translatable("screen.incore.battle_pass.rewards_selected", selectedLevel.level()), x + 8, infoY, UIScreenTheme.BattlepassTasks.DETAILS_TEXT, false);
                guiContext.graphics.drawString(BattlePassUiSupport.font(), Component.translatable("screen.incore.battle_pass.selected_xp_to_reach", selectedLevel.requiredXp()), x + 8, infoY + 10, UIScreenTheme.BattlepassTasks.HEADER_META_TEXT, false);
                guiContext.graphics.drawString(BattlePassUiSupport.font(), Component.translatable("screen.incore.battle_pass.selected_xp_for_level", selectedLevel.xpForLevel()), x + 8, infoY + 20, UIScreenTheme.BattlepassTasks.HEADER_META_TEXT, false);
            }
        }
    }

    private void drawTrackLabel(
            GUIContext guiContext,
            int x,
            int y,
            int width,
            int height,
            BattlePassClientCache.LaneEntry lane
    ) {
        String laneId = lane.id();
        int color;
        Component text = Component.literal(lane.displayName());
        if ("basic".equals(laneId)) {
            color = UIScreenTheme.BattlepassTasks.BADGE_FILL_DEFAULT;
        } else if ("originium".equals(laneId) || "northium".equals(laneId)) {
            color = UIScreenTheme.BattlepassTasks.REQUIREMENT_FILL_WARNING;
        } else {
            color = UIScreenTheme.BattlepassTasks.REQUIREMENT_FILL_DANGER;
        }
        if (!lane.unlocked()) {
            color = UIScreenTheme.BattlepassTasks.REQUIREMENT_FILL_BLOCKED;
            text = Component.literal("\uD83D\uDD12 ").append(text);
        }

        guiContext.graphics.fill(x, y, x + width, y + height, color);
        drawBoxBorder(guiContext, x, y, width, height, UIScreenTheme.BattlepassTasks.BORDER_DARK);
        guiContext.graphics.drawString(BattlePassUiSupport.font(), text, x + 8, y + 12, UIScreenTheme.BattlepassTasks.HEADER_TITLE_TEXT, false);
    }

    private void renderMissionsTab(GUIContext guiContext, int x, int y, int width, int height) {
        List<BattlePassClientCache.TaskEntry> allTasks = new ArrayList<>(BattlePassClientCache.getTasks());
        if (allTasks.isEmpty()) {
            guiContext.graphics.drawCenteredString(
                    BattlePassUiSupport.font(),
                    Component.translatable("screen.incore.battle_pass.tasks_empty"),
                    x + width / 2,
                    y + height / 2 - 4,
                    UIScreenTheme.BattlepassTasks.EMPTY_TEXT
            );
            return;
        }

        List<BattlePassUiSupport.MissionCategory> categories = BattlePassUiSupport.buildMissionCategories(allTasks, BattlePassClientCache.getCurrentWeek());
        if (categories.isEmpty()) {
            guiContext.graphics.drawCenteredString(
                    BattlePassUiSupport.font(),
                    Component.translatable("screen.incore.battle_pass.tasks_empty"),
                    x + width / 2,
                    y + height / 2 - 4,
                    UIScreenTheme.BattlepassTasks.EMPTY_TEXT
            );
            return;
        }

        this.state.setSelectedMissionCategoryIndex(BattlePassUiSupport.clamp(this.state.selectedMissionCategoryIndex(), 0, categories.size() - 1));
        int categoryX = x + 6;
        int categoryY = y + 10;
        for (int i = 0; i < categories.size(); i++) {
            int rowY = categoryY + i * (BattlePassUiSupport.MISSION_CATEGORY_ROW_HEIGHT + BattlePassUiSupport.MISSION_CATEGORY_GAP);
            if (rowY + BattlePassUiSupport.MISSION_CATEGORY_ROW_HEIGHT > y + height - 22) {
                break;
            }

            boolean selectedCategory = i == this.state.selectedMissionCategoryIndex();
            int bg = selectedCategory ? UIScreenTheme.BattlepassTasks.REQUIREMENT_FILL_WARNING : UIScreenTheme.BattlepassTasks.LIST_FILL_DEFAULT;
            int border = selectedCategory ? UIScreenTheme.BattlepassTasks.ACCENT_GOLD : UIScreenTheme.BattlepassTasks.BORDER_MUTED;
            guiContext.graphics.fill(categoryX, rowY, categoryX + BattlePassUiSupport.MISSION_CATEGORY_WIDTH, rowY + BattlePassUiSupport.MISSION_CATEGORY_ROW_HEIGHT, bg);
            drawBoxBorder(guiContext, categoryX, rowY, BattlePassUiSupport.MISSION_CATEGORY_WIDTH, BattlePassUiSupport.MISSION_CATEGORY_ROW_HEIGHT, border);
            BattlePassUiSupport.MissionCategory category = categories.get(i);
            guiContext.graphics.drawString(BattlePassUiSupport.font(), category.label(), categoryX + 8, rowY + 7, UIScreenTheme.BattlepassTasks.TEXT_SOFT, false);
            guiContext.graphics.drawString(BattlePassUiSupport.font(), category.progressLabel(), categoryX + 8, rowY + 18, UIScreenTheme.BattlepassTasks.TEXT_MUTED, false);
        }

        BattlePassUiSupport.MissionCategory selectedCategory = categories.get(this.state.selectedMissionCategoryIndex());
        List<BattlePassClientCache.TaskEntry> tasks = BattlePassUiSupport.tasksForCategory(allTasks, selectedCategory);
        int listX = x + BattlePassUiSupport.MISSION_CATEGORY_WIDTH + 12;
        int listWidth = width - BattlePassUiSupport.MISSION_CATEGORY_WIDTH - 20;
        int listTop = y + 10;
        int listBottom = y + height - 22;
        int listHeight = listBottom - listTop;
        int visibleRows = Math.max(1, listHeight / BattlePassUiSupport.MISSION_ROW_HEIGHT);
        int maxScroll = Math.max(0, tasks.size() - visibleRows);
        this.state.setMissionScroll(BattlePassUiSupport.clamp(this.state.missionScroll(), 0, maxScroll));
        BattlePassClientCache.TaskEntry selectedTask = null;

        for (int i = 0; i < visibleRows; i++) {
            int index = this.state.missionScroll() + i;
            if (index >= tasks.size()) {
                break;
            }

            BattlePassClientCache.TaskEntry task = tasks.get(index);
            int rowY = listTop + i * BattlePassUiSupport.MISSION_ROW_HEIGHT;
            boolean selected = task.id().equals(this.state.selectedMissionTaskId());
            if (selected) {
                selectedTask = task;
            }

            int rowBg = selected ? UIScreenTheme.BattlepassTasks.TASK_ROW_FILL_SELECTED : UIScreenTheme.BattlepassTasks.LIST_FILL_DEFAULT;
            guiContext.graphics.fill(listX, rowY, listX + listWidth, rowY + BattlePassUiSupport.MISSION_ROW_HEIGHT - 4, rowBg);
            drawBoxBorder(guiContext, listX, rowY, listWidth, BattlePassUiSupport.MISSION_ROW_HEIGHT - 4, selected ? UIScreenTheme.BattlepassTasks.ACCENT_GOLD : UIScreenTheme.BattlepassTasks.BORDER_MUTED);

            Component type = task.weekly()
                    ? Component.translatable("screen.incore.battle_pass.task_weekly", task.week())
                    : Component.translatable("screen.incore.battle_pass.task_permanent");
            guiContext.graphics.drawString(BattlePassUiSupport.font(), type, listX + 6, rowY + 5, UIScreenTheme.BattlepassTasks.TASK_TYPE_TEXT, false);
            guiContext.graphics.drawString(BattlePassUiSupport.font(), Component.literal("+" + task.xpReward() + " XP"), listX + listWidth - 72, rowY + 5, UIScreenTheme.BattlepassTasks.TASK_XP_TEXT, false);

            int tierColor = BattlePassUiSupport.tierColor(task.tier());
            String line = BattlePassUiSupport.truncate(task.description(), listWidth - 228);
            guiContext.graphics.drawString(BattlePassUiSupport.font(), Component.literal(line), listX + 6, rowY + 18, UIScreenTheme.BattlepassTasks.TEXT_SOFT, false);
            guiContext.graphics.drawString(BattlePassUiSupport.font(), Component.literal(BattlePassUiSupport.capitalize(task.tier())), listX + listWidth - 132, rowY + 18, tierColor, false);

            int statusColor = task.completed()
                    ? UIScreenTheme.BattlepassTasks.TASK_STATUS_COMPLETE
                    : (task.completableNow() ? UIScreenTheme.BattlepassTasks.DETAILS_TEXT : UIScreenTheme.BattlepassTasks.TASK_STATUS_LOCKED);
            int progressCurrent = Math.min(task.progressCurrent(), task.progressGoal());
            int progressGoal = Math.max(1, task.progressGoal());
            int progressBarWidth = 152;
            int progressBarX = listX + listWidth - progressBarWidth - 10;
            int progressBarY = rowY + 30;
            drawTaskProgressBar(guiContext, progressBarX, progressBarY, progressBarWidth, 6, progressCurrent, progressGoal, task.completed(), task.completableNow());

            String progressLabel = progressCurrent + "/" + progressGoal;
            guiContext.graphics.drawString(BattlePassUiSupport.font(), Component.literal(progressLabel), progressBarX + progressBarWidth - BattlePassUiSupport.font().width(progressLabel), rowY + 20, UIScreenTheme.BattlepassTasks.DETAILS_TEXT, false);

            int statusMaxWidth = Math.max(30, progressBarX - (listX + 10));
            guiContext.graphics.drawString(BattlePassUiSupport.font(), Component.literal(BattlePassUiSupport.truncate(task.status(), statusMaxWidth)), listX + 6, rowY + 31, statusColor, false);
        }

        if (selectedTask != null) {
            guiContext.graphics.drawString(BattlePassUiSupport.font(), Component.translatable("screen.incore.battle_pass.task_id", selectedTask.id()), listX, y + height - 12, UIScreenTheme.BattlepassTasks.FOOTER_TASK_ID_TEXT, false);
        } else {
            guiContext.graphics.drawString(BattlePassUiSupport.font(), Component.translatable("screen.incore.battle_pass.task_select_hint"), listX, y + height - 12, UIScreenTheme.BattlepassTasks.FOOTER_HINT_TEXT, false);
        }
    }

    private void handleClick(double mouseX, double mouseY, int button) {
        if (button != 0 || !BattlePassClientCache.hasActiveSet()) {
            return;
        }

        int left = Math.round(getPositionX());
        int top = Math.round(getPositionY());
        int width = Math.round(getSizeWidth());
        int height = Math.round(getSizeHeight());
        int contentX = left;
        int contentY = top + BattlePassUiSupport.HEADER_HEIGHT + 8;
        int contentWidth = width;
        int contentHeight = Math.max(0, height - BattlePassUiSupport.HEADER_HEIGHT - 8);
        if (mouseX < contentX || mouseX >= contentX + contentWidth || mouseY < contentY || mouseY >= contentY + contentHeight) {
            return;
        }

        if (this.state.activeTab() == BattlePassUiSupport.BattlePassTab.MISSIONS) {
            List<BattlePassClientCache.TaskEntry> allTasks = new ArrayList<>(BattlePassClientCache.getTasks());
            List<BattlePassUiSupport.MissionCategory> categories = BattlePassUiSupport.buildMissionCategories(allTasks, BattlePassClientCache.getCurrentWeek());
            if (categories.isEmpty()) {
                return;
            }

            this.state.setSelectedMissionCategoryIndex(BattlePassUiSupport.clamp(this.state.selectedMissionCategoryIndex(), 0, categories.size() - 1));
            int categoryX = contentX + 6;
            int categoryY = contentY + 10;
            for (int i = 0; i < categories.size(); i++) {
                int rowY = categoryY + i * (BattlePassUiSupport.MISSION_CATEGORY_ROW_HEIGHT + BattlePassUiSupport.MISSION_CATEGORY_GAP);
                if (rowY + BattlePassUiSupport.MISSION_CATEGORY_ROW_HEIGHT > contentY + contentHeight - 22) {
                    break;
                }

                if (mouseX >= categoryX && mouseX < categoryX + BattlePassUiSupport.MISSION_CATEGORY_WIDTH
                        && mouseY >= rowY && mouseY < rowY + BattlePassUiSupport.MISSION_CATEGORY_ROW_HEIGHT) {
                    this.state.setSelectedMissionCategoryIndex(i);
                    this.state.setMissionScroll(0);
                    this.state.setSelectedMissionTaskId(null);
                    return;
                }
            }

            BattlePassUiSupport.MissionCategory selectedCategory = categories.get(this.state.selectedMissionCategoryIndex());
            List<BattlePassClientCache.TaskEntry> tasks = BattlePassUiSupport.tasksForCategory(allTasks, selectedCategory);
            int listX = contentX + BattlePassUiSupport.MISSION_CATEGORY_WIDTH + 12;
            int listTop = contentY + 10;
            int listBottom = contentY + contentHeight - 22;
            int listWidth = contentWidth - BattlePassUiSupport.MISSION_CATEGORY_WIDTH - 20;
            if (mouseX >= listX && mouseX <= listX + listWidth && mouseY >= listTop && mouseY <= listBottom) {
                int row = (int) ((mouseY - listTop) / BattlePassUiSupport.MISSION_ROW_HEIGHT);
                int listHeight = listBottom - listTop;
                int visibleRows = Math.max(1, listHeight / BattlePassUiSupport.MISSION_ROW_HEIGHT);
                int maxScroll = Math.max(0, tasks.size() - visibleRows);
                this.state.setMissionScroll(BattlePassUiSupport.clamp(this.state.missionScroll(), 0, maxScroll));
                int index = this.state.missionScroll() + row;
                if (index >= 0 && index < tasks.size()) {
                    this.state.setSelectedMissionTaskId(tasks.get(index).id());
                }
            }
            return;
        }

        List<BattlePassClientCache.RewardLevelEntry> levels = BattlePassUiSupport.orderedRewardLevels();
        List<BattlePassClientCache.LaneEntry> lanes = BattlePassUiSupport.rewardLanes();
        if (levels.isEmpty()) {
            return;
        }

        int trackCount = Math.max(1, Math.min(3, lanes.size()));
        int trackLabelWidth = 126;
        int cardsX = contentX + trackLabelWidth + 8;
        int cardsWidth = contentWidth - trackLabelWidth - 14;
        int columns = Math.max(1, cardsWidth / (BattlePassUiSupport.REWARD_CARD_WIDTH + BattlePassUiSupport.REWARD_CARD_GAP));
        BattlePassUiSupport.RewardTrackLayout trackLayout = BattlePassUiSupport.buildRewardTrackLayout(levels, columns, this.state.rewardLevelScroll(), false);
        this.state.setRewardLevelScroll(trackLayout.scroll());
        int gridY = contentY + 28;

        for (int track = 0; track < trackCount; track++) {
            int rowY = gridY + track * (BattlePassUiSupport.REWARD_CARD_HEIGHT + 8);
            for (int col = 0; col < trackLayout.visibleLevelIndices().size(); col++) {
                int levelIndex = trackLayout.visibleLevelIndices().get(col);
                int cardX = cardsX + col * (BattlePassUiSupport.REWARD_CARD_WIDTH + BattlePassUiSupport.REWARD_CARD_GAP);
                if (mouseX >= cardX && mouseX < cardX + BattlePassUiSupport.REWARD_CARD_WIDTH
                        && mouseY >= rowY && mouseY < rowY + BattlePassUiSupport.REWARD_CARD_HEIGHT) {
                    this.state.setSelectedRewardLevel(levels.get(levelIndex).level());
                    this.state.setSelectedRewardTrack(track);
                    return;
                }
            }
        }
    }

    private void handleScroll(double deltaY) {
        if (deltaY == 0.0D || !BattlePassClientCache.hasActiveSet()) {
            return;
        }

        int direction = deltaY > 0.0D ? -1 : 1;
        if (this.state.activeTab() == BattlePassUiSupport.BattlePassTab.MISSIONS) {
            List<BattlePassClientCache.TaskEntry> allTasks = new ArrayList<>(BattlePassClientCache.getTasks());
            List<BattlePassUiSupport.MissionCategory> categories = BattlePassUiSupport.buildMissionCategories(allTasks, BattlePassClientCache.getCurrentWeek());
            if (categories.isEmpty()) {
                return;
            }

            this.state.setSelectedMissionCategoryIndex(BattlePassUiSupport.clamp(this.state.selectedMissionCategoryIndex(), 0, categories.size() - 1));
            BattlePassUiSupport.MissionCategory selectedCategory = categories.get(this.state.selectedMissionCategoryIndex());
            List<BattlePassClientCache.TaskEntry> tasks = BattlePassUiSupport.tasksForCategory(allTasks, selectedCategory);
            int listHeight = Math.max(0, Math.round(getSizeHeight()) - BattlePassUiSupport.HEADER_HEIGHT - 30);
            int visibleRows = Math.max(1, listHeight / BattlePassUiSupport.MISSION_ROW_HEIGHT);
            int maxScroll = Math.max(0, tasks.size() - visibleRows);
            this.state.setMissionScroll(BattlePassUiSupport.clamp(this.state.missionScroll() + direction, 0, maxScroll));
            return;
        }

        int contentWidth = Math.round(getSizeWidth());
        int trackLabelWidth = 126;
        int cardsWidth = contentWidth - trackLabelWidth - 14;
        List<BattlePassClientCache.RewardLevelEntry> levels = BattlePassUiSupport.orderedRewardLevels();
        int columns = Math.max(1, cardsWidth / (BattlePassUiSupport.REWARD_CARD_WIDTH + BattlePassUiSupport.REWARD_CARD_GAP));
        BattlePassUiSupport.RewardTrackLayout trackLayout = BattlePassUiSupport.buildRewardTrackLayout(levels, columns, this.state.rewardLevelScroll(), false);
        this.state.setRewardLevelScroll(BattlePassUiSupport.clamp(trackLayout.scroll() + direction, 0, trackLayout.maxScroll()));
    }

    private void drawProgressBar(GUIContext guiContext, int x, int y, int width, int current, int max) {
        int clampedMax = Math.max(1, max);
        int clampedCurrent = Math.max(0, Math.min(current, clampedMax));
        drawSlicedXpBarSprite(guiContext, BattlePassUiSupport.XP_BAR_BACKGROUND, x, y, width);
        int filled = Math.max(0, Math.min(width, Math.round(width * (clampedCurrent / (float) clampedMax))));
        if (filled > 0) {
            guiContext.graphics.enableScissor(x, y, x + filled, y + BattlePassUiSupport.XP_BAR_HEIGHT);
            drawSlicedXpBarSprite(guiContext, BattlePassUiSupport.XP_BAR_PROGRESS, x, y, width);
            guiContext.graphics.disableScissor();
        }
    }

    private void drawSlicedXpBarSprite(GUIContext guiContext, ResourceLocation sprite, int x, int y, int width) {
        int barWidth = Math.max(1, width);
        if (barWidth <= BattlePassUiSupport.XP_BAR_CAP_WIDTH * 2) {
            guiContext.graphics.blitSprite(sprite, BattlePassUiSupport.XP_BAR_TEXTURE_WIDTH, BattlePassUiSupport.XP_BAR_TEXTURE_HEIGHT, 0, 0, x, y, barWidth, BattlePassUiSupport.XP_BAR_HEIGHT);
            return;
        }

        int centerWidth = BattlePassUiSupport.XP_BAR_TEXTURE_WIDTH - (BattlePassUiSupport.XP_BAR_CAP_WIDTH * 2);
        int rightX = x + barWidth - BattlePassUiSupport.XP_BAR_CAP_WIDTH;
        guiContext.graphics.blitSprite(sprite, BattlePassUiSupport.XP_BAR_TEXTURE_WIDTH, BattlePassUiSupport.XP_BAR_TEXTURE_HEIGHT, 0, 0, x, y, BattlePassUiSupport.XP_BAR_CAP_WIDTH, BattlePassUiSupport.XP_BAR_HEIGHT);
        guiContext.graphics.blitSprite(
                sprite,
                BattlePassUiSupport.XP_BAR_TEXTURE_WIDTH,
                BattlePassUiSupport.XP_BAR_TEXTURE_HEIGHT,
                BattlePassUiSupport.XP_BAR_TEXTURE_WIDTH - BattlePassUiSupport.XP_BAR_CAP_WIDTH,
                0,
                rightX,
                y,
                BattlePassUiSupport.XP_BAR_CAP_WIDTH,
                BattlePassUiSupport.XP_BAR_HEIGHT
        );

        int drawX = x + BattlePassUiSupport.XP_BAR_CAP_WIDTH;
        int remaining = barWidth - (BattlePassUiSupport.XP_BAR_CAP_WIDTH * 2);
        while (remaining > 0) {
            int chunk = Math.min(centerWidth, remaining);
            guiContext.graphics.blitSprite(sprite, BattlePassUiSupport.XP_BAR_TEXTURE_WIDTH, BattlePassUiSupport.XP_BAR_TEXTURE_HEIGHT, BattlePassUiSupport.XP_BAR_CAP_WIDTH, 0, drawX, y, chunk, BattlePassUiSupport.XP_BAR_HEIGHT);
            drawX += chunk;
            remaining -= chunk;
        }
    }

    private void drawTaskProgressBar(
            GUIContext guiContext,
            int x,
            int y,
            int width,
            int height,
            int current,
            int goal,
            boolean completed,
            boolean available
    ) {
        int background = available ? UIScreenTheme.BattlepassTasks.PROGRESS_BG_AVAILABLE : UIScreenTheme.BattlepassTasks.PROGRESS_BG_LOCKED;
        int fill = completed
                ? UIScreenTheme.BattlepassTasks.PROGRESS_FILL_CLAIMED
                : (available ? UIScreenTheme.BattlepassTasks.PROGRESS_FILL_AVAILABLE : UIScreenTheme.BattlepassTasks.PROGRESS_FILL_LOCKED);
        int border = completed ? UIScreenTheme.BattlepassTasks.PROGRESS_BORDER_CLAIMED : UIScreenTheme.BattlepassTasks.PROGRESS_BORDER;
        guiContext.graphics.fill(x, y, x + width, y + height, background);
        int clampedGoal = Math.max(1, goal);
        int clampedCurrent = Math.max(0, Math.min(current, clampedGoal));
        int fillWidth = Math.max(0, Math.min(width, Math.round(width * (clampedCurrent / (float) clampedGoal))));
        if (fillWidth > 0) {
            guiContext.graphics.fill(x, y, x + fillWidth, y + height, fill);
        }
        drawBoxBorder(guiContext, x, y, width, height, border);
    }

    private void drawBoxBorder(GUIContext guiContext, int x, int y, int width, int height, int color) {
        BattlePassUiSupport.themed(guiContext.graphics).drawBorder(x, y, x + width, y + height, color);
    }
}
