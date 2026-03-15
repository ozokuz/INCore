package ozokuz.incore.client.features.research;

import ozokuz.incore.client.features.machines.ResearchScreenRenderer;
import ozokuz.incore.features.research.discovery.TranslatorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class TranslatorScreen extends AbstractContainerScreen<TranslatorMenu> {
    private static final int ACCENT_COLOR = 0xFF7CB9FF;
    private static final int PLAYER_SLOT_OUTER = 0xFF3A312B;
    private static final int PLAYER_SLOT_INNER = 0xFF1B1714;
    private static final int PLAYER_SLOT_HIGHLIGHT = 0xFF7F6A5C;

    public TranslatorScreen(TranslatorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        ResearchScreenRenderer.drawAccentedWindow(guiGraphics, x, y, imageWidth, imageHeight, ACCENT_COLOR);
        ResearchScreenRenderer.drawAccentedPanel(guiGraphics, x + 8, y + 18, imageWidth - 16, 54, ACCENT_COLOR);
        ResearchScreenRenderer.drawAccentedPanel(guiGraphics, x + 8, y + 80, imageWidth - 16, 78, ACCENT_COLOR);

        ResearchScreenRenderer.drawMachineSlotFrame(guiGraphics, x + 44, y + 34, ACCENT_COLOR);
        ResearchScreenRenderer.drawMachineSlotFrame(guiGraphics, x + 116, y + 34, ACCENT_COLOR);
        drawPlayerInventorySlots(guiGraphics, x, y);

        int barX = x + 10;
        int barY = y + 72;
        int barW = imageWidth - 20;
        int fill = menu.progressScaled(barW - 2);
        ResearchScreenRenderer.drawProgressBar(guiGraphics, barX, barY, barW, 6, fill / (float) (barW - 2), ResearchScreenRenderer.theme().progress().fill());
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, 8, 6, ResearchScreenRenderer.titleText(), false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.translator.input"), 35, 20, ResearchScreenRenderer.secondaryText(), false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.translator.output"), 108, 20, ResearchScreenRenderer.secondaryText(), false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.translator.progress", menu.progressTicks(), menu.maxProgressTicks()), 10, 52, ResearchScreenRenderer.accentText(), false);
        String statusKey = menu.getSlot(1).hasItem()
                ? "screen.incore.translator.status.ready"
                : (menu.getSlot(0).hasItem() ? "screen.incore.translator.status.decoding" : "screen.incore.translator.status.idle");
        guiGraphics.drawString(font, Component.translatable(statusKey), 10, 64, ResearchScreenRenderer.secondaryText(), false);
        guiGraphics.drawString(font, playerInventoryTitle, 8, 86, ResearchScreenRenderer.secondaryText(), false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        ResearchScreenRenderer.drawBackdrop(guiGraphics, width, height);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private void drawPlayerInventorySlots(GuiGraphics guiGraphics, int left, int top) {
        for (int slotIndex = 2; slotIndex < menu.slots.size(); slotIndex++) {
            Slot slot = menu.slots.get(slotIndex);
            if (!slot.isActive()) {
                continue;
            }
            ResearchScreenRenderer.drawSlotFrame(
                    guiGraphics,
                    left + slot.x,
                    top + slot.y,
                    PLAYER_SLOT_OUTER,
                    PLAYER_SLOT_INNER,
                    PLAYER_SLOT_HIGHLIGHT
            );
        }
    }
}
