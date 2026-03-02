package io.github.ozokuz.incore.features.researchv2.client;

import io.github.ozokuz.incore.features.researchv2.network.ResearchV2Networking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ResearchV2TreeScreen extends Screen {
    private static final int WINDOW_MARGIN = 18;
    private static final int PANEL_GAP = 8;
    private static final int NODE_WIDTH = 100;
    private static final int NODE_HEIGHT = 42;
    private static final int NODE_X_STEP = 132;
    private static final int NODE_Y_STEP = 72;
    private static final int GRAPH_PADDING = 16;
    private static final int LIST_ROW_HEIGHT = 18;
    private static final int QUEUE_CARD_W = 30;
    private static final int QUEUE_CARD_H = 34;
    private static final int QUEUE_CARD_GAP = 2;

    private ResearchV2ClientCache.Snapshot snapshot = ResearchV2ClientCache.snapshot();
    private String searchQuery = "";
    private FilterMode activeFilter = FilterMode.ALL;
    private String selectedNodeId = "";
    private int listScroll;
    private int graphPanX;
    private int graphPanY;
    private boolean draggingGraph;
    private Integer previousMenuBlur;

    private @Nullable CycleButton<String> treeSelector;
    private @Nullable EditBox searchBox;
    private @Nullable Button queueResearchButton;
    private final Map<FilterMode, Button> filterButtons = new EnumMap<>(FilterMode.class);
    private final Map<String, NodeBounds> queueCardBounds = new LinkedHashMap<>();
    private final Map<String, NodeBounds> queueRemoveBounds = new LinkedHashMap<>();
    private final Map<String, NodeBounds> graphNodeBounds = new LinkedHashMap<>();
    private final Map<String, NodeBounds> listNodeBounds = new LinkedHashMap<>();

    public ResearchV2TreeScreen() {
        super(Component.translatable("screen.incore.research_v2.title"));
    }

    @Override
    protected void init() {
        if (this.previousMenuBlur == null && this.minecraft != null) {
            this.previousMenuBlur = this.minecraft.options.getMenuBackgroundBlurriness();
            if (this.previousMenuBlur > 0) {
                this.minecraft.options.menuBackgroundBlurriness().set(0);
            }
        }

        updateFromCache();
        ResearchV2Networking.requestSnapshot();
    }

    @Override
    public void removed() {
        if (this.minecraft != null && this.previousMenuBlur != null) {
            this.minecraft.options.menuBackgroundBlurriness().set(this.previousMenuBlur);
        }
        this.previousMenuBlur = null;
        super.removed();
    }

    public void updateFromCache() {
        this.snapshot = ResearchV2ClientCache.snapshot();
        ensureSelectedTree();
        ensureSelection();
        rebuildGraphLayout();
        clampGraphPan();
        rebuildScreenWidgets();
    }

    @Override
    public void tick() {
        super.tick();
        updateQueueButtonState();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        Layout layout = layout();
        buildQueueCardLayout(layout);
        if (button == 0) {
            for (var entry : queueRemoveBounds.entrySet()) {
                if (!entry.getValue().contains(mouseX, mouseY)) {
                    continue;
                }
                ResourceLocation nodeId = ResourceLocation.tryParse(entry.getKey());
                if (nodeId != null) {
                    ResearchV2Networking.cancelQueueItem(nodeId);
                }
                return true;
            }

            for (var entry : queueCardBounds.entrySet()) {
                if (!entry.getValue().contains(mouseX, mouseY)) {
                    continue;
                }
                selectedNodeId = entry.getKey();
                updateQueueButtonState();
                return true;
            }
        }

        if (button == 0 && inGraphViewport(mouseX, mouseY, layout)) {
            for (var entry : graphNodeBounds.entrySet()) {
                NodeBounds shifted = shifted(entry.getValue(), graphPanX, graphPanY);
                if (shifted.contains(mouseX, mouseY)) {
                    selectedNodeId = entry.getKey();
                    updateQueueButtonState();
                    return true;
                }
            }
            draggingGraph = true;
            return true;
        }

        if (button == 0 && inListRows(mouseX, mouseY, layout)) {
            for (var entry : listNodeBounds.entrySet()) {
                if (entry.getValue().contains(mouseX, mouseY)) {
                    selectedNodeId = entry.getKey();
                    updateQueueButtonState();
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingGraph = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingGraph && button == 0) {
            graphPanX += (int) Math.round(dragX);
            graphPanY += (int) Math.round(dragY);
            clampGraphPan();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        Layout layout = layout();
        if (inListRows(mouseX, mouseY, layout)) {
            int direction = (int) Math.signum(scrollY);
            listScroll = Math.max(0, listScroll - direction);
            clampListScroll(layout);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        Layout layout = layout();

        drawPanel(guiGraphics, layout.windowX(), layout.windowY(), layout.windowWidth(), layout.windowHeight(), 0xCA101722, 0xFF5F7086);
        drawPanel(guiGraphics, layout.topLeftX(), layout.topLeftY(), layout.topLeftWidth(), layout.topLeftHeight(), 0xB3192230, 0x805A6B80);
        drawPanel(guiGraphics, layout.topCenterX(), layout.topCenterY(), layout.topCenterWidth(), layout.topCenterHeight(), 0xB3192230, 0x805A6B80);
        drawPanel(guiGraphics, layout.topRightX(), layout.topRightY(), layout.topRightWidth(), layout.topRightHeight(), 0xB3192230, 0x805A6B80);
        drawPanel(guiGraphics, layout.bottomLeftX(), layout.bottomLeftY(), layout.bottomLeftWidth(), layout.bottomLeftHeight(), 0xB3192230, 0x805A6B80);
        drawPanel(guiGraphics, layout.bottomRightX(), layout.bottomRightY(), layout.bottomRightWidth(), layout.bottomRightHeight(), 0xB3192230, 0x805A6B80);

        guiGraphics.drawString(font, title, layout.windowX() + 10, layout.windowY() + 8, 0xFFE9F2FF, false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.research_v2.tree_selector"), layout.topLeftX() + 10, layout.topLeftY() + 8, 0xFFD6E7FF, false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.research_v2.status"), layout.topCenterX() + 10, layout.topCenterY() + 8, 0xFFD6E7FF, false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.research_v2.queue"), layout.topRightX() + 10, layout.topRightY() + 8, 0xFFD6E7FF, false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.research_v2.list"), layout.bottomLeftX() + 10, layout.bottomLeftY() + 8, 0xFFD6E7FF, false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.research_v2.graph"), layout.bottomRightX() + 10, layout.bottomRightY() + 8, 0xFFD6E7FF, false);

        drawStatusPanel(guiGraphics, layout);
        drawQueue(guiGraphics, layout, mouseX, mouseY);
        drawList(guiGraphics, layout);
        drawGraph(guiGraphics, layout);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void drawStatusPanel(GuiGraphics guiGraphics, Layout layout) {
        ResearchV2ClientCache.NodeEntry selected = selectedNode();
        int panelX = layout.topCenterX() + 10;
        int panelY = layout.topCenterY() + 24;
        int panelW = layout.topCenterWidth() - 20;
        int leftW = Math.max(130, (panelW * 45) / 100);
        int rightW = Math.max(120, panelW - leftW - 8);
        int leftX = panelX;
        int rightX = leftX + leftW + 8;
        int rowY = panelY;

        guiGraphics.drawString(
                font,
                Component.translatable("screen.incore.research_v2.controller_tier", snapshot.controllerTier()),
                leftX,
                rowY,
                0xFFAFC5E4,
                false
        );

        if (selected == null) {
            guiGraphics.drawString(
                    font,
                    Component.translatable("screen.incore.research_v2.no_selection"),
                    leftX,
                    rowY + 16,
                    0xFFB8C4D6,
                    false
            );
            return;
        }

        String title = trimToWidth(nodeDisplayName(selected), leftW - 8);
        guiGraphics.drawString(font, Component.literal(title), leftX, rowY + 16, 0xFFF2F6FF, false);
        guiGraphics.drawString(font, Component.literal(nodeStatusLabel(selected)), leftX, rowY + 30, 0xFF9DB7D9, false);

        if (!canShowRequirements(selected)) {
            guiGraphics.drawString(
                    font,
                    Component.translatable("screen.incore.research_v2.hidden_requirements"),
                    rightX,
                    rowY + 4,
                    0xFF9AA8BC,
                    false
            );
            return;
        }

        guiGraphics.drawString(
                font,
                Component.translatable("screen.incore.research_v2.requirement_time", selected.researchTime()),
                rightX,
                rowY + 4,
                0xFFE0ECFF,
                false
        );
        guiGraphics.drawString(
                font,
                Component.translatable("screen.incore.research_v2.requirement_runs", selected.requiredRuns()),
                rightX,
                rowY + 14,
                0xFFE0ECFF,
                false
        );
        guiGraphics.drawString(
                font,
                Component.translatable("screen.incore.research_v2.requirement_modules", trimToWidth(formatModuleRequirements(selected), rightW - 52)),
                rightX,
                rowY + 26,
                0xFFCBDBF0,
                false
        );
        guiGraphics.drawString(
                font,
                Component.translatable("screen.incore.research_v2.requirement_materials", trimToWidth(formatMaterialRequirements(selected), rightW - 56)),
                rightX,
                rowY + 40,
                0xFFCBDBF0,
                false
        );
    }

    private void drawQueue(GuiGraphics guiGraphics, Layout layout, int mouseX, int mouseY) {
        buildQueueCardLayout(layout);
        List<ResearchV2ClientCache.QueueEntry> queue = snapshot.researchQueue();
        if (queue.isEmpty()) {
            guiGraphics.drawString(font, Component.literal("-"), layout.topRightX() + 10, layout.topRightY() + 24, 0xFFAFC1D8, false);
            return;
        }

        int visible = Math.min(queueVisibleColumns(layout), queue.size());
        for (int i = 0; i < queue.size() && i < visible; i++) {
            ResearchV2ClientCache.QueueEntry entry = queue.get(i);
            NodeBounds bounds = queueCardBounds.get(entry.nodeId());
            if (bounds == null) {
                continue;
            }

            int fill = i == 0 ? 0xFF256EAB : 0xFF7A6A2D;
            int border = i == 0 ? 0xFF9FD2F6 : 0xFFE5D189;
            drawPanel(guiGraphics, bounds.x(), bounds.y(), bounds.width(), bounds.height(), fill, border);

            ResearchV2ClientCache.NodeEntry node = snapshot.nodeById().get(entry.nodeId());
            ItemStack icon = queueIcon(node);
            if (!icon.isEmpty()) {
                guiGraphics.renderItem(icon, bounds.x() + 7, bounds.y() + 6);
            } else {
                guiGraphics.drawString(font, "?", bounds.x() + 11, bounds.y() + 8, 0xFFFFFFFF, false);
            }
            drawQueueProgressBar(guiGraphics, bounds, overallProgress(entry), overallRequired(entry));

            if (entry.nodeId().equals(selectedNodeId)) {
                drawSelectionOutline(guiGraphics, bounds);
            }

            NodeBounds removeBounds = queueRemoveBounds.get(entry.nodeId());
            if (removeBounds != null) {
                boolean hover = removeBounds.contains(mouseX, mouseY);
                int removeFill = hover ? 0xFF9B2A2A : 0xFF2A2F39;
                int removeBorder = hover ? 0xFFFF9A9A : 0xFF5E6878;
                drawPanel(guiGraphics, removeBounds.x(), removeBounds.y(), removeBounds.width(), removeBounds.height(), removeFill, removeBorder);
                guiGraphics.drawCenteredString(font, Component.literal("x"), removeBounds.centerX(), removeBounds.y() + 1, 0xFFFFFFFF);
            }
        }

        int hidden = queue.size() - visible;
        if (hidden > 0) {
            guiGraphics.drawString(
                    font,
                    Component.translatable("screen.incore.research_v2.queue_more", hidden),
                    layout.topRightX() + 10,
                    layout.topRightY() + 24 + QUEUE_CARD_H + 4,
                    0xFF95AECF,
                    false
            );
        }
    }

    private void drawList(GuiGraphics guiGraphics, Layout layout) {
        listNodeBounds.clear();
        List<ResearchV2ClientCache.NodeEntry> visibleNodes = filteredNodesForList();
        int rowsVisible = listRowsVisible(layout);
        int maxScroll = Math.max(0, visibleNodes.size() - rowsVisible);
        listScroll = Math.clamp(listScroll, 0, maxScroll);

        int startX = layout.bottomLeftX() + 10;
        int startY = layout.bottomLeftY() + 56;
        int rowWidth = layout.bottomLeftWidth() - 20;
        for (int i = 0; i < rowsVisible; i++) {
            int index = listScroll + i;
            if (index >= visibleNodes.size()) {
                break;
            }

            ResearchV2ClientCache.NodeEntry node = visibleNodes.get(index);
            int y = startY + i * LIST_ROW_HEIGHT;
            int fill = nodeRowFill(node);
            drawPanel(guiGraphics, startX, y, rowWidth, LIST_ROW_HEIGHT - 2, fill, 0x804D6078);
            if (node.id().equals(selectedNodeId)) {
                drawSelectionOutline(guiGraphics, new NodeBounds(startX, y, rowWidth, LIST_ROW_HEIGHT - 2));
            }

            String left = trimToWidth(nodeDisplayName(node), rowWidth - 92);
            String right = trimToWidth(categoryName(node.categoryId()), 80);
            guiGraphics.drawString(font, Component.literal(left), startX + 5, y + 5, 0xFFE8F1FF, false);
            guiGraphics.drawString(font, Component.literal(right), startX + rowWidth - 84, y + 5, 0xFFB7CBE6, false);

            listNodeBounds.put(node.id(), new NodeBounds(startX, y, rowWidth, LIST_ROW_HEIGHT - 2));
        }
    }

    private void drawGraph(GuiGraphics guiGraphics, Layout layout) {
        List<ResearchV2ClientCache.NodeEntry> treeNodes = nodesInSelectedTree();
        if (treeNodes.isEmpty()) {
            return;
        }

        GraphViewport viewport = graphViewport(layout);
        guiGraphics.enableScissor(viewport.x(), viewport.y(), viewport.x() + viewport.width(), viewport.y() + viewport.height());

        Map<String, ResearchV2ClientCache.NodeEntry> nodeById = new HashMap<>();
        for (ResearchV2ClientCache.NodeEntry node : treeNodes) {
            nodeById.put(node.id(), node);
        }

        for (ResearchV2ClientCache.NodeEntry node : treeNodes) {
            NodeBounds target = shifted(graphNodeBounds.get(node.id()), graphPanX, graphPanY);
            if (target == null) {
                continue;
            }

            for (String prerequisiteId : node.prerequisites()) {
                ResearchV2ClientCache.NodeEntry sourceNode = nodeById.get(prerequisiteId);
                if (sourceNode == null) {
                    continue;
                }
                NodeBounds source = shifted(graphNodeBounds.get(sourceNode.id()), graphPanX, graphPanY);
                if (source == null) {
                    continue;
                }
                drawConnector(guiGraphics, source.centerX(), source.centerY(), target.centerX(), target.centerY(), 0xAA6A7E97);
            }
        }

        for (ResearchV2ClientCache.NodeEntry node : treeNodes) {
            NodeBounds shifted = shifted(graphNodeBounds.get(node.id()), graphPanX, graphPanY);
            if (shifted == null) {
                continue;
            }

            drawPanel(guiGraphics, shifted.x(), shifted.y(), shifted.width(), shifted.height(), nodeRowFill(node), 0x80556781);
            if (node.id().equals(selectedNodeId)) {
                drawSelectionOutline(guiGraphics, shifted);
            }

            String label = trimToWidth(nodeDisplayName(node), shifted.width() - 8);
            guiGraphics.drawCenteredString(font, Component.literal(label), shifted.centerX(), shifted.y() + 8, 0xFFF3F7FF);

            ResearchV2ClientCache.QueueEntry queueEntry = queueEntry(node.id());
            if (queueEntry != null) {
                int progressWidth = shifted.width() - 8;
                int filled = Math.min(progressWidth, (overallProgress(queueEntry) * progressWidth) / Math.max(1, overallRequired(queueEntry)));
                int barX = shifted.x() + 4;
                int barY = shifted.y() + shifted.height() - 11;
                guiGraphics.fill(barX, barY, barX + progressWidth, barY + 4, 0xFF1C2733);
                if (filled > 0) {
                    guiGraphics.fill(barX, barY, barX + filled, barY + 4, 0xFF63A8E6);
                }
            }
        }

        guiGraphics.disableScissor();
    }

    private void rebuildScreenWidgets() {
        Layout layout = layout();
        clearWidgets();
        filterButtons.clear();
        queueCardBounds.clear();
        queueRemoveBounds.clear();
        treeSelector = null;
        searchBox = null;
        queueResearchButton = null;

        List<String> treeIds = snapshot.trees().stream().map(ResearchV2ClientCache.TreeEntry::id).toList();
        if (!treeIds.isEmpty()) {
            treeSelector = addRenderableWidget(CycleButton.<String>builder(value -> Component.literal(treeLabel(value)))
                    .withValues(treeIds)
                    .withInitialValue(selectedTreeId())
                    .create(
                            layout.topLeftX() + 10,
                            layout.topLeftY() + 22,
                            layout.topLeftWidth() - 20,
                            16,
                            Component.empty(),
                            (button, value) -> {
                                ResearchV2ClientCache.setSelectedTreeId(value);
                                listScroll = 0;
                                ensureSelection();
                                rebuildGraphLayout();
                                clampGraphPan();
                                updateQueueButtonState();
                            }
                    ));
        } else {
            Button noTrees = addRenderableWidget(Button.builder(Component.translatable("screen.incore.research_v2.no_trees"), b -> {
            }).bounds(layout.topLeftX() + 10, layout.topLeftY() + 20, layout.topLeftWidth() - 20, 20).build());
            noTrees.active = false;
        }

        queueResearchButton = addRenderableWidget(Button.builder(
                        Component.translatable("screen.incore.research_v2.queue_research"),
                        button -> queueSelectedNode()
                ).bounds(
                        layout.topCenterX() + layout.topCenterWidth() - 106,
                        layout.topCenterY() + 5,
                        96,
                        16
                )
                .build());

        searchBox = addRenderableWidget(new EditBox(
                font,
                layout.bottomLeftX() + 10,
                layout.bottomLeftY() + 24,
                layout.bottomLeftWidth() - 20,
                16,
                Component.translatable("screen.incore.research_v2.search")
        ));
        searchBox.setMaxLength(64);
        searchBox.setValue(searchQuery);
        searchBox.setResponder(value -> {
            searchQuery = value;
            listScroll = 0;
            clampListScroll(layout);
        });

        int filterY = layout.bottomLeftY() + 42;
        int spacing = 3;
        int filterCount = FilterMode.values().length;
        int totalWidth = layout.bottomLeftWidth() - 20;
        int buttonWidth = Math.max(32, (totalWidth - (filterCount - 1) * spacing) / filterCount);
        int x = layout.bottomLeftX() + 10;
        for (FilterMode mode : FilterMode.values()) {
            Button filterButton = addRenderableWidget(Button.builder(mode.label(), button -> {
                activeFilter = mode;
                listScroll = 0;
                ensureSelection();
                updateFilterButtons();
                updateQueueButtonState();
            }).bounds(x, filterY, buttonWidth, 14).build());
            filterButtons.put(mode, filterButton);
            x += buttonWidth + spacing;
        }
        updateFilterButtons();

        updateQueueButtonState();
    }

    private void queueSelectedNode() {
        ResearchV2ClientCache.NodeEntry selected = selectedNode();
        if (selected == null || !canQueueNode(selected)) {
            return;
        }

        ResourceLocation nodeId = ResourceLocation.tryParse(selected.id());
        if (nodeId != null) {
            ResearchV2Networking.queueResearch(nodeId);
        }
    }

    private void rebuildGraphLayout() {
        graphNodeBounds.clear();
        List<ResearchV2ClientCache.NodeEntry> treeNodes = nodesInSelectedTree();
        if (treeNodes.isEmpty()) {
            graphPanX = 0;
            graphPanY = 0;
            return;
        }

        Map<String, ResearchV2ClientCache.NodeEntry> nodeById = new HashMap<>();
        for (ResearchV2ClientCache.NodeEntry node : treeNodes) {
            nodeById.put(node.id(), node);
        }

        Map<String, Integer> depthByNode = new HashMap<>();
        for (ResearchV2ClientCache.NodeEntry node : treeNodes) {
            computeDepth(node.id(), nodeById, depthByNode, new ArrayDeque<>());
        }

        Map<Integer, List<ResearchV2ClientCache.NodeEntry>> byDepth = new LinkedHashMap<>();
        for (ResearchV2ClientCache.NodeEntry node : treeNodes) {
            int depth = depthByNode.getOrDefault(node.id(), 0);
            byDepth.computeIfAbsent(depth, ignored -> new ArrayList<>()).add(node);
        }

        int baseX = graphViewport(layout()).x() + GRAPH_PADDING;
        int baseY = graphViewport(layout()).y() + GRAPH_PADDING;
        byDepth.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(depthEntry -> {
                    int depth = depthEntry.getKey();
                    List<ResearchV2ClientCache.NodeEntry> nodesAtDepth = depthEntry.getValue();
                    nodesAtDepth.sort(Comparator.comparing(this::nodeSortKey, String.CASE_INSENSITIVE_ORDER).thenComparing(ResearchV2ClientCache.NodeEntry::id));
                    int x = baseX + depth * NODE_X_STEP;
                    for (int i = 0; i < nodesAtDepth.size(); i++) {
                        int y = baseY + i * NODE_Y_STEP;
                        graphNodeBounds.put(nodesAtDepth.get(i).id(), new NodeBounds(x, y, NODE_WIDTH, NODE_HEIGHT));
                    }
                });
    }

    private int computeDepth(
            String nodeId,
            Map<String, ResearchV2ClientCache.NodeEntry> nodeById,
            Map<String, Integer> cache,
            Deque<String> stack
    ) {
        if (cache.containsKey(nodeId)) {
            return cache.get(nodeId);
        }
        if (stack.contains(nodeId)) {
            return 0;
        }

        ResearchV2ClientCache.NodeEntry node = nodeById.get(nodeId);
        if (node == null || node.prerequisites().isEmpty()) {
            cache.put(nodeId, 0);
            return 0;
        }

        stack.push(nodeId);
        int depth = 0;
        for (String prerequisite : node.prerequisites()) {
            if (!nodeById.containsKey(prerequisite)) {
                continue;
            }
            depth = Math.max(depth, computeDepth(prerequisite, nodeById, cache, stack) + 1);
        }
        stack.pop();

        cache.put(nodeId, depth);
        return depth;
    }

    private void clampGraphPan() {
        if (graphNodeBounds.isEmpty()) {
            graphPanX = 0;
            graphPanY = 0;
            return;
        }

        GraphViewport viewport = graphViewport(layout());
        int contentMinX = Integer.MAX_VALUE;
        int contentMinY = Integer.MAX_VALUE;
        int contentMaxX = Integer.MIN_VALUE;
        int contentMaxY = Integer.MIN_VALUE;

        for (NodeBounds bounds : graphNodeBounds.values()) {
            contentMinX = Math.min(contentMinX, bounds.x());
            contentMinY = Math.min(contentMinY, bounds.y());
            contentMaxX = Math.max(contentMaxX, bounds.x() + bounds.width());
            contentMaxY = Math.max(contentMaxY, bounds.y() + bounds.height());
        }

        int contentWidth = Math.max(0, contentMaxX - contentMinX);
        int contentHeight = Math.max(0, contentMaxY - contentMinY);
        int paddedViewportWidth = Math.max(0, viewport.width() - GRAPH_PADDING * 2);
        int paddedViewportHeight = Math.max(0, viewport.height() - GRAPH_PADDING * 2);

        if (contentWidth <= paddedViewportWidth) {
            graphPanX = viewport.x() + (viewport.width() - contentWidth) / 2 - contentMinX;
        } else {
            int minPanX = viewport.x() + viewport.width() - GRAPH_PADDING - contentMaxX;
            int maxPanX = viewport.x() + GRAPH_PADDING - contentMinX;
            graphPanX = Math.clamp(graphPanX, minPanX, maxPanX);
        }

        if (contentHeight <= paddedViewportHeight) {
            graphPanY = viewport.y() + (viewport.height() - contentHeight) / 2 - contentMinY;
        } else {
            int minPanY = viewport.y() + viewport.height() - GRAPH_PADDING - contentMaxY;
            int maxPanY = viewport.y() + GRAPH_PADDING - contentMinY;
            graphPanY = Math.clamp(graphPanY, minPanY, maxPanY);
        }
    }

    private void ensureSelectedTree() {
        if (!ResearchV2ClientCache.selectedTreeId().isBlank() && snapshot.treeById().containsKey(ResearchV2ClientCache.selectedTreeId())) {
            return;
        }

        if (!snapshot.trees().isEmpty()) {
            ResearchV2ClientCache.setSelectedTreeId(snapshot.trees().get(0).id());
        }
    }

    private void ensureSelection() {
        if (selectedNodeId != null && !selectedNodeId.isBlank()) {
            ResearchV2ClientCache.NodeEntry selected = snapshot.nodeById().get(selectedNodeId);
            if (selected != null && selected.treeId().equals(selectedTreeId())) {
                return;
            }
        }

        List<ResearchV2ClientCache.NodeEntry> nodes = nodesInSelectedTree();
        selectedNodeId = nodes.isEmpty() ? "" : nodes.get(0).id();
    }

    private void updateQueueButtonState() {
        if (queueResearchButton == null) {
            return;
        }
        ResearchV2ClientCache.NodeEntry selected = selectedNode();
        queueResearchButton.active = selected != null && canQueueNode(selected);
    }

    private void updateFilterButtons() {
        for (var entry : filterButtons.entrySet()) {
            entry.getValue().active = entry.getKey() != activeFilter;
        }
    }

    private void clampListScroll(Layout layout) {
        int rowsVisible = listRowsVisible(layout);
        int maxScroll = Math.max(0, filteredNodesForList().size() - rowsVisible);
        listScroll = Math.clamp(listScroll, 0, maxScroll);
    }

    private boolean canQueueNode(ResearchV2ClientCache.NodeEntry node) {
        return isDiscovered(node)
                && !isCompleted(node)
                && !isQueued(node.id())
                && node.treeId().equals(selectedTreeId());
    }

    private boolean canShowRequirements(ResearchV2ClientCache.NodeEntry node) {
        return isDiscovered(node) || isCompleted(node);
    }

    private boolean shouldRevealDiscoveredNodeNames() {
        return snapshot.controllerTier() >= 3;
    }

    private ResearchV2ClientCache.NodeEntry selectedNode() {
        return snapshot.nodeById().get(selectedNodeId);
    }

    private List<ResearchV2ClientCache.NodeEntry> nodesInSelectedTree() {
        String treeId = selectedTreeId();
        return snapshot.nodes().stream()
                .filter(node -> treeId.equals(node.treeId()))
                .sorted(Comparator.comparing(this::nodeSortKey, String.CASE_INSENSITIVE_ORDER).thenComparing(ResearchV2ClientCache.NodeEntry::id))
                .toList();
    }

    private List<ResearchV2ClientCache.NodeEntry> filteredNodesForList() {
        String query = searchQuery == null ? "" : searchQuery.strip().toLowerCase(Locale.ROOT);
        return nodesInSelectedTree().stream()
                .filter(this::matchesFilter)
                .filter(node -> query.isEmpty() || listSearchLabel(node).toLowerCase(Locale.ROOT).contains(query))
                .toList();
    }

    private boolean matchesFilter(ResearchV2ClientCache.NodeEntry node) {
        return switch (activeFilter) {
            case ALL -> true;
            case UNDISCOVERED -> !isDiscovered(node) && !isCompleted(node);
            case DISCOVERED -> isDiscovered(node) && !isCompleted(node);
            case COMPLETED -> isCompleted(node);
            case QUEUED -> isQueued(node.id());
        };
    }

    private String listSearchLabel(ResearchV2ClientCache.NodeEntry node) {
        String name = nodeDisplayName(node);
        String category = categoryName(node.categoryId());
        return name + " " + category;
    }

    private String nodeDisplayName(ResearchV2ClientCache.NodeEntry node) {
        if (node == null) {
            return "?";
        }
        if (isCompleted(node)) {
            return node.name();
        }
        if (isDiscovered(node)) {
            return shouldRevealDiscoveredNodeNames() ? node.name() : categoryName(node.categoryId());
        }
        return "???";
    }

    private String nodeSortKey(ResearchV2ClientCache.NodeEntry node) {
        return nodeDisplayName(node);
    }

    private String nodeStatusLabel(ResearchV2ClientCache.NodeEntry node) {
        if (isCompleted(node)) {
            return Component.translatable("screen.incore.research_v2.status.completed").getString();
        }
        if (isQueued(node.id())) {
            return Component.translatable("screen.incore.research_v2.status.queued").getString();
        }
        if (isDiscovered(node)) {
            return Component.translatable("screen.incore.research_v2.status.discovered").getString();
        }
        return Component.translatable("screen.incore.research_v2.status.undiscovered").getString();
    }

    private String categoryName(String categoryId) {
        ResearchV2ClientCache.CategoryEntry category = snapshot.categoriesById().get(categoryId);
        if (category != null) {
            return category.name();
        }
        return humanizeIdPath(categoryId);
    }

    private String treeLabel(String treeId) {
        ResearchV2ClientCache.TreeEntry tree = snapshot.treeById().get(treeId);
        return tree == null ? treeId : tree.name();
    }

    private String formatModuleRequirements(ResearchV2ClientCache.NodeEntry node) {
        if (node.requiredLogicModules().isEmpty()) {
            return "-";
        }
        return node.requiredLogicModules().stream()
                .map(requirement -> requirement.moduleTier() + " x" + requirement.durabilityCost())
                .limit(3)
                .reduce((left, right) -> left + ", " + right)
                .orElse("-");
    }

    private String formatMaterialRequirements(ResearchV2ClientCache.NodeEntry node) {
        if (node.requiredResearchMaterials().isEmpty()) {
            return "-";
        }
        return node.requiredResearchMaterials().stream()
                .map(requirement -> humanizeIdPath(requirement.materialId()) + " x" + requirement.count())
                .limit(3)
                .reduce((left, right) -> left + ", " + right)
                .orElse("-");
    }

    private int nodeRowFill(ResearchV2ClientCache.NodeEntry node) {
        if (isCompleted(node)) {
            return 0xAA1E6C45;
        }
        if (isQueued(node.id())) {
            return 0xAA265C8A;
        }
        if (isDiscovered(node)) {
            return 0xAA6E5825;
        }
        return 0xAA3A3F49;
    }

    private boolean isDiscovered(ResearchV2ClientCache.NodeEntry node) {
        return snapshot.discoveredNodeIds().contains(node.id());
    }

    private boolean isCompleted(ResearchV2ClientCache.NodeEntry node) {
        return snapshot.completedNodeIds().contains(node.id());
    }

    private boolean isQueued(String nodeId) {
        return queueEntry(nodeId) != null;
    }

    private @Nullable ResearchV2ClientCache.QueueEntry queueEntry(String nodeId) {
        for (ResearchV2ClientCache.QueueEntry entry : snapshot.researchQueue()) {
            if (entry.nodeId().equals(nodeId)) {
                return entry;
            }
        }
        return null;
    }

    private String selectedTreeId() {
        return ResearchV2ClientCache.selectedTreeId();
    }

    private int listRowsVisible(Layout layout) {
        return Math.max(1, (layout.bottomLeftHeight() - 62) / LIST_ROW_HEIGHT);
    }

    private int queueVisibleColumns(Layout layout) {
        return Math.max(1, (layout.topRightWidth() - 20) / (QUEUE_CARD_W + QUEUE_CARD_GAP));
    }

    private boolean inGraphViewport(double mouseX, double mouseY, Layout layout) {
        GraphViewport viewport = graphViewport(layout);
        return mouseX >= viewport.x()
                && mouseX < viewport.x() + viewport.width()
                && mouseY >= viewport.y()
                && mouseY < viewport.y() + viewport.height();
    }

    private boolean inListRows(double mouseX, double mouseY, Layout layout) {
        int x = layout.bottomLeftX() + 10;
        int y = layout.bottomLeftY() + 56;
        int w = layout.bottomLeftWidth() - 20;
        int h = listRowsVisible(layout) * LIST_ROW_HEIGHT;
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private GraphViewport graphViewport(Layout layout) {
        return new GraphViewport(
                layout.bottomRightX() + 8,
                layout.bottomRightY() + 20,
                layout.bottomRightWidth() - 16,
                layout.bottomRightHeight() - 28
        );
    }

    private Layout layout() {
        int windowWidth = Math.max(720, Math.min(1040, width - WINDOW_MARGIN * 2));
        int windowHeight = Math.max(420, Math.min(640, height - WINDOW_MARGIN * 2));
        int windowX = (width - windowWidth) / 2;
        int windowY = (height - windowHeight) / 2;

        int topHeight = Math.max(84, Math.min(96, (windowHeight * 18) / 100));
        int bottomHeight = windowHeight - topHeight - PANEL_GAP;

        int selectorWidth = Math.max(160, Math.min(220, (windowWidth * 18) / 100));
        int queueWidth = Math.max(210, Math.min(280, (windowWidth * 22) / 100));
        int statusWidth = windowWidth - selectorWidth - queueWidth - (PANEL_GAP * 2);
        if (statusWidth < 220) {
            int deficit = 220 - statusWidth;
            queueWidth = Math.max(190, queueWidth - deficit);
            statusWidth = windowWidth - selectorWidth - queueWidth - (PANEL_GAP * 2);
        }

        int bottomLeftWidth = Math.max(220, Math.min(320, (windowWidth * 30) / 100));
        int bottomRightWidth = windowWidth - bottomLeftWidth - PANEL_GAP;

        int topLeftX = windowX;
        int topCenterX = topLeftX + selectorWidth + PANEL_GAP;
        int topRightX = topCenterX + statusWidth + PANEL_GAP;

        int bottomLeftX = windowX;
        int bottomRightX = bottomLeftX + bottomLeftWidth + PANEL_GAP;
        int topY = windowY + 20;
        int bottomY = topY + topHeight + PANEL_GAP;

        return new Layout(
                windowX,
                windowY,
                windowWidth,
                windowHeight,
                topLeftX,
                topY,
                selectorWidth,
                topHeight,
                topCenterX,
                topY,
                statusWidth,
                topHeight,
                topRightX,
                topY,
                queueWidth,
                topHeight,
                bottomLeftX,
                bottomY,
                bottomLeftWidth,
                bottomHeight,
                bottomRightX,
                bottomY,
                bottomRightWidth,
                bottomHeight
        );
    }

    private void buildQueueCardLayout(Layout layout) {
        queueCardBounds.clear();
        queueRemoveBounds.clear();
        List<ResearchV2ClientCache.QueueEntry> queue = snapshot.researchQueue();
        if (queue.isEmpty()) {
            return;
        }

        int columns = queueVisibleColumns(layout);
        int startX = layout.topRightX() + 10;
        int y = layout.topRightY() + 24;
        for (int i = 0; i < queue.size() && i < columns; i++) {
            ResearchV2ClientCache.QueueEntry entry = queue.get(i);
            int x = startX + i * (QUEUE_CARD_W + QUEUE_CARD_GAP);
            queueCardBounds.put(entry.nodeId(), new NodeBounds(x, y, QUEUE_CARD_W, QUEUE_CARD_H));

            int removeW = 10;
            int removeH = 8;
            int removeX = x + (QUEUE_CARD_W - removeW) / 2;
            int removeY = y + QUEUE_CARD_H - removeH - 1;
            queueRemoveBounds.put(entry.nodeId(), new NodeBounds(removeX, removeY, removeW, removeH));
        }
    }

    private static NodeBounds shifted(NodeBounds bounds, int offsetX, int offsetY) {
        if (bounds == null) {
            return null;
        }
        return new NodeBounds(bounds.x() + offsetX, bounds.y() + offsetY, bounds.width(), bounds.height());
    }

    private String trimToWidth(String value, int maxWidth) {
        if (value == null) {
            return "";
        }
        return font.plainSubstrByWidth(value, Math.max(0, maxWidth));
    }

    private static String humanizeIdPath(String id) {
        if (id == null || id.isBlank()) {
            return "Unknown";
        }

        String path = id;
        int separator = id.indexOf(':');
        if (separator >= 0 && separator + 1 < id.length()) {
            path = id.substring(separator + 1);
        }

        String[] parts = path.split("_");
        StringBuilder builder = new StringBuilder(path.length() + 4);
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }

        return builder.length() == 0 ? "Unknown" : builder.toString();
    }

    private static void drawPanel(GuiGraphics guiGraphics, int x, int y, int width, int height, int fill, int border) {
        guiGraphics.fill(x, y, x + width, y + height, fill);
        guiGraphics.fill(x, y, x + width, y + 1, border);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, border);
        guiGraphics.fill(x, y, x + 1, y + height, border);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, border);
    }

    private static void drawSelectionOutline(GuiGraphics guiGraphics, NodeBounds bounds) {
        int x = bounds.x();
        int y = bounds.y();
        int w = bounds.width();
        int h = bounds.height();
        guiGraphics.fill(x - 1, y - 1, x + w + 1, y, 0xFFFFFFFF);
        guiGraphics.fill(x - 1, y + h, x + w + 1, y + h + 1, 0xFFFFFFFF);
        guiGraphics.fill(x - 1, y, x, y + h, 0xFFFFFFFF);
        guiGraphics.fill(x + w, y, x + w + 1, y + h, 0xFFFFFFFF);
    }

    private static void drawConnector(GuiGraphics guiGraphics, int sourceX, int sourceY, int targetX, int targetY, int color) {
        int midX = (sourceX + targetX) / 2;
        guiGraphics.hLine(Math.min(sourceX, midX), Math.max(sourceX, midX), sourceY, color);
        guiGraphics.vLine(midX, Math.min(sourceY, targetY), Math.max(sourceY, targetY), color);
        guiGraphics.hLine(Math.min(midX, targetX), Math.max(midX, targetX), targetY, color);
    }

    private static void drawQueueProgressBar(GuiGraphics guiGraphics, NodeBounds bounds, int progress, int requiredTime) {
        int cost = Math.max(1, requiredTime);
        int normalizedProgress = Math.clamp(progress, 0, cost);
        if (normalizedProgress <= 0 || normalizedProgress >= cost) {
            return;
        }

        int barX = bounds.x() + 2;
        int barY = bounds.y() + bounds.height() - 11;
        int barW = bounds.width() - 4;
        int fillW = Math.clamp((normalizedProgress * barW) / cost, 1, barW);
        guiGraphics.fill(barX, barY, barX + barW, barY + 2, 0xFF20242C);
        guiGraphics.fill(barX, barY, barX + fillW, barY + 2, 0xFF42C86F);
    }

    private static int overallRequired(ResearchV2ClientCache.QueueEntry entry) {
        int tickRequired = Math.max(1, entry.runTickRequired());
        int runsRequired = Math.max(1, entry.requiredRuns());
        return tickRequired * runsRequired;
    }

    private static int overallProgress(ResearchV2ClientCache.QueueEntry entry) {
        int tickRequired = Math.max(1, entry.runTickRequired());
        int runsRequired = Math.max(1, entry.requiredRuns());
        int completedRuns = Math.max(0, Math.min(entry.completedRuns(), runsRequired));
        int runTickProgress = Math.max(0, Math.min(entry.runTickProgress(), tickRequired));
        int totalProgress = (completedRuns * tickRequired) + runTickProgress;
        return Math.min(overallRequired(entry), totalProgress);
    }

    private ItemStack queueIcon(@Nullable ResearchV2ClientCache.NodeEntry node) {
        if (node == null) {
            return ItemStack.EMPTY;
        }
        ResearchV2ClientCache.CategoryEntry category = snapshot.categoriesById().get(node.categoryId());
        if (category == null) {
            return ItemStack.EMPTY;
        }
        return itemStackFromId(category.iconId());
    }

    private static ItemStack itemStackFromId(String itemIdString) {
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

    private enum FilterMode {
        ALL("screen.incore.research_v2.filter.all"),
        UNDISCOVERED("screen.incore.research_v2.filter.undiscovered"),
        DISCOVERED("screen.incore.research_v2.filter.discovered"),
        COMPLETED("screen.incore.research_v2.filter.completed"),
        QUEUED("screen.incore.research_v2.filter.queued");

        private final String translationKey;

        FilterMode(String translationKey) {
            this.translationKey = translationKey;
        }

        Component label() {
            return Component.translatable(translationKey);
        }
    }

    private record Layout(
            int windowX,
            int windowY,
            int windowWidth,
            int windowHeight,
            int topLeftX,
            int topLeftY,
            int topLeftWidth,
            int topLeftHeight,
            int topCenterX,
            int topCenterY,
            int topCenterWidth,
            int topCenterHeight,
            int topRightX,
            int topRightY,
            int topRightWidth,
            int topRightHeight,
            int bottomLeftX,
            int bottomLeftY,
            int bottomLeftWidth,
            int bottomLeftHeight,
            int bottomRightX,
            int bottomRightY,
            int bottomRightWidth,
            int bottomRightHeight
    ) {
    }

    private record GraphViewport(int x, int y, int width, int height) {
    }

    private record NodeBounds(int x, int y, int width, int height) {
        int centerX() {
            return x + width / 2;
        }

        int centerY() {
            return y + height / 2;
        }

        boolean contains(double px, double py) {
            return px >= x && px < x + width && py >= y && py < y + height;
        }
    }
}
