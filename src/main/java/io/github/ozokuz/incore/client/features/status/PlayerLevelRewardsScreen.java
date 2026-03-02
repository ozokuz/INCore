package io.github.ozokuz.incore.client.features.status;

import io.github.ozokuz.incore.features.playerlevel.network.PlayerLevelClientCache;
import io.github.ozokuz.incore.features.playerlevel.network.PlayerLevelSyncPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PlayerLevelRewardsScreen extends Screen {
    private static final int TARGET_WINDOW_WIDTH = 660;
    private static final int TARGET_WINDOW_HEIGHT = 368;
    private static final int HERO_HEIGHT = 82;
    private static final int SIDEBAR_TARGET_WIDTH = 220;
    private static final int LEVEL_CARD_HEIGHT = 30;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SCROLLBAR_GAP = 3;
    private static final int MIN_SCROLLBAR_THUMB_HEIGHT = 12;
    private static final int REWARD_CARD_SIZE = 38;
    private static final int REWARD_CARD_GAP = 8;
    private static final ResourceLocation XP_BAR_BACKGROUND = ResourceLocation.parse("incore:hud/experience_bar_background_white");
    private static final ResourceLocation XP_BAR_PROGRESS = ResourceLocation.parse("incore:hud/experience_bar_progress_white");
    private static final int XP_BAR_HEIGHT = 5;

    private static final int COLOR_TEXT_PRIMARY = 0xFFF3F8FF;
    private static final int COLOR_TEXT_SECONDARY = 0xFFD4E3F0;
    private static final int COLOR_TEXT_MUTED = 0xFFA6BED2;

    private final Screen parent;
    private Integer previousMenuBlur;
    private int selectedLevel = -1;
    private int sidebarScroll;
    private boolean pendingInitialFocus = true;

    public PlayerLevelRewardsScreen(Screen parent) {
        super(Component.translatable("screen.incore.player_level_rewards.title"));
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
        Layout layout = layout();
        int backWidth = 96;
        int backX = layout.windowLeft() + layout.windowWidth() - backWidth - 12;
        int backY = layout.windowTop() + layout.windowHeight() - 28;
        this.addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> this.onClose())
                .bounds(backX, backY, backWidth, 20)
                .build());

        focusNextLevel();
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
        SidebarMetrics sidebar = sidebarMetrics(layout);
        float pulse = 0.5F + 0.5F * Mth.sin((System.currentTimeMillis() % 4000L) / 220.0F);

        guiGraphics.fillGradient(0, 0, this.width, this.height, 0xD5090B10, 0xE0010206);
        drawMainPanel(guiGraphics, layout.windowLeft(), layout.windowTop(), layout.windowWidth(), layout.windowHeight());

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawString(this.font, this.title, layout.windowLeft() + 12, layout.windowTop() + 8, COLOR_TEXT_PRIMARY, false);

        drawCard(guiGraphics, layout.heroX(), layout.heroY(), layout.heroWidth(), layout.heroHeight());
        drawCard(guiGraphics, layout.railX(), layout.railY(), layout.railWidth(), layout.railHeight());
        drawCard(guiGraphics, layout.galleryX(), layout.galleryY(), layout.galleryWidth(), layout.galleryHeight());

        List<PlayerLevelClientCache.RewardPreview> ordered = getOrderedPreviews();
        syncSelection(ordered, sidebar.visibleRows());

        drawHeroCard(guiGraphics, layout, pulse);
        drawLevelRail(guiGraphics, ordered, sidebar, mouseX, mouseY, pulse);
        drawRewardGallery(guiGraphics, layout, ordered, mouseX, mouseY, pulse);
    }

    private void drawHeroCard(GuiGraphics guiGraphics, Layout layout, float pulse) {
        int currentLevel = PlayerLevelClientCache.getLevel();
        int currentExperience = PlayerLevelClientCache.getCurrentExperience();
        int experienceToNextLevel = PlayerLevelClientCache.getExperienceToNextLevel();

        int x = layout.heroX() + 10;
        int y = layout.heroY() + 8;
        int width = layout.heroWidth() - 20;

        int glowAlpha = 36 + Math.round(26 * pulse);
        guiGraphics.fill(layout.heroX() + 1, layout.heroY() + 1, layout.heroX() + layout.heroWidth() - 1, layout.heroY() + 2, withAlpha(0x7DE9FF, glowAlpha));

        guiGraphics.drawString(this.font, this.title, x, y, COLOR_TEXT_PRIMARY, false);
        Component levelChip = Component.translatable("screen.incore.player_level_rewards.current_level", currentLevel);
        drawChip(guiGraphics, x, y + 12, levelChip, 0xA7243648, 0xFFEAF6FF);

        int progressTextY = y + 30;
        guiGraphics.drawString(
                this.font,
                Component.translatable("screen.incore.player_level_rewards.current_progress", currentExperience, experienceToNextLevel),
                x,
                progressTextY,
                COLOR_TEXT_SECONDARY,
                false
        );

        int barY = progressTextY + 13;
        drawProgressBar(guiGraphics, x, barY, Math.max(130, width - 4), currentExperience, experienceToNextLevel);

        Component focusLine;
        if (this.selectedLevel > 0) {
            focusLine = Component.translatable("screen.incore.player_level_rewards.details_level", this.selectedLevel);
        } else {
            focusLine = Component.translatable("screen.incore.player_level_rewards.none");
        }
        int focusWidth = this.font.width(focusLine);
        int focusX = layout.heroX() + layout.heroWidth() - focusWidth - 12;
        guiGraphics.drawString(this.font, focusLine, focusX, y + 12, 0xFFD3ECFF, false);
    }

    private void drawLevelRail(
            GuiGraphics guiGraphics,
            List<PlayerLevelClientCache.RewardPreview> ordered,
            SidebarMetrics sidebar,
            int mouseX,
            int mouseY,
            float pulse
    ) {
        guiGraphics.drawString(
                this.font,
                Component.translatable("screen.incore.player_level_rewards.sidebar_title"),
                sidebar.rowsLeft(),
                sidebar.rowsTop() - 16,
                COLOR_TEXT_PRIMARY,
                false
        );

        for (int i = 0; i < sidebar.visibleRows(); i++) {
            int index = this.sidebarScroll + i;
            if (index >= ordered.size()) {
                break;
            }

            PlayerLevelClientCache.RewardPreview preview = ordered.get(index);
            int rowX = sidebar.rowsLeft();
            int rowY = sidebar.rowsTop() + i * LEVEL_CARD_HEIGHT;
            int rowBottom = rowY + LEVEL_CARD_HEIGHT - 2;
            int rowRight = sidebar.rowsRight();

            boolean hovered = mouseX >= rowX && mouseX < rowRight && mouseY >= rowY && mouseY < rowBottom;
            boolean selected = preview.level() == this.selectedLevel;

            int rowFill = selected ? 0xB1263E58 : 0x98202A36;
            if (hovered && !selected) {
                rowFill = 0xAE253446;
            }
            guiGraphics.fill(rowX, rowY, rowRight, rowBottom, rowFill);

            int borderColor = selected ? withAlpha(0x89EFFF, 140 + Math.round(70 * pulse)) : (hovered ? 0xAA6ABFDF : 0x66516D86);
            drawCardOutline(guiGraphics, rowX, rowY, rowRight, rowBottom, borderColor);

            int accent = selected ? 0xFF7AEFFF : 0xAA4B9AB8;
            guiGraphics.fill(rowX, rowY, rowX + 3, rowBottom, accent);

            Component levelText = Component.translatable("screen.incore.player_level_rewards.sidebar_level", preview.level());
            guiGraphics.drawString(this.font, levelText, rowX + 8, rowY + 6, selected ? 0xFFF3FBFF : 0xFFE1EDF8, false);

            Component xpText = Component.translatable("screen.incore.player_level_rewards.sidebar_xp", preview.requiredExperience());
            int xpWidth = this.font.width(xpText) + 8;
            int xpX = rowRight - xpWidth - 5;
            int xpY = rowY + 5;
            guiGraphics.fill(xpX, xpY, xpX + xpWidth, xpY + 11, selected ? 0xB03A607D : 0x8A2F4255);
            guiGraphics.drawString(this.font, xpText, xpX + 4, xpY + 2, 0xFFE6F6FF, false);
        }

        drawSidebarScrollbar(guiGraphics, sidebar, ordered.size());
    }

    private void drawRewardGallery(
            GuiGraphics guiGraphics,
            Layout layout,
            List<PlayerLevelClientCache.RewardPreview> ordered,
            int mouseX,
            int mouseY,
            float pulse
    ) {
        int x = layout.galleryX() + 10;
        int y = layout.galleryY() + 8;

        guiGraphics.drawString(this.font, Component.translatable("screen.incore.player_level_rewards.details_title"), x, y, COLOR_TEXT_PRIMARY, false);

        PlayerLevelClientCache.RewardPreview selectedPreview = ordered.stream()
                .filter(preview -> preview.level() == this.selectedLevel)
                .findFirst()
                .orElse(null);

        if (selectedPreview == null) {
            guiGraphics.drawString(this.font, Component.translatable("screen.incore.player_level_rewards.none"), x, y + 18, COLOR_TEXT_MUTED, false);
            return;
        }

        Component levelChip = Component.translatable("screen.incore.player_level_rewards.details_level", selectedPreview.level());
        Component xpChip = Component.translatable("screen.incore.player_level_rewards.details_required_xp", selectedPreview.requiredExperience());
        drawChip(guiGraphics, x, y + 12, levelChip, 0xA9283B4E, 0xFFEAF6FF);

        int xpChipWidth = this.font.width(xpChip) + 10;
        int xpX = layout.galleryX() + layout.galleryWidth() - xpChipWidth - 10;
        drawChip(guiGraphics, xpX, y + 12, xpChip, 0xA82B425A, 0xFFDBEEFF);

        guiGraphics.drawString(this.font, Component.translatable("screen.incore.player_level_rewards.details_rewards"), x, y + 30, COLOR_TEXT_SECONDARY, false);

        if (selectedPreview.rewards().isEmpty()) {
            guiGraphics.drawString(this.font, Component.translatable("screen.incore.player_level_rewards.level_empty"), x, y + 46, COLOR_TEXT_MUTED, false);
            return;
        }

        PlayerLevelClientCache.RewardEntry hoveredReward = null;
        ItemStack hoveredStack = ItemStack.EMPTY;

        int cardsX = x;
        int cardsY = y + 46;
        int availableWidth = Math.max(32, layout.galleryWidth() - 20);
        int availableHeight = Math.max(32, layout.galleryHeight() - 60);
        int columns = Math.max(1, (availableWidth + REWARD_CARD_GAP) / (REWARD_CARD_SIZE + REWARD_CARD_GAP));
        int rows = Math.max(1, (availableHeight + REWARD_CARD_GAP) / (REWARD_CARD_SIZE + REWARD_CARD_GAP));
        int maxCards = columns * rows;
        int rewardCount = Math.min(maxCards, selectedPreview.rewards().size());

        for (int i = 0; i < rewardCount; i++) {
            PlayerLevelClientCache.RewardEntry reward = selectedPreview.rewards().get(i);
            int col = i % columns;
            int row = i / columns;
            int cardX = cardsX + col * (REWARD_CARD_SIZE + REWARD_CARD_GAP);
            int cardY = cardsY + row * (REWARD_CARD_SIZE + REWARD_CARD_GAP);
            int cardRight = cardX + REWARD_CARD_SIZE;
            int cardBottom = cardY + REWARD_CARD_SIZE;

            boolean hovered = mouseX >= cardX && mouseX < cardRight && mouseY >= cardY && mouseY < cardBottom;
            int fill = rewardCardFill(reward.kind());
            if (hovered) {
                fill = brighten(fill, 18);
            }
            guiGraphics.fill(cardX, cardY, cardRight, cardBottom, fill);

            int borderAlpha = hovered ? 200 : (120 + Math.round(40 * pulse));
            drawCardOutline(guiGraphics, cardX, cardY, cardRight, cardBottom, withAlpha(0x79E9FF, borderAlpha));

            ItemStack iconStack = iconStackFor(reward);
            int iconX = cardX + (REWARD_CARD_SIZE - 16) / 2;
            int iconY = cardY + (REWARD_CARD_SIZE - 16) / 2 + (hovered ? -1 : 0);
            guiGraphics.renderItem(iconStack, iconX, iconY);

            if (reward.kind() == PlayerLevelSyncPayload.REWARD_KIND_ITEM && reward.amount() > 1) {
                String qty = "x" + reward.amount();
                int qtyWidth = this.font.width(qty);
                int qtyX = cardRight - qtyWidth - 3;
                int qtyY = cardBottom - this.font.lineHeight - 2;
                guiGraphics.fill(qtyX - 2, qtyY - 1, qtyX + qtyWidth + 2, qtyY + this.font.lineHeight, 0xC0141F2A);
                guiGraphics.drawString(this.font, qty, qtyX, qtyY, 0xFFF0F6FF, false);
            }

            if (hovered) {
                hoveredReward = reward;
                hoveredStack = iconStack;
            }
        }

        if (rewardCount < selectedPreview.rewards().size()) {
            Component overflow = Component.translatable(
                    "screen.incore.player_level_rewards.more_rewards",
                    selectedPreview.rewards().size() - rewardCount
            );
            int overflowX = layout.galleryX() + layout.galleryWidth() - this.font.width(overflow) - 10;
            int overflowY = layout.galleryY() + layout.galleryHeight() - 12;
            guiGraphics.drawString(this.font, overflow, overflowX, overflowY, COLOR_TEXT_MUTED, false);
        }

        if (hoveredReward != null) {
            renderRewardTooltip(guiGraphics, hoveredReward, hoveredStack, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY == 0.0D) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        SidebarMetrics sidebar = sidebarMetrics(layout());
        if (mouseX < sidebar.rowsLeft() || mouseX > sidebar.scrollTrackRight() || mouseY < sidebar.rowsTop() || mouseY > sidebar.rowsBottom()) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        List<PlayerLevelClientCache.RewardPreview> ordered = getOrderedPreviews();
        int maxScroll = Math.max(0, ordered.size() - sidebar.visibleRows());
        int direction = scrollY > 0.0D ? -1 : 1;
        this.sidebarScroll = Math.clamp(this.sidebarScroll + direction, 0, maxScroll);
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            SidebarMetrics sidebar = sidebarMetrics(layout());
            if (mouseX >= sidebar.rowsLeft() && mouseX < sidebar.rowsRight() && mouseY >= sidebar.rowsTop() && mouseY < sidebar.rowsBottom()) {
                int row = (int) ((mouseY - sidebar.rowsTop()) / LEVEL_CARD_HEIGHT);
                int index = this.sidebarScroll + row;
                List<PlayerLevelClientCache.RewardPreview> ordered = getOrderedPreviews();
                if (index >= 0 && index < ordered.size()) {
                    this.selectedLevel = ordered.get(index).level();
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
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
        return Math.min(TARGET_WINDOW_WIDTH, Math.max(440, this.width - 24));
    }

    private int windowHeight() {
        return Math.min(TARGET_WINDOW_HEIGHT, Math.max(300, this.height - 24));
    }

    private Layout layout() {
        int windowLeft = windowLeft();
        int windowTop = windowTop();
        int windowWidth = windowWidth();
        int windowHeight = windowHeight();

        int contentX = windowLeft + 12;
        int contentY = windowTop + 30;
        int contentWidth = windowWidth - 24;
        int contentHeight = windowHeight - 42;

        int railWidth = Math.min(SIDEBAR_TARGET_WIDTH, Math.max(170, contentWidth / 3));
        int galleryX = contentX + railWidth + 8;
        int galleryWidth = contentWidth - railWidth - 8;

        return new Layout(
                windowLeft,
                windowTop,
                windowWidth,
                windowHeight,
                contentX,
                contentY,
                contentWidth,
                contentHeight,
                railWidth,
                galleryX,
                galleryWidth
        );
    }

    private static void drawMainPanel(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        guiGraphics.fill(x, y, x + width, y + height, 0xCF10171F);
        guiGraphics.fill(x, y, x + width, y + 1, 0xFF67DFFF);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, 0xFF1E2732);
        guiGraphics.fill(x, y, x + 1, y + height, 0xFF4CAFCB);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, 0xFF1E2732);
    }

    private static void drawCard(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        guiGraphics.fill(x, y, x + width, y + height, 0xA516202B);
        guiGraphics.fill(x, y, x + width, y + 1, 0xA058C7E6);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, 0x80131A22);
        guiGraphics.fill(x, y, x + 1, y + height, 0x804A9EB9);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, 0x80131A22);
    }

    private static void drawCardOutline(GuiGraphics guiGraphics, int left, int top, int right, int bottom, int color) {
        guiGraphics.fill(left, top, right, top + 1, color);
        guiGraphics.fill(left, bottom - 1, right, bottom, color);
        guiGraphics.fill(left, top, left + 1, bottom, color);
        guiGraphics.fill(right - 1, top, right, bottom, color);
    }

    private void drawChip(GuiGraphics guiGraphics, int x, int y, Component text, int fillColor, int textColor) {
        int width = this.font.width(text) + 10;
        guiGraphics.fill(x, y, x + width, y + 11, fillColor);
        guiGraphics.drawString(this.font, text, x + 5, y + 2, textColor, false);
    }

    private List<PlayerLevelClientCache.RewardPreview> getOrderedPreviews() {
        return PlayerLevelClientCache.getRewardPreviews().stream()
                .sorted(Comparator.comparingInt(PlayerLevelClientCache.RewardPreview::level).reversed())
                .toList();
    }

    private void focusNextLevel() {
        List<PlayerLevelClientCache.RewardPreview> ordered = getOrderedPreviews();
        if (ordered.isEmpty()) {
            this.selectedLevel = -1;
            this.sidebarScroll = 0;
            this.pendingInitialFocus = false;
            return;
        }

        int nextLevel = PlayerLevelClientCache.getLevel() + 1;
        int index = -1;
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).level() == nextLevel) {
                index = i;
                break;
            }
        }

        if (index < 0) {
            index = ordered.size() - 1;
        }

        this.selectedLevel = ordered.get(index).level();
        this.sidebarScroll = 0;
        this.pendingInitialFocus = true;
    }

    private void syncSelection(List<PlayerLevelClientCache.RewardPreview> ordered, int visibleRows) {
        if (ordered.isEmpty()) {
            this.selectedLevel = -1;
            this.sidebarScroll = 0;
            this.pendingInitialFocus = false;
            return;
        }

        int selectedIndex = indexForSelectedLevel(ordered);
        if (selectedIndex < 0) {
            focusNextLevel();
            selectedIndex = indexForSelectedLevel(ordered);
            if (selectedIndex < 0) {
                selectedIndex = Math.max(0, ordered.size() - 1);
            }
        }

        int maxScroll = Math.max(0, ordered.size() - visibleRows);
        if (this.pendingInitialFocus) {
            this.sidebarScroll = Math.max(0, selectedIndex - visibleRows + 1);
            this.pendingInitialFocus = false;
        }

        this.sidebarScroll = Math.clamp(this.sidebarScroll, 0, maxScroll);
    }

    private int indexForSelectedLevel(List<PlayerLevelClientCache.RewardPreview> ordered) {
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).level() == this.selectedLevel) {
                return i;
            }
        }
        return -1;
    }

    private static SidebarMetrics sidebarMetrics(Layout layout) {
        int rowsTop = layout.railY() + 24;
        int rowsBottom = layout.railY() + layout.railHeight() - 8;
        int trackRight = layout.railX() + layout.railWidth() - 6;
        int trackLeft = trackRight - SCROLLBAR_WIDTH;
        int rowsRight = trackLeft - SCROLLBAR_GAP;
        int rowsLeft = layout.railX() + 8;
        int visibleRows = Math.max(1, (rowsBottom - rowsTop) / LEVEL_CARD_HEIGHT);
        return new SidebarMetrics(rowsLeft, rowsTop, rowsRight, rowsBottom, visibleRows, trackLeft, trackRight);
    }

    private void drawSidebarScrollbar(GuiGraphics guiGraphics, SidebarMetrics sidebar, int totalRows) {
        int trackLeft = sidebar.scrollTrackLeft();
        int trackRight = sidebar.scrollTrackRight();
        int trackTop = sidebar.rowsTop();
        int trackBottom = sidebar.rowsBottom();
        int trackHeight = Math.max(1, trackBottom - trackTop);

        guiGraphics.fill(trackLeft, trackTop, trackRight, trackBottom, 0x4C101926);
        guiGraphics.fill(trackLeft, trackTop, trackRight, trackTop + 1, 0x8060CAE8);
        guiGraphics.fill(trackLeft, trackBottom - 1, trackRight, trackBottom, 0x80131A22);

        int maxScroll = Math.max(0, totalRows - sidebar.visibleRows());
        if (maxScroll <= 0) {
            guiGraphics.fill(trackLeft + 1, trackTop + 1, trackRight - 1, trackBottom - 1, 0x334C6B80);
            return;
        }

        int thumbHeight = Math.max(MIN_SCROLLBAR_THUMB_HEIGHT, Math.round((float) trackHeight * (float) sidebar.visibleRows() / (float) totalRows));
        thumbHeight = Math.min(trackHeight, thumbHeight);
        int thumbTravel = Math.max(0, trackHeight - thumbHeight);
        int thumbOffset = Math.round((float) this.sidebarScroll / (float) maxScroll * (float) thumbTravel);
        int thumbTop = trackTop + thumbOffset;
        int thumbBottom = thumbTop + thumbHeight;

        guiGraphics.fill(trackLeft + 1, thumbTop, trackRight - 1, thumbBottom, 0xAA7DE9FF);
        guiGraphics.fill(trackLeft + 1, thumbTop, trackRight - 1, thumbTop + 1, 0xFFB7F2FF);
        guiGraphics.fill(trackLeft + 1, thumbBottom - 1, trackRight - 1, thumbBottom, 0x805A8CA1);
    }

    private static void drawProgressBar(GuiGraphics guiGraphics, int x, int y, int width, int currentExperience, int experienceToNextLevel) {
        guiGraphics.blitSprite(XP_BAR_BACKGROUND, x, y, width, XP_BAR_HEIGHT);
        float progress = Math.max(0.0F, Math.min(1.0F, (float) currentExperience / (float) Math.max(1, experienceToNextLevel)));
        int fillWidth = Math.max(0, Math.min(width, Math.round(width * progress)));
        if (fillWidth > 0) {
            guiGraphics.enableScissor(x, y, x + fillWidth, y + XP_BAR_HEIGHT);
            guiGraphics.blitSprite(XP_BAR_PROGRESS, x, y, width, XP_BAR_HEIGHT);
            guiGraphics.disableScissor();
        }
    }

    private static ItemStack iconStackFor(PlayerLevelClientCache.RewardEntry reward) {
        ResourceLocation itemId = ResourceLocation.tryParse(reward.iconItemId());
        Item item = itemId != null ? BuiltInRegistries.ITEM.get(itemId) : Items.AIR;
        if (item == Items.AIR) {
            item = Items.BARRIER;
        }

        int count = reward.kind() == PlayerLevelSyncPayload.REWARD_KIND_ITEM ? Math.max(1, reward.amount()) : 1;
        return new ItemStack(item, Math.min(99, count));
    }

    private void renderRewardTooltip(GuiGraphics guiGraphics, PlayerLevelClientCache.RewardEntry reward, ItemStack iconStack, int mouseX, int mouseY) {
        if (reward.kind() == PlayerLevelSyncPayload.REWARD_KIND_ITEM) {
            guiGraphics.renderTooltip(this.font, iconStack, mouseX, mouseY);
            return;
        }
        guiGraphics.renderComponentTooltip(this.font, tooltipForNonItemReward(reward), mouseX, mouseY);
    }

    private static List<Component> tooltipForNonItemReward(PlayerLevelClientCache.RewardEntry reward) {
        List<Component> lines = new ArrayList<>();
        if (reward.kind() == PlayerLevelSyncPayload.REWARD_KIND_SANITY_CAP) {
            lines.add(Component.translatable("screen.incore.player_level_rewards.tooltip_sanity_cap_title"));
            lines.add(Component.translatable("screen.incore.player_level_rewards.tooltip_sanity_cap", reward.amount()).withStyle(ChatFormatting.GRAY));
            if (!reward.text().isBlank()) {
                lines.add(Component.literal(reward.text()).withStyle(ChatFormatting.DARK_GRAY));
            }
            return lines;
        }

        if (reward.kind() == PlayerLevelSyncPayload.REWARD_KIND_COMMAND) {
            lines.add(Component.translatable("screen.incore.player_level_rewards.tooltip_command_title"));
            lines.add(Component.translatable("screen.incore.player_level_rewards.tooltip_command", reward.text()).withStyle(ChatFormatting.GRAY));
            return lines;
        }

        lines.add(Component.translatable("screen.incore.player_level_rewards.tooltip_other_title"));
        lines.add(Component.literal(reward.text()).withStyle(ChatFormatting.GRAY));
        return lines;
    }

    private static int rewardCardFill(int kind) {
        return switch (kind) {
            case PlayerLevelSyncPayload.REWARD_KIND_SANITY_CAP -> 0xB0193A3A;
            case PlayerLevelSyncPayload.REWARD_KIND_COMMAND -> 0xB03A301B;
            default -> 0xB01A2735;
        };
    }

    private static int withAlpha(int rgb, int alpha) {
        int clamped = Math.clamp(alpha, 0, 255);
        return (clamped << 24) | (rgb & 0x00FFFFFF);
    }

    private static int brighten(int color, int amount) {
        int a = (color >>> 24) & 0xFF;
        int r = Math.clamp(((color >>> 16) & 0xFF) + amount, 0, 255);
        int g = Math.clamp(((color >>> 8) & 0xFF) + amount, 0, 255);
        int b = Math.clamp((color & 0xFF) + amount, 0, 255);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private record Layout(
            int windowLeft,
            int windowTop,
            int windowWidth,
            int windowHeight,
            int contentX,
            int contentY,
            int contentWidth,
            int contentHeight,
            int railWidth,
            int galleryX,
            int galleryWidth
    ) {
        int heroX() {
            return galleryX;
        }

        int heroY() {
            return contentY;
        }

        int heroWidth() {
            return galleryWidth;
        }

        int heroHeight() {
            return HERO_HEIGHT;
        }

        int railX() {
            return contentX;
        }

        int railY() {
            return contentY;
        }

        int railHeight() {
            return contentHeight;
        }

        int galleryY() {
            return contentY + HERO_HEIGHT + 8;
        }

        int galleryHeight() {
            return contentHeight - HERO_HEIGHT - 8;
        }
    }

    private record SidebarMetrics(
            int rowsLeft,
            int rowsTop,
            int rowsRight,
            int rowsBottom,
            int visibleRows,
            int scrollTrackLeft,
            int scrollTrackRight
    ) {
    }
}
