package io.github.ozokuz.incore.client.features.roguelike;

import io.github.ozokuz.incore.features.roguelike.content.CustomDungeonCrystalForgeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class CustomDungeonCrystalForgeScreen extends AbstractContainerScreen<CustomDungeonCrystalForgeMenu> {
    public CustomDungeonCrystalForgeScreen(CustomDungeonCrystalForgeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 216;
        this.imageHeight = 180;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xCC111724);
        guiGraphics.fill(x + 8, y + 14, x + imageWidth - 8, y + 70, 0x99202A3A);
        guiGraphics.fill(x + 8, y + 82, x + imageWidth - 8, y + imageHeight - 8, 0x99202A3A);

        drawSlot(guiGraphics, x + CustomDungeonCrystalForgeMenu.INPUT_X, y + CustomDungeonCrystalForgeMenu.INPUT_Y);
        drawSlot(guiGraphics, x + CustomDungeonCrystalForgeMenu.THEME_X, y + CustomDungeonCrystalForgeMenu.THEME_Y);
        drawSlot(guiGraphics, x + CustomDungeonCrystalForgeMenu.OBJECTIVE_X, y + CustomDungeonCrystalForgeMenu.OBJECTIVE_Y);
        drawSlot(guiGraphics, x + CustomDungeonCrystalForgeMenu.MODIFIER_X, y + CustomDungeonCrystalForgeMenu.MODIFIER_Y);
        drawSlot(guiGraphics, x + CustomDungeonCrystalForgeMenu.MODIFIER_X, y + CustomDungeonCrystalForgeMenu.MODIFIER_Y + 20);
        drawSlot(guiGraphics, x + CustomDungeonCrystalForgeMenu.MODIFIER_X, y + CustomDungeonCrystalForgeMenu.MODIFIER_Y + 40);
        drawSlot(guiGraphics, x + CustomDungeonCrystalForgeMenu.OUTPUT_X, y + CustomDungeonCrystalForgeMenu.OUTPUT_Y);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, 10, 4, 0xE8EEF8, false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.custom_crystal_forge.input"), 20, 12, 0xD6E0EF, false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.custom_crystal_forge.theme"), 56, 12, 0xD6E0EF, false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.custom_crystal_forge.objective"), 92, 12, 0xD6E0EF, false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.custom_crystal_forge.modifiers"), 128, 12, 0xD6E0EF, false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.custom_crystal_forge.output"), 172, 12, 0xD6E0EF, false);

        int statusColor = menu.validPreview() ? 0x8EF7A0 : 0xFF7A7A;
        String statusKey = menu.validPreview()
                ? "screen.incore.custom_crystal_forge.status.ready"
                : "screen.incore.custom_crystal_forge.status.invalid";
        guiGraphics.drawString(font, Component.translatable(statusKey), 12, 72, statusColor, false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.custom_crystal_forge.hint"), 12, 84, 0x98A6B8, false);
        guiGraphics.drawString(font, playerInventoryTitle, 20, 92, 0xD6E0EF, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private static void drawSlot(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF46566F);
        guiGraphics.fill(x, y, x + 16, y + 16, 0xFF181F2A);
    }
}
