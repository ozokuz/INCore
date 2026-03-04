package io.github.ozokuz.incore.client.features.tasks;

import io.github.ozokuz.incore.client.ui.UIScreenTheme;
import io.github.ozokuz.incore.client.ui.render.ThemedUi;
import io.github.ozokuz.incore.features.tasks.network.TaskNetworking;
import net.minecraft.ChatFormatting;
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

public class TaskOverviewScreen extends Screen {
    private static final UIScreenTheme THEME = UIScreenTheme.BATTLEPASS_TASKS;
    private static final int TARGET_WINDOW_WIDTH = 560;
    private static final int TARGET_WINDOW_HEIGHT = 320;
    private static final int SIDEBAR_TARGET_WIDTH = 166;
    private static final int CARD_GAP = 6;
    private static final int MIN_CARD_HEIGHT = 30;
    private static final int MAX_CARD_HEIGHT = 40;
    private static final int TIER_TRACK_HEIGHT = 62;
    private static final int PANEL_PADDING = 8;
    private static final int HEADER_TO_LIST_GAP = 8;
    private static final int BUTTON_HEIGHT = 20;
    private static final int WEEKLY_CARD_HEIGHT = 36;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SCROLLBAR_GAP = 3;
    private static final int MIN_SCROLLBAR_THUMB_HEIGHT = 12;
    private static final int REWARD_ICON_SIZE = 16;
    private static final int REWARD_ICON_GAP = 2;
    private static final int CONTENT_PADDING_X = 15;
    private static final int CONTENT_PADDING_TOP = 24;
    private static final int CONTENT_PADDING_BOTTOM = 15;
    private static final int MIN_WINDOW_WIDTH = 252;
    private static final int MIN_WINDOW_HEIGHT = 140;
    private static final int COLOR_WINDOW_FILL = UIScreenTheme.BattlepassTasks.WINDOW_FILL;
    private static final int COLOR_WINDOW_BORDER_LIGHT = UIScreenTheme.BattlepassTasks.WINDOW_BORDER_LIGHT;
    private static final int COLOR_WINDOW_BORDER_DARK = UIScreenTheme.BattlepassTasks.WINDOW_BORDER_DARK;
    private static final int COLOR_BACKDROP = UIScreenTheme.BattlepassTasks.PANEL_FILL;
    private static final int COLOR_PANEL = UIScreenTheme.BattlepassTasks.PANEL_FILL;
    private static final int COLOR_PANEL_BORDER = UIScreenTheme.BattlepassTasks.BORDER_MUTED;
    private static final int COLOR_CARD = UIScreenTheme.BattlepassTasks.CARD_FILL_DEFAULT;
    private static final int COLOR_CARD_COMPLETE = UIScreenTheme.BattlepassTasks.CARD_FILL_COMPLETE;
    private static final int COLOR_ACCENT = UIScreenTheme.BattlepassTasks.ACCENT_GOLD;
    private static final int COLOR_ACCENT_COMPLETE = UIScreenTheme.BattlepassTasks.PROGRESS_FILL_COMPLETE;
    private static final int COLOR_TEXT_PRIMARY = UIScreenTheme.BattlepassTasks.TEXT_PRIMARY;
    private static final int COLOR_TEXT_SECONDARY = UIScreenTheme.BattlepassTasks.TEXT_SECONDARY;
    private static final int COLOR_CARD_TEXT_DARK = UIScreenTheme.BattlepassTasks.TEXT_SOFT;
    private static final int COLOR_CARD_TEXT_MID = UIScreenTheme.BattlepassTasks.TEXT_MUTED;

    private Integer previousMenuBlur;
    private Button claimDailyButton;
    private Button claimWeeklyButton;
    private int weeklyScroll;
    private boolean draggingWeeklyScrollbar;
    private int weeklyScrollbarDragOffset;

    public TaskOverviewScreen() {
        super(Component.translatable("screen.incore.tasks.title"));
    }

