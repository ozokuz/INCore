package io.github.ozokuz.incore.client.status;

import io.github.ozokuz.incore.features.sanity.SanityClientCache;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class PlayerStatusScreen extends Screen {
    private static final ResourceLocation XP_BAR_BACKGROUND = ResourceLocation.parse("minecraft:hud/experience_bar_background");
    private static final ResourceLocation XP_BAR_PROGRESS = ResourceLocation.parse("incore:hud/experience_bar_progress_white");
    private static final int XP_BAR_WIDTH = 182;
    private static final int XP_BAR_HEIGHT = 5;

    public PlayerStatusScreen() {
        super(Component.translatable("screen.incore.player_status.title"));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int titleY = this.height / 2 - 70;
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, titleY, 0xF5F5F5);

        int meterX = (this.width - XP_BAR_WIDTH) / 2;
        int meterY = this.height / 2 - 24;

        int cap = Math.max(1, SanityClientCache.getCap());
        int sanity = Math.min(cap, SanityClientCache.getCurrent());
        float ratio = (float) sanity / (float) cap;

        guiGraphics.drawString(this.font, Component.translatable("screen.incore.player_status.sanity"), meterX, meterY - 14, 0xDADADA);
        guiGraphics.blitSprite(XP_BAR_BACKGROUND, meterX, meterY, XP_BAR_WIDTH, XP_BAR_HEIGHT);

        int fillWidth = Math.max(0, Math.min(XP_BAR_WIDTH, Math.round(XP_BAR_WIDTH * ratio)));
        if (fillWidth > 0) {
            guiGraphics.enableScissor(meterX, meterY, meterX + fillWidth, meterY + XP_BAR_HEIGHT);
            guiGraphics.blitSprite(XP_BAR_PROGRESS, meterX, meterY, XP_BAR_WIDTH, XP_BAR_HEIGHT);
            guiGraphics.disableScissor();
        }

        Component valueText = Component.literal(sanity + " / " + cap);
        guiGraphics.drawCenteredString(this.font, valueText, this.width / 2, meterY + 10, 0x80FF20);
        guiGraphics.drawCenteredString(
                this.font,
                Component.translatable(
                        "screen.incore.player_status.next_gain",
                        formatCountdown(SanityClientCache.getMillisUntilNextIncrease())
                ),
                this.width / 2,
                meterY + 24,
                0xFFE0E0E0
        );
        guiGraphics.drawCenteredString(
                this.font,
                Component.translatable(
                        "screen.incore.player_status.full_in",
                        formatCountdown(SanityClientCache.getMillisUntilFull())
                ),
                this.width / 2,
                meterY + 36,
                0xFFE0E0E0
        );

        guiGraphics.drawCenteredString(
                this.font,
                Component.translatable("screen.incore.player_status.hint"),
                this.width / 2,
                meterY + 54,
                0xFF9A9A9A
        );
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
