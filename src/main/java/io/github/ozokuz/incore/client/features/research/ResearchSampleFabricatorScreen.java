package io.github.ozokuz.incore.client.features.research;

import io.github.ozokuz.incore.client.features.machines.ResearchScreenRenderer;
import io.github.ozokuz.incore.features.research.discovery.ResearchSampleFabricatorMenu;
import io.github.ozokuz.incore.features.research.network.ResearchNetworking;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public class ResearchSampleFabricatorScreen extends AbstractContainerScreen<ResearchSampleFabricatorMenu> {
    private static final int WINDOW_WIDTH = 196;
    private static final int WINDOW_HEIGHT = 190;
    private static final int LIST_X = 34;
    private static final int LIST_Y = 40;
    private static final int LIST_W = 94;
    private static final int LIST_H = 60;
    private static final int ROW_H = 12;
    private static final int SCROLL_X = LIST_X + LIST_W + 2;
    private static final int SEARCH_X = LIST_X;
    private static final int SEARCH_Y = 20;
    private static final int SEARCH_W = LIST_W;
    private static final int SEARCH_H = 12;
    private static final int PROGRESS_X = 34;
    private static final int PROGRESS_Y = 104;
    private static final int PROGRESS_W = 128;
    private static final int ACCENT_COLOR = 0xFF7A9FD8;
    private static final int SLOT_OUTER = 0xFF2D2621;
    private static final int SLOT_INNER = 0xFF1F1915;
    private static final int SLOT_HIGHLIGHT = 0xFFBDA17E;
    private static final int PLAYER_SLOT_OUTER = 0xFF3A312B;
    private static final int PLAYER_SLOT_INNER = 0xFF1B1714;
    private static final int PLAYER_SLOT_HIGHLIGHT = 0xFF7F6A5C;
    private static final int ROW_FILL = 0x18212B38;
    private static final int ROW_FILL_HOVER = 0x22314255;
    private static final int ROW_FILL_SELECTED = 0x44314962;
    private static final int ROW_BORDER = 0x55324153;
    private static final int ROW_BORDER_HOVER = 0x66648DB6;
    private static final int ROW_BORDER_SELECTED = 0xAA7A9FD8;

    private int scroll;
    private @Nullable String selectedNodeId;
    private @Nullable EditBox searchBox;
    private ResearchActionButton fabricateButton;
    private ResearchActionButton scrollUpButton;
    private ResearchActionButton scrollDownButton;

    public ResearchSampleFabricatorScreen(ResearchSampleFabricatorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = WINDOW_WIDTH;
        this.imageHeight = WINDOW_HEIGHT;
        this.inventoryLabelY = 96;
    }

    @Override
    protected void init() {
        super.init();
        ResearchNetworking.requestSnapshot();

        searchBox = addRenderableWidget(new EditBox(font, leftPos + SEARCH_X + 3, topPos + SEARCH_Y + 2, SEARCH_W - 6, SEARCH_H - 4, Component.translatable("screen.incore.research_sample_fabricator.search")));
        searchBox.setBordered(false);
        searchBox.setMaxLength(64);
        searchBox.setTextColor(ResearchScreenRenderer.primaryText());
        searchBox.setResponder(value -> {
            scroll = 0;
            refreshVisibleRows();
        });

        fabricateButton = addRenderableWidget(new ResearchActionButton(
                leftPos + 140,
                topPos + 66,
                50,
                20,
                Component.translatable("screen.incore.research_sample_fabricator.fabricate"),
                ACCENT_COLOR,
                button -> {
                    if (selectedNodeId == null) {
                        return;
                    }
                    ResourceLocation nodeId = ResourceLocation.tryParse(selectedNodeId);
                    if (nodeId != null) {
                        ResearchNetworking.fabricateResearchSample(menu.blockPos(), nodeId);
                    }
                }
        ));

        scrollUpButton = addRenderableWidget(new ResearchActionButton(
                leftPos + SCROLL_X,
                topPos + LIST_Y,
                10,
                10,
                Component.literal("^"),
                ACCENT_COLOR,
                button -> scrollBy(-1)
        ));
        scrollDownButton = addRenderableWidget(new ResearchActionButton(
                leftPos + SCROLL_X,
                topPos + LIST_Y + LIST_H - 10,
                10,
                10,
                Component.literal("v"),
                ACCENT_COLOR,
                button -> scrollBy(1)
        ));

        refreshVisibleRows();
    }

    public void updateFromCache() {
        refreshVisibleRows();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        refreshVisibleRows();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (searchBox != null && searchBox.mouseClicked(mouseX, mouseY, button)) {
            setFocused(searchBox);
            return true;
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && !menu.isProcessing()) {
            int row = rowAt(mouseX, mouseY);
            if (row >= 0) {
                selectRow(row);
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchBox != null && searchBox.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                searchBox.setFocused(false);
                setFocused(null);
                return true;
            }
            if (searchBox.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (searchBox != null && searchBox.isFocused() && searchBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (filteredNodes().size() <= rowsVisible()) {
            return false;
        }
        scrollBy(-(int) Math.signum(scrollY));
        return true;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        ResearchScreenRenderer.drawAccentedWindow(guiGraphics, x, y, imageWidth, imageHeight, ACCENT_COLOR);
        ResearchScreenRenderer.drawAccentedPanel(guiGraphics, x + SEARCH_X, y + SEARCH_Y, SEARCH_W, SEARCH_H, ACCENT_COLOR);
        ResearchScreenRenderer.drawAccentedPanel(guiGraphics, x + LIST_X, y + LIST_Y, LIST_W, LIST_H, ACCENT_COLOR);
        ResearchScreenRenderer.drawSlotFrame(guiGraphics, x + inputSlot().x, y + inputSlot().y, SLOT_OUTER, SLOT_INNER, SLOT_HIGHLIGHT);
        ResearchScreenRenderer.drawSlotFrame(guiGraphics, x + outputSlot().x, y + outputSlot().y, SLOT_OUTER, SLOT_INNER, SLOT_HIGHLIGHT);
        drawPlayerInventorySlots(guiGraphics, x, y);
        ResearchScreenRenderer.drawProgressBar(guiGraphics, x + PROGRESS_X, y + PROGRESS_Y, PROGRESS_W, 6, progressRatio(), ResearchScreenRenderer.theme().progress().fill());

        List<ResearchClientCache.NodeEntry> nodes = filteredNodes();
        if (nodes.isEmpty()) {
            guiGraphics.drawString(font, Component.translatable("screen.incore.research_sample_fabricator.empty"), x + LIST_X + 6, y + LIST_Y + 24, ResearchScreenRenderer.mutedText(), false);
        } else {
            for (int row = 0; row < rowsVisible(); row++) {
                int index = scroll + row;
                if (index >= nodes.size()) {
                    break;
                }

                ResearchClientCache.NodeEntry node = nodes.get(index);
                int rowX = x + LIST_X + 2;
                int rowY = y + LIST_Y + (row * ROW_H) + 1;
                int rowWidth = LIST_W - 4;
                int rowHeight = ROW_H - 2;
                boolean selected = node.id().equals(selectedNodeId);
                boolean hovered = rowAt(mouseX, mouseY) == row && !menu.isProcessing();
                int fill = selected ? ROW_FILL_SELECTED : (hovered ? ROW_FILL_HOVER : ROW_FILL);
                int border = selected ? ROW_BORDER_SELECTED : (hovered ? ROW_BORDER_HOVER : ROW_BORDER);

                ResearchScreenRenderer.drawRowFrame(guiGraphics, rowX, rowY, rowWidth, rowHeight, fill, border);
                guiGraphics.drawString(
                        font,
                        Component.literal(trim(node.name(), 13)),
                        rowX + 4,
                        rowY + 2,
                        selected ? ResearchScreenRenderer.primaryText() : ResearchScreenRenderer.secondaryText(),
                        false
                );
            }
            drawScrollbar(guiGraphics, nodes.size());
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, 8, 8, ResearchScreenRenderer.titleText(), false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.research_sample_fabricator.search"), SEARCH_X, 11, ResearchScreenRenderer.secondaryText(), false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.research_sample_fabricator.list"), LIST_X, 31, ResearchScreenRenderer.secondaryText(), false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.research_sample_fabricator.input"), 7, 25, ResearchScreenRenderer.secondaryText(), false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.research_sample_fabricator.output"), 135, 25, ResearchScreenRenderer.secondaryText(), false);
        guiGraphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, ResearchScreenRenderer.secondaryText(), false);

        List<ResearchClientCache.NodeEntry> nodes = filteredNodes();
        guiGraphics.drawString(font, Component.translatable("screen.incore.research_sample_fabricator.count", nodes.size()), 140, 31, ResearchScreenRenderer.mutedText(), false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.research_sample_fabricator.progress", menu.progressTicks(), menu.maxProgressTicks()), PROGRESS_X, 94, ResearchScreenRenderer.accentText(), false);
        String statusKey = menu.getSlot(1).hasItem()
                ? "screen.incore.research_sample_fabricator.status.ready"
                : (menu.isProcessing() ? "screen.incore.research_sample_fabricator.status.processing" : "screen.incore.research_sample_fabricator.status.idle");
        guiGraphics.drawString(font, Component.translatable(statusKey), PROGRESS_X, 112, ResearchScreenRenderer.secondaryText(), false);
        if (selectedNodeId != null) {
            ResearchClientCache.NodeEntry selected = ResearchClientCache.snapshot().nodeById().get(selectedNodeId);
            if (selected != null) {
                guiGraphics.drawString(font, trim(selected.name(), 18), 140, 94, ResearchScreenRenderer.primaryText(), false);
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        ResearchScreenRenderer.drawBackdrop(guiGraphics, width, height);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (searchBox != null && searchBox.isHoveredOrFocused() && !searchBox.getValue().isBlank()) {
            guiGraphics.renderTooltip(font, Component.literal(searchBox.getValue()), mouseX, mouseY);
        }
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private void drawScrollbar(GuiGraphics guiGraphics, int totalRows) {
        int visibleRows = rowsVisible();
        int trackTop = topPos + LIST_Y + 12;
        int trackHeight = LIST_H - 24;
        if (totalRows <= visibleRows || trackHeight <= 0) {
            return;
        }

        int maxScroll = Math.max(1, totalRows - visibleRows);
        float position = scroll / (float) maxScroll;
        float visibleRatio = visibleRows / (float) totalRows;
        ResearchScreenRenderer.drawScrollbar(guiGraphics, leftPos + SCROLL_X, trackTop, 10, trackHeight, position, visibleRatio, ACCENT_COLOR, 0xFFB7D6F6);
    }

    private void scrollBy(int delta) {
        int maxScroll = Math.max(0, filteredNodes().size() - rowsVisible());
        scroll = Math.clamp(scroll + delta, 0, maxScroll);
        refreshVisibleRows();
    }

    private void updateButtonState() {
        if (fabricateButton != null) {
            fabricateButton.active = selectedNodeId != null
                    && menu.getSlot(0).hasItem()
                    && !menu.getSlot(1).hasItem()
                    && !menu.isProcessing();
        }
        int maxScroll = Math.max(0, filteredNodes().size() - rowsVisible());
        if (scrollUpButton != null) {
            scrollUpButton.active = scroll > 0;
        }
        if (scrollDownButton != null) {
            scrollDownButton.active = scroll < maxScroll;
        }
    }

    private void refreshVisibleRows() {
        List<ResearchClientCache.NodeEntry> nodes = filteredNodes();
        int maxScroll = Math.max(0, nodes.size() - rowsVisible());
        scroll = Math.clamp(scroll, 0, maxScroll);
        if (selectedNodeId == null && !nodes.isEmpty()) {
            selectedNodeId = nodes.get(0).id();
        } else if (selectedNodeId != null && nodes.stream().noneMatch(node -> node.id().equals(selectedNodeId))) {
            selectedNodeId = nodes.isEmpty() ? null : nodes.get(0).id();
        }
        if (searchBox != null) {
            searchBox.setEditable(!menu.isProcessing());
        }
        updateButtonState();
    }

    private void selectRow(int row) {
        List<ResearchClientCache.NodeEntry> nodes = filteredNodes();
        int index = scroll + row;
        if (index < 0 || index >= nodes.size()) {
            return;
        }
        selectedNodeId = nodes.get(index).id();
        refreshVisibleRows();
    }

    private int rowAt(double mouseX, double mouseY) {
        int rowX = leftPos + LIST_X + 2;
        int rowWidth = LIST_W - 4;
        if (mouseX < rowX || mouseX >= rowX + rowWidth) {
            return -1;
        }
        for (int row = 0; row < rowsVisible(); row++) {
            int rowY = topPos + LIST_Y + (row * ROW_H) + 1;
            int rowHeight = ROW_H - 2;
            if (mouseY >= rowY && mouseY < rowY + rowHeight) {
                int index = scroll + row;
                if (index < filteredNodes().size()) {
                    return row;
                }
                return -1;
            }
        }
        return -1;
    }

    private float progressRatio() {
        int innerWidth = Math.max(1, PROGRESS_W - 2);
        return menu.progressScaled(innerWidth) / (float) innerWidth;
    }

    private Slot inputSlot() {
        return menu.getSlot(0);
    }

    private Slot outputSlot() {
        return menu.getSlot(1);
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

    private List<ResearchClientCache.NodeEntry> filteredNodes() {
        ResearchClientCache.Snapshot snapshot = ResearchClientCache.snapshot();
        String query = searchBox == null ? "" : searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        return snapshot.nodes().stream()
                .filter(node -> snapshot.completedNodeIds().contains(node.id()))
                .filter(node -> matchesQuery(snapshot, node, query))
                .sorted(Comparator.comparing(ResearchClientCache.NodeEntry::name).thenComparing(ResearchClientCache.NodeEntry::id))
                .toList();
    }

    private boolean matchesQuery(ResearchClientCache.Snapshot snapshot, ResearchClientCache.NodeEntry node, String query) {
        if (query.isBlank()) {
            return true;
        }
        String categoryName = "";
        ResearchClientCache.CategoryEntry category = snapshot.categoriesById().get(node.categoryId());
        if (category != null) {
            categoryName = category.name();
        }
        return node.name().toLowerCase(Locale.ROOT).contains(query)
                || node.id().toLowerCase(Locale.ROOT).contains(query)
                || node.treeId().toLowerCase(Locale.ROOT).contains(query)
                || node.categoryId().toLowerCase(Locale.ROOT).contains(query)
                || categoryName.toLowerCase(Locale.ROOT).contains(query);
    }

    private int rowsVisible() {
        return Math.max(1, LIST_H / ROW_H);
    }

    private String trim(String text, int maxLen) {
        if (text == null || text.length() <= maxLen) {
            return text == null ? "" : text;
        }
        return text.substring(0, Math.max(0, maxLen - 3)) + "...";
    }
}
