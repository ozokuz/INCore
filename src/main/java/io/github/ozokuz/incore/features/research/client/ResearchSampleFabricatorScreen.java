package io.github.ozokuz.incore.features.research.client;

import io.github.ozokuz.incore.features.research.discovery.ResearchSampleFabricatorMenu;
import io.github.ozokuz.incore.features.research.network.ResearchNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

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

    private int scroll;
    private @Nullable String selectedNodeId;
    private @Nullable EditBox searchBox;
    private Button fabricateButton;
    private Button scrollUpButton;
    private Button scrollDownButton;
    private final List<Button> rowButtons = new ArrayList<>();

    public ResearchSampleFabricatorScreen(ResearchSampleFabricatorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = WINDOW_WIDTH;
        this.imageHeight = WINDOW_HEIGHT;
        this.inventoryLabelY = 96;
    }

    @Override
    protected void init() {
        super.init();
        rowButtons.clear();
        ResearchNetworking.requestSnapshot();

        searchBox = addRenderableWidget(new EditBox(font, leftPos + SEARCH_X, topPos + SEARCH_Y, SEARCH_W, SEARCH_H, Component.translatable("screen.incore.research_sample_fabricator.search")));
        searchBox.setBordered(false);
        searchBox.setMaxLength(64);
        searchBox.setResponder(value -> {
            scroll = 0;
            refreshVisibleRows();
        });

        fabricateButton = addRenderableWidget(Button.builder(
                Component.translatable("screen.incore.research_sample_fabricator.fabricate"),
                button -> {
                    if (selectedNodeId == null) {
                        return;
                    }
                    ResourceLocation nodeId = ResourceLocation.tryParse(selectedNodeId);
                    if (nodeId != null) {
                        ResearchNetworking.fabricateResearchSample(menu.blockPos(), nodeId);
                    }
                }
        ).bounds(leftPos + 140, topPos + 66, 50, 20).build());

        scrollUpButton = addRenderableWidget(Button.builder(
                Component.literal("^"),
                button -> scrollBy(-1)
        ).bounds(leftPos + SCROLL_X, topPos + LIST_Y, 10, 10).build());
        scrollDownButton = addRenderableWidget(Button.builder(
                Component.literal("v"),
                button -> scrollBy(1)
        ).bounds(leftPos + SCROLL_X, topPos + LIST_Y + LIST_H - 10, 10, 10).build());

        int rowsVisible = rowsVisible();
        for (int row = 0; row < rowsVisible; row++) {
            final int rowIndex = row;
            rowButtons.add(addRenderableWidget(Button.builder(
                    CommonComponents.EMPTY,
                    button -> selectRow(rowIndex)
            ).bounds(leftPos + LIST_X + 2, topPos + LIST_Y + (row * ROW_H) + 1, LIST_W - 4, ROW_H - 2).build()));
        }

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
        guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF141110);
        guiGraphics.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + imageHeight - 1, 0xFF1D1816);
        guiGraphics.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + 3, 0xFF7A9FD8);

        guiGraphics.fill(leftPos + SEARCH_X, topPos + SEARCH_Y, leftPos + SEARCH_X + SEARCH_W, topPos + SEARCH_Y + SEARCH_H, 0xFF10151B);
        guiGraphics.fill(leftPos + SEARCH_X, topPos + SEARCH_Y, leftPos + SEARCH_X + SEARCH_W, topPos + SEARCH_Y + 1, 0xFF7A9FD8);
        guiGraphics.fill(leftPos + LIST_X, topPos + LIST_Y, leftPos + LIST_X + LIST_W, topPos + LIST_Y + LIST_H, 0xFF151A21);
        guiGraphics.fill(leftPos + LIST_X, topPos + LIST_Y, leftPos + LIST_X + LIST_W, topPos + LIST_Y + 1, 0xFF7A9FD8);
        guiGraphics.fill(leftPos + SCROLL_X, topPos + LIST_Y + 12, leftPos + SCROLL_X + 10, topPos + LIST_Y + LIST_H - 12, 0xFF202833);
        guiGraphics.fill(leftPos + 7, topPos + 36, leftPos + 25, topPos + 54, 0xFF2D2621);
        guiGraphics.fill(leftPos + 7, topPos + 36, leftPos + 25, topPos + 37, 0xFFBDA17E);
        guiGraphics.fill(leftPos + 141, topPos + 36, leftPos + 159, topPos + 54, 0xFF2D2621);
        guiGraphics.fill(leftPos + 141, topPos + 36, leftPos + 159, topPos + 37, 0xFFBDA17E);
        guiGraphics.fill(leftPos + PROGRESS_X, topPos + PROGRESS_Y, leftPos + PROGRESS_X + PROGRESS_W, topPos + PROGRESS_Y + 6, 0xFF243143);
        guiGraphics.fill(leftPos + PROGRESS_X + 1, topPos + PROGRESS_Y + 1, leftPos + PROGRESS_X + PROGRESS_W - 1, topPos + PROGRESS_Y + 5, 0xFF101722);

        List<ResearchClientCache.NodeEntry> nodes = filteredNodes();
        if (nodes.isEmpty()) {
            guiGraphics.drawString(font, Component.translatable("screen.incore.research_sample_fabricator.empty"), leftPos + LIST_X + 6, topPos + LIST_Y + 24, 0xFF8D98A7, false);
        } else {
            for (int row = 0; row < rowsVisible(); row++) {
                int index = scroll + row;
                if (index >= nodes.size()) {
                    break;
                }
                ResearchClientCache.NodeEntry node = nodes.get(index);
                int y = topPos + LIST_Y + (row * ROW_H);
                boolean selected = node.id().equals(selectedNodeId);
                guiGraphics.fill(leftPos + LIST_X + 2, y + 1, leftPos + LIST_X + LIST_W - 2, y + ROW_H - 1, selected ? 0x44314962 : 0x182F4255);
            }
            drawScrollThumb(guiGraphics, nodes.size());
        }

        int fill = menu.progressScaled(PROGRESS_W - 2);
        if (fill > 0) {
            guiGraphics.fill(leftPos + PROGRESS_X + 1, topPos + PROGRESS_Y + 1, leftPos + PROGRESS_X + 1 + fill, topPos + PROGRESS_Y + 5, 0xFF55A9E6);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, 8, 8, 0xFFF3E6D3, false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.research_sample_fabricator.search"), SEARCH_X, 11, 0xFFD2BDA2, false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.research_sample_fabricator.list"), LIST_X, 31, 0xFFD2BDA2, false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.research_sample_fabricator.input"), 7, 25, 0xFFD2BDA2, false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.research_sample_fabricator.output"), 135, 25, 0xFFD2BDA2, false);
        guiGraphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0xFFD2BDA2, false);

        List<ResearchClientCache.NodeEntry> nodes = filteredNodes();
        guiGraphics.drawString(font, Component.translatable("screen.incore.research_sample_fabricator.count", nodes.size()), 140, 31, 0xFF9FB5CE, false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.research_sample_fabricator.progress", menu.progressTicks(), menu.maxProgressTicks()), PROGRESS_X, 94, 0xFFCBDBF0, false);
        String statusKey = menu.getSlot(1).hasItem()
                ? "screen.incore.research_sample_fabricator.status.ready"
                : (menu.isProcessing() ? "screen.incore.research_sample_fabricator.status.processing" : "screen.incore.research_sample_fabricator.status.idle");
        guiGraphics.drawString(font, Component.translatable(statusKey), PROGRESS_X, 112, 0xFFB7C8D9, false);
        if (selectedNodeId != null) {
            ResearchClientCache.NodeEntry selected = ResearchClientCache.snapshot().nodeById().get(selectedNodeId);
            if (selected != null) {
                guiGraphics.drawString(font, trim(selected.name(), 18), 140, 94, 0xFFE4ECF5, false);
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (searchBox != null && searchBox.isHoveredOrFocused() && !searchBox.getValue().isBlank()) {
            guiGraphics.renderTooltip(font, Component.literal(searchBox.getValue()), mouseX, mouseY);
        }
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private void drawScrollThumb(GuiGraphics guiGraphics, int totalRows) {
        int visibleRows = rowsVisible();
        int trackTop = topPos + LIST_Y + 12;
        int trackHeight = LIST_H - 24;
        if (totalRows <= visibleRows || trackHeight <= 0) {
            return;
        }
        int thumbHeight = Math.max(10, (trackHeight * visibleRows) / totalRows);
        int maxScroll = Math.max(1, totalRows - visibleRows);
        int thumbTravel = Math.max(0, trackHeight - thumbHeight);
        int thumbOffset = (thumbTravel * scroll) / maxScroll;
        guiGraphics.fill(leftPos + SCROLL_X + 1, trackTop + thumbOffset, leftPos + SCROLL_X + 9, trackTop + thumbOffset + thumbHeight, 0xFF7A9FD8);
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

        for (int row = 0; row < rowButtons.size(); row++) {
            Button button = rowButtons.get(row);
            int index = scroll + row;
            if (index >= nodes.size()) {
                button.visible = false;
                button.active = false;
                button.setMessage(CommonComponents.EMPTY);
                continue;
            }

            ResearchClientCache.NodeEntry node = nodes.get(index);
            button.visible = true;
            button.active = !menu.isProcessing();
            button.setMessage(Component.literal((node.id().equals(selectedNodeId) ? "> " : "  ") + trim(node.name(), 13)));
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
