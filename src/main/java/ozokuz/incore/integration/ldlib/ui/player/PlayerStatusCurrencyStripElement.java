package ozokuz.incore.integration.ldlib.ui.player;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import ozokuz.incore.client.ui.UIScreenTheme;
import ozokuz.incore.features.status.network.PlayerStatusCurrencyClientCache;

public final class PlayerStatusCurrencyStripElement extends UIElement {
    private static final float SCALE = 0.75F;

    public PlayerStatusCurrencyStripElement() {
        setAllowHitTest(false);
        internalSetup();
    }

    @Override
    public void drawBackgroundAdditional(GUIContext guiContext) {
        Font font = Minecraft.getInstance().font;
        PlayerStatusCurrencyClientCache.Snapshot snapshot = PlayerStatusCurrencyClientCache.snapshot();
        List<CostRenderLine> lines = snapshot.entries().stream()
                .map(entry -> new CostRenderLine(iconFromId(entry.iconItemId()), "x" + Math.max(0, entry.amount()), UIScreenTheme.Info.STATUS_BALANCE_TEXT))
                .toList();

        int left = Math.round(getPositionX());
        int right = left + Math.round(getSizeWidth()) - 2;
        int minX = left;
        int y = Math.round(getPositionY()) + 1;
        GuiGraphics graphics = guiContext.graphics;

        if (lines.isEmpty()) {
            Component text = snapshot.loaded()
                    ? Component.translatable("screen.incore.player_status.currencies_none")
                    : Component.translatable("screen.incore.player_status.currencies_loading");
            int textX = Math.max(minX, right - font.width(text));
            graphics.drawString(font, text, textX, y + 1, UIScreenTheme.Info.STATUS_LINE_TEXT, false);
            return;
        }

        int cursorRight = right;
        int rendered = 0;
        for (int index = lines.size() - 1; index >= 0; index--) {
            CostRenderLine line = lines.get(index);
            int width = scaledCostLineWidth(font, line);
            int x = cursorRight - width;
            if (x < minX) {
                break;
            }
            renderCostLine(graphics, font, x, y, line);
            cursorRight = x - 4;
            rendered++;
        }

        Component label = Component.translatable("screen.incore.player_status.section_currencies");
        int labelWidth = font.width(label);
        if (cursorRight - labelWidth >= minX) {
            graphics.drawString(font, label, cursorRight - labelWidth, y + 2, UIScreenTheme.Info.PRIMARY_TEXT, false);
            cursorRight -= labelWidth + 4;
        }

        if (rendered < lines.size()) {
            Component overflow = Component.translatable("screen.incore.player_status.currencies_overflow", lines.size() - rendered);
            int overflowWidth = font.width(overflow);
            int overflowX = Math.max(minX, cursorRight - overflowWidth);
            graphics.drawString(font, overflow, overflowX, y + 4, UIScreenTheme.Info.STATUS_OVERFLOW_TEXT, false);
        }
    }

    private static void renderCostLine(GuiGraphics guiGraphics, Font font, int x, int y, CostRenderLine line) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0.0F);
        guiGraphics.pose().scale(SCALE, SCALE, 1.0F);

        int textX = 0;
        if (!line.stack().isEmpty()) {
            guiGraphics.renderItem(line.stack(), 0, 0);
            textX = 20;
        }

        guiGraphics.drawString(font, Component.literal(line.text()), textX, 4, line.color(), false);
        guiGraphics.pose().popPose();
    }

    private static int scaledCostLineWidth(Font font, CostRenderLine line) {
        int width = font.width(line.text());
        if (!line.stack().isEmpty()) {
            width += 20;
        }
        return (int) Math.ceil(width * SCALE);
    }

    private static ItemStack iconFromId(String itemIdString) {
        if (itemIdString == null || itemIdString.isBlank()) {
            return ItemStack.EMPTY;
        }

        ResourceLocation itemId = ResourceLocation.tryParse(itemIdString);
        if (itemId == null) {
            return ItemStack.EMPTY;
        }

        Item item = BuiltInRegistries.ITEM.get(itemId);
        return item == Items.AIR ? ItemStack.EMPTY : item.getDefaultInstance();
    }

    private record CostRenderLine(ItemStack stack, String text, int color) {
    }
}
