package io.github.ozokuz.incore.client.features.sanity;

import io.github.ozokuz.incore.Registration;
import io.github.ozokuz.incore.features.sanity.SanityClientCache;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class SanityBarHudFeature {
    private static final float SANITY_GAIN_MIN_VISIBLE_ALPHA = 0.01F;
    private static final ResourceLocation SANITY_GAIN_BAR_BACKGROUND = ResourceLocation.parse("incore:hud/experience_bar_background_white_short");
    private static final ResourceLocation SANITY_GAIN_BAR_PROGRESS = ResourceLocation.parse("incore:hud/experience_bar_progress_white_short");
    private static final int SANITY_BAR_WIDTH = 78;
    private static final int SANITY_BAR_HEIGHT = 5;
    private static final long SANITY_GAIN_FILL_DURATION_MS = 450L;
    private static final long SANITY_GAIN_HOLD_DURATION_MS = 800L;
    private static final long SANITY_GAIN_FADE_DURATION_MS = 350L;
    private static long lastHandledBoosterAnimationToken = 0L;
    private static long sanityGainAnimationStartedAtMs = -1L;
    private static int sanityGainFrom = 0;
    private static int sanityGainTo = 0;
    private static int sanityGainCap = 1;
    private static int sanityGainAmount = 0;

    private SanityBarHudFeature() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(SanityBarHudFeature::onClientTick);
        NeoForge.EVENT_BUS.addListener(SanityBarHudFeature::onRenderGui);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            clearSanityGainState();
        }
    }

    private static void onRenderGui(RenderGuiEvent.Post event) {
        GuiGraphics guiGraphics = event.getGuiGraphics();
        renderSanityGainBar(guiGraphics);
        renderFullSanityIcon(guiGraphics);
    }

    private static void renderFullSanityIcon(GuiGraphics guiGraphics) {
        int cap = SanityClientCache.getCap();
        if (cap <= 0 || SanityClientCache.getCurrent() < cap) {
            return;
        }

        int centerX = guiGraphics.guiWidth() / 2;
        int x = centerX + 98;
        int y = guiGraphics.guiHeight() - 33;

        ItemStack icon = Registration.SANITY_VESSEL_ITEM.get().getDefaultInstance();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0);
        guiGraphics.pose().scale(0.75F, 0.75F, 1.0F);
        guiGraphics.renderItem(icon, 0, 0);
        guiGraphics.pose().popPose();
    }

    private static void renderSanityGainBar(GuiGraphics guiGraphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.isSpectator() || minecraft.options.hideGui) {
            return;
        }

        updateSanityGainAnimationState();

        if (sanityGainAnimationStartedAtMs < 0L) {
            return;
        }

        long now = Util.getMillis();
        long elapsed = Math.max(0L, now - sanityGainAnimationStartedAtMs);
        long totalDuration = SANITY_GAIN_FILL_DURATION_MS + SANITY_GAIN_HOLD_DURATION_MS + SANITY_GAIN_FADE_DURATION_MS;
        if (elapsed >= totalDuration) {
            sanityGainAnimationStartedAtMs = -1L;
            return;
        }

        float fillProgress = elapsed >= SANITY_GAIN_FILL_DURATION_MS
                ? 1.0F
                : (float) elapsed / (float) SANITY_GAIN_FILL_DURATION_MS;
        float alpha = 1.0F;
        long fadeStart = SANITY_GAIN_FILL_DURATION_MS + SANITY_GAIN_HOLD_DURATION_MS;
        if (elapsed > fadeStart) {
            alpha = 1.0F - ((float) (elapsed - fadeStart) / (float) SANITY_GAIN_FADE_DURATION_MS);
        }
        if (alpha <= SANITY_GAIN_MIN_VISIBLE_ALPHA) {
            sanityGainAnimationStartedAtMs = -1L;
            return;
        }

        int start = Mth.clamp(sanityGainFrom, 0, sanityGainCap);
        int target = Mth.clamp(sanityGainTo, start, sanityGainCap);
        int current = Mth.floor(Mth.lerp(fillProgress, start, target));
        float currentRatio = sanityGainCap <= 0 ? 0.0F : (float) current / (float) sanityGainCap;

        int centerX = guiGraphics.guiWidth() / 2;
        int x = centerX + 92;
        int y = guiGraphics.guiHeight() - 28;

        int textColor = withAlpha(alpha, 0xFFFFFF);

        int fillWidth = Math.clamp(Math.round(SANITY_BAR_WIDTH * currentRatio), 0, SANITY_BAR_WIDTH);

        guiGraphics.setColor(1.0F, 1.0F, 1.0F, alpha);
        guiGraphics.blitSprite(SANITY_GAIN_BAR_BACKGROUND, x, y, SANITY_BAR_WIDTH, SANITY_BAR_HEIGHT);
        if (fillWidth > 0) {
            guiGraphics.enableScissor(x, y, x + fillWidth, y + SANITY_BAR_HEIGHT);
            guiGraphics.blitSprite(SANITY_GAIN_BAR_PROGRESS, x, y, SANITY_BAR_WIDTH, SANITY_BAR_HEIGHT);
            guiGraphics.disableScissor();
        }
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        guiGraphics.drawString(minecraft.font, "+" + sanityGainAmount, x + SANITY_BAR_WIDTH + 6, y - 1, textColor, true);
    }

    private static void updateSanityGainAnimationState() {
        long animationToken = SanityClientCache.getBoosterAnimationToken();
        if (animationToken <= 0L || animationToken == lastHandledBoosterAnimationToken) {
            return;
        }

        sanityGainFrom = SanityClientCache.getBoosterAnimationFrom();
        sanityGainTo = SanityClientCache.getBoosterAnimationTo();
        sanityGainCap = Math.max(1, SanityClientCache.getBoosterAnimationCap());
        sanityGainAmount = SanityClientCache.getBoosterAnimationGain();
        if (sanityGainAmount <= 0 || sanityGainTo <= sanityGainFrom) {
            lastHandledBoosterAnimationToken = animationToken;
            return;
        }
        sanityGainAnimationStartedAtMs = Util.getMillis();
        lastHandledBoosterAnimationToken = animationToken;
    }

    private static int withAlpha(float alpha, int rgb) {
        int a = Math.clamp(Math.round(Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F), 0, 255);
        return (a << 24) | (rgb & 0x00FFFFFF);
    }

    private static void clearSanityGainState() {
        lastHandledBoosterAnimationToken = SanityClientCache.getBoosterAnimationToken();
        sanityGainAnimationStartedAtMs = -1L;
        sanityGainFrom = 0;
        sanityGainTo = 0;
        sanityGainCap = 1;
        sanityGainAmount = 0;
    }
}
