package io.github.ozokuz.incore.client.status;

import io.github.ozokuz.incore.features.playerlevel.network.PlayerLevelClientCache;
import io.github.ozokuz.incore.features.playerlevel.network.PlayerLevelSyncPayload;
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

public class PlayerLevelRewardsScreen extends Screen {
    private static final int TARGET_WINDOW_WIDTH = 504;
    private static final int TARGET_WINDOW_HEIGHT = 280;
    private static final int SIDEBAR_WIDTH = 172;
    private static final int LIST_ROW_HEIGHT = 14;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SCROLLBAR_GAP = 3;
    private static final int MIN_SCROLLBAR_THUMB_HEIGHT = 12;
    private static final int ICON_SIZE = 16;
    private static final int ICON_STEP = 20;
    private static final int DEFAULT_BACK_BUTTON_WIDTH = 100;
    private static final ResourceLocation XP_BAR_BACKGROUND = ResourceLocation.parse("incore:hud/experience_bar_background_white");
    private static final ResourceLocation XP_BAR_PROGRESS = ResourceLocation.parse("incore:hud/experience_bar_progress_white");
    private static final int XP_BAR_WIDTH = 182;
    private static final int XP_BAR_HEIGHT = 5;

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

        int buttonWidth = Math.min(DEFAULT_BACK_BUTTON_WIDTH, Math.max(80, this.windowWidth() - 20));
        int buttonX = this.windowLeft() + (this.windowWidth() - buttonWidth) / 2;
        int buttonY = this.windowTop() + this.windowHeight() + 4;
        this.addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> this.onClose())
                .bounds(buttonX, buttonY, buttonWidth, 20)
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
        int windowLeft = this.windowLeft();
        int windowTop = this.windowTop();
        int insideLeft = windowLeft + AdvancementWindowRenderer.BORDER_LEFT + 6;
        int insideRight = windowLeft + this.windowWidth() - AdvancementWindowRenderer.BORDER_RIGHT - 6;
        int insideTop = windowTop + AdvancementWindowRenderer.BORDER_TOP + 6;
        int insideBottom = windowTop + this.windowHeight() - AdvancementWindowRenderer.BORDER_BOTTOM - 6;
        int sidebarWidth = Math.min(SIDEBAR_WIDTH, Math.max(120, (insideRight - insideLeft) / 2));
        int sidebarX = insideLeft;
        int detailsX = sidebarX + sidebarWidth + 10;
        int detailsWidth = Math.max(120, insideRight - detailsX);

        AdvancementWindowRenderer.draw(guiGraphics, windowLeft, windowTop, this.windowWidth(), this.windowHeight());
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int titleY = windowTop + (AdvancementWindowRenderer.BORDER_TOP - this.font.lineHeight) / 2 + 1;
        int titleX = windowLeft + AdvancementWindowRenderer.BORDER_LEFT + 8;
        guiGraphics.drawString(this.font, this.title, titleX, titleY, 0xF5F5F5);

        drawSectionBackground(guiGraphics, sidebarX, insideTop, sidebarWidth, insideBottom - insideTop);
        drawSectionBackground(guiGraphics, detailsX, insideTop, detailsWidth, insideBottom - insideTop);

        guiGraphics.drawString(this.font, Component.translatable("screen.incore.player_level_rewards.sidebar_title"), sidebarX + 6, insideTop + 6, 0xE5E5E5);

        List<PlayerLevelClientCache.RewardPreview> ordered = getOrderedPreviews();
        SidebarMetrics sidebar = sidebarMetrics(sidebarX, sidebarWidth, insideTop, insideBottom);
        syncSelection(ordered, sidebar.visibleRows());

        for (int i = 0; i < sidebar.visibleRows(); i++) {
            int index = this.sidebarScroll + i;
            if (index >= ordered.size()) {
                break;
            }

            PlayerLevelClientCache.RewardPreview preview = ordered.get(index);
            int rowY = sidebar.rowsTop() + i * LIST_ROW_HEIGHT;
            boolean selected = preview.level() == this.selectedLevel;
            if (selected) {
                guiGraphics.fill(sidebar.rowsLeft(), rowY - 1, sidebar.rowsRight(), rowY + LIST_ROW_HEIGHT - 1, 0x55FFFFFF);
            }

            Component rowLevel = Component.translatable("screen.incore.player_level_rewards.sidebar_level", preview.level());
            Component rowXp = Component.translatable("screen.incore.player_level_rewards.sidebar_xp", preview.requiredExperience());
            int rowXpX = sidebar.rowsRight() - this.font.width(rowXp) - 4;
            guiGraphics.drawString(this.font, rowLevel, sidebar.rowsLeft() + 4, rowY + 2, selected ? 0x1F1F1F : 0xF0F0F0);
            guiGraphics.drawString(this.font, rowXp, rowXpX, rowY + 2, selected ? 0x1F1F1F : 0xBEBEBE);
        }
        drawSidebarScrollbar(guiGraphics, sidebar, ordered.size());

        PlayerLevelClientCache.RewardPreview selectedPreview = ordered.stream()
                .filter(preview -> preview.level() == this.selectedLevel)
                .findFirst()
                .orElse(null);

        int currentLevel = PlayerLevelClientCache.getLevel();
        int currentExperience = PlayerLevelClientCache.getCurrentExperience();
        int experienceToNextLevel = PlayerLevelClientCache.getExperienceToNextLevel();

        int detailsTop = insideTop + 6;
        int detailsTextX = detailsX + 8;
        int detailsTextColor = 0xFFFFFF;
        int detailsSubtextColor = 0xDADADA;
        int detailsBarY = detailsTop + 36;
        int detailsBarWidth = Math.min(XP_BAR_WIDTH, Math.max(80, detailsWidth - 16));

        guiGraphics.drawString(this.font, Component.translatable("screen.incore.player_level_rewards.details_title"), detailsTextX, detailsTop, 0xE5E5E5);
        guiGraphics.drawString(
                this.font,
                Component.translatable("screen.incore.player_level_rewards.current_level", currentLevel),
                detailsTextX,
                detailsTop + 12,
                detailsTextColor
        );
        guiGraphics.drawString(
                this.font,
                Component.translatable("screen.incore.player_level_rewards.current_progress", currentExperience, experienceToNextLevel),
                detailsTextX,
                detailsTop + 24,
                detailsTextColor
        );
        drawProgressBar(guiGraphics, detailsTextX, detailsBarY, detailsBarWidth, currentExperience, experienceToNextLevel);

        int selectedDetailsTop = detailsTop + 52;
        if (selectedPreview == null) {
            guiGraphics.drawString(this.font, Component.translatable("screen.incore.player_level_rewards.none"), detailsTextX, selectedDetailsTop + 10, detailsSubtextColor);
            return;
        }

        guiGraphics.drawString(
                this.font,
                Component.translatable("screen.incore.player_level_rewards.details_level", selectedPreview.level()),
                detailsTextX,
                selectedDetailsTop + 10,
                detailsTextColor
        );
        guiGraphics.drawString(
                this.font,
                Component.translatable("screen.incore.player_level_rewards.details_required_xp", selectedPreview.requiredExperience()),
                detailsTextX,
                selectedDetailsTop + 22,
                detailsSubtextColor
        );
        guiGraphics.drawString(
                this.font,
                Component.translatable("screen.incore.player_level_rewards.details_rewards"),
                detailsTextX,
                selectedDetailsTop + 36,
                0xE5E5E5
        );

        if (selectedPreview.rewards().isEmpty()) {
            guiGraphics.drawString(this.font, Component.translatable("screen.incore.player_level_rewards.level_empty"), detailsTextX, selectedDetailsTop + 50, detailsSubtextColor);
            return;
        }

        PlayerLevelClientCache.RewardEntry hoveredReward = null;
        ItemStack hoveredStack = ItemStack.EMPTY;
        int iconsStartX = detailsTextX;
        int iconsStartY = selectedDetailsTop + 50;
        int iconColumns = Math.max(1, Math.max(ICON_SIZE, detailsWidth - 16) / ICON_STEP);
        int maxIconsRows = Math.max(1, (insideBottom - iconsStartY - 4) / ICON_STEP);
        int maxIcons = iconColumns * maxIconsRows;
        int rewardCount = Math.min(maxIcons, selectedPreview.rewards().size());

        for (int i = 0; i < rewardCount; i++) {
            PlayerLevelClientCache.RewardEntry reward = selectedPreview.rewards().get(i);
            int col = i % iconColumns;
            int row = i / iconColumns;
            int iconX = iconsStartX + col * ICON_STEP;
            int iconY = iconsStartY + row * ICON_STEP;
            ItemStack iconStack = iconStackFor(reward);

            guiGraphics.renderItem(iconStack, iconX, iconY);
            if (reward.kind() == PlayerLevelSyncPayload.REWARD_KIND_ITEM) {
                guiGraphics.renderItemDecorations(this.font, iconStack, iconX, iconY);
            }

            boolean hovered = mouseX >= iconX && mouseX < iconX + ICON_SIZE && mouseY >= iconY && mouseY < iconY + ICON_SIZE;
            if (hovered) {
                hoveredReward = reward;
                hoveredStack = iconStack;
            }
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

        int insideTop = this.windowTop() + AdvancementWindowRenderer.BORDER_TOP + 6;
        int insideBottom = this.windowTop() + this.windowHeight() - AdvancementWindowRenderer.BORDER_BOTTOM - 6;
        int insideLeft = this.windowLeft() + AdvancementWindowRenderer.BORDER_LEFT + 6;
        int insideRight = this.windowLeft() + this.windowWidth() - AdvancementWindowRenderer.BORDER_RIGHT - 6;
        int sidebarWidth = Math.min(SIDEBAR_WIDTH, Math.max(120, (insideRight - insideLeft) / 2));
        SidebarMetrics sidebar = sidebarMetrics(insideLeft, sidebarWidth, insideTop, insideBottom);

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
            int insideTop = this.windowTop() + AdvancementWindowRenderer.BORDER_TOP + 6;
            int insideBottom = this.windowTop() + this.windowHeight() - AdvancementWindowRenderer.BORDER_BOTTOM - 6;
            int insideLeft = this.windowLeft() + AdvancementWindowRenderer.BORDER_LEFT + 6;
            int insideRight = this.windowLeft() + this.windowWidth() - AdvancementWindowRenderer.BORDER_RIGHT - 6;
            int sidebarWidth = Math.min(SIDEBAR_WIDTH, Math.max(120, (insideRight - insideLeft) / 2));
            SidebarMetrics sidebar = sidebarMetrics(insideLeft, sidebarWidth, insideTop, insideBottom);

            if (mouseX >= sidebar.rowsLeft() && mouseX < sidebar.rowsRight() && mouseY >= sidebar.rowsTop() && mouseY < sidebar.rowsBottom()) {
                int row = (int) ((mouseY - sidebar.rowsTop()) / LIST_ROW_HEIGHT);
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
        return Math.min(TARGET_WINDOW_WIDTH, Math.max(AdvancementWindowRenderer.BASE_WIDTH, this.width - 16));
    }

    private int windowHeight() {
        return Math.min(TARGET_WINDOW_HEIGHT, Math.max(AdvancementWindowRenderer.BASE_HEIGHT, this.height - 40));
    }

    private static void drawSectionBackground(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        guiGraphics.fill(x, y, x + width, y + height, 0x22000000);
        guiGraphics.fill(x, y, x + width, y + 1, 0x50000000);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, 0x50000000);
        guiGraphics.fill(x, y, x + 1, y + height, 0x50000000);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, 0x50000000);
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

    private static SidebarMetrics sidebarMetrics(int sidebarX, int sidebarWidth, int insideTop, int insideBottom) {
        int rowsTop = insideTop + 22;
        int rowsBottom = insideBottom - 6;
        int scrollTrackRight = sidebarX + sidebarWidth - 3;
        int scrollTrackLeft = scrollTrackRight - SCROLLBAR_WIDTH;
        int rowsRight = scrollTrackLeft - SCROLLBAR_GAP;
        int visibleRows = Math.max(1, (rowsBottom - rowsTop) / LIST_ROW_HEIGHT);
        return new SidebarMetrics(sidebarX, rowsTop, rowsRight, rowsBottom, visibleRows, scrollTrackLeft, scrollTrackRight);
    }

    private void drawSidebarScrollbar(GuiGraphics guiGraphics, SidebarMetrics sidebar, int totalRows) {
        int trackLeft = sidebar.scrollTrackLeft();
        int trackRight = sidebar.scrollTrackRight();
        int trackTop = sidebar.rowsTop();
        int trackBottom = sidebar.rowsBottom();
        int trackHeight = Math.max(1, trackBottom - trackTop);

        guiGraphics.fill(trackLeft, trackTop, trackRight, trackBottom, 0x44000000);
        guiGraphics.fill(trackLeft, trackTop, trackRight, trackTop + 1, 0x66000000);
        guiGraphics.fill(trackLeft, trackBottom - 1, trackRight, trackBottom, 0x66000000);
        guiGraphics.fill(trackLeft, trackTop, trackLeft + 1, trackBottom, 0x66000000);
        guiGraphics.fill(trackRight - 1, trackTop, trackRight, trackBottom, 0x66000000);

        int maxScroll = Math.max(0, totalRows - sidebar.visibleRows());
        if (maxScroll <= 0) {
            guiGraphics.fill(trackLeft + 1, trackTop + 1, trackRight - 1, trackBottom - 1, 0x33666666);
            return;
        }

        int thumbHeight = Math.max(MIN_SCROLLBAR_THUMB_HEIGHT, Math.round((float) trackHeight * (float) sidebar.visibleRows() / (float) totalRows));
        thumbHeight = Math.min(trackHeight, thumbHeight);
        int thumbTravel = Math.max(0, trackHeight - thumbHeight);
        int thumbOffset = Math.round((float) this.sidebarScroll / (float) maxScroll * (float) thumbTravel);
        int thumbTop = trackTop + thumbOffset;
        int thumbBottom = thumbTop + thumbHeight;

        guiGraphics.fill(trackLeft + 1, thumbTop, trackRight - 1, thumbBottom, 0x88FFFFFF);
        guiGraphics.fill(trackLeft + 1, thumbTop, trackRight - 1, thumbTop + 1, 0xCCFFFFFF);
        guiGraphics.fill(trackLeft + 1, thumbBottom - 1, trackRight - 1, thumbBottom, 0x66404040);
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

    private int indexForSelectedLevel(List<PlayerLevelClientCache.RewardPreview> ordered) {
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).level() == this.selectedLevel) {
                return i;
            }
        }
        return -1;
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
