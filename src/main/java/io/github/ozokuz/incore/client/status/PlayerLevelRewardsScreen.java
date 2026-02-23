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
    private static final int TARGET_WINDOW_WIDTH = 640;
    private static final int TARGET_WINDOW_HEIGHT = 352;
    private static final int SIDEBAR_TARGET_WIDTH = 210;
    private static final int LIST_ROW_HEIGHT = 16;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SCROLLBAR_GAP = 3;
    private static final int MIN_SCROLLBAR_THUMB_HEIGHT = 12;
    private static final int REWARD_TILE_SIZE = 24;
    private static final int REWARD_TILE_GAP = 6;
    private static final ResourceLocation XP_BAR_BACKGROUND = ResourceLocation.parse("incore:hud/experience_bar_background_white");
    private static final ResourceLocation XP_BAR_PROGRESS = ResourceLocation.parse("incore:hud/experience_bar_progress_white");
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

        guiGraphics.fillGradient(0, 0, this.width, this.height, 0xD5090B10, 0xE0010206);
        drawMainPanel(guiGraphics, layout.windowLeft(), layout.windowTop(), layout.windowWidth(), layout.windowHeight());

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawString(this.font, this.title, layout.windowLeft() + 12, layout.windowTop() + 8, 0xFFF3F8FF, false);

        drawCard(guiGraphics, layout.sidebarX(), layout.sidebarY(), layout.sidebarWidth(), layout.sidebarHeight());
        drawCard(guiGraphics, layout.summaryX(), layout.summaryY(), layout.summaryWidth(), layout.summaryHeight());
        drawCard(guiGraphics, layout.detailsX(), layout.detailsY(), layout.detailsWidth(), layout.detailsHeight());

        guiGraphics.drawString(
                this.font,
                Component.translatable("screen.incore.player_level_rewards.sidebar_title"),
                layout.sidebarX() + 8,
                layout.sidebarY() + 8,
                0xFFD6F1FF,
                false
        );

        List<PlayerLevelClientCache.RewardPreview> ordered = getOrderedPreviews();
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
                guiGraphics.fill(sidebar.rowsLeft(), rowY, sidebar.rowsRight(), rowY + LIST_ROW_HEIGHT - 1, 0x6636B4D6);
                guiGraphics.fill(sidebar.rowsLeft(), rowY, sidebar.rowsLeft() + 2, rowY + LIST_ROW_HEIGHT - 1, 0xFF74E9FF);
            }

            Component rowLevel = Component.translatable("screen.incore.player_level_rewards.sidebar_level", preview.level());
            Component rowXp = Component.translatable("screen.incore.player_level_rewards.sidebar_xp", preview.requiredExperience());
            int xpX = sidebar.rowsRight() - this.font.width(rowXp) - 4;
            guiGraphics.drawString(this.font, rowLevel, sidebar.rowsLeft() + 6, rowY + 4, selected ? 0xFFEEF9FF : 0xFFE1E8F2, false);
            guiGraphics.drawString(this.font, rowXp, xpX, rowY + 4, selected ? 0xFFD4F3FF : 0xFFAFC1D2, false);
        }

        drawSidebarScrollbar(guiGraphics, sidebar, ordered.size());
        drawSummaryCard(guiGraphics, layout);
        drawDetailsCard(guiGraphics, layout, ordered, mouseX, mouseY);
    }

    private void drawSummaryCard(GuiGraphics guiGraphics, Layout layout) {
        int currentLevel = PlayerLevelClientCache.getLevel();
        int currentExperience = PlayerLevelClientCache.getCurrentExperience();
        int experienceToNextLevel = PlayerLevelClientCache.getExperienceToNextLevel();

        int textX = layout.summaryX() + 10;
        int top = layout.summaryY() + 8;

        guiGraphics.drawString(this.font, Component.translatable("screen.incore.player_level_rewards.current_level", currentLevel), textX, top, 0xFFFFFFFF, false);
        guiGraphics.drawString(
                this.font,
                Component.translatable("screen.incore.player_level_rewards.current_progress", currentExperience, experienceToNextLevel),
                textX,
                top + 14,
                0xDCE7F3,
                false
        );

        int barY = top + 30;
        int barWidth = Math.max(120, layout.summaryWidth() - 20);
        drawProgressBar(guiGraphics, textX, barY, barWidth, currentExperience, experienceToNextLevel);
    }

    private void drawDetailsCard(
            GuiGraphics guiGraphics,
            Layout layout,
            List<PlayerLevelClientCache.RewardPreview> ordered,
            int mouseX,
            int mouseY
    ) {
        int detailsTextX = layout.detailsX() + 10;
        int detailsTop = layout.detailsY() + 8;

        guiGraphics.drawString(this.font, Component.translatable("screen.incore.player_level_rewards.details_title"), detailsTextX, detailsTop, 0xFFD6F1FF, false);

        PlayerLevelClientCache.RewardPreview selectedPreview = ordered.stream()
                .filter(preview -> preview.level() == this.selectedLevel)
                .findFirst()
                .orElse(null);

        if (selectedPreview == null) {
            guiGraphics.drawString(
                    this.font,
                    Component.translatable("screen.incore.player_level_rewards.none"),
                    detailsTextX,
                    detailsTop + 18,
                    0xAFC8D8,
                    false
            );
            return;
        }

        guiGraphics.drawString(
                this.font,
                Component.translatable("screen.incore.player_level_rewards.details_level", selectedPreview.level()),
                detailsTextX,
                detailsTop + 16,
                0xFFFFFFFF,
                false
        );
        guiGraphics.drawString(
                this.font,
                Component.translatable("screen.incore.player_level_rewards.details_required_xp", selectedPreview.requiredExperience()),
                detailsTextX,
                detailsTop + 30,
                0xDCE7F3,
                false
        );
        guiGraphics.drawString(
                this.font,
                Component.translatable("screen.incore.player_level_rewards.details_rewards"),
                detailsTextX,
                detailsTop + 44,
                0xFFD6F1FF,
                false
        );

        if (selectedPreview.rewards().isEmpty()) {
            guiGraphics.drawString(
                    this.font,
                    Component.translatable("screen.incore.player_level_rewards.level_empty"),
                    detailsTextX,
                    detailsTop + 58,
                    0xAFC8D8,
                    false
            );
            return;
        }

        PlayerLevelClientCache.RewardEntry hoveredReward = null;
        ItemStack hoveredStack = ItemStack.EMPTY;

        int tilesX = detailsTextX;
        int tilesY = detailsTop + 58;
        int availableWidth = Math.max(32, layout.detailsWidth() - 20);
        int availableHeight = Math.max(32, layout.detailsHeight() - 70);

        int columns = Math.max(1, (availableWidth + REWARD_TILE_GAP) / (REWARD_TILE_SIZE + REWARD_TILE_GAP));
        int rows = Math.max(1, (availableHeight + REWARD_TILE_GAP) / (REWARD_TILE_SIZE + REWARD_TILE_GAP));
        int maxTiles = columns * rows;
        int rewardCount = Math.min(maxTiles, selectedPreview.rewards().size());

        for (int i = 0; i < rewardCount; i++) {
            PlayerLevelClientCache.RewardEntry reward = selectedPreview.rewards().get(i);
            int col = i % columns;
            int row = i / columns;
            int tileX = tilesX + col * (REWARD_TILE_SIZE + REWARD_TILE_GAP);
            int tileY = tilesY + row * (REWARD_TILE_SIZE + REWARD_TILE_GAP);

            guiGraphics.fill(tileX, tileY, tileX + REWARD_TILE_SIZE, tileY + REWARD_TILE_SIZE, 0xAC172331);
            guiGraphics.fill(tileX, tileY, tileX + REWARD_TILE_SIZE, tileY + 1, 0xA452C1E4);
            guiGraphics.fill(tileX, tileY + REWARD_TILE_SIZE - 1, tileX + REWARD_TILE_SIZE, tileY + REWARD_TILE_SIZE, 0x80111A24);

            ItemStack iconStack = iconStackFor(reward);
            int iconX = tileX + (REWARD_TILE_SIZE - 16) / 2;
            int iconY = tileY + (REWARD_TILE_SIZE - 16) / 2;
            guiGraphics.renderItem(iconStack, iconX, iconY);
            if (reward.kind() == PlayerLevelSyncPayload.REWARD_KIND_ITEM) {
                guiGraphics.renderItemDecorations(this.font, iconStack, iconX, iconY);
            }

            if (mouseX >= tileX && mouseX < tileX + REWARD_TILE_SIZE && mouseY >= tileY && mouseY < tileY + REWARD_TILE_SIZE) {
                hoveredReward = reward;
                hoveredStack = iconStack;
            }
        }

        if (rewardCount < selectedPreview.rewards().size()) {
            guiGraphics.drawString(
                    this.font,
                    Component.translatable("screen.incore.player_level_rewards.more_rewards", selectedPreview.rewards().size() - rewardCount),
                    detailsTextX,
                    layout.detailsY() + layout.detailsHeight() - 12,
                    0xAFC8D8,
                    false
            );
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
        return Math.min(TARGET_WINDOW_WIDTH, Math.max(420, this.width - 24));
    }

    private int windowHeight() {
        return Math.min(TARGET_WINDOW_HEIGHT, Math.max(280, this.height - 24));
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

        int sidebarWidth = Math.min(SIDEBAR_TARGET_WIDTH, Math.max(160, contentWidth / 3));
        int rightX = contentX + sidebarWidth + 8;
        int rightWidth = contentWidth - sidebarWidth - 8;

        int summaryHeight = 74;
        int detailsHeight = contentHeight - summaryHeight - 8;
        if (detailsHeight < 96) {
            int shift = 96 - detailsHeight;
            summaryHeight = Math.max(58, summaryHeight - shift);
            detailsHeight = contentHeight - summaryHeight - 8;
        }

        return new Layout(
                windowLeft,
                windowTop,
                windowWidth,
                windowHeight,
                contentX,
                contentY,
                sidebarWidth,
                contentHeight,
                rightX,
                rightWidth,
                summaryHeight,
                detailsHeight
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
        int rowsTop = layout.sidebarY() + 24;
        int rowsBottom = layout.sidebarY() + layout.sidebarHeight() - 8;
        int trackRight = layout.sidebarX() + layout.sidebarWidth() - 6;
        int trackLeft = trackRight - SCROLLBAR_WIDTH;
        int rowsRight = trackLeft - SCROLLBAR_GAP;
        int rowsLeft = layout.sidebarX() + 6;
        int visibleRows = Math.max(1, (rowsBottom - rowsTop) / LIST_ROW_HEIGHT);
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

    private record Layout(
            int windowLeft,
            int windowTop,
            int windowWidth,
            int windowHeight,
            int contentX,
            int contentY,
            int sidebarWidth,
            int sidebarHeight,
            int rightX,
            int rightWidth,
            int summaryHeight,
            int detailsHeight
    ) {
        int sidebarX() {
            return contentX;
        }

        int sidebarY() {
            return contentY;
        }

        int summaryX() {
            return rightX;
        }

        int summaryY() {
            return contentY;
        }

        int summaryWidth() {
            return rightWidth;
        }

        int detailsX() {
            return rightX;
        }

        int detailsY() {
            return contentY + summaryHeight + 8;
        }

        int detailsWidth() {
            return rightWidth;
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