    @Override
    protected void init() {
        if (this.previousMenuBlur == null) {
            this.previousMenuBlur = this.minecraft.options.getMenuBackgroundBlurriness();
            if (this.previousMenuBlur > 0) {
                this.minecraft.options.menuBackgroundBlurriness().set(0);
            }
        }
        this.weeklyScroll = 0;
        this.draggingWeeklyScrollbar = false;

        this.claimDailyButton = this.addRenderableWidget(Button.builder(
                Component.translatable("screen.incore.tasks.claim_daily"),
                button -> TaskNetworking.requestDailyRewardClaim()
        ).bounds(0, 0, 90, 20).build());
        this.claimWeeklyButton = this.addRenderableWidget(Button.builder(
                Component.translatable("screen.incore.tasks.claim_weekly"),
                button -> TaskNetworking.requestWeeklyRewardsClaim()
        ).bounds(0, 0, 110, 20).build());
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
        TaskClientCache.TaskSnapshot snapshot = TaskClientCache.snapshot();
        int windowLeft = this.windowLeft();
        int windowTop = this.windowTop();
        int windowWidth = this.windowWidth();
        int windowHeight = this.windowHeight();

        ContentLayout layout = this.contentLayout();
        this.updateClaimButtons(snapshot, layout.sidebarLeft(), layout.sidebarRight(), layout.contentTop(), layout.mainLeft(), layout.mainRight(), layout.contentBottom());

        themed(guiGraphics).drawBackdrop(this.width, this.height);
        drawWindowPanel(guiGraphics, windowLeft, windowTop, windowLeft + windowWidth, windowTop + windowHeight);
        guiGraphics.fill(layout.contentLeft(), layout.contentTop(), layout.contentRight(), layout.contentBottom(), COLOR_BACKDROP);
        guiGraphics.drawString(this.font, this.title, windowLeft + 12, windowTop + 8, COLOR_TEXT_PRIMARY, false);

        HoveredReward hovered = this.drawSidebar(
                guiGraphics,
                snapshot,
                layout.sidebarLeft(),
                layout.contentTop(),
                layout.sidebarRight(),
                layout.contentBottom(),
                mouseX,
                mouseY
        );
        HoveredReward hoveredInMain = this.drawMainPanel(
                guiGraphics,
                snapshot,
                layout.mainLeft(),
                layout.contentTop(),
                layout.mainRight(),
                layout.contentBottom(),
                mouseX,
                mouseY
        );
        if (hoveredInMain != null) {
            hovered = hoveredInMain;
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (hovered != null) {
            this.renderRewardTooltip(guiGraphics, hovered.reward(), hovered.stack(), mouseX, mouseY);
        }
    }

    private HoveredReward drawSidebar(
            GuiGraphics guiGraphics,
            TaskClientCache.TaskSnapshot snapshot,
            int left,
            int top,
            int right,
            int bottom,
            int mouseX,
            int mouseY
    ) {
        drawPanel(guiGraphics, left, top, right, bottom, COLOR_PANEL, COLOR_PANEL_BORDER);

        int x = left + PANEL_PADDING;
        int y = top + PANEL_PADDING;
        int availableWidth = Math.max(40, right - left - (PANEL_PADDING * 2));

        String headerText = "Daily: " + snapshot.fixedDailyCompleted() + "/7";
        guiGraphics.drawString(this.font, Component.literal(headerText), x, y, COLOR_TEXT_PRIMARY, false);
        y += 12;

        int rewardsLabelY = this.claimDailyButton != null ? this.claimDailyButton.getY() - 32 : bottom - 32;
        int listBottom = rewardsLabelY - 6;
        int rowHeight = 22;
        int progressBarWidth = 8;
        int progressBarGap = 4;

        int rowGap = 2;
        int totalTasks = 7;
        int completedTasks = snapshot.fixedDailyCompleted();
        int progressBarHeight = totalTasks * rowHeight + (totalTasks - 1) * rowGap;
        int filledHeight = Math.round((float) completedTasks / totalTasks * progressBarHeight);
        int emptyProgressColor = UIScreenTheme.BattlepassTasks.PROGRESS_BORDER;
        int filledProgressColor = COLOR_ACCENT;

        int barX = x;
        guiGraphics.fill(barX, y, barX + progressBarWidth, y + progressBarHeight, emptyProgressColor);
        if (filledHeight > 0) {
            int filledY = y + progressBarHeight - filledHeight;
            guiGraphics.fill(barX, filledY, barX + progressBarWidth, y + progressBarHeight, filledProgressColor);
        }

        int taskListX = x + progressBarWidth + progressBarGap;
        int taskListWidth = availableWidth - progressBarWidth - progressBarGap;

        if (snapshot.fixedDailyTasks().isEmpty()) {
            guiGraphics.drawString(this.font, Component.translatable("screen.incore.tasks.no_daily"), taskListX, y, COLOR_TEXT_SECONDARY);
        } else {
            List<TaskClientCache.DailyTaskEntry> sortedTasks = snapshot.fixedDailyTasks().stream()
                .sorted((a, b) -> {
                    boolean aComplete = a.progress() >= a.goal();
                    boolean bComplete = b.progress() >= b.goal();
                    return Boolean.compare(aComplete, bComplete);
                })
                .toList();
            for (TaskClientCache.DailyTaskEntry entry : sortedTasks) {
                int rowBottom = y + rowHeight;
                if (rowBottom > listBottom) {
                    break;
                }

                int progress = Math.min(entry.progress(), entry.goal());
                boolean complete = progress >= entry.goal();
                int rowFill = complete ? UIScreenTheme.BattlepassTasks.ROW_FILL_COMPLETE : COLOR_CARD;
                int rowBorder = complete ? UIScreenTheme.BattlepassTasks.ROW_BORDER_COMPLETE : COLOR_PANEL_BORDER;
                guiGraphics.fill(taskListX, y, right - PANEL_PADDING, rowBottom, rowFill);
                drawRectBorder(guiGraphics, taskListX, y, right - PANEL_PADDING, rowBottom, rowBorder);

                String titleText = ellipsize(entry.title(), taskListWidth - 8);
                int titleColor = complete ? COLOR_ACCENT_COMPLETE : COLOR_CARD_TEXT_DARK;
                guiGraphics.drawString(this.font, Component.literal(titleText), taskListX + 4, y + 3, titleColor, false);
                String progressText;
                if (complete) {
                    progressText = "\u2713";
                } else {
                    progressText = progress + "/" + entry.goal();
                }
                int progressTextWidth = this.font.width(progressText);
                guiGraphics.drawString(this.font, Component.literal(progressText), right - PANEL_PADDING - 2 - progressTextWidth, y + 3, COLOR_CARD_TEXT_MID, false);

                y += rowHeight + 2;
            }
        }

        guiGraphics.drawString(this.font, Component.translatable("screen.incore.tasks.daily_reward"), x, rewardsLabelY, COLOR_TEXT_SECONDARY);
        return this.drawRewardIcons(
                guiGraphics,
                snapshot.dailyRewards(),
                x,
                rewardsLabelY + 11,
                right - PANEL_PADDING,
                mouseX,
                mouseY,
                4
        );
    }

    private HoveredReward drawMainPanel(
            GuiGraphics guiGraphics,
            TaskClientCache.TaskSnapshot snapshot,
            int left,
            int top,
            int right,
            int bottom,
            int mouseX,
            int mouseY
    ) {
        drawPanel(guiGraphics, left, top, right, bottom, COLOR_PANEL, COLOR_PANEL_BORDER);

        int headerX = left + PANEL_PADDING + 2;
        int headerY = top + PANEL_PADDING;
        guiGraphics.drawString(this.font, Component.translatable("screen.incore.tasks.weekly_routine"), headerX, headerY, COLOR_TEXT_PRIMARY);
        String refreshText = Component.translatable("screen.incore.tasks.refreshes_weekly").getString();
        guiGraphics.drawString(this.font, Component.literal(refreshText), headerX, headerY + 10, COLOR_TEXT_SECONDARY);

        int tierTop = bottom - TIER_TRACK_HEIGHT;
        List<TaskClientCache.TaskEntry> sortedWeekly = sortedWeeklyEntries(snapshot.weekly());
        WeeklyListMetrics metrics = this.weeklyListMetrics(left, top, right, bottom, sortedWeekly.size());

        if (sortedWeekly.isEmpty()) {
            guiGraphics.drawString(this.font, Component.translatable("screen.incore.tasks.no_weekly"), headerX, metrics.rowsTop() + 8, COLOR_TEXT_SECONDARY);
            drawWeeklyScrollbar(guiGraphics, metrics);
            return this.drawTierTrack(guiGraphics, snapshot, left + PANEL_PADDING, tierTop, right - PANEL_PADDING, bottom - 6, mouseX, mouseY);
        }

        this.weeklyScroll = Math.clamp(this.weeklyScroll, 0, metrics.maxScroll());
        int y = metrics.rowsTop();
        for (int i = 0; i < metrics.visibleRows(); i++) {
            int index = this.weeklyScroll + i;
            if (index >= sortedWeekly.size()) {
                break;
            }
            TaskClientCache.TaskEntry entry = sortedWeekly.get(index);
            int cardBottom = y + WEEKLY_CARD_HEIGHT;
            drawWeeklyCard(guiGraphics, entry, metrics.rowsLeft(), y, metrics.rowsRight(), cardBottom);
            y = cardBottom + CARD_GAP;
        }
        drawWeeklyScrollbar(guiGraphics, metrics);

        return this.drawTierTrack(guiGraphics, snapshot, left + PANEL_PADDING, tierTop, right - PANEL_PADDING, bottom - 6, mouseX, mouseY);
    }

    private void drawWeeklyCard(GuiGraphics guiGraphics, TaskClientCache.TaskEntry entry, int left, int top, int right, int bottom) {
        int progress = Math.min(entry.progress(), entry.goal());
        boolean complete = progress >= entry.goal();
        int cardFill = complete ? COLOR_CARD_COMPLETE : COLOR_CARD;
        int cardBorder = complete ? UIScreenTheme.BattlepassTasks.CHIP_BORDER_COMPLETE : COLOR_PANEL_BORDER;
        guiGraphics.fill(left, top, right, bottom, cardFill);
        drawRectBorder(guiGraphics, left, top, right, bottom, cardBorder);

        int badgeWidth = 52;
        int badgeRight = Math.min(right - 8, left + badgeWidth);
        int badgeFill = complete ? UIScreenTheme.BattlepassTasks.BADGE_FILL_COMPLETE : UIScreenTheme.BattlepassTasks.BADGE_FILL_DEFAULT;
        guiGraphics.fill(left + 1, top + 1, badgeRight, bottom - 1, badgeFill);
        guiGraphics.fill(badgeRight - 1, top + 1, badgeRight, bottom - 1, complete ? UIScreenTheme.BattlepassTasks.CHIP_BORDER_COMPLETE : UIScreenTheme.BattlepassTasks.BADGE_EDGE_INACTIVE);

        String pointsText = "▲ " + entry.points();
        int pointsTextWidth = this.font.width(pointsText);
        int pointsY = top + ((bottom - top - this.font.lineHeight) / 2);
        guiGraphics.drawString(this.font, Component.literal(pointsText), left + (badgeWidth - pointsTextWidth) / 2, pointsY, COLOR_ACCENT, false);

        int contentLeft = badgeRight + 6;
        int pillWidth = 80;
        int contentRight = Math.max(contentLeft + 24, right - pillWidth - 10);
        String titleText = ellipsize(entry.title(), Math.max(20, contentRight - contentLeft));
        guiGraphics.drawString(this.font, Component.literal(titleText), contentLeft, top + 4, COLOR_CARD_TEXT_DARK, false);
        drawProgressBar(
                guiGraphics,
                contentLeft,
                bottom - 9,
                Math.max(24, contentRight - contentLeft),
                5,
                progressRatio(progress, entry.goal()),
                UIScreenTheme.BattlepassTasks.PROGRESS_BORDER,
                complete ? COLOR_ACCENT_COMPLETE : COLOR_ACCENT
        );
        String progressText = progress + "/" + entry.goal();
        guiGraphics.drawString(this.font, Component.literal(progressText), contentLeft, top + 15, COLOR_CARD_TEXT_MID, false);

        int pillLeft = right - pillWidth - 8;
        int pillTop = top + (bottom - top - 14) / 2;
        int pillBottom = pillTop + 14;
        int pillColor = complete ? UIScreenTheme.BattlepassTasks.CHIP_FILL_COMPLETE : UIScreenTheme.BattlepassTasks.CHIP_FILL_DEFAULT;
        int pillBorder = complete ? UIScreenTheme.BattlepassTasks.CHIP_BORDER_COMPLETE : UIScreenTheme.BattlepassTasks.CHIP_BORDER_DEFAULT;
        guiGraphics.fill(pillLeft, pillTop, pillLeft + pillWidth, pillBottom, pillColor);
        drawRectBorder(guiGraphics, pillLeft, pillTop, pillLeft + pillWidth, pillBottom, pillBorder);
        String stateText = complete
                ? Component.translatable("screen.incore.tasks.status_complete").getString()
                : Component.translatable("screen.incore.tasks.status_in_progress").getString();
        guiGraphics.drawString(
                this.font,
                Component.literal(ellipsize(stateText, pillWidth - 8)),
                pillLeft + 4,
                pillTop + 3,
                COLOR_TEXT_PRIMARY,
                false
        );
    }

    private void updateClaimButtons(
            TaskClientCache.TaskSnapshot snapshot,
            int sidebarLeft,
            int sidebarRight,
            int mainTop,
            int mainLeft,
            int mainRight,
            int contentBottom
    ) {
        if (this.claimDailyButton == null || this.claimWeeklyButton == null) {
            return;
        }

        int dailyButtonWidth = Math.max(90, sidebarRight - sidebarLeft - 16);
        int dailyButtonX = sidebarLeft + PANEL_PADDING;
        int dailyButtonY = contentBottom - 26;
        this.claimDailyButton.setX(dailyButtonX);
        this.claimDailyButton.setY(dailyButtonY);
        this.claimDailyButton.setWidth(dailyButtonWidth);

        boolean dailyClaimable = snapshot.fixedDailyAllCompleted() && !snapshot.fixedDailyRewardClaimed();
        this.claimDailyButton.active = dailyClaimable;
        if (dailyClaimable) {
            this.claimDailyButton.setMessage(Component.translatable("screen.incore.tasks.claim_daily"));
        } else if (snapshot.fixedDailyRewardClaimed()) {
            this.claimDailyButton.setMessage(Component.translatable("screen.incore.tasks.claimed_daily"));
        } else {
            this.claimDailyButton.setMessage(Component.translatable("screen.incore.tasks.claim_daily_locked"));
        }

        int weeklyButtonWidth = Math.min(170, Math.max(126, mainRight - mainLeft - 16));
        int weeklyButtonX = mainRight - weeklyButtonWidth - PANEL_PADDING;
        int weeklyButtonY = mainTop + PANEL_PADDING - 2;
        this.claimWeeklyButton.setX(weeklyButtonX);
        this.claimWeeklyButton.setY(weeklyButtonY);
        this.claimWeeklyButton.setWidth(weeklyButtonWidth);

        int claimableWeekly = countClaimableWeeklyTiers(snapshot);
        this.claimWeeklyButton.active = claimableWeekly > 0;
        if (claimableWeekly > 0) {
            this.claimWeeklyButton.setMessage(Component.translatable("screen.incore.tasks.claim_weekly_count", claimableWeekly));
        } else {
            this.claimWeeklyButton.setMessage(Component.translatable("screen.incore.tasks.claim_weekly_locked"));
        }
    }

    private static int countClaimableWeeklyTiers(TaskClientCache.TaskSnapshot snapshot) {
        int claimable = 0;
        for (TaskClientCache.TierEntry tier : snapshot.tiers()) {
            if (tier.unlocked() && !tier.claimed()) {
                claimable++;
            }
        }
        return claimable;
    }

    private static List<TaskClientCache.TaskEntry> sortedWeeklyEntries(List<TaskClientCache.TaskEntry> weekly) {
        if (weekly == null || weekly.isEmpty()) {
            return List.of();
        }
        return weekly.stream()
                .sorted(Comparator
                        .comparingInt(TaskClientCache.TaskEntry::points)
                        .reversed()
                        .thenComparing(entry -> entry.title() == null ? "" : entry.title(), String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private WeeklyListMetrics weeklyListMetrics(int left, int top, int right, int bottom, int totalEntries) {
        int tierTop = bottom - TIER_TRACK_HEIGHT;
        int listTop = top + PANEL_PADDING + 16;
        if (this.claimWeeklyButton != null) {
            listTop = Math.max(listTop, this.claimWeeklyButton.getY() + BUTTON_HEIGHT + HEADER_TO_LIST_GAP);
        }
        int listBottom = tierTop - 8;

        int rowsLeft = left + PANEL_PADDING;
        int rowsRight = right - PANEL_PADDING - SCROLLBAR_WIDTH - SCROLLBAR_GAP;
        int rowsTop = listTop;
        int rowsBottom = listBottom;
        int visibleRows = Math.max(1, (rowsBottom - rowsTop + CARD_GAP) / (WEEKLY_CARD_HEIGHT + CARD_GAP));
        int maxScroll = Math.max(0, totalEntries - visibleRows);

        int trackLeft = rowsRight + SCROLLBAR_GAP;
        int trackRight = trackLeft + SCROLLBAR_WIDTH;
        int trackTop = rowsTop;
        int trackBottom = rowsBottom;

        return new WeeklyListMetrics(rowsLeft, rowsRight, rowsTop, rowsBottom, visibleRows, maxScroll, trackLeft, trackRight, trackTop, trackBottom);
    }

    private void drawWeeklyScrollbar(GuiGraphics guiGraphics, WeeklyListMetrics metrics) {
        guiGraphics.fill(metrics.trackLeft(), metrics.trackTop(), metrics.trackRight(), metrics.trackBottom(), UIScreenTheme.BattlepassTasks.SCROLL_TRACK_FILL);
        drawRectBorder(guiGraphics, metrics.trackLeft(), metrics.trackTop(), metrics.trackRight(), metrics.trackBottom(), COLOR_PANEL_BORDER);

        if (metrics.maxScroll() <= 0) {
            guiGraphics.fill(metrics.trackLeft() + 1, metrics.trackTop() + 1, metrics.trackRight() - 1, metrics.trackBottom() - 1, UIScreenTheme.BattlepassTasks.SCROLL_TRACK_EMPTY_FILL);
            return;
        }

        int thumbTop = this.weeklyThumbTop(metrics);
        int thumbBottom = thumbTop + this.weeklyThumbHeight(metrics);
        guiGraphics.fill(metrics.trackLeft() + 1, thumbTop, metrics.trackRight() - 1, thumbBottom, UIScreenTheme.BattlepassTasks.SCROLL_THUMB_FILL);
        drawRectBorder(guiGraphics, metrics.trackLeft() + 1, thumbTop, metrics.trackRight() - 1, thumbBottom, UIScreenTheme.BattlepassTasks.SCROLL_THUMB_BORDER);
    }

    private int weeklyThumbHeight(WeeklyListMetrics metrics) {
        int trackHeight = Math.max(1, metrics.trackBottom() - metrics.trackTop());
        if (metrics.maxScroll() <= 0) {
            return trackHeight;
        }
        int estimated = Math.round((float) trackHeight * (float) metrics.visibleRows() / (float) (metrics.visibleRows() + metrics.maxScroll()));
        return Math.max(MIN_SCROLLBAR_THUMB_HEIGHT, Math.min(trackHeight, estimated));
    }

    private int weeklyThumbTop(WeeklyListMetrics metrics) {
        int trackTop = metrics.trackTop();
        int trackHeight = Math.max(1, metrics.trackBottom() - metrics.trackTop());
        int thumbHeight = this.weeklyThumbHeight(metrics);
        int travel = Math.max(0, trackHeight - thumbHeight);
        if (metrics.maxScroll() <= 0 || travel <= 0) {
            return trackTop;
        }
        int clampedScroll = Math.clamp(this.weeklyScroll, 0, metrics.maxScroll());
        int offset = Math.round((float) clampedScroll / (float) metrics.maxScroll() * (float) travel);
        return trackTop + offset;
    }

    private int weeklyScrollFromThumb(WeeklyListMetrics metrics, int thumbTop) {
        int trackHeight = Math.max(1, metrics.trackBottom() - metrics.trackTop());
        int thumbHeight = this.weeklyThumbHeight(metrics);
        int travel = Math.max(1, trackHeight - thumbHeight);
        int clampedThumbTop = Math.clamp(thumbTop, metrics.trackTop(), metrics.trackTop() + travel);
        float ratio = (float) (clampedThumbTop - metrics.trackTop()) / (float) travel;
        return Math.round(ratio * metrics.maxScroll());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY == 0.0D) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        ContentLayout layout = this.contentLayout();
        List<TaskClientCache.TaskEntry> sortedWeekly = sortedWeeklyEntries(TaskClientCache.snapshot().weekly());
        WeeklyListMetrics metrics = this.weeklyListMetrics(
                layout.mainLeft(),
                layout.contentTop(),
                layout.mainRight(),
                layout.contentBottom(),
                sortedWeekly.size()
        );
        if (metrics.maxScroll() <= 0) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        if (mouseX < metrics.rowsLeft() || mouseX > metrics.trackRight() || mouseY < metrics.rowsTop() || mouseY > metrics.rowsBottom()) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        int direction = scrollY > 0.0D ? -1 : 1;
        this.weeklyScroll = Math.clamp(this.weeklyScroll + direction, 0, metrics.maxScroll());
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            ContentLayout layout = this.contentLayout();
            List<TaskClientCache.TaskEntry> sortedWeekly = sortedWeeklyEntries(TaskClientCache.snapshot().weekly());
            WeeklyListMetrics metrics = this.weeklyListMetrics(
                    layout.mainLeft(),
                    layout.contentTop(),
                    layout.mainRight(),
                    layout.contentBottom(),
                    sortedWeekly.size()
            );

            if (metrics.maxScroll() > 0
                    && mouseX >= metrics.trackLeft() && mouseX <= metrics.trackRight()
                    && mouseY >= metrics.trackTop() && mouseY <= metrics.trackBottom()) {
                int thumbTop = this.weeklyThumbTop(metrics);
                int thumbBottom = thumbTop + this.weeklyThumbHeight(metrics);
                if (mouseY >= thumbTop && mouseY <= thumbBottom) {
                    this.draggingWeeklyScrollbar = true;
                    this.weeklyScrollbarDragOffset = (int) mouseY - thumbTop;
                } else {
                    int newThumbTop = (int) mouseY - this.weeklyThumbHeight(metrics) / 2;
                    this.weeklyScroll = Math.clamp(this.weeklyScrollFromThumb(metrics, newThumbTop), 0, metrics.maxScroll());
                }
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && this.draggingWeeklyScrollbar) {
            ContentLayout layout = this.contentLayout();
            List<TaskClientCache.TaskEntry> sortedWeekly = sortedWeeklyEntries(TaskClientCache.snapshot().weekly());
            WeeklyListMetrics metrics = this.weeklyListMetrics(
                    layout.mainLeft(),
                    layout.contentTop(),
                    layout.mainRight(),
                    layout.contentBottom(),
                    sortedWeekly.size()
            );

            int proposedThumbTop = (int) mouseY - this.weeklyScrollbarDragOffset;
            this.weeklyScroll = Math.clamp(this.weeklyScrollFromThumb(metrics, proposedThumbTop), 0, metrics.maxScroll());
            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && this.draggingWeeklyScrollbar) {
            this.draggingWeeklyScrollbar = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private HoveredReward drawTierTrack(
            GuiGraphics guiGraphics,
            TaskClientCache.TaskSnapshot snapshot,
            int left,
            int top,
            int right,
            int bottom,
            int mouseX,
            int mouseY
    ) {
        int maxRequiredPoints = 0;
        for (TaskClientCache.TierEntry tier : snapshot.tiers()) {
            maxRequiredPoints = Math.max(maxRequiredPoints, tier.requiredPoints());
        }
        int currentPoints = Math.max(0, snapshot.weeklyPoints());
        int clampedPoints = maxRequiredPoints > 0 ? Math.min(currentPoints, maxRequiredPoints) : currentPoints;

        guiGraphics.drawString(this.font, Component.translatable("screen.incore.tasks.tiers"), left + 2, top, COLOR_TEXT_SECONDARY);
        String pointsSummary = Component.translatable("screen.incore.tasks.tier_points_progress", clampedPoints, maxRequiredPoints).getString();
        String pointsIcon = "▲";
        int pointsTextWidth = this.font.width(pointsSummary);
        int pointsTextX = right - 2 - pointsTextWidth;
        int pointsIconX = pointsTextX - this.font.width(pointsIcon) - 3;
        guiGraphics.drawString(this.font, Component.literal(pointsIcon), pointsIconX, top, COLOR_ACCENT);
        guiGraphics.drawString(this.font, Component.literal(pointsSummary), pointsTextX, top, COLOR_TEXT_SECONDARY);

        int barHeight = 5;
        int barGap = 3;
        int rowTop = top + 10;
        int rowBottom = bottom - (barHeight + barGap + 2);
        int tiers = Math.max(1, snapshot.tiers().size());
        int totalGap = (tiers - 1) * 4;
        int slotWidth = Math.max(24, (right - left - totalGap) / tiers);
        int usedRowWidth = Math.max(0, tiers * slotWidth + totalGap);
        int rowLeft = left + Math.max(0, (right - left - usedRowWidth) / 2);
        int slotLeft = rowLeft;
        HoveredReward hovered = null;

        for (int i = 0; i < tiers; i++) {
            int slotRight = Math.min(right, slotLeft + slotWidth);
            if (i < snapshot.tiers().size()) {
                TaskClientCache.TierEntry tier = snapshot.tiers().get(i);
                int fill = tier.claimed() ? UIScreenTheme.BattlepassTasks.ROW_FILL_COMPLETE : (tier.unlocked() ? UIScreenTheme.BattlepassTasks.TIER_ROW_UNLOCKED : COLOR_CARD);
                int border = tier.claimed() ? COLOR_ACCENT_COMPLETE : (tier.unlocked() ? COLOR_ACCENT : COLOR_PANEL_BORDER);
                int pointsColor = tier.claimed() ? UIScreenTheme.BattlepassTasks.TIER_POINTS_COMPLETE : (tier.unlocked() ? UIScreenTheme.BattlepassTasks.TIER_POINTS_UNLOCKED : COLOR_TEXT_SECONDARY);
                guiGraphics.fill(slotLeft, rowTop, slotRight, rowBottom, fill);
                drawRectBorder(guiGraphics, slotLeft, rowTop, slotRight, rowBottom, border);

                String tierText = "T" + tier.tier();
                guiGraphics.drawString(this.font, Component.literal(tierText), slotLeft + 3, rowTop + 2, COLOR_TEXT_PRIMARY);
                String pointsText = pointsIcon + " " + tier.requiredPoints();
                guiGraphics.drawString(
                        this.font,
                        Component.literal(pointsText),
                        slotRight - 3 - this.font.width(pointsText),
                        rowTop + 2,
                        pointsColor
                );

                HoveredReward candidate = this.drawRewardIcons(
                        guiGraphics,
                        tier.rewards(),
                        slotLeft + 3,
                        rowBottom - REWARD_ICON_SIZE - 3,
                        slotRight - 3,
                        mouseX,
                        mouseY,
                        2
                );
                if (hovered == null && candidate != null) {
                    hovered = candidate;
                }
            } else {
                guiGraphics.fill(slotLeft, rowTop, slotRight, rowBottom, UIScreenTheme.BattlepassTasks.TIER_SLOT_FILL);
                drawRectBorder(guiGraphics, slotLeft, rowTop, slotRight, rowBottom, UIScreenTheme.BattlepassTasks.TIER_SLOT_BORDER);
            }
            slotLeft = slotRight + 4;
        }

        if (maxRequiredPoints > 0) {
            int barLeft = Math.max(left + 1, rowLeft);
            int barRight = Math.min(right - 1, rowLeft + usedRowWidth);
            int barWidth = Math.max(12, barRight - barLeft);
            int barTop = rowBottom + barGap;
            drawProgressBar(
                    guiGraphics,
                    barLeft,
                    barTop,
                    barWidth,
                    barHeight,
                    progressRatio(clampedPoints, maxRequiredPoints),
                    UIScreenTheme.BattlepassTasks.PROGRESS_BORDER,
                    COLOR_ACCENT
            );
        }
        return hovered;
    }

    private HoveredReward drawRewardIcons(
            GuiGraphics guiGraphics,
            List<TaskClientCache.RewardEntry> rewards,
            int left,
            int top,
            int right,
            int mouseX,
            int mouseY,
            int maxIcons
    ) {
        if (rewards == null || rewards.isEmpty()) {
            guiGraphics.drawString(this.font, Component.literal("-"), left, top + 4, COLOR_TEXT_SECONDARY, false);
            return null;
        }

        int availableWidth = Math.max(REWARD_ICON_SIZE, right - left);
        int iconsFit = Math.max(1, (availableWidth + REWARD_ICON_GAP) / (REWARD_ICON_SIZE + REWARD_ICON_GAP));
        int visibleIcons = Math.min(rewards.size(), Math.min(maxIcons, iconsFit));
        int iconX = left;
        HoveredReward hovered = null;

        for (int i = 0; i < visibleIcons; i++) {
            TaskClientCache.RewardEntry reward = rewards.get(i);
            ItemStack stack = iconStackFor(reward);
            guiGraphics.renderItem(stack, iconX, top);
            if ("item".equals(reward.kind())) {
                guiGraphics.renderItemDecorations(this.font, stack, iconX, top);
            }

            if (mouseX >= iconX && mouseX < iconX + REWARD_ICON_SIZE && mouseY >= top && mouseY < top + REWARD_ICON_SIZE) {
                hovered = new HoveredReward(reward, stack);
            }
            iconX += REWARD_ICON_SIZE + REWARD_ICON_GAP;
        }

        if (rewards.size() > visibleIcons) {
            String moreText = "+" + (rewards.size() - visibleIcons);
            int moreX = Math.min(iconX + 1, right - this.font.width(moreText));
            guiGraphics.drawString(this.font, Component.literal(moreText), moreX, top + 5, COLOR_TEXT_PRIMARY);
        }

        return hovered;
    }

    private static ItemStack iconStackFor(TaskClientCache.RewardEntry reward) {
        if (reward == null) {
            return new ItemStack(Items.BARRIER);
        }

        String kind = reward.kind() == null ? "" : reward.kind();
        ResourceLocation itemId = ResourceLocation.tryParse(reward.itemId());
        Item item = switch (kind) {
            case "item" -> itemId != null ? BuiltInRegistries.ITEM.get(itemId) : Items.AIR;
            case "sanity" -> {
                ResourceLocation sanityItem = ResourceLocation.tryParse("incore:sanity_vessel");
                yield sanityItem != null ? BuiltInRegistries.ITEM.get(sanityItem) : Items.EXPERIENCE_BOTTLE;
            }
            case "command" -> Items.COMMAND_BLOCK;
            default -> itemId != null ? BuiltInRegistries.ITEM.get(itemId) : Items.AIR;
        };

        if (item == Items.AIR) {
            if ("sanity".equals(kind)) {
                item = Items.EXPERIENCE_BOTTLE;
            } else if ("command".equals(kind)) {
                item = Items.COMMAND_BLOCK;
            } else {
                item = Items.BARRIER;
            }
        }

        int amount = "item".equals(kind) ? Math.max(1, reward.amount()) : 1;
        return new ItemStack(item, Math.min(99, amount));
    }

    private void renderRewardTooltip(
            GuiGraphics guiGraphics,
            TaskClientCache.RewardEntry reward,
            ItemStack stack,
            int mouseX,
            int mouseY
    ) {
        if (reward == null) {
            return;
        }
        if ("item".equals(reward.kind())) {
            guiGraphics.renderTooltip(this.font, stack, mouseX, mouseY);
            return;
        }
        guiGraphics.renderComponentTooltip(this.font, tooltipForNonItemReward(reward), mouseX, mouseY);
    }

    private static List<Component> tooltipForNonItemReward(TaskClientCache.RewardEntry reward) {
        List<Component> lines = new ArrayList<>();
        String kind = reward.kind() == null ? "" : reward.kind();
        if ("sanity".equals(kind)) {
            lines.add(Component.translatable("screen.incore.tasks.tooltip_sanity_title"));
            lines.add(Component.translatable("screen.incore.tasks.tooltip_sanity_line", Math.max(1, reward.amount())).withStyle(ChatFormatting.GRAY));
            return lines;
        }

        if ("command".equals(kind)) {
            lines.add(Component.translatable("screen.incore.tasks.tooltip_command_title"));
            if (reward.text() == null || reward.text().isBlank()) {
                lines.add(Component.translatable("screen.incore.tasks.tooltip_command_empty").withStyle(ChatFormatting.GRAY));
            } else {
                lines.add(Component.literal(reward.text()).withStyle(ChatFormatting.GRAY));
            }
            return lines;
        }

        lines.add(Component.translatable("screen.incore.tasks.tooltip_unknown_title"));
        if (reward.text() != null && !reward.text().isBlank()) {
            lines.add(Component.literal(reward.text()).withStyle(ChatFormatting.GRAY));
        }
        return lines;
    }

    private static void drawPanel(GuiGraphics guiGraphics, int left, int top, int right, int bottom, int fillColor, int borderColor) {
        ThemedUi ui = themed(guiGraphics);
        ui.drawRect(left, top, right, bottom, fillColor);
        ui.drawBorder(left, top, right, bottom, borderColor);
    }

    private static void drawRectBorder(GuiGraphics guiGraphics, int left, int top, int right, int bottom, int color) {
        themed(guiGraphics).drawBorder(left, top, right, bottom, color);
    }

    private static void drawWindowPanel(GuiGraphics guiGraphics, int left, int top, int right, int bottom) {
        themed(guiGraphics).drawWindow(left, top, right - left, bottom - top);
    }

    private static void drawProgressBar(
            GuiGraphics guiGraphics,
            int left,
            int top,
            int width,
            int height,
            float ratio,
            int emptyColor,
            int fillColor
    ) {
        int clampedWidth = Math.max(1, width);
        int clampedHeight = Math.max(1, height);
        guiGraphics.fill(left, top, left + clampedWidth, top + clampedHeight, emptyColor);
        int filled = Math.round(clampedWidth * Math.max(0.0F, Math.min(1.0F, ratio)));
        if (filled > 0) {
            guiGraphics.fill(left, top, left + filled, top + clampedHeight, fillColor);
        }
    }

    private static float progressRatio(int progress, int goal) {
        if (goal <= 0) {
            return 0.0F;
        }
        return (float) Math.max(0, progress) / (float) goal;
    }

    private String ellipsize(String text, int maxWidth) {
        if (text == null || text.isEmpty() || maxWidth <= 0) {
            return "";
        }
        if (this.font.width(text) <= maxWidth) {
            return text;
        }

        String suffix = "...";
        int suffixWidth = this.font.width(suffix);
        if (suffixWidth >= maxWidth) {
            return suffix;
        }

        int end = text.length();
        while (end > 0 && this.font.width(text.substring(0, end)) + suffixWidth > maxWidth) {
            end--;
        }
        if (end <= 0) {
            return suffix;
        }
        return text.substring(0, end) + suffix;
    }

    private ContentLayout contentLayout() {
        int contentLeft = this.windowLeft() + CONTENT_PADDING_X;
        int contentTop = this.windowTop() + CONTENT_PADDING_TOP;
        int contentRight = this.windowLeft() + this.windowWidth() - CONTENT_PADDING_X;
        int contentBottom = this.windowTop() + this.windowHeight() - CONTENT_PADDING_BOTTOM;
        int sidebarWidth = Math.min(SIDEBAR_TARGET_WIDTH, Math.max(132, (contentRight - contentLeft) / 3));
        int sidebarLeft = contentLeft;
        int sidebarRight = sidebarLeft + sidebarWidth;
        int mainLeft = sidebarRight + 8;
        int mainRight = contentRight;
        return new ContentLayout(contentLeft, contentTop, contentRight, contentBottom, sidebarLeft, sidebarRight, mainLeft, mainRight);
    }

    private static ThemedUi themed(GuiGraphics guiGraphics) {
        return new ThemedUi(guiGraphics, THEME.theme());
    }

    private int windowLeft() {
        return (this.width - this.windowWidth()) / 2;
    }

    private int windowTop() {
        return (this.height - this.windowHeight()) / 2;
    }

    private int windowWidth() {
        return Math.min(TARGET_WINDOW_WIDTH, Math.max(MIN_WINDOW_WIDTH, this.width - 16));
    }

    private int windowHeight() {
        return Math.min(TARGET_WINDOW_HEIGHT, Math.max(MIN_WINDOW_HEIGHT, this.height - 16));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record WeeklyListMetrics(
            int rowsLeft,
            int rowsRight,
            int rowsTop,
            int rowsBottom,
            int visibleRows,
            int maxScroll,
            int trackLeft,
            int trackRight,
            int trackTop,
            int trackBottom
    ) {
    }

    private record HoveredReward(TaskClientCache.RewardEntry reward, ItemStack stack) {
    }

    private record ContentLayout(
            int contentLeft,
            int contentTop,
            int contentRight,
            int contentBottom,
            int sidebarLeft,
            int sidebarRight,
            int mainLeft,
            int mainRight
    ) {
    }
}
