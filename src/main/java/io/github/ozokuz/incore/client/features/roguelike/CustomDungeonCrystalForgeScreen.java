package io.github.ozokuz.incore.client.features.roguelike;

import io.github.ozokuz.incore.client.ui.UIScreenTheme;
import io.github.ozokuz.incore.client.ui.render.ThemedUi;
import io.github.ozokuz.incore.features.roguelike.content.CustomDungeonCrystalForgeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class CustomDungeonCrystalForgeScreen extends AbstractContainerScreen<CustomDungeonCrystalForgeMenu> {
    private static final UIScreenTheme THEME = UIScreenTheme.CRAFTING;

    public CustomDungeonCrystalForgeScreen(CustomDungeonCrystalForgeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 216;
        this.imageHeight = 180;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        ThemedUi ui = themed(guiGraphics);
        ui.drawWindow(x, y, imageWidth, imageHeight);
        ui.drawPanel(x + 8, y + 14, imageWidth - 16, 56);
        ui.drawPanel(x + 8, y + 82, imageWidth - 16, imageHeight - 90);

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
        guiGraphics.drawString(font, title, 10, 4, UIScreenTheme.Crafting.TITLE_TEXT, false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.custom_crystal_forge.input"), 20, 12, UIScreenTheme.Crafting.BODY_TEXT, false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.custom_crystal_forge.theme"), 56, 12, UIScreenTheme.Crafting.BODY_TEXT, false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.custom_crystal_forge.objective"), 92, 12, UIScreenTheme.Crafting.BODY_TEXT, false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.custom_crystal_forge.modifiers"), 128, 12, UIScreenTheme.Crafting.BODY_TEXT, false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.custom_crystal_forge.output"), 172, 12, UIScreenTheme.Crafting.BODY_TEXT, false);

        int statusColor = menu.validPreview() ? UIScreenTheme.Crafting.SUCCESS_TEXT : UIScreenTheme.Crafting.DANGER_TEXT;
        String statusKey = menu.validPreview()
                ? "screen.incore.custom_crystal_forge.status.ready"
                : "screen.incore.custom_crystal_forge.status.invalid";
        guiGraphics.drawString(font, Component.translatable(statusKey), 12, 72, statusColor, false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.custom_crystal_forge.hint"), 12, 84, UIScreenTheme.Crafting.MUTED_TEXT, false);
        guiGraphics.drawString(font, playerInventoryTitle, 20, 92, UIScreenTheme.Crafting.BODY_TEXT, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        themed(guiGraphics).drawBackdrop(this.width, this.height);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private static void drawSlot(GuiGraphics guiGraphics, int x, int y) {
        themed(guiGraphics).drawSlotFrame(x, y);
    }

    private static ThemedUi themed(GuiGraphics guiGraphics) {
        return new ThemedUi(guiGraphics, THEME.theme());
    }
}
