package io.github.ozokuz.incore.client.tasks;

import io.github.ozokuz.incore.client.status.AdvancementWindowRenderer;
import io.github.ozokuz.incore.features.tasks.client.TaskClientCache;
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
    private static final int COLOR_BACKDROP = 0xB014171D;
    private static final int COLOR_PANEL = 0x7A101419;
    private static final int COLOR_PANEL_BORDER = 0x8FE6E6E6;
    private static final int COLOR_ACCENT = 0xFFE6CC33;
    private static final int COLOR_TEXT_PRIMARY = 0xF3F3F3;
    private static final int COLOR_TEXT_SECONDARY = 0xC8C8C8;
    private static final int COLOR_CARD_TEXT_DARK = 0xFFF1F2F4;
    private static final int COLOR_CARD_TEXT_MID = 0xFFD2D7DE;

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

        int insideLeft = windowLeft + AdvancementWindowRenderer.BORDER_LEFT;
        int insideTop = windowTop + AdvancementWindowRenderer.BORDER_TOP;
        int insideRight = windowLeft + windowWidth - AdvancementWindowRenderer.BORDER_RIGHT;
        int insideBottom = windowTop + windowHeight - AdvancementWindowRenderer.BORDER_BOTTOM;

        int contentLeft = insideLeft + 6;
        int contentTop = insideTop + 6;
        int contentRight = insideRight - 6;
        int contentBottom = insideBottom - 6;

        int sidebarWidth = Math.min(SIDEBAR_TARGET_WIDTH, Math.max(132, (contentRight - contentLeft) / 3));
        int sidebarLeft = contentLeft;
        int sidebarRight = sidebarLeft + sidebarWidth;
        int mainLeft = sidebarRight + 8;
        int mainRight = contentRight;
        this.updateClaimButtons(snapshot, sidebarLeft, sidebarRight, contentTop, mainLeft, mainRight, contentBottom);

        AdvancementWindowRenderer.draw(guiGraphics, windowLeft, windowTop, windowWidth, windowHeight);
        guiGraphics.fill(insideLeft, insideTop, insideRight, insideBottom, COLOR_BACKDROP);

        int titleY = windowTop + (AdvancementWindowRenderer.BORDER_TOP - this.font.lineHeight) / 2 + 1;
        guiGraphics.drawString(this.font, this.title, windowLeft + AdvancementWindowRenderer.BORDER_LEFT + 8, titleY, COLOR_TEXT_PRIMARY);

        HoveredReward hovered = this.drawSidebar(guiGraphics, snapshot, sidebarLeft, contentTop, sidebarRight, contentBottom, mouseX, mouseY);
        HoveredReward hoveredInMain = this.drawMainPanel(guiGraphics, snapshot, mainLeft, contentTop, mainRight, contentBottom, mouseX, mouseY);
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

        String dailyState = snapshot.fixedDailyAllCompleted()
                ? Component.translatable("screen.incore.tasks.daily_complete").getString()
                : Component.translatable("screen.incore.tasks.daily_in_progress").getString();
        String headerText = "Daily: " + snapshot.fixedDailyCompleted() + "/7";
        guiGraphics.drawString(this.font, Component.literal(headerText), x, y, COLOR_TEXT_SECONDARY);
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
        int emptyProgressColor = 0xFF3A3F47;
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

                guiGraphics.fill(taskListX, y, right - PANEL_PADDING, rowBottom, 0x8A333A44);
                guiGraphics.fill(taskListX, rowBottom - 1, right - PANEL_PADDING, rowBottom, 0x55FFFFFF);

                int progress = Math.min(entry.progress(), entry.goal());
                boolean complete = progress >= entry.goal();
                String titleText = ellipsize(entry.title(), taskListWidth - 8);
                int titleColor = complete ? 0xFF6FD980 : 0xFFFFFFFF;
                guiGraphics.drawString(this.font, Component.literal(titleText), taskListX + 4, y + 3, titleColor);
                String progressText;
                if (complete) {
                    progressText = "\u2713";
                } else {
                    progressText = progress + "/" + entry.goal();
                }
                int progressTextWidth = this.font.width(progressText);
                guiGraphics.drawString(this.font, Component.literal(progressText), right - PANEL_PADDING - 2 - progressTextWidth, y + 3, 0xE5E5E5);

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
        guiGraphics.fill(left, top, right, bottom, 0xDA363C46);
        guiGraphics.fill(left, top, left + 1, bottom, 0xA0F7F7F7);
        guiGraphics.fill(left, top, right, top + 1, 0xA0F7F7F7);
        guiGraphics.fill(left, bottom - 1, right, bottom, 0x99000000);

        int badgeWidth = 52;
        int badgeRight = Math.min(right - 8, left + badgeWidth);
        guiGraphics.fill(left, top, badgeRight, bottom, 0xE22A3038);
        guiGraphics.fill(badgeRight - 1, top + 2, badgeRight, bottom - 2, 0x88FFFFFF);

        String pointsText = "▲ " + entry.points();
        int pointsTextWidth = this.font.width(pointsText);
        int pointsY = top + ((bottom - top - this.font.lineHeight) / 2);
        guiGraphics.drawString(this.font, Component.literal(pointsText), left + (badgeWidth - pointsTextWidth) / 2, pointsY, COLOR_ACCENT);

        int contentLeft = badgeRight + 6;
        int pillWidth = 80;
        int contentRight = right - pillWidth - 10;
        int progress = Math.min(entry.progress(), entry.goal());
        boolean complete = progress >= entry.goal();
        String titleText = ellipsize(entry.title(), Math.max(20, contentRight - contentLeft));
        guiGraphics.drawString(this.font, Component.literal(titleText), contentLeft, top + 4, COLOR_CARD_TEXT_DARK, true);
        drawProgressBar(guiGraphics, contentLeft, bottom - 9, Math.max(24, contentRight - contentLeft), 5, progressRatio(progress, entry.goal()), 0xFF5C6572, COLOR_ACCENT);
        String progressText = progress + "/" + entry.goal();
        guiGraphics.drawString(this.font, Component.literal(progressText), contentLeft, top + 15, COLOR_CARD_TEXT_MID, true);

        int pillLeft = right - pillWidth - 8;
        int pillTop = top + (bottom - top - 14) / 2;
        int pillBottom = pillTop + 14;
        int pillColor = complete ? 0xAA3A7A45 : 0xAA4B4B54;
        guiGraphics.fill(pillLeft, pillTop, pillLeft + pillWidth, pillBottom, pillColor);
        guiGraphics.fill(pillLeft, pillTop, pillLeft + pillWidth, pillTop + 1, 0x66FFFFFF);
        String stateText = complete
                ? Component.translatable("screen.incore.tasks.status_complete").getString()
                : Component.translatable("screen.incore.tasks.status_in_progress").getString();
        guiGraphics.drawString(
                this.font,
                Component.literal(ellipsize(stateText, pillWidth - 8)),
                pillLeft + 4,
                pillTop + 3,
                COLOR_TEXT_PRIMARY,
                true
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
        guiGraphics.fill(metrics.trackLeft(), metrics.trackTop(), metrics.trackRight(), metrics.trackBottom(), 0x500F1318);
        guiGraphics.fill(metrics.trackLeft(), metrics.trackTop(), metrics.trackRight(), metrics.trackTop() + 1, 0x70FFFFFF);
        guiGraphics.fill(metrics.trackLeft(), metrics.trackBottom() - 1, metrics.trackRight(), metrics.trackBottom(), 0x70101010);

        if (metrics.maxScroll() <= 0) {
            guiGraphics.fill(metrics.trackLeft() + 1, metrics.trackTop() + 1, metrics.trackRight() - 1, metrics.trackBottom() - 1, 0x55353940);
            return;
        }

        int thumbTop = this.weeklyThumbTop(metrics);
        int thumbBottom = thumbTop + this.weeklyThumbHeight(metrics);
        guiGraphics.fill(metrics.trackLeft() + 1, thumbTop, metrics.trackRight() - 1, thumbBottom, 0xAAE2E6EB);
        guiGraphics.fill(metrics.trackLeft() + 1, thumbTop, metrics.trackRight() - 1, thumbTop + 1, 0xD0FFFFFF);
        guiGraphics.fill(metrics.trackLeft() + 1, thumbBottom - 1, metrics.trackRight() - 1, thumbBottom, 0x80404040);
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

        int insideLeft = this.windowLeft() + AdvancementWindowRenderer.BORDER_LEFT;
        int insideTop = this.windowTop() + AdvancementWindowRenderer.BORDER_TOP;
        int insideRight = this.windowLeft() + this.windowWidth() - AdvancementWindowRenderer.BORDER_RIGHT;
        int insideBottom = this.windowTop() + this.windowHeight() - AdvancementWindowRenderer.BORDER_BOTTOM;
        int contentLeft = insideLeft + 6;
        int contentTop = insideTop + 6;
        int contentRight = insideRight - 6;
        int contentBottom = insideBottom - 6;
        int sidebarWidth = Math.min(SIDEBAR_TARGET_WIDTH, Math.max(132, (contentRight - contentLeft) / 3));
        int mainLeft = contentLeft + sidebarWidth + 8;
        int mainRight = contentRight;
        List<TaskClientCache.TaskEntry> sortedWeekly = sortedWeeklyEntries(TaskClientCache.snapshot().weekly());
        WeeklyListMetrics metrics = this.weeklyListMetrics(mainLeft, contentTop, mainRight, contentBottom, sortedWeekly.size());
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
            int insideLeft = this.windowLeft() + AdvancementWindowRenderer.BORDER_LEFT;
            int insideTop = this.windowTop() + AdvancementWindowRenderer.BORDER_TOP;
            int insideRight = this.windowLeft() + this.windowWidth() - AdvancementWindowRenderer.BORDER_RIGHT;
            int insideBottom = this.windowTop() + this.windowHeight() - AdvancementWindowRenderer.BORDER_BOTTOM;
            int contentLeft = insideLeft + 6;
            int contentTop = insideTop + 6;
            int contentRight = insideRight - 6;
            int contentBottom = insideBottom - 6;
            int sidebarWidth = Math.min(SIDEBAR_TARGET_WIDTH, Math.max(132, (contentRight - contentLeft) / 3));
            int mainLeft = contentLeft + sidebarWidth + 8;
            int mainRight = contentRight;
            List<TaskClientCache.TaskEntry> sortedWeekly = sortedWeeklyEntries(TaskClientCache.snapshot().weekly());
            WeeklyListMetrics metrics = this.weeklyListMetrics(mainLeft, contentTop, mainRight, contentBottom, sortedWeekly.size());

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
            int insideLeft = this.windowLeft() + AdvancementWindowRenderer.BORDER_LEFT;
            int insideTop = this.windowTop() + AdvancementWindowRenderer.BORDER_TOP;
            int insideRight = this.windowLeft() + this.windowWidth() - AdvancementWindowRenderer.BORDER_RIGHT;
            int insideBottom = this.windowTop() + this.windowHeight() - AdvancementWindowRenderer.BORDER_BOTTOM;
            int contentLeft = insideLeft + 6;
            int contentTop = insideTop + 6;
            int contentRight = insideRight - 6;
            int contentBottom = insideBottom - 6;
            int sidebarWidth = Math.min(SIDEBAR_TARGET_WIDTH, Math.max(132, (contentRight - contentLeft) / 3));
            int mainLeft = contentLeft + sidebarWidth + 8;
            int mainRight = contentRight;
            List<TaskClientCache.TaskEntry> sortedWeekly = sortedWeeklyEntries(TaskClientCache.snapshot().weekly());
            WeeklyListMetrics metrics = this.weeklyListMetrics(mainLeft, contentTop, mainRight, contentBottom, sortedWeekly.size());

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
                int fill = tier.claimed() ? 0xAA2A5F3A : (tier.unlocked() ? 0xAA8C7418 : 0xAA353A40);
                int border = tier.claimed() ? 0xFF6FD980 : (tier.unlocked() ? COLOR_ACCENT : 0xFF8E9197);
                guiGraphics.fill(slotLeft, rowTop, slotRight, rowBottom, fill);
                guiGraphics.fill(slotLeft, rowTop, slotRight, rowTop + 1, border);
                guiGraphics.fill(slotLeft, rowBottom - 1, slotRight, rowBottom, border);
                guiGraphics.fill(slotLeft, rowTop, slotLeft + 1, rowBottom, border);
                guiGraphics.fill(slotRight - 1, rowTop, slotRight, rowBottom, border);

                String tierText = "T" + tier.tier();
                guiGraphics.drawString(this.font, Component.literal(tierText), slotLeft + 3, rowTop + 2, COLOR_TEXT_PRIMARY);
                String pointsText = pointsIcon + " " + tier.requiredPoints();
                guiGraphics.drawString(
                        this.font,
                        Component.literal(pointsText),
                        slotRight - 3 - this.font.width(pointsText),
                        rowTop + 2,
                        0xE2E2E2
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
                guiGraphics.fill(slotLeft, rowTop, slotRight, rowBottom, 0xAA30343A);
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
                    0xFF4F5560,
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
            guiGraphics.drawString(this.font, Component.literal("-"), left, top + 4, 0xA0A0A0);
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
        guiGraphics.fill(left, top, right, bottom, fillColor);
        guiGraphics.fill(left, top, right, top + 1, borderColor);
        guiGraphics.fill(left, bottom - 1, right, bottom, borderColor);
        guiGraphics.fill(left, top, left + 1, bottom, borderColor);
        guiGraphics.fill(right - 1, top, right, bottom, borderColor);
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

    private int windowLeft() {
        return (this.width - this.windowWidth()) / 2;
    }

    private int windowTop() {
        return (this.height - this.windowHeight()) / 2;
    }

    private int windowWidth() {
        return Math.min(TARGET_WINDOW_WIDTH, Math.max(AdvancementWindowRenderer.BASE_WIDTH, this.width - 16));
    }

    private int windowHeight() {
        return Math.min(TARGET_WINDOW_HEIGHT, Math.max(AdvancementWindowRenderer.BASE_HEIGHT, this.height - 16));
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
}
