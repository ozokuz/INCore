package io.github.ozokuz.incore.client.features.machines;

import io.github.ozokuz.incore.features.machines.multiblock.PowerInputMenu;
import io.github.ozokuz.incore.features.machines.multiblock.MachinePowerFamily;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.Locale;

public class PowerInputScreen extends StationStatusScreen<PowerInputMenu> {
    public PowerInputScreen(PowerInputMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected int accentColor() {
        return menu.family() == MachinePowerFamily.MECHANICAL ? 0xFF9B6A39 : 0xFF4A8DB2;
    }

    @Override
    protected void renderStatusBody(GuiGraphics guiGraphics, int left, int top, int mouseX, int mouseY) {
        int x = left + 18;
        int y = top + 30;

        drawKeyValue(guiGraphics, x, y, Component.translatable("screen.incore.power_input.family"), familyText(menu.family()), valueColor());
        drawKeyValue(guiGraphics, x, y + 12, Component.translatable("screen.incore.power_input.tier"), Component.literal(Integer.toString(menu.powerTier())), valueColor());
        drawKeyValue(guiGraphics, x, y + 24, Component.translatable("screen.incore.power_input.available_power"), Component.literal(Integer.toString(menu.availablePower())), okColor());

        if (menu.family() == MachinePowerFamily.MECHANICAL) {
            drawKeyValue(guiGraphics, x, y + 42, Component.translatable("screen.incore.power_input.speed"), Component.literal(formatFixed2(menu.primaryValue())), valueColor());
            drawKeyValue(guiGraphics, x, y + 54, Component.translatable("screen.incore.power_input.available_rpm"), Component.literal(formatFixed2(menu.secondaryValue())), valueColor());
            drawKeyValue(guiGraphics, x, y + 66, Component.translatable("screen.incore.power_input.operational"), yesNo(menu.detailAValue() > 0), menu.detailAValue() > 0 ? okColor() : warnColor());
            drawKeyValue(guiGraphics, x, y + 78, Component.translatable("screen.incore.power_input.overstressed"), yesNo(menu.detailBValue() > 0), menu.detailBValue() > 0 ? warnColor() : okColor());
            return;
        }

        drawKeyValue(guiGraphics, x, y + 42, Component.translatable("screen.incore.power_input.energy"), Component.literal(menu.primaryValue() + " / " + menu.secondaryValue()), valueColor());
        drawKeyValue(guiGraphics, x, y + 54, Component.translatable("screen.incore.power_input.max_receive"), Component.literal(Integer.toString(menu.detailAValue())), valueColor());
        drawKeyValue(guiGraphics, x, y + 66, Component.translatable("screen.incore.power_input.max_draw"), Component.literal(Integer.toString(menu.detailBValue())), valueColor());
        drawKeyValue(guiGraphics, x, y + 78, Component.translatable("screen.incore.power_input.max_operation"), Component.literal(Integer.toString(menu.detailCValue())), valueColor());
    }

    private static Component familyText(MachinePowerFamily family) {
        if (family == null) {
            return Component.translatable("screen.incore.power_input.family.none");
        }
        return switch (family) {
            case ELECTRIC -> Component.translatable("screen.incore.power_input.family.electric");
            case MECHANICAL -> Component.translatable("screen.incore.power_input.family.mechanical");
            case BURNER -> Component.translatable("screen.incore.power_input.family.none");
        };
    }

    private static Component yesNo(boolean value) {
        return Component.translatable(value ? "screen.incore.common.yes" : "screen.incore.common.no");
    }

    private static String formatFixed2(int hundredths) {
        return String.format(Locale.ROOT, "%.2f", hundredths / 100.0D);
    }
}
