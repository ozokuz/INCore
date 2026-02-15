package io.github.ozokuz.incore.client.status;

import io.github.ozokuz.incore.features.playerlevel.network.PlayerLevelClientCache;
import io.github.ozokuz.incore.features.sanity.SanityClientCache;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class PlayerStatusScreen extends Screen {
    private static final int TARGET_WINDOW_WIDTH = 504;
    private static final int TARGET_WINDOW_HEIGHT = 280;
    private static final ResourceLocation XP_BAR_BACKGROUND = ResourceLocation.parse("incore:hud/experience_bar_background_white");
    private static final ResourceLocation XP_BAR_PROGRESS = ResourceLocation.parse("incore:hud/experience_bar_progress_white");
    private static final int XP_BAR_WIDTH = 182;
    private static final int XP_BAR_HEIGHT = 5;
    private Integer previousMenuBlur;

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

        int windowLeft = this.windowLeft();
        int windowTop = this.windowTop();
        int insideLeft = windowLeft + AdvancementWindowRenderer.BORDER_LEFT + 6;
        int insideRight = windowLeft + this.windowWidth() - AdvancementWindowRenderer.BORDER_RIGHT - 6;
        int insideBottom = windowTop + this.windowHeight() - AdvancementWindowRenderer.BORDER_BOTTOM - 8;
        int sectionGap = 12;
        int sectionWidth = (insideRight - insideLeft - sectionGap) / 2;
        int levelSectionX = insideLeft + sectionWidth + sectionGap;

        int buttonWidth = Math.max(120, Math.min(170, sectionWidth - 16));
        int buttonX = levelSectionX + (sectionWidth - buttonWidth) / 2;
        int rewardsButtonY = insideBottom - 44;
        int battlePassButtonY = insideBottom - 20;

        this.addRenderableWidget(Button.builder(
                        Component.translatable("screen.incore.player_status.open_rewards"),
                        button -> this.minecraft.setScreen(new PlayerLevelRewardsScreen(this))
                ).bounds(buttonX, rewardsButtonY, buttonWidth, 20)
                .build());
        this.addRenderableWidget(Button.builder(
                        Component.translatable("screen.incore.player_status.open_battle_pass"),
                        button -> this.minecraft.setScreen(new BattlePassScreen(this))
                ).bounds(buttonX, battlePassButtonY, buttonWidth, 20)
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
        int insideLeft = windowLeft + AdvancementWindowRenderer.BORDER_LEFT + 6;
        int insideRight = windowLeft + this.windowWidth() - AdvancementWindowRenderer.BORDER_RIGHT - 6;
        int insideBottom = windowTop + this.windowHeight() - AdvancementWindowRenderer.BORDER_BOTTOM - 8;
        int sectionGap = 12;
        int sectionWidth = (insideRight - insideLeft - sectionGap) / 2;
        int sanitySectionX = insideLeft;
        int levelSectionX = insideLeft + sectionWidth + sectionGap;
        AdvancementWindowRenderer.draw(guiGraphics, windowLeft, windowTop, this.windowWidth(), this.windowHeight());

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int titleY = windowTop + (AdvancementWindowRenderer.BORDER_TOP - this.font.lineHeight) / 2 + 1;
        int titleX = windowLeft + AdvancementWindowRenderer.BORDER_LEFT + 8;
        guiGraphics.drawString(this.font, this.title, titleX, titleY, 0xF5F5F5);

        drawSectionBackground(guiGraphics, sanitySectionX, insideTop + 4, sectionWidth, insideBottom - (insideTop + 4));
        drawSectionBackground(guiGraphics, levelSectionX, insideTop + 4, sectionWidth, insideBottom - (insideTop + 4));

        int sanityTextX = sanitySectionX + 8;
        int meterX = sanitySectionX + 8;
        int meterWidth = Math.min(XP_BAR_WIDTH, sectionWidth - 16);
        int meterY = insideTop + 28;

        int cap = Math.max(1, SanityClientCache.getCap());
        int sanity = Math.min(cap, SanityClientCache.getCurrent());
        float ratio = (float) sanity / (float) cap;

        guiGraphics.drawString(this.font, Component.translatable("screen.incore.player_status.sanity"), sanityTextX, insideTop + 12, 0xDADADA);
        guiGraphics.blitSprite(XP_BAR_BACKGROUND, meterX, meterY, meterWidth, XP_BAR_HEIGHT);

        int fillWidth = Math.max(0, Math.min(meterWidth, Math.round(meterWidth * ratio)));
        if (fillWidth > 0) {
            guiGraphics.enableScissor(meterX, meterY, meterX + fillWidth, meterY + XP_BAR_HEIGHT);
            guiGraphics.blitSprite(XP_BAR_PROGRESS, meterX, meterY, meterWidth, XP_BAR_HEIGHT);
            guiGraphics.disableScissor();
        }

        Component valueText = Component.literal(sanity + " / " + cap);
        guiGraphics.drawString(this.font, valueText, sanityTextX, meterY + 10, 0xFFFFFF);
        guiGraphics.drawString(
                this.font,
                Component.translatable(
                        "screen.incore.player_status.next_gain",
                        formatCountdown(SanityClientCache.getMillisUntilNextIncrease())
                ),
                sanityTextX,
                meterY + 26,
                0xFFFFFF
        );
        guiGraphics.drawString(
                this.font,
                Component.translatable(
                        "screen.incore.player_status.full_in",
                        formatCountdown(SanityClientCache.getMillisUntilFull())
                ),
                sanityTextX,
                meterY + 40,
                0xFFFFFF
        );

        int level = PlayerLevelClientCache.getLevel();
        int levelExperience = PlayerLevelClientCache.getCurrentExperience();
        int nextLevelCost = PlayerLevelClientCache.getExperienceToNextLevel();
        int levelTextX = levelSectionX + 8;
        int levelHeaderY = insideTop + 12;
        guiGraphics.drawString(
                this.font,
                Component.translatable("screen.incore.player_status.section_level"),
                levelTextX,
                levelHeaderY,
                0xDADADA
        );
        guiGraphics.drawString(
                this.font,
                Component.translatable("screen.incore.player_status.level", level),
                levelTextX,
                insideTop + 32,
                0xFFFFFF
        );
        guiGraphics.drawString(
                this.font,
                Component.translatable("screen.incore.player_status.level_progress", levelExperience, nextLevelCost),
                levelTextX,
                insideTop + 46,
                0xFFFFFF
        );

        guiGraphics.drawString(
                this.font,
                Component.translatable("screen.incore.player_status.hint"),
                levelTextX,
                insideTop + 66,
                0xD0D0D0
        );
    }

    private static void drawSectionBackground(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        guiGraphics.fill(x, y, x + width, y + height, 0x26000000);
        guiGraphics.fill(x, y, x + width, y + 1, 0x60000000);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, 0x60000000);
        guiGraphics.fill(x, y, x + 1, y + height, 0x60000000);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, 0x60000000);
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
}
