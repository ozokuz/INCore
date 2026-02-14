package io.github.ozokuz.incore.client.status;

import io.github.ozokuz.incore.features.playerlevel.network.PlayerLevelClientCache;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class PlayerLevelRewardsScreen extends Screen {
    private static final int TARGET_WINDOW_WIDTH = 504;
    private static final int TARGET_WINDOW_HEIGHT = 280;
    private static final int LINE_HEIGHT = 12;
    private static final ResourceLocation XP_BAR_BACKGROUND = ResourceLocation.parse("incore:hud/experience_bar_background_white");
    private static final ResourceLocation XP_BAR_PROGRESS = ResourceLocation.parse("incore:hud/experience_bar_progress_white");
    private static final int XP_BAR_WIDTH = 182;
    private static final int XP_BAR_HEIGHT = 5;
    private final Screen parent;
    private int scrollOffset;
    private Integer previousMenuBlur;

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

        int buttonWidth = 100;
        int buttonX = this.windowLeft() + (this.windowWidth() - buttonWidth) / 2;
        int buttonY = this.windowTop() + this.windowHeight() + 4;
        this.addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> this.onClose())
                .bounds(buttonX, buttonY, buttonWidth, 20)
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
        int windowLeft = this.windowLeft();
        int windowTop = this.windowTop();
        int insideTop = windowTop + AdvancementWindowRenderer.BORDER_TOP;
        int insideBottom = windowTop + this.windowHeight() - AdvancementWindowRenderer.BORDER_BOTTOM;
        AdvancementWindowRenderer.draw(guiGraphics, windowLeft, windowTop, this.windowWidth(), this.windowHeight());

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = windowLeft + this.windowWidth() / 2;
        int currentLevel = PlayerLevelClientCache.getLevel();
        int currentExperience = PlayerLevelClientCache.getCurrentExperience();
        int experienceToNextLevel = PlayerLevelClientCache.getExperienceToNextLevel();

        int titleY = windowTop + (AdvancementWindowRenderer.BORDER_TOP - this.font.lineHeight) / 2 + 1;
        int titleX = windowLeft + AdvancementWindowRenderer.BORDER_LEFT + 8;
        guiGraphics.drawString(this.font, this.title, titleX, titleY, 0xF5F5F5);
        guiGraphics.drawCenteredString(
                this.font,
                Component.translatable("screen.incore.player_level_rewards.current_level", currentLevel),
                centerX,
                insideTop + 8,
                0xFFFFFF
        );
        guiGraphics.drawCenteredString(
                this.font,
                Component.translatable("screen.incore.player_level_rewards.current_progress", currentExperience, experienceToNextLevel),
                centerX,
                insideTop + 22,
                0xFFFFFF
        );

        int barX = windowLeft + (this.windowWidth() - XP_BAR_WIDTH) / 2;
        int barY = insideTop + 36;
        float progressRatio = Math.max(0.0F, Math.min(1.0F, (float) currentExperience / (float) Math.max(1, experienceToNextLevel)));

        guiGraphics.blitSprite(XP_BAR_BACKGROUND, barX, barY, XP_BAR_WIDTH, XP_BAR_HEIGHT);
        int fillWidth = Math.max(0, Math.min(XP_BAR_WIDTH, Math.round(XP_BAR_WIDTH * progressRatio)));
        if (fillWidth > 0) {
            guiGraphics.enableScissor(barX, barY, barX + fillWidth, barY + XP_BAR_HEIGHT);
            guiGraphics.blitSprite(XP_BAR_PROGRESS, barX, barY, XP_BAR_WIDTH, XP_BAR_HEIGHT);
            guiGraphics.disableScissor();
        }

        int listTop = insideTop + 58;
        int listBottom = insideBottom - 8;
        int listHeight = Math.max(0, listBottom - listTop);
        int maxVisibleLines = Math.max(1, listHeight / LINE_HEIGHT);
        List<Component> lines = buildUpcomingLines();
        int maxScroll = Math.max(0, lines.size() - maxVisibleLines);
        this.scrollOffset = Math.clamp(this.scrollOffset, 0, maxScroll);

        if (lines.isEmpty()) {
            guiGraphics.drawCenteredString(
                    this.font,
                    Component.translatable("screen.incore.player_level_rewards.none"),
                    centerX,
                    listTop,
                    0xDADADA
            );
            return;
        }

        for (int i = 0; i < maxVisibleLines; i++) {
            int index = this.scrollOffset + i;
            if (index >= lines.size()) {
                break;
            }
            guiGraphics.drawCenteredString(this.font, lines.get(index), centerX, listTop + i * LINE_HEIGHT, 0xEAEAEA);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY == 0.0D) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        int insideTop = this.windowTop() + AdvancementWindowRenderer.BORDER_TOP;
        int insideBottom = this.windowTop() + this.windowHeight() - AdvancementWindowRenderer.BORDER_BOTTOM;
        int listTop = insideTop + 58;
        int listBottom = insideBottom - 8;
        int listHeight = Math.max(0, listBottom - listTop);
        int maxVisibleLines = Math.max(1, listHeight / LINE_HEIGHT);
        int maxScroll = Math.max(0, buildUpcomingLines().size() - maxVisibleLines);

        int direction = scrollY > 0.0D ? -1 : 1;
        this.scrollOffset = Math.clamp(this.scrollOffset + direction, 0, maxScroll);
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
        return Math.min(TARGET_WINDOW_WIDTH, Math.max(AdvancementWindowRenderer.BASE_WIDTH, this.width - 16));
    }

    private int windowHeight() {
        return Math.min(TARGET_WINDOW_HEIGHT, Math.max(AdvancementWindowRenderer.BASE_HEIGHT, this.height - 40));
    }

    private static List<Component> buildUpcomingLines() {
        List<Component> lines = new ArrayList<>();
        List<PlayerLevelClientCache.RewardPreview> previews = PlayerLevelClientCache.getRewardPreviews();

        for (PlayerLevelClientCache.RewardPreview preview : previews) {
            lines.add(Component.translatable("screen.incore.player_level_rewards.level_row", preview.level(), preview.requiredExperience()));
            if (preview.rewards().isEmpty()) {
                lines.add(Component.translatable("screen.incore.player_level_rewards.level_empty"));
            } else {
                for (String reward : preview.rewards()) {
                    lines.add(Component.literal(" - " + reward));
                }
            }
            lines.add(Component.empty());
        }

        return lines;
    }
}
