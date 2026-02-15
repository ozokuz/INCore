package io.github.ozokuz.incore.features.research.client;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.ozokuz.incore.features.research.LabMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class LabScreen extends AbstractContainerScreen<LabMenu> {
    private static final ResourceLocation BG = ResourceLocation.parse("textures/gui/container/generic_54.png");

    public LabScreen(LabMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderTexture(0, BG);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        guiGraphics.blit(BG, x, y, 0, 0, imageWidth, imageHeight, 256, 256);

        int progress = menu.progressScaled(24);
        guiGraphics.fill(x + 106, y + 34, x + 106 + progress, y + 50, 0xFF6CCB5F);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.research_lab.progress", menu.progress(), menu.maxProgress()), 8, 64, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
