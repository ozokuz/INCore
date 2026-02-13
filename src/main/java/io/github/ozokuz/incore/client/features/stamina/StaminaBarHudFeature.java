package io.github.ozokuz.incore.client.features.stamina;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.common.NeoForge;
import tictim.paraglider.api.stamina.Stamina;

public final class StaminaBarHudFeature {
    private static final ResourceLocation PARAGLIDER_STAMINA_WHEEL_LAYER = ResourceLocation.fromNamespaceAndPath("paraglider", "stamina_wheel");
    private static final ResourceLocation STAMINA_BAR_BACKGROUND_SPRITE = ResourceLocation.parse("incore:hud/stamina_bar_background");
    private static final ResourceLocation STAMINA_BAR_PROGRESS_SPRITE = ResourceLocation.parse("incore:hud/stamina_bar_progress");
    private static final int STAMINA_BAR_WIDTH = 9;
    private static final int STAMINA_BAR_HEIGHT = 40;
    private static final int STAMINA_BAR_X_OFFSET_FROM_CROSSHAIR = 22;

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
            return;
        }

        Stamina stamina = Stamina.get(minecraft.player);
        if (!stamina.renderStaminaWheel()) {
            return;
        }

        float capacity = (float) stamina.maxStamina();
        if (capacity <= 0.0F) {
            return;
        }

        float current = Mth.clamp((float) stamina.stamina(), 0.0F, capacity);
        float ratio = current / capacity;

        int x = (guiGraphics.guiWidth() / 2) + STAMINA_BAR_X_OFFSET_FROM_CROSSHAIR;
        int y = (guiGraphics.guiHeight() - STAMINA_BAR_HEIGHT) / 2;

        guiGraphics.blitSprite(STAMINA_BAR_BACKGROUND_SPRITE, x, y, STAMINA_BAR_WIDTH, STAMINA_BAR_HEIGHT);

        int fillHeight = Math.clamp(Math.round(STAMINA_BAR_HEIGHT * ratio), 0, STAMINA_BAR_HEIGHT);
        if (fillHeight > 0) {
            int fillTop = y + STAMINA_BAR_HEIGHT - fillHeight;
            guiGraphics.enableScissor(x, fillTop, x + STAMINA_BAR_WIDTH, y + STAMINA_BAR_HEIGHT);
            guiGraphics.blitSprite(STAMINA_BAR_PROGRESS_SPRITE, x, y, STAMINA_BAR_WIDTH, STAMINA_BAR_HEIGHT);
            guiGraphics.disableScissor();
        }
    }
}
