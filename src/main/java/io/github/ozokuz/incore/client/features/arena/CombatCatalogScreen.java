package io.github.ozokuz.incore.client.features.arena;

import com.google.gson.Gson;
import io.github.ozokuz.incore.client.ui.UIScreenTheme;
import io.github.ozokuz.incore.client.ui.render.ThemedUi;
import io.github.ozokuz.incore.features.arena.ArenaService;
import io.github.ozokuz.incore.features.arena.network.ArenaNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class CombatCatalogScreen extends Screen {
    private static final Gson GSON = new Gson();
    private static final UIScreenTheme THEME = UIScreenTheme.OTHER_CONTENT;
    private static final int ROW_HEIGHT = 30;

    private ArenaService.ScreenData data;
    private String selectedCategoryId;
    private String selectedEntryId;

    public CombatCatalogScreen(String json) {
        super(Component.translatable("screen.incore.arena_catalog.title"));
        this.data = parse(json);
    }

    public void updatePayload(String json) {
        this.data = parse(json);
        if (this.minecraft != null) {
            rebuildWidgets();
        }
    }

    @Override
    protected void init() {
        rebuildWidgets();
    }

    @Override
    protected void rebuildWidgets() {
        clearWidgets();

        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> this.onClose())
                .bounds(this.width - 90, this.height - 26, 80, 20)
                .build());

        if (data == null || data.entries() == null || data.entries().isEmpty()) {
            return;
        }

        if (selectedCategoryId == null || data.categories().stream().noneMatch(c -> c.id().equals(selectedCategoryId))) {
            selectedCategoryId = data.categories().isEmpty() ? null : data.categories().getFirst().id();
        }

        List<ArenaService.ScreenEntry> entries = entriesForSelectedCategory();
        if (selectedEntryId == null || entries.stream().noneMatch(e -> e.id().equals(selectedEntryId))) {
            selectedEntryId = entries.isEmpty() ? null : entries.getFirst().id();
        }

        int categoryLeft = 16;
        int categoryTop = 36;
        int categoryWidth = 170;

        for (int i = 0; i < data.categories().size(); i++) {
            ArenaService.CategoryView category = data.categories().get(i);
            int y = categoryTop + i * ROW_HEIGHT;
            this.addRenderableWidget(Button.builder(Component.empty(), b -> {
                        selectedCategoryId = category.id();
                        selectedEntryId = null;
                        rebuildWidgets();
                    }).bounds(categoryLeft, y, categoryWidth, ROW_HEIGHT - 2)
                    .build());
        }

        int entryLeft = categoryLeft + categoryWidth + 12;
        int entryTop = 36;
        int entryWidth = 220;

        for (int i = 0; i < entries.size(); i++) {
            ArenaService.ScreenEntry entry = entries.get(i);
            int y = entryTop + i * ROW_HEIGHT;
            if (y + ROW_HEIGHT > this.height - 64) {
                break;
            }

            this.addRenderableWidget(Button.builder(Component.empty(), b -> {
                        selectedEntryId = entry.id();
                        rebuildWidgets();
                    }).bounds(entryLeft, y, entryWidth, ROW_HEIGHT - 2)
                    .build());
        }

        Button startButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("screen.incore.arena_catalog.start"),
                        b -> {
                            if (selectedEntryId == null) {
                                return;
                            }

                            ResourceLocation selected = ResourceLocation.tryParse(selectedEntryId);
                            if (selected == null) {
                                return;
                            }

                            ArenaNetworking.requestStartRun(selected);
                            this.onClose();
                        }
                ).bounds(entryLeft, this.height - 52, 220, 20)
                .build());

        startButton.active = selectedEntryId != null;
    }

    private List<ArenaService.ScreenEntry> entriesForSelectedCategory() {
        if (data == null || data.entries() == null || selectedCategoryId == null) {
            return List.of();
        }

        List<ArenaService.ScreenEntry> result = new ArrayList<>();
        for (ArenaService.ScreenEntry entry : data.entries()) {
            if (selectedCategoryId.equals(entry.categoryId())) {
                result.add(entry);
            }
        }

        return result;
    }

    private ArenaService.ScreenEntry selectedEntry() {
        if (selectedEntryId == null || data == null || data.entries() == null) {
            return null;
        }

        for (ArenaService.ScreenEntry entry : data.entries()) {
            if (selectedEntryId.equals(entry.id())) {
                return entry;
            }
        }

        return null;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        themed(guiGraphics).drawBackdrop(this.width, this.height);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int categoryLeft = 16;
        int categoryTop = 36;
        int categoryWidth = 170;
        int footerY = this.height - 44;
        int listBottom = footerY - 4;

        int entryLeft = categoryLeft + categoryWidth + 12;
        int entryWidth = 220;

        int detailsLeft = entryLeft + entryWidth + 12;
        int detailsRight = this.width - 12;

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 14, THEME.theme().text().primary());
        guiGraphics.fill(categoryLeft, categoryTop, categoryLeft + categoryWidth, listBottom, 0x991A1A1A);
        guiGraphics.fill(entryLeft, categoryTop, entryLeft + entryWidth, listBottom, 0x991A1A1A);
        guiGraphics.fill(detailsLeft, categoryTop, detailsRight, listBottom, 0x99202020);

        guiGraphics.drawString(this.font, Component.translatable("screen.incore.arena_catalog.categories"), categoryLeft + 6, categoryTop - 10, THEME.theme().text().secondary(), false);
        guiGraphics.drawString(this.font, Component.translatable("screen.incore.arena_catalog.difficulties"), entryLeft + 6, categoryTop - 10, THEME.theme().text().secondary(), false);

        for (int i = 0; i < data.categories().size(); i++) {
            ArenaService.CategoryView category = data.categories().get(i);
            int y = categoryTop + i * ROW_HEIGHT;
            if (y + ROW_HEIGHT > listBottom) {
                break;
            }

            boolean selected = category.id().equals(selectedCategoryId);
            int border = selected ? 0xFF89C9FF : 0xFF3D4558;
            int fill = selected ? 0xBB2A2A2A : 0x99232323;
            guiGraphics.fill(categoryLeft + 1, y + 1, categoryLeft + categoryWidth - 1, y + ROW_HEIGHT - 3, fill);
            themed(guiGraphics).drawBorder(categoryLeft, y, categoryLeft + categoryWidth, y + ROW_HEIGHT - 2, border);
            guiGraphics.drawString(this.font, Component.literal(category.name()), categoryLeft + 8, y + 10, selected ? 0xFFFFFF : 0xE8E8E8, false);
        }

        List<ArenaService.ScreenEntry> entries = entriesForSelectedCategory();
        for (int i = 0; i < entries.size(); i++) {
            ArenaService.ScreenEntry entry = entries.get(i);
            int y = categoryTop + i * ROW_HEIGHT;
            if (y + ROW_HEIGHT > listBottom) {
                break;
            }

            boolean selected = entry.id().equals(selectedEntryId);
            int border = selected ? 0xFF89C9FF : 0xFF3D4558;
            int fill = selected ? 0xBB2A2A2A : 0x99232323;
            guiGraphics.fill(entryLeft + 1, y + 1, entryLeft + entryWidth - 1, y + ROW_HEIGHT - 3, fill);
            themed(guiGraphics).drawBorder(entryLeft, y, entryLeft + entryWidth, y + ROW_HEIGHT - 2, border);
            guiGraphics.drawString(this.font, Component.literal(entry.difficultyName()), entryLeft + 8, y + 10, selected ? 0xFFFFFF : 0xE8E8E8, false);
        }

        ArenaService.ScreenEntry selected = selectedEntry();
        if (selected == null) {
            guiGraphics.drawString(this.font, Component.translatable("screen.incore.arena_catalog.none"), detailsLeft + 8, categoryTop + 24, THEME.theme().text().muted(), false);
            return;
        }

        int detailsX = detailsLeft + 8;
        int y = categoryTop + 8;
        guiGraphics.drawString(this.font, Component.translatable("screen.incore.arena_catalog.details"), detailsX, y, 0xF0F0F0, false);
        y += 16;
        guiGraphics.drawString(this.font, Component.literal(selected.categoryName()), detailsX, y, 0xD9D9D9, false);
        y += 12;
        guiGraphics.drawString(this.font, Component.literal(selected.difficultyName()), detailsX, y, 0xD9D9D9, false);
        y += 12;
        guiGraphics.drawString(this.font, Component.translatable("screen.incore.arena_catalog.gateway", selected.gatewayId()), detailsX, y, 0xD9D9D9, false);
        y += 12;
        guiGraphics.drawString(this.font, Component.translatable("screen.incore.arena_catalog.sanity", selected.rewardSanityCost()), detailsX, y, 0xFFE6CC, false);
        y += 16;
        guiGraphics.drawString(this.font, Component.translatable("screen.incore.arena_catalog.rewards"), detailsX, y, 0xF0F0F0, false);
        y += 10;

        ItemStack hoveredStack = renderRewardIcons(guiGraphics, selected, detailsX, y, mouseX, mouseY);
        if (hoveredStack != null) {
            guiGraphics.renderTooltip(this.font, hoveredStack, mouseX, mouseY);
        }

        if (!selected.rewardSummary().isEmpty()) {
            int rows = Math.max(1, (selected.rewardItems().size() + 1) / 2);
            int summaryY = y + (rows * 22) + 6;
            guiGraphics.drawString(this.font, Component.literal(selected.rewardSummary()), detailsX, summaryY, 0xE8E8E8, false);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static ArenaService.ScreenData parse(String json) {
        ArenaService.ScreenData parsed = GSON.fromJson(json, ArenaService.ScreenData.class);
        if (parsed == null || parsed.entries() == null) {
            return new ArenaService.ScreenData(List.of(), List.of());
        }

        List<ArenaService.CategoryView> categories = parsed.categories() == null ? List.of() : parsed.categories();
        return new ArenaService.ScreenData(categories, parsed.entries());
    }

    private ItemStack renderRewardIcons(GuiGraphics guiGraphics, ArenaService.ScreenEntry selected, int startX, int startY, int mouseX, int mouseY) {
        final int columns = 2;
        final int iconSize = 16;
        final int cellWidth = 80;
        final int rowHeight = 22;

        ItemStack hovered = null;
        for (int i = 0; i < selected.rewardItems().size(); i++) {
            ArenaService.RewardView reward = selected.rewardItems().get(i);
            ResourceLocation itemId = ResourceLocation.tryParse(reward.itemId());
            if (itemId == null) {
                continue;
            }

            Item item = BuiltInRegistries.ITEM.get(itemId);
            if (item == null || item == net.minecraft.world.item.Items.AIR) {
                continue;
            }

            int col = i % columns;
            int row = i / columns;
            int x = startX + (col * cellWidth);
            int y = startY + (row * rowHeight);

            ItemStack displayStack = new ItemStack(item, reward.count());
            guiGraphics.renderItem(displayStack, x, y);
            guiGraphics.drawString(this.font, Component.literal("x" + reward.count()), x + iconSize + 4, y + 4, 0xD9D9D9, false);

            if (mouseX >= x && mouseX <= x + iconSize && mouseY >= y && mouseY <= y + iconSize) {
                hovered = displayStack;
            }
        }

        return hovered;
    }

    private ThemedUi themed(GuiGraphics guiGraphics) {
        return new ThemedUi(guiGraphics, this.font, THEME.theme());
    }
}
