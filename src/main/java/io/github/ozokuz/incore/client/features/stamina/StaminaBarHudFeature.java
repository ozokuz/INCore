package io.github.ozokuz.incore.client.features.stamina;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.Util;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.common.NeoForge;
import tictim.paraglider.api.stamina.Stamina;

public final class StaminaBarHudFeature {
    private static final ResourceLocation PARAGLIDER_STAMINA_WHEEL_LAYER = ResourceLocation.fromNamespaceAndPath("paraglider", "stamina_wheel");
    private static final ResourceLocation STAMINA_BAR_BACKGROUND_SPRITE = ResourceLocation.parse("incore:hud/stamina_bar_background");
    private static final ResourceLocation STAMINA_BAR_PROGRESS_SPRITE = ResourceLocation.parse("incore:hud/stamina_bar_progress");
    private static final int STAMINA_BAR_WIDTH = 10;
    private static final int STAMINA_BAR_HEIGHT = 40;
    private static final int STAMINA_BAR_X_OFFSET_FROM_CROSSHAIR = 22;
    private static final float DEPLETED_BRIGHTNESS = 0.85F;
    private static final float DEPLETED_RED_TINT_GREEN_BLUE = 0.45F;
    private static final float FULL_RATIO_EPSILON = 0.001F;
    private static final float DEPLETED_TINT_BLEND_START_RATIO = 0.80F;
    private static final float DEPLETED_TINT_BLEND_END_RATIO = 1.10F;
    private static final float DEPLETED_TINT_POST_FULL_SECONDS = 0.35F;
    private static final float FADE_IN_ALPHA_PER_SECOND = 12.0F;
    private static final float FADE_OUT_ALPHA_PER_SECOND = 2.5F;
    private static final float MIN_VISIBLE_ALPHA = 0.01F;
    private static float renderedAlpha = 0.0F;
    private static long lastRenderTimeMs = -1L;
    private static float depletedTintPostFullProgress = 0.0F;

    private StaminaBarHudFeature() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(StaminaBarHudFeature::onRenderGuiLayerPre);
        NeoForge.EVENT_BUS.addListener(StaminaBarHudFeature::onRenderGui);
    }

    private static void onRenderGuiLayerPre(RenderGuiLayerEvent.Pre event) {
        if (PARAGLIDER_STAMINA_WHEEL_LAYER.equals(event.getName())) {
            event.setCanceled(true);
        }
    }

    private static void onRenderGui(RenderGuiEvent.Post event) {
        renderStaminaBar(event.getGuiGraphics());
    }

    private static void renderStaminaBar(GuiGraphics guiGraphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null || minecraft.player.isSpectator()) {
            renderedAlpha = 0.0F;
            lastRenderTimeMs = -1L;
            depletedTintPostFullProgress = 0.0F;
            return;
        }

        Stamina stamina = Stamina.get(minecraft.player);
        float capacity = (float) stamina.maxStamina();
        if (capacity <= 0.0F) {
            renderedAlpha = 0.0F;
            lastRenderTimeMs = -1L;
            depletedTintPostFullProgress = 0.0F;
            return;
        }

        long now = Util.getMillis();
        float deltaSeconds = lastRenderTimeMs < 0L ? 0.0F : Mth.clamp((now - lastRenderTimeMs) / 1000.0F, 0.0F, 0.25F);
        lastRenderTimeMs = now;

        float current = Mth.clamp((float) stamina.stamina(), 0.0F, capacity);
        float ratio = current / capacity;
        boolean isDepleted = stamina.isDepleted();
        boolean shouldDisplay = isDepleted || ratio < (1.0F - FULL_RATIO_EPSILON);
        float targetAlpha = shouldDisplay ? 1.0F : 0.0F;
        if (renderedAlpha < targetAlpha) {
            renderedAlpha = Math.min(targetAlpha, renderedAlpha + (deltaSeconds * FADE_IN_ALPHA_PER_SECOND));
        } else if (renderedAlpha > targetAlpha) {
            renderedAlpha = Math.max(targetAlpha, renderedAlpha - (deltaSeconds * FADE_OUT_ALPHA_PER_SECOND));
        }
        if (renderedAlpha <= MIN_VISIBLE_ALPHA) {
            renderedAlpha = 0.0F;
            return;
        }
        float red = 1.0F;
        float green = 1.0F;
        float blue = 1.0F;
        if (isDepleted) {
            if (ratio >= (1.0F - FULL_RATIO_EPSILON)) {
                depletedTintPostFullProgress = Math.min(1.0F, depletedTintPostFullProgress + (deltaSeconds / DEPLETED_TINT_POST_FULL_SECONDS));
            } else {
                depletedTintPostFullProgress = 0.0F;
            }

            float depletedRed = DEPLETED_BRIGHTNESS;
            float depletedGreenBlue = DEPLETED_BRIGHTNESS * DEPLETED_RED_TINT_GREEN_BLUE;
            float extendedRatio = ratio + (depletedTintPostFullProgress * (DEPLETED_TINT_BLEND_END_RATIO - 1.0F));
            float fillBlend = Mth.clamp(
                    (extendedRatio - DEPLETED_TINT_BLEND_START_RATIO) / (DEPLETED_TINT_BLEND_END_RATIO - DEPLETED_TINT_BLEND_START_RATIO),
                    0.0F,
                    1.0F
            );
            fillBlend = fillBlend * fillBlend * (3.0F - (2.0F * fillBlend));
            red = Mth.lerp(fillBlend, depletedRed, 1.0F);
            green = Mth.lerp(fillBlend, depletedGreenBlue, 1.0F);
            blue = Mth.lerp(fillBlend, depletedGreenBlue, 1.0F);
        } else {
            depletedTintPostFullProgress = 0.0F;
        }

        int x = (guiGraphics.guiWidth() / 2) + STAMINA_BAR_X_OFFSET_FROM_CROSSHAIR;
        int y = (guiGraphics.guiHeight() - STAMINA_BAR_HEIGHT) / 2;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.setColor(red, green, blue, renderedAlpha);
        guiGraphics.blitSprite(STAMINA_BAR_BACKGROUND_SPRITE, x, y, STAMINA_BAR_WIDTH, STAMINA_BAR_HEIGHT);

        int fillHeight = Math.clamp(Math.round(STAMINA_BAR_HEIGHT * ratio), 0, STAMINA_BAR_HEIGHT);
        if (fillHeight > 0) {
            int fillTop = y + STAMINA_BAR_HEIGHT - fillHeight;
            guiGraphics.enableScissor(x, fillTop, x + STAMINA_BAR_WIDTH, y + STAMINA_BAR_HEIGHT);
            guiGraphics.blitSprite(STAMINA_BAR_PROGRESS_SPRITE, x, y, STAMINA_BAR_WIDTH, STAMINA_BAR_HEIGHT);
            guiGraphics.disableScissor();
        }
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }
}
