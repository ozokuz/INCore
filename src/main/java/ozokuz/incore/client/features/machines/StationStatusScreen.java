package ozokuz.incore.client.features.machines;

import ozokuz.incore.client.ui.UITheme;
import ozokuz.incore.features.machines.multiblock.MachinePowerFamily;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.Nullable;

public abstract class StationStatusScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    private static final UITheme THEME = ResearchScreenRenderer.theme();

    protected StationStatusScreen(T menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 122;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int left = leftPos;
        int top = topPos;
        ResearchScreenRenderer.drawAccentedWindow(guiGraphics, left, top, imageWidth, imageHeight, accentColor());
        ResearchScreenRenderer.drawAccentedPanel(guiGraphics, left + 8, top + 20, imageWidth - 16, imageHeight - 28, accentColor());
        renderStatusBody(guiGraphics, left, top, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, 12, 8, titleColor(), false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        ResearchScreenRenderer.drawBackdrop(guiGraphics, width, height);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    protected int accentColor() {
        return 0xFF4D8B9D;
    }

    protected int panelFillColor() {
        return THEME.window().fill();
    }

    protected int innerPanelFillColor() {
        return THEME.panel().fill();
    }

    protected int borderColor() {
        return THEME.window().borderTop();
    }

    protected int titleColor() {
        return THEME.text().primary();
    }

    protected int labelColor() {
        return THEME.text().secondary();
    }

    protected int valueColor() {
        return THEME.text().primary();
    }

    protected int okColor() {
        return THEME.text().success();
    }

    protected int warnColor() {
        return THEME.text().warning();
    }

    protected abstract void renderStatusBody(GuiGraphics guiGraphics, int left, int top, int mouseX, int mouseY);

    protected void drawKeyValue(GuiGraphics guiGraphics, int x, int y, Component label, Component value, int color) {
        drawKeyValue(guiGraphics, x, y, label, value, color, 78);
    }

    protected void drawKeyValue(GuiGraphics guiGraphics, int x, int y, Component label, Component value, int color, int valueOffset) {
        guiGraphics.drawString(font, label, x, y, labelColor(), false);
        guiGraphics.drawString(font, value, x + valueOffset, y, color, false);
    }

    protected static Component powerFamilyLabel(@Nullable MachinePowerFamily family) {
        if (family == null) {
            return Component.translatable("screen.incore.power_input.family.none");
        }
        return switch (family) {
            case ELECTRIC -> Component.translatable("screen.incore.power_input.family.electric");
            case MECHANICAL -> Component.translatable("screen.incore.power_input.family.mechanical");
            case BURNER -> Component.translatable("screen.incore.power_input.family.burner");
        };
    }
}
