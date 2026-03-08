package io.github.ozokuz.incore.client.features.entropy;

import io.github.ozokuz.incore.Registration;
import io.github.ozokuz.incore.features.entropy.EntropyClientCache;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class EntropyBarHudFeature {
    private static final float ENTROPY_GAIN_MIN_VISIBLE_ALPHA = 0.01F;
    private static final ResourceLocation ENTROPY_GAIN_BAR_BACKGROUND = ResourceLocation.parse("incore:hud/experience_bar_background_white_short");
    private static final ResourceLocation ENTROPY_GAIN_BAR_PROGRESS = ResourceLocation.parse("incore:hud/experience_bar_progress_white_short");
    private static final int ENTROPY_BAR_WIDTH = 78;
    private static final int ENTROPY_BAR_HEIGHT = 5;
    private static final long ENTROPY_GAIN_FILL_DURATION_MS = 450L;
    private static final long ENTROPY_GAIN_HOLD_DURATION_MS = 800L;
    private static final long ENTROPY_GAIN_FADE_DURATION_MS = 350L;
    private static long lastHandledBoosterAnimationToken = 0L;
    private static long entropyGainAnimationStartedAtMs = -1L;
    private static int entropyGainFrom = 0;
    private static int entropyGainTo = 0;
    private static int entropyGainCap = 1;
    private static int entropyGainAmount = 0;

    private EntropyBarHudFeature() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(EntropyBarHudFeature::onClientTick);
        NeoForge.EVENT_BUS.addListener(EntropyBarHudFeature::onRenderGui);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            clearEntropyGainState();
        }
    }

    private static void onRenderGui(RenderGuiEvent.Post event) {
        GuiGraphics guiGraphics = event.getGuiGraphics();
        renderEntropyGainBar(guiGraphics);
        renderFullEntropyIcon(guiGraphics);
    }

    private static void renderFullEntropyIcon(GuiGraphics guiGraphics) {
        int cap = EntropyClientCache.getCap();
        if (cap <= 0 || EntropyClientCache.getCurrent() < cap) {
            return;
        }

        int centerX = guiGraphics.guiWidth() / 2;
        int x = centerX + 98;
        int y = guiGraphics.guiHeight() - 33;

        ItemStack icon = Registration.ENTROPY_VESSEL_ITEM.get().getDefaultInstance();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0);
        guiGraphics.pose().scale(0.75F, 0.75F, 1.0F);
        guiGraphics.renderItem(icon, 0, 0);
        guiGraphics.pose().popPose();
    }

    private static void renderEntropyGainBar(GuiGraphics guiGraphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.isSpectator() || minecraft.options.hideGui) {
            return;
        }

        updateEntropyGainAnimationState();

        if (entropyGainAnimationStartedAtMs < 0L) {
            return;
        }

        long now = Util.getMillis();
        long elapsed = Math.max(0L, now - entropyGainAnimationStartedAtMs);
        long totalDuration = ENTROPY_GAIN_FILL_DURATION_MS + ENTROPY_GAIN_HOLD_DURATION_MS + ENTROPY_GAIN_FADE_DURATION_MS;
        if (elapsed >= totalDuration) {
            entropyGainAnimationStartedAtMs = -1L;
            return;
        }

        float fillProgress = elapsed >= ENTROPY_GAIN_FILL_DURATION_MS
                ? 1.0F
                : (float) elapsed / (float) ENTROPY_GAIN_FILL_DURATION_MS;
        float alpha = 1.0F;
        long fadeStart = ENTROPY_GAIN_FILL_DURATION_MS + ENTROPY_GAIN_HOLD_DURATION_MS;
        if (elapsed > fadeStart) {
            alpha = 1.0F - ((float) (elapsed - fadeStart) / (float) ENTROPY_GAIN_FADE_DURATION_MS);
        }
        if (alpha <= ENTROPY_GAIN_MIN_VISIBLE_ALPHA) {
            entropyGainAnimationStartedAtMs = -1L;
            return;
        }

        int start = Mth.clamp(entropyGainFrom, 0, entropyGainCap);
        int target = Mth.clamp(entropyGainTo, start, entropyGainCap);
        int current = Mth.floor(Mth.lerp(fillProgress, start, target));
        float currentRatio = entropyGainCap <= 0 ? 0.0F : (float) current / (float) entropyGainCap;

        int centerX = guiGraphics.guiWidth() / 2;
        int x = centerX + 92;
        int y = guiGraphics.guiHeight() - 28;

        int textColor = withAlpha(alpha, 0xFFFFFF);

        int fillWidth = Math.clamp(Math.round(ENTROPY_BAR_WIDTH * currentRatio), 0, ENTROPY_BAR_WIDTH);

        guiGraphics.setColor(1.0F, 1.0F, 1.0F, alpha);
        guiGraphics.blitSprite(ENTROPY_GAIN_BAR_BACKGROUND, x, y, ENTROPY_BAR_WIDTH, ENTROPY_BAR_HEIGHT);
        if (fillWidth > 0) {
            guiGraphics.enableScissor(x, y, x + fillWidth, y + ENTROPY_BAR_HEIGHT);
            guiGraphics.blitSprite(ENTROPY_GAIN_BAR_PROGRESS, x, y, ENTROPY_BAR_WIDTH, ENTROPY_BAR_HEIGHT);
            guiGraphics.disableScissor();
        }
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        guiGraphics.drawString(minecraft.font, "+" + entropyGainAmount, x + ENTROPY_BAR_WIDTH + 6, y - 1, textColor, true);
    }

    private static void updateEntropyGainAnimationState() {
        long animationToken = EntropyClientCache.getBoosterAnimationToken();
        if (animationToken <= 0L || animationToken == lastHandledBoosterAnimationToken) {
            return;
        }

        entropyGainFrom = EntropyClientCache.getBoosterAnimationFrom();
        entropyGainTo = EntropyClientCache.getBoosterAnimationTo();
        entropyGainCap = Math.max(1, EntropyClientCache.getBoosterAnimationCap());
        entropyGainAmount = EntropyClientCache.getBoosterAnimationGain();
        if (entropyGainAmount <= 0 || entropyGainTo <= entropyGainFrom) {
            lastHandledBoosterAnimationToken = animationToken;
            return;
        }
        entropyGainAnimationStartedAtMs = Util.getMillis();
        lastHandledBoosterAnimationToken = animationToken;
    }

    private static int withAlpha(float alpha, int rgb) {
        int a = Math.clamp(Math.round(Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F), 0, 255);
        return (a << 24) | (rgb & 0x00FFFFFF);
    }

    private static void clearEntropyGainState() {
        lastHandledBoosterAnimationToken = EntropyClientCache.getBoosterAnimationToken();
        entropyGainAnimationStartedAtMs = -1L;
        entropyGainFrom = 0;
        entropyGainTo = 0;
        entropyGainCap = 1;
        entropyGainAmount = 0;
    }
}
