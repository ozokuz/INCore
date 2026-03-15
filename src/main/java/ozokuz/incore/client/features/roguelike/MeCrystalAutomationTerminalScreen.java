package ozokuz.incore.client.features.roguelike;

import ozokuz.incore.client.ui.UIScreenTheme;
import ozokuz.incore.client.ui.render.ThemedUi;
import ozokuz.incore.features.roguelike.content.DungeonAltarAutomatorBlockEntity;
import ozokuz.incore.features.roguelike.content.MeCrystalAutomationTerminalMenu;
import ozokuz.incore.features.roguelike.content.MeCrystalAutomationTerminalPart;
import ozokuz.incore.features.roguelike.network.RoguelikeNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class MeCrystalAutomationTerminalScreen extends AbstractContainerScreen<MeCrystalAutomationTerminalMenu> {
    private static final UIScreenTheme THEME = UIScreenTheme.CRAFTING;

    public MeCrystalAutomationTerminalScreen(MeCrystalAutomationTerminalMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 268;
        this.imageHeight = 198;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("screen.incore.me_crystal_automation_terminal.request"), button -> {
            RoguelikeNetworking.requestMeCrystalAutomationTerminalAction(menu.hostPos(), menu.side(), false);
        }).bounds(leftPos + 14, topPos + 170, 138, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.incore.me_crystal_automation_terminal.refresh"), button -> {
            RoguelikeNetworking.requestMeCrystalAutomationTerminalAction(menu.hostPos(), menu.side(), true);
        }).bounds(leftPos + 158, topPos + 170, 96, 20).build());
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        ThemedUi ui = themed(guiGraphics);
        ui.drawWindow(x, y, imageWidth, imageHeight);
        ui.drawPanel(x + 8, y + 24, imageWidth - 16, 138);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, 12, 8, UIScreenTheme.Crafting.TITLE_TEXT, false);
        MeCrystalAutomationTerminalPart terminal = menu.part();

        guiGraphics.drawString(font, Component.translatable("screen.incore.me_crystal_automation_terminal.status"), 12, 28, UIScreenTheme.Crafting.BODY_TEXT, false);
        guiGraphics.drawString(font, MeCrystalAutomationTerminalPart.statusText(terminal.statusForDisplay()), 62, 28, statusColor(terminal.statusForDisplay()), false);

        guiGraphics.drawString(font, Component.translatable("screen.incore.me_crystal_automation_terminal.binding"), 12, 42, UIScreenTheme.Crafting.BODY_TEXT, false);
        guiGraphics.drawString(font, formatPos(terminal.boundAutomatorPos(), "screen.incore.me_crystal_automation_terminal.binding.none"), 62, 42, UIScreenTheme.Crafting.MUTED_TEXT, false);

        guiGraphics.drawString(font, Component.translatable("screen.incore.me_crystal_automation_terminal.altar"), 12, 56, UIScreenTheme.Crafting.BODY_TEXT, false);
        guiGraphics.drawString(font, formatPos(terminal.boundAltarPos(), "screen.incore.me_crystal_automation_terminal.binding.none"), 62, 56, UIScreenTheme.Crafting.MUTED_TEXT, false);

        guiGraphics.drawString(font, Component.translatable("screen.incore.me_crystal_automation_terminal.requirements"), 12, 74, UIScreenTheme.Crafting.BODY_TEXT, false);
        int y = 88;
        for (DungeonAltarAutomatorBlockEntity.RequestView entry : terminal.requestViews()) {
            renderEntry(guiGraphics, entry, y);
            y += 18;
            if (y > 150) {
                break;
            }
        }
    }

    private void renderEntry(GuiGraphics guiGraphics, DungeonAltarAutomatorBlockEntity.RequestView entry, int y) {
        Item item = BuiltInRegistries.ITEM.get(entry.itemId());
        if (item != Items.AIR) {
            guiGraphics.renderItem(new ItemStack(item), 12, y - 2);
        }
        String name = requestItemName(entry.itemId());
        int nameX = 32;
        int countsX = 128;
        int availableWidth = countsX - nameX - 4;
        if (font.width(name) > availableWidth) {
            String ellipsis = "...";
            int ellipsisWidth = font.width(ellipsis);
            name = font.plainSubstrByWidth(name, Math.max(0, availableWidth - ellipsisWidth)) + ellipsis;
        }
        guiGraphics.drawString(font, Component.literal(name), nameX, y, UIScreenTheme.Crafting.BODY_TEXT, false);
        String counts = entry.submitted() + "/" + entry.required() + "  B:" + entry.buffered() + "  ME:" + entry.meAvailable();
        guiGraphics.drawString(font, Component.literal(counts), countsX, y, UIScreenTheme.Crafting.MUTED_TEXT, false);
        Component tail = entry.requesting()
                ? Component.translatable("screen.incore.me_crystal_automation_terminal.entry.pending")
                : entry.craftable()
                ? Component.translatable("screen.incore.me_crystal_automation_terminal.entry.craftable")
                : Component.translatable("screen.incore.me_crystal_automation_terminal.entry.missing", entry.missing());
        guiGraphics.drawString(font, tail, 32, y + 9, entry.requesting() ? UIScreenTheme.Crafting.ACCENT_TEXT : UIScreenTheme.Crafting.MUTED_TEXT, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        themed(guiGraphics).drawBackdrop(width, height);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private static int statusColor(int status) {
        return switch (status) {
            case MeCrystalAutomationTerminalPart.STATUS_READY -> UIScreenTheme.Crafting.ACCENT_TEXT;
            case MeCrystalAutomationTerminalPart.STATUS_REQUESTING -> UIScreenTheme.Crafting.ACCENT_TEXT;
            default -> UIScreenTheme.Crafting.DANGER_TEXT;
        };
    }

    private static String requestItemName(ResourceLocation itemId) {
        Item item = BuiltInRegistries.ITEM.get(itemId);
        return item == Items.AIR ? itemId.toString() : item.getDescription().getString();
    }

    private static ThemedUi themed(GuiGraphics guiGraphics) {
        return new ThemedUi(guiGraphics, THEME.theme());
    }

    private static Component formatPos(net.minecraft.core.BlockPos pos, String noneKey) {
        return pos == null ? Component.translatable(noneKey) : Component.literal(pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
    }
}
