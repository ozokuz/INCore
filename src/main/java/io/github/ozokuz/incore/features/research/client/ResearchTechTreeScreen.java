package io.github.ozokuz.incore.features.research.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.ozokuz.incore.features.research.network.ResearchNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ResearchTechTreeScreen extends Screen {
    private static final int GUI_WIDTH = 620;
    private static final int GUI_HEIGHT = 340;
    private static final int LEFT_PANEL_WIDTH = 236;
    private static final int QUEUE_PANEL_Y = 26;
    private static final int QUEUE_PANEL_H = 60;
    private static final int SEARCH_PANEL_Y = QUEUE_PANEL_Y + QUEUE_PANEL_H + 2;
    private static final int SEARCH_PANEL_H = 18;
    private static final int DETAILS_PANEL_Y = SEARCH_PANEL_Y + SEARCH_PANEL_H + 2;
    private static final int DETAILS_PANEL_H = 128;
    private static final int LIST_PANEL_Y = DETAILS_PANEL_Y + DETAILS_PANEL_H + 4;
    private static final int LIST_PANEL_H = GUI_HEIGHT - LIST_PANEL_Y - 4;
    private static final int TECH_LIST_COLUMNS = 6;
    private static final int TECH_LIST_CARD_W = 30;
    private static final int TECH_LIST_CARD_H = 34;
    private static final int TECH_LIST_CARD_GAP = 2;
    private static final int TREE_NODE_W = 90;
    private static final int TREE_NODE_H = 38;
    private static final int TREE_LAYOUT_PADDING_X = 16;
    private static final int TREE_LAYOUT_PADDING_Y = 12;
    private static final int TREE_MIN_X_STEP = 120;
    private static final int TREE_MIN_Y_STEP = 58;
    private static final int TREE_PAN_PADDING = 10;
    private static final int TAB_BUTTON_Y = 6;
    private static final int TAB_BUTTON_H = 16;
    private static final int MANUAL_PANEL_Y = 28;
    private static final int MANUAL_PANEL_H = GUI_HEIGHT - MANUAL_PANEL_Y - 4;
    private static final int SELECTION_COLOR = 0xFFFFFFFF;
    private static final int SEARCH_DEBOUNCE_TICKS = 4;

    private final Set<String> unlocked = new HashSet<>();
    private final Set<String> completedTasks = new HashSet<>();
    private final List<String> queue = new ArrayList<>();
    private final List<TechEntry> entries = new ArrayList<>();
    private final List<ManualTaskEntry> tasks = new ArrayList<>();
    private final Map<String, ManualTaskEntry> taskById = new HashMap<>();
    private final Map<String, TechEntry> entryById = new HashMap<>();
    private final Map<String, NodeBounds> nodeBounds = new HashMap<>();
    private final Map<String, NodeBounds> queueBounds = new HashMap<>();
    private final Map<String, NodeBounds> queueRemoveBounds = new HashMap<>();
    private final Map<String, NodeBounds> techListBounds = new HashMap<>();
    private final Map<String, Integer> progressByEntry = new HashMap<>();

    private int guiX;
    private int guiY;
    private int treePanX;
    private int treePanY;
    private long refreshTicks;
    private Integer previousMenuBlur;

    private @Nullable String selectedTechId;
    private @Nullable String selectedTaskId;
    private @Nullable Button startResearchButton;
    private @Nullable Button submitManualTaskButton;
    private @Nullable Button tabTechTreeButton;
    private @Nullable Button tabManualResearchButton;
    private @Nullable EditBox techSearchBox;
    private @Nullable String draggingQueueEntryId;
    private @Nullable String activeResearchId;
    private int draggingQueueOriginalIndex = -1;
    private int draggingQueueStartIndex = -1;
    private int draggingQueueOffsetX;
    private int draggingQueueOffsetY;
    private double draggingMouseX;
    private double draggingMouseY;
    private String techSearchQuery = "";
    private String appliedTechSearchQuery = "";
    private @Nullable String pendingTechSearchQuery;
    private long pendingTechSearchApplyTick = -1L;
    private ResearchTab activeTab = ResearchTab.TECH_TREE;

    private enum ResearchTab {
        TECH_TREE,
        MANUAL_RESEARCH
    }

    public ResearchTechTreeScreen(String json) {
        super(Component.translatable("screen.incore.research.title"));
        applyPayload(JsonParser.parseString(json).getAsJsonObject());
    }

    public void updatePayload(String json) {
        String selectedBefore = selectedTechId;
        applyPayload(JsonParser.parseString(json).getAsJsonObject());

        if (selectedBefore != null && entryById.containsKey(selectedBefore)) {
            selectedTechId = selectedBefore;
        }

        if (minecraft != null && minecraft.screen == this) {
            rebuildWidgetsAndLayout();
        }
    }

    @Override
    protected void init() {
        super.init();
        if (this.previousMenuBlur == null && this.minecraft != null) {
            this.previousMenuBlur = this.minecraft.options.getMenuBackgroundBlurriness();
            if (this.previousMenuBlur > 0) {
                this.minecraft.options.menuBackgroundBlurriness().set(0);
            }
        }
        rebuildWidgetsAndLayout();
    }

    @Override
    public void removed() {
        if (this.minecraft != null && this.previousMenuBlur != null) {
            this.minecraft.options.menuBackgroundBlurriness().set(this.previousMenuBlur);
        }
        this.previousMenuBlur = null;
        super.removed();
    }

    @Override
    public void tick() {
        super.tick();
        refreshTicks++;
        if (pendingTechSearchQuery != null && refreshTicks >= pendingTechSearchApplyTick) {
            applySearchQueryNow(pendingTechSearchQuery);
        }
        if (refreshTicks % 20L == 0L) {
            ResearchNetworking.requestOpen();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (activeTab != ResearchTab.TECH_TREE) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        if (button == 1 && techSearchBox != null && techSearchBox.isMouseOver(mouseX, mouseY)) {
            techSearchBox.setValue("");
            techSearchQuery = "";
            applySearchQueryNow("");
            techSearchBox.setFocused(true);
            setFocused(techSearchBox);
            return true;
        }
        if (techSearchBox != null && techSearchBox.isFocused() && !techSearchBox.isMouseOver(mouseX, mouseY)) {
            techSearchBox.setFocused(false);
            if (getFocused() == techSearchBox) {
                setFocused(null);
            }
        }

        if (button == 0) {
            for (var entry : queueRemoveBounds.entrySet()) {
                NodeBounds bounds = entry.getValue();
                if (bounds.contains(mouseX, mouseY)) {
                    String entryId = entry.getKey();
                    ResourceLocation id = ResourceLocation.tryParse(entryId);
                    if (id != null) {
                        ResearchNetworking.requestRemoveQueueEntry(id);
                        removeQueueLocally(entryId);
                        rebuildWidgetsAndLayout();
                    }
                    return true;
                }
            }
        }

        for (var entry : queueBounds.entrySet()) {
            NodeBounds bounds = entry.getValue();
            if (bounds.contains(mouseX, mouseY)) {
                String entryId = entry.getKey();
                boolean startDrag = button == 0;
                int startIndex = queue.indexOf(entryId);
                selectedTechId = entryId;
                rebuildWidgetsAndLayout();
                if (startDrag) {
                    draggingQueueEntryId = entryId;
                    draggingQueueOriginalIndex = startIndex;
                    draggingQueueStartIndex = startIndex;
                    draggingMouseX = mouseX;
                    draggingMouseY = mouseY;
                    NodeBounds refreshedBounds = queueBounds.get(entryId);
                    if (refreshedBounds != null) {
                        draggingQueueOffsetX = (int) Math.round(mouseX - refreshedBounds.x());
                        draggingQueueOffsetY = (int) Math.round(mouseY - refreshedBounds.y());
                    } else {
                        draggingQueueOffsetX = TECH_LIST_CARD_W / 2;
                        draggingQueueOffsetY = TECH_LIST_CARD_H / 2;
                    }
                }
                return true;
            }
        }

        for (var entry : techListBounds.entrySet()) {
            NodeBounds bounds = entry.getValue();
            if (bounds.contains(mouseX, mouseY)) {
                selectedTechId = entry.getKey();
                rebuildWidgetsAndLayout();
                return true;
            }
        }

        TreeViewport viewport = treeViewport();
        boolean inTreeViewport = viewport.contains(mouseX, mouseY);
        for (var entry : nodeBounds.entrySet()) {
            NodeBounds bounds = shifted(entry.getValue(), treePanX, treePanY);
            TechEntry techEntry = entryById.get(entry.getKey());
            if (inTreeViewport && bounds.contains(mouseX, mouseY) && matchesSearch(techEntry)) {
                selectedTechId = entry.getKey();
                rebuildWidgetsAndLayout();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingQueueEntryId != null) {
            draggingMouseX = mouseX;
            draggingMouseY = mouseY;
            int toIndex = queueDropIndex(mouseX, mouseY);
            if (toIndex >= 0 && draggingQueueStartIndex != toIndex && canReorderQueue(draggingQueueStartIndex, toIndex)) {
                moveQueueLocally(draggingQueueStartIndex, toIndex);
                draggingQueueStartIndex = toIndex;
                buildQueueLayout();
                buildTechListLayout();
            }
            return true;
        }

        if (button == 0 && activeTab == ResearchTab.TECH_TREE) {
            TreeViewport viewport = treeViewport();
            if (viewport.contains(mouseX, mouseY)) {
                treePanX += (int) Math.round(dragX);
                treePanY += (int) Math.round(dragY);
                clampTreePan();
                return true;
            }
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingQueueEntryId != null) {
            if (draggingQueueOriginalIndex >= 0
                    && draggingQueueStartIndex >= 0
                    && draggingQueueOriginalIndex != draggingQueueStartIndex) {
                ResearchNetworking.requestMoveQueue(draggingQueueOriginalIndex, draggingQueueStartIndex);
            }
            draggingQueueEntryId = null;
            draggingQueueOriginalIndex = -1;
            draggingQueueStartIndex = -1;
            draggingQueueOffsetX = 0;
            draggingQueueOffsetY = 0;
            rebuildWidgetsAndLayout();
            return true;
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        drawPanel(guiGraphics, guiX, guiY, GUI_WIDTH, GUI_HEIGHT, 0xFF2E2F33, 0xFF54575E);
        drawTabBackground(guiGraphics);

        if (activeTab == ResearchTab.TECH_TREE) {
            drawPanel(guiGraphics, guiX + 4, guiY + QUEUE_PANEL_Y, LEFT_PANEL_WIDTH - 6, QUEUE_PANEL_H, 0xFF26272B, 0xFF474A51);
            drawPanel(guiGraphics, guiX + 4, guiY + SEARCH_PANEL_Y, LEFT_PANEL_WIDTH - 6, SEARCH_PANEL_H, 0xFF26272B, 0xFF474A51);
            drawPanel(guiGraphics, guiX + 4, guiY + DETAILS_PANEL_Y, LEFT_PANEL_WIDTH - 6, DETAILS_PANEL_H, 0xFF26272B, 0xFF474A51);
            drawPanel(guiGraphics, guiX + 4, guiY + LIST_PANEL_Y, LEFT_PANEL_WIDTH - 6, LIST_PANEL_H, 0xFF26272B, 0xFF474A51);
            drawPanel(guiGraphics, guiX + LEFT_PANEL_WIDTH, guiY + 4, GUI_WIDTH - LEFT_PANEL_WIDTH - 4, GUI_HEIGHT - 8, 0xFF26272B, 0xFF474A51);
            drawResearchQueue(guiGraphics, mouseX, mouseY);
            drawSelectedTechnologyPanel(guiGraphics);
            drawTechnologyList(guiGraphics);
            drawTechnologyTree(guiGraphics);
        } else {
            drawPanel(guiGraphics, guiX + 4, guiY + MANUAL_PANEL_Y, LEFT_PANEL_WIDTH - 6, MANUAL_PANEL_H, 0xFF26272B, 0xFF474A51);
            drawPanel(guiGraphics, guiX + LEFT_PANEL_WIDTH, guiY + 4, GUI_WIDTH - LEFT_PANEL_WIDTH - 4, GUI_HEIGHT - 8, 0xFF26272B, 0xFF474A51);
            drawManualResearchTab(guiGraphics);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void drawTabBackground(GuiGraphics guiGraphics) {
        drawPanel(guiGraphics, guiX + 4, guiY + 4, LEFT_PANEL_WIDTH - 6, 20, 0xFF1E2025, 0xFF4A4D55);
    }

    private void drawResearchQueue(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, Component.translatable("screen.incore.research.queue"), guiX + 10, guiY + QUEUE_PANEL_Y + 4, 0xFFE7D9BA, false);
        if (queue.isEmpty()) {
            guiGraphics.drawString(font, Component.literal("-"), guiX + 10, guiY + QUEUE_PANEL_Y + 18, 0xFFB7BCC6, false);
            return;
        }

        for (int i = 0; i < queue.size(); i++) {
            String entryId = queue.get(i);
            TechEntry queued = entryById.get(entryId);
            NodeBounds bounds = queueBounds.get(entryId);
            if (queued == null || bounds == null) {
                continue;
            }

            if (entryId.equals(draggingQueueEntryId)) {
                drawPanel(guiGraphics, bounds.x(), bounds.y(), bounds.width(), bounds.height(), 0xFF2E323C, 0xFF6A717D);
                continue;
            }

            int fill = isActiveEntry(entryId) ? 0xFF256EAB : 0xFF7A6A2D;
            int border = isActiveEntry(entryId) ? 0xFF9FD2F6 : 0xFFE5D189;
            drawPanel(guiGraphics, bounds.x(), bounds.y(), bounds.width(), bounds.height(), fill, border);
            guiGraphics.renderItem(techIcon(queued), bounds.x() + 7, bounds.y() + 6);
            drawMiniProgressBar(guiGraphics, bounds, progressByEntry.getOrDefault(queued.id(), 0), queued.cost(), true);

            if (entryId.equals(selectedTechId)) {
                drawSelectionOutline(guiGraphics, bounds);
            }

            NodeBounds removeBounds = queueRemoveBounds.get(entryId);
            if (removeBounds != null) {
                boolean hover = removeBounds.contains(mouseX, mouseY);
                int removeFill = hover ? 0xFF9B2A2A : 0xFF2A2F39;
                int removeBorder = hover ? 0xFFFF9A9A : 0xFF5E6878;
                drawPanel(guiGraphics, removeBounds.x(), removeBounds.y(), removeBounds.width(), removeBounds.height(), removeFill, removeBorder);
                guiGraphics.drawCenteredString(font, Component.literal("x"), removeBounds.centerX(), removeBounds.y() + 1, 0xFFFFFFFF);
            }
        }

        if (draggingQueueEntryId != null) {
            TechEntry dragging = entryById.get(draggingQueueEntryId);
            if (dragging != null) {
                int drawX = (int) Math.round(draggingMouseX) - draggingQueueOffsetX;
                int drawY = (int) Math.round(draggingMouseY) - draggingQueueOffsetY;
                drawPanel(guiGraphics, drawX, drawY, TECH_LIST_CARD_W, TECH_LIST_CARD_H, 0xCC7A6A2D, 0xFFE5D189);
                guiGraphics.renderItem(techIcon(dragging), drawX + 7, drawY + 6);
            }
        }
    }

    private void drawSelectedTechnologyPanel(GuiGraphics guiGraphics) {
        TechEntry selected = selectedEntry();
        if (selected == null) {
            return;
        }

        int panelX = guiX + 8;
        int panelY = guiY + DETAILS_PANEL_Y + 4;
        int contentWidth = LEFT_PANEL_WIDTH - 14;
        int iconPanelW = 48;
        int iconPanelTop = panelY + 12;
        int topRowHeight = 56;
        int buttonTop = guiY + DETAILS_PANEL_Y + DETAILS_PANEL_H - 22;
        int iconPanelH = topRowHeight;
        int infoX = panelX + iconPanelW + 6;
        int infoW = Math.max(84, contentWidth - iconPanelW - 6);

        String titleText = trimToWidth(labelWithState(selected), contentWidth);
        guiGraphics.drawString(font, Component.literal(titleText), panelX, panelY, titleColor(selected), false);

        drawPanel(guiGraphics, panelX, iconPanelTop, iconPanelW, iconPanelH, 0xFFD6C89A, 0xFF8F7E53);
        guiGraphics.fill(panelX + 1, iconPanelTop + iconPanelH - 12, panelX + iconPanelW - 1, iconPanelTop + iconPanelH - 1, 0xFF87731D);
        guiGraphics.renderItem(techIcon(selected), panelX + 19, iconPanelTop + 20);

        drawPanel(guiGraphics, infoX, iconPanelTop, infoW, 22, 0xFF3A3B3F, 0xFF5A5D66);
        guiGraphics.drawString(font, Component.translatable("screen.incore.research.cost_label"), infoX + 6, iconPanelTop + 5, 0xFFE7D9BA, false);
        guiGraphics.drawString(font, Component.literal(String.valueOf(selected.cost())), infoX + 40, iconPanelTop + 5, 0xFFD6E3FF, false);
        if (!selected.researchMaterials().isEmpty()) {
            TechMaterial primaryMaterial = selected.researchMaterials().get(0);
            ItemStack materialIcon = itemStackFromId(primaryMaterial.itemId());
            if (!materialIcon.isEmpty()) {
                int iconX = infoX + infoW - 38;
                guiGraphics.renderItem(materialIcon, iconX, iconPanelTop + 3);
                String materialCount = "x" + Math.max(1, primaryMaterial.itemCount());
                guiGraphics.drawString(font, Component.literal(materialCount), iconX + 17, iconPanelTop + 7, 0xFFCDD3DE, false);
            }
        }

        int reqPanelY = iconPanelTop + 24;
        drawPanel(guiGraphics, infoX, reqPanelY, infoW, 20, 0xFF3A3B3F, 0xFF5A5D66);
        guiGraphics.drawString(font, Component.literal("Req"), infoX + 6, reqPanelY + 6, 0xFFE7D9BA, false);

        int effectX = infoX + 28;
        int effectY = reqPanelY + 2;
        int renderedEffects = 0;
        for (TechMaterial material : selected.researchMaterials()) {
            ItemStack materialIcon = itemStackFromId(material.itemId());
            if (materialIcon.isEmpty()) {
                continue;
            }
            if (effectX + 16 > infoX + infoW - 4) {
                break;
            }
            guiGraphics.renderItem(materialIcon, effectX, effectY);
            String countText = String.valueOf(Math.max(1, material.itemCount()));
            guiGraphics.drawString(font, Component.literal(countText), effectX + 9, effectY + 8, 0xFFE4E9F2, false);
            effectX += 17;
            renderedEffects++;
        }

        for (String taskId : selected.requiredTasks()) {
            ManualTaskEntry task = taskById.get(taskId);
            if (task == null) {
                continue;
            }
            if (effectX + 16 > infoX + infoW - 4) {
                break;
            }

            int color = completedTasks.contains(taskId) ? 0xFF53C67A : 0xFFD05E5E;
            guiGraphics.fill(effectX - 1, effectY - 1, effectX + 15, effectY + 15, color);
            guiGraphics.renderItem(taskIcon(task), effectX, effectY);
            effectX += 17;
            renderedEffects++;
        }

        if (renderedEffects == 0) {
            guiGraphics.drawString(font, Component.literal("-"), infoX + 28, reqPanelY + 6, 0xFFB7BCC6, false);
        }

        int progressPanelY = reqPanelY + 22;
        int progressWidth = infoW - 8;
        int selectedProgress = progressByEntry.getOrDefault(selected.id(), 0);
        int progressCost = Math.max(selected.cost(), 1);
        int filled = Math.min(progressWidth, (selectedProgress * progressWidth) / progressCost);
        drawPanel(guiGraphics, infoX, progressPanelY, infoW, 12, 0xFF1B1E23, 0xFF3E434E);
        guiGraphics.fill(infoX + 2, progressPanelY + 2, infoX + 2 + progressWidth, progressPanelY + 10, 0xFF2A2E35);
        if (filled > 0) {
            guiGraphics.fill(infoX + 2, progressPanelY + 2, infoX + 2 + filled, progressPanelY + 10, 0xFF42C86F);
        }
        guiGraphics.drawCenteredString(font, Component.translatable("screen.incore.research.progress", Math.max(0, selectedProgress), progressCost), infoX + (infoW / 2), progressPanelY + 2, 0xFFE4E9F2);

        int descriptionPanelY = iconPanelTop + topRowHeight + 4;
        int descriptionPanelH = Math.max(0, buttonTop - descriptionPanelY - 2);
        if (descriptionPanelH >= 14) {
            drawPanel(guiGraphics, panelX, descriptionPanelY, contentWidth, descriptionPanelH, 0xFF2D3037, 0xFF50545E);
            List<FormattedCharSequence> wrapped = font.split(Component.literal(selected.description()), contentWidth - 8);
            int descY = descriptionPanelY + 4;
            int maxDescY = descriptionPanelY + descriptionPanelH - 2 - font.lineHeight;
            for (FormattedCharSequence line : wrapped) {
                if (descY > maxDescY) {
                    break;
                }
                guiGraphics.drawString(font, line, panelX + 4, descY, 0xFFCED3DF);
                descY += font.lineHeight;
            }
        }
    }

    private void drawTechnologyList(GuiGraphics guiGraphics) {
        int panelX = guiX + 10;
        int panelY = guiY + LIST_PANEL_Y + 6;
        guiGraphics.drawString(font, Component.translatable("screen.incore.research.list"), panelX, panelY, 0xFFE7D9BA, false);

        for (TechEntry entry : orderedTechListEntries()) {
            NodeBounds bounds = techListBounds.get(entry.id());
            if (bounds == null) {
                continue;
            }

            int fill;
            int border;
            if (unlocked.contains(entry.id())) {
                fill = 0xFF1D8F4A;
                border = 0xFF95E2B0;
            } else if (isActiveEntry(entry.id())) {
                fill = 0xFF256EAB;
                border = 0xFF9FD2F6;
            } else if (queue.contains(entry.id())) {
                fill = 0xFF7A6A2D;
                border = 0xFFE5D189;
            } else if (canQueue(entry)) {
                fill = 0xFF7A6A2D;
                border = 0xFFE5D189;
            } else {
                fill = 0xFF8D3B46;
                border = 0xFFF1A4AD;
            }

            drawPanel(guiGraphics, bounds.x(), bounds.y(), bounds.width(), bounds.height(), fill, border);
            guiGraphics.renderItem(techIcon(entry), bounds.x() + 7, bounds.y() + 6);
            drawMiniProgressBar(guiGraphics, bounds, progressByEntry.getOrDefault(entry.id(), 0), entry.cost(), false);

            if (entry.id().equals(selectedTechId)) {
                drawSelectionOutline(guiGraphics, bounds);
            }
        }
    }

    private void drawManualResearchTab(GuiGraphics guiGraphics) {
        int leftX = guiX + 10;
        int leftY = guiY + MANUAL_PANEL_Y + 8;
        guiGraphics.drawString(font, Component.translatable("screen.incore.research.manual.title"), leftX, leftY, 0xFFE7D9BA, false);
        guiGraphics.drawString(font, Component.literal("Select a task from the list."), leftX, leftY + 12, 0xFFB7BCC6, false);

        int rightX = guiX + LEFT_PANEL_WIDTH + 10;
        int rightY = guiY + 12;
        int rightW = GUI_WIDTH - LEFT_PANEL_WIDTH - 20;

        ManualTaskEntry selected = selectedTask();
        if (selected == null) {
            guiGraphics.drawString(font, Component.translatable("screen.incore.research.manual.none"), rightX + 12, rightY + 12, 0xFFCDD3DE, false);
            return;
        }

        boolean done = completedTasks.contains(selected.id());
        guiGraphics.drawString(font, Component.literal(trimToWidth(selected.title(), rightW - 24)), rightX + 10, rightY + 8, 0xFFE7D9BA, false);
        guiGraphics.drawString(
                font,
                Component.translatable(done ? "screen.incore.research.manual.status.completed" : "screen.incore.research.manual.status.pending"),
                rightX + 10,
                rightY + 22,
                done ? 0xFF6FDB91 : 0xFFE38A8A,
                false
        );

        int summaryY = rightY + 36;
        drawPanel(guiGraphics, rightX + 10, summaryY, rightW - 20, 74, 0xFF2A2D34, 0xFF555A67);
        drawPanel(guiGraphics, rightX + 16, summaryY + 8, 44, 44, 0xFF1F2530, 0xFF4A5365);
        guiGraphics.renderItem(taskIcon(selected), rightX + 30, summaryY + 22);

        int infoX = rightX + 70;
        guiGraphics.drawString(font, Component.translatable("screen.incore.research.manual.item"), infoX, summaryY + 10, 0xFFD6E3FF, false);
        guiGraphics.drawString(font, Component.literal(trimToWidth(selected.itemId().isBlank() ? "N/A" : selected.itemId(), rightW - 86)), infoX, summaryY + 22, 0xFFCDD3DE, false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.research.manual.count", selected.itemCount()), infoX, summaryY + 34, 0xFFCDD3DE, false);
        guiGraphics.drawString(
                font,
                Component.translatable("screen.incore.research.manual.repeatable", selected.repeatable() ? "yes" : "no"),
                infoX,
                summaryY + 46,
                0xFFCDD3DE,
                false
        );

        int descriptionY = summaryY + 78;
        int descriptionH = GUI_HEIGHT - 26 - (descriptionY - guiY);
        drawPanel(guiGraphics, rightX + 10, descriptionY, rightW - 20, descriptionH, 0xFF23262C, 0xFF494D57);
        List<FormattedCharSequence> wrapped = font.split(Component.literal(selected.description()), rightW - 30);
        int descY = descriptionY + 6;
        int maxDescY = descriptionY + descriptionH - 6 - font.lineHeight;
        for (FormattedCharSequence line : wrapped) {
            if (descY > maxDescY) {
                break;
            }
            guiGraphics.drawString(font, line, rightX + 15, descY, 0xFFCED3DF);
            descY += font.lineHeight;
        }
    }

    private void drawTechnologyTree(GuiGraphics guiGraphics) {
        int treeX = guiX + LEFT_PANEL_WIDTH + 8;
        int treeY = guiY + 12;
        int treeW = GUI_WIDTH - LEFT_PANEL_WIDTH - 16;
        int treeH = GUI_HEIGHT - 20;
        TreeViewport viewport = treeViewport();

        guiGraphics.drawString(font, Component.translatable("screen.incore.research.tree"), treeX, treeY - 6, 0xFFE7D9BA, false);
        drawPanel(guiGraphics, treeX, treeY + 8, treeW, treeH - 14, 0xFF201E1B, 0xFF3A3B41);
        guiGraphics.enableScissor(viewport.x(), viewport.y(), viewport.x() + viewport.width(), viewport.y() + viewport.height());

        for (TechEntry entry : entries) {
            if (!matchesSearch(entry)) {
                continue;
            }
            NodeBounds target = shifted(nodeBounds.get(entry.id()), treePanX, treePanY);
            if (target == null) {
                continue;
            }
            for (String prereq : entry.prerequisites()) {
                TechEntry sourceEntry = entryById.get(prereq);
                if (!matchesSearch(sourceEntry)) {
                    continue;
                }
                NodeBounds source = shifted(nodeBounds.get(prereq), treePanX, treePanY);
                if (source == null) {
                    continue;
                }
                int lineColor = unlocked.contains(prereq) ? 0xFF69CB86 : 0xFF8A8D94;
                drawConnector(guiGraphics, source.centerX(), source.centerY(), target.centerX(), target.centerY(), lineColor);
            }
        }

        for (TechEntry entry : entries) {
            if (!matchesSearch(entry)) {
                continue;
            }
            NodeBounds bounds = shifted(nodeBounds.get(entry.id()), treePanX, treePanY);
            if (bounds == null) {
                continue;
            }

            int fill;
            int border;
            if (unlocked.contains(entry.id())) {
                fill = 0xFF1D8F4A;
                border = 0xFF95E2B0;
            } else if (isActiveEntry(entry.id())) {
                fill = 0xFF2676AD;
                border = 0xFF9FD2F6;
            } else if (queue.contains(entry.id())) {
                fill = 0xFF7A6A2D;
                border = 0xFFE5D189;
            } else if (canQueue(entry)) {
                fill = 0xFF7A6A2D;
                border = 0xFFE5D189;
            } else {
                fill = 0xFF8D3B46;
                border = 0xFFF1A4AD;
            }

            drawPanel(guiGraphics, bounds.x(), bounds.y(), bounds.width(), bounds.height(), fill, border);

            guiGraphics.renderItem(techIcon(entry), bounds.x() + 4, bounds.y() + 11);
            String level = String.valueOf(entry.prerequisites().size() + 1);
            guiGraphics.drawString(font, level, bounds.x() + 3, bounds.y() + bounds.height() - 11, 0xFFF6F6F6, false);
            guiGraphics.drawCenteredString(font, Component.literal(shortLabel(entry.title(), 11)), bounds.x() + 56, bounds.y() + 7, 0xFFF6F6F6);
            guiGraphics.drawCenteredString(font, Component.literal(String.valueOf(entry.cost())), bounds.x() + 56, bounds.y() + 21, 0xFFE7D9BA);
            drawNodeProgressBar(guiGraphics, bounds, progressByEntry.getOrDefault(entry.id(), 0), entry.cost());

            if (entry.id().equals(selectedTechId)) {
                drawSelectionOutline(guiGraphics, bounds);
            }
        }

        guiGraphics.disableScissor();
    }

    private void rebuildWidgetsAndLayout() {
        guiX = (width - GUI_WIDTH) / 2;
        guiY = (height - GUI_HEIGHT) / 2;
        ensureSelections();
        buildNodeLayout();
        clampTreePan();
        buildQueueLayout();
        buildTechListLayout();
        boolean restoreSearchFocus = techSearchBox != null && techSearchBox.isFocused();

        clearWidgets();
        startResearchButton = null;
        submitManualTaskButton = null;
        tabTechTreeButton = null;
        tabManualResearchButton = null;
        techSearchBox = null;
        draggingQueueEntryId = null;
        draggingQueueOriginalIndex = -1;
        draggingQueueStartIndex = -1;
        draggingQueueOffsetX = 0;
        draggingQueueOffsetY = 0;

        tabTechTreeButton = addRenderableWidget(Button.builder(Component.translatable("screen.incore.research.tab.tech_tree"), b -> {
            activeTab = ResearchTab.TECH_TREE;
            rebuildWidgetsAndLayout();
        }).bounds(guiX + 10, guiY + TAB_BUTTON_Y, 94, TAB_BUTTON_H).build());
        tabTechTreeButton.active = activeTab != ResearchTab.TECH_TREE;

        tabManualResearchButton = addRenderableWidget(Button.builder(Component.translatable("screen.incore.research.tab.manual"), b -> {
            activeTab = ResearchTab.MANUAL_RESEARCH;
            rebuildWidgetsAndLayout();
        }).bounds(guiX + 108, guiY + TAB_BUTTON_Y, 106, TAB_BUTTON_H).build());
        tabManualResearchButton.active = activeTab != ResearchTab.MANUAL_RESEARCH;

        if (activeTab == ResearchTab.TECH_TREE) {
            techSearchBox = addRenderableWidget(new EditBox(
                    font,
                    guiX + 10,
                    guiY + SEARCH_PANEL_Y + 2,
                    LEFT_PANEL_WIDTH - 18,
                    14,
                    Component.translatable("screen.incore.research.search")
            ));
            techSearchBox.setMaxLength(64);
            techSearchBox.setValue(techSearchQuery);
            techSearchBox.setResponder(value -> {
                techSearchQuery = value;
                scheduleSearchApply(value);
            });
            if (restoreSearchFocus) {
                techSearchBox.setFocused(true);
                setFocused(techSearchBox);
            }

            TechEntry selected = selectedEntry();
            if (selected != null) {
                startResearchButton = addRenderableWidget(Button.builder(Component.translatable("screen.incore.research.start"), b -> {
                    ResourceLocation id = ResourceLocation.tryParse(selected.id());
                    if (id != null) {
                        ResearchNetworking.requestUnlock(id);
                    }
                }).bounds(guiX + 12, guiY + DETAILS_PANEL_Y + DETAILS_PANEL_H - 22, 188, 20).build());
                startResearchButton.active = canQueue(selected);
            }
            return;
        }

        int taskY = guiY + MANUAL_PANEL_Y + 30;
        int maxTaskY = guiY + GUI_HEIGHT - 30;
        for (ManualTaskEntry task : tasks) {
            Button taskButton = addRenderableWidget(Button.builder(manualTaskLabel(task), b -> {
                selectedTaskId = task.id();
                rebuildWidgetsAndLayout();
            }).bounds(guiX + 12, taskY, 188, 20).build());
            taskButton.active = !task.id().equals(selectedTaskId);
            taskY += 22;
            if (taskY > maxTaskY) {
                break;
            }
        }

        ManualTaskEntry selectedTask = selectedTask();
        if (selectedTask != null) {
            submitManualTaskButton = addRenderableWidget(Button.builder(Component.translatable("screen.incore.research.submit_tasks"), b -> {
                ResourceLocation id = ResourceLocation.tryParse(selectedTask.id());
                if (id != null) {
                    ResearchNetworking.requestTaskSubmit(id);
                }
            }).bounds(guiX + LEFT_PANEL_WIDTH + 16, guiY + GUI_HEIGHT - 26, GUI_WIDTH - LEFT_PANEL_WIDTH - 34, 20).build());
            submitManualTaskButton.active = selectedTask.repeatable() || !completedTasks.contains(selectedTask.id());
        }
    }

    private void buildQueueLayout() {
        queueBounds.clear();
        queueRemoveBounds.clear();
        int startX = queueStartX();
        int y = guiY + QUEUE_PANEL_Y + 18;
        int maxColumns = queueVisibleColumns();

        for (int i = 0; i < queue.size() && i < maxColumns; i++) {
            String entryId = queue.get(i);
            int x = startX + i * (TECH_LIST_CARD_W + TECH_LIST_CARD_GAP);
            queueBounds.put(entryId, new NodeBounds(x, y, TECH_LIST_CARD_W, TECH_LIST_CARD_H));
            int removeW = 10;
            int removeH = 8;
            int removeX = x + (TECH_LIST_CARD_W - removeW) / 2;
            int removeY = y + TECH_LIST_CARD_H - removeH - 1;
            queueRemoveBounds.put(entryId, new NodeBounds(removeX, removeY, removeW, removeH));
        }
    }

    private int queueDropIndex(double mouseX, double mouseY) {
        if (queue.isEmpty()) {
            return -1;
        }
        int visibleCount = Math.min(queue.size(), queueVisibleColumns());
        if (visibleCount <= 0) {
            return -1;
        }
        double firstCenter = queueStartX() + (TECH_LIST_CARD_W / 2.0);
        double step = TECH_LIST_CARD_W + TECH_LIST_CARD_GAP;
        int snapped = (int) Math.round((mouseX - firstCenter) / step);
        return Math.clamp(snapped, 0, visibleCount - 1);
    }

    private void moveQueueLocally(int fromIndex, int toIndex) {
        if (fromIndex < 0 || toIndex < 0 || fromIndex >= queue.size() || toIndex >= queue.size() || fromIndex == toIndex) {
            return;
        }
        if (!canReorderQueue(fromIndex, toIndex)) {
            return;
        }
        String moved = queue.remove(fromIndex);
        queue.add(toIndex, moved);
    }

    private void removeQueueLocally(String entryId) {
        queue.removeIf(entryId::equals);
        queueBounds.remove(entryId);
        queueRemoveBounds.remove(entryId);
    }

    private int queueStartX() {
        return guiX + 10;
    }

    private int queueVisibleColumns() {
        return Math.max(1, (LEFT_PANEL_WIDTH - 20) / (TECH_LIST_CARD_W + TECH_LIST_CARD_GAP));
    }

    private void buildTechListLayout() {
        techListBounds.clear();
        int startX = guiX + 10;
        int startY = guiY + LIST_PANEL_Y + 20;
        int maxRows = (LIST_PANEL_H - 24) / (TECH_LIST_CARD_H + TECH_LIST_CARD_GAP);
        int maxEntries = Math.max(0, maxRows * TECH_LIST_COLUMNS);
        List<TechEntry> ordered = orderedTechListEntries();

        for (int i = 0; i < ordered.size() && i < maxEntries; i++) {
            TechEntry entry = ordered.get(i);
            int row = i / TECH_LIST_COLUMNS;
            int col = i % TECH_LIST_COLUMNS;
            int x = startX + col * (TECH_LIST_CARD_W + TECH_LIST_CARD_GAP);
            int y = startY + row * (TECH_LIST_CARD_H + TECH_LIST_CARD_GAP);
            techListBounds.put(entry.id(), new NodeBounds(x, y, TECH_LIST_CARD_W, TECH_LIST_CARD_H));
        }
    }

    private void ensureSelections() {
        if (selectedTechId == null || !entryById.containsKey(selectedTechId)) {
            if (activeResearchId != null && entryById.containsKey(activeResearchId)) {
                selectedTechId = activeResearchId;
            } else if (!queue.isEmpty() && entryById.containsKey(queue.get(0))) {
                selectedTechId = queue.get(0);
            } else {
                selectedTechId = entries.isEmpty() ? null : entries.get(0).id();
            }
        }

        if (selectedTaskId == null || !taskById.containsKey(selectedTaskId)) {
            selectedTaskId = tasks.isEmpty() ? null : tasks.get(0).id();
        }
    }

    private @Nullable ManualTaskEntry selectedTask() {
        if (selectedTaskId == null) {
            return null;
        }
        return taskById.get(selectedTaskId);
    }

    private void applyPayload(JsonObject payload) {
        activeResearchId = payload.has("active_research") ? payload.get("active_research").getAsString() : null;
        if (activeResearchId != null && activeResearchId.isBlank()) {
            activeResearchId = null;
        }

        unlocked.clear();
        readStringArray(payload.getAsJsonArray("unlocked"), unlocked);

        completedTasks.clear();
        readStringArray(payload.getAsJsonArray("completed_tasks"), completedTasks);

        queue.clear();
        readStringArray(payload.getAsJsonArray("queue"), queue);

        progressByEntry.clear();
        JsonObject progressObject = payload.getAsJsonObject("progress");
        if (progressObject != null) {
            for (var entry : progressObject.entrySet()) {
                String id = entry.getKey();
                int value = Math.max(0, entry.getValue().getAsInt());
                if (value > 0) {
                    progressByEntry.put(id, value);
                }
            }
        } else if (!queue.isEmpty()) {
            int activeProgress = getInt(payload, "active_progress");
            if (activeProgress > 0) {
                progressByEntry.put(queue.get(0), activeProgress);
            }
        }

        entries.clear();
        entryById.clear();
        JsonArray entriesArray = payload.getAsJsonArray("entries");
        if (entriesArray != null) {
            for (JsonElement element : entriesArray) {
                JsonObject entry = element.getAsJsonObject();
                String id = entry.get("id").getAsString();
                TechEntry tech = new TechEntry(
                        id,
                        entry.get("title").getAsString(),
                        entry.get("description").getAsString(),
                        getInt(entry, "cost"),
                        entry.has("icon_item") ? entry.get("icon_item").getAsString() : "",
                        getInt(entry, "run_duration_ticks"),
                        readStringList(entry.getAsJsonArray("unlocks")),
                        readMaterialList(entry.getAsJsonArray("research_materials")),
                        readStringList(entry.getAsJsonArray("prerequisites")),
                        readStringList(entry.getAsJsonArray("required_tasks"))
                );
                entries.add(tech);
                entryById.put(id, tech);
            }
        }

        entries.sort(Comparator.comparing(TechEntry::id));

        tasks.clear();
        taskById.clear();
        JsonArray tasksArray = payload.getAsJsonArray("tasks");
        if (tasksArray != null) {
            for (JsonElement element : tasksArray) {
                JsonObject task = element.getAsJsonObject();
                ManualTaskEntry taskEntry = new ManualTaskEntry(
                        task.get("id").getAsString(),
                        task.get("title").getAsString(),
                        task.get("description").getAsString(),
                        task.has("item") ? task.get("item").getAsString() : "",
                        getInt(task, "count"),
                        task.get("repeatable").getAsBoolean()
                );
                tasks.add(taskEntry);
                taskById.put(taskEntry.id(), taskEntry);
            }
        }

        tasks.sort(Comparator.comparing(ManualTaskEntry::id));

        if (selectedTechId == null || !entryById.containsKey(selectedTechId)) {
            if (activeResearchId != null && entryById.containsKey(activeResearchId)) {
                selectedTechId = activeResearchId;
            } else if (!queue.isEmpty() && entryById.containsKey(queue.get(0))) {
                selectedTechId = queue.get(0);
            } else {
                selectedTechId = entries.isEmpty() ? null : entries.get(0).id();
            }
        }
    }

    private void buildNodeLayout() {
        nodeBounds.clear();
        if (entries.isEmpty()) {
            return;
        }

        Map<String, Integer> depthCache = new HashMap<>();
        for (TechEntry entry : entries) {
            computeDepth(entry.id(), depthCache, new ArrayDeque<>());
        }

        Map<Integer, List<TechEntry>> byDepth = new LinkedHashMap<>();
        int maxDepth = 0;
        for (TechEntry entry : entries) {
            int depth = depthCache.getOrDefault(entry.id(), 0);
            byDepth.computeIfAbsent(depth, ignored -> new ArrayList<>()).add(entry);
            maxDepth = Math.max(maxDepth, depth);
        }

        byDepth.values().forEach(list -> list.sort(Comparator.comparing(TechEntry::title)));

        TreeViewport viewport = treeViewport();
        int left = viewport.x() + TREE_LAYOUT_PADDING_X;
        int top = viewport.y() + TREE_LAYOUT_PADDING_Y;
        int right = viewport.x() + viewport.width() - TREE_LAYOUT_PADDING_X - TREE_NODE_W;
        int bottom = viewport.y() + viewport.height() - TREE_LAYOUT_PADDING_Y - TREE_NODE_H;
        if (right < left) {
            right = left;
        }
        if (bottom < top) {
            bottom = top;
        }

        int fitXStep = maxDepth <= 0 ? 0 : Math.max(1, (right - left) / maxDepth);
        int xStep = Math.max(TREE_MIN_X_STEP, fitXStep);
        int availableY = Math.max(0, bottom - top);

        for (var depthEntry : byDepth.entrySet()) {
            int depth = depthEntry.getKey();
            List<TechEntry> list = depthEntry.getValue();
            int x = maxDepth == 0 ? left + Math.max(0, (right - left) / 2) : left + depth * xStep;
            int fitYStep = list.size() <= 1 ? 0 : Math.max(1, availableY / (list.size() - 1));
            int yStep = Math.max(TREE_MIN_Y_STEP, fitYStep);
            for (int i = 0; i < list.size(); i++) {
                int y;
                if (list.size() == 1) {
                    y = top + (availableY / 2);
                } else {
                    y = top + i * yStep;
                }
                nodeBounds.put(list.get(i).id(), new NodeBounds(x, y, TREE_NODE_W, TREE_NODE_H));
            }
        }
    }

    private void clampTreePan() {
        if (nodeBounds.isEmpty()) {
            treePanX = 0;
            treePanY = 0;
            return;
        }

        TreeViewport viewport = treeViewport();
        if (viewport.width() <= 0 || viewport.height() <= 0) {
            treePanX = 0;
            treePanY = 0;
            return;
        }

        int contentMinX = Integer.MAX_VALUE;
        int contentMinY = Integer.MAX_VALUE;
        int contentMaxX = Integer.MIN_VALUE;
        int contentMaxY = Integer.MIN_VALUE;
        for (NodeBounds bounds : nodeBounds.values()) {
            contentMinX = Math.min(contentMinX, bounds.x());
            contentMinY = Math.min(contentMinY, bounds.y());
            contentMaxX = Math.max(contentMaxX, bounds.x() + bounds.width());
            contentMaxY = Math.max(contentMaxY, bounds.y() + bounds.height());
        }

        int contentW = Math.max(0, contentMaxX - contentMinX);
        int contentH = Math.max(0, contentMaxY - contentMinY);

        if (contentW <= viewport.width()) {
            treePanX = 0;
        } else {
            int minPanX = viewport.x() + viewport.width() - TREE_PAN_PADDING - contentMaxX;
            int maxPanX = viewport.x() + TREE_PAN_PADDING - contentMinX;
            treePanX = Math.clamp(treePanX, minPanX, maxPanX);
        }

        if (contentH <= viewport.height()) {
            treePanY = 0;
        } else {
            int minPanY = viewport.y() + viewport.height() - TREE_PAN_PADDING - contentMaxY;
            int maxPanY = viewport.y() + TREE_PAN_PADDING - contentMinY;
            treePanY = Math.clamp(treePanY, minPanY, maxPanY);
        }
    }

    private int computeDepth(String id, Map<String, Integer> cache, Deque<String> stack) {
        if (cache.containsKey(id)) {
            return cache.get(id);
        }
        if (stack.contains(id)) {
            return 0;
        }

        TechEntry entry = entryById.get(id);
        if (entry == null || entry.prerequisites().isEmpty()) {
            cache.put(id, 0);
            return 0;
        }

        stack.push(id);
        int depth = 0;
        for (String prereq : entry.prerequisites()) {
            depth = Math.max(depth, computeDepth(prereq, cache, stack) + 1);
        }
        stack.pop();

        cache.put(id, depth);
        return depth;
    }

    private @Nullable TechEntry selectedEntry() {
        if (selectedTechId == null) {
            return null;
        }
        return entryById.get(selectedTechId);
    }

    private boolean isActiveEntry(@Nullable String id) {
        return id != null && activeResearchId != null && activeResearchId.equals(id);
    }

    private boolean matchesSearch(@Nullable TechEntry entry) {
        if (entry == null) {
            return false;
        }
        String query = appliedTechSearchQuery == null ? "" : appliedTechSearchQuery.strip().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            return true;
        }
        if (entry.title().toLowerCase(Locale.ROOT).contains(query)) {
            return true;
        }
        for (String unlockedThing : entry.unlocks()) {
            if (unlockedThing.toLowerCase(Locale.ROOT).contains(query)) {
                return true;
            }
        }
        return false;
    }

    private void scheduleSearchApply(String query) {
        pendingTechSearchQuery = query;
        pendingTechSearchApplyTick = refreshTicks + SEARCH_DEBOUNCE_TICKS;
    }

    private void applySearchQueryNow(String query) {
        pendingTechSearchQuery = null;
        pendingTechSearchApplyTick = -1L;
        if (query.equals(appliedTechSearchQuery)) {
            return;
        }
        appliedTechSearchQuery = query;
        buildTechListLayout();
        reconcileSelectedWithSearch();
    }

    private void reconcileSelectedWithSearch() {
        if (selectedTechId == null) {
            return;
        }
        TechEntry selected = entryById.get(selectedTechId);
        if (matchesSearch(selected)) {
            return;
        }
        selectedTechId = queue.stream()
                .filter(id -> matchesSearch(entryById.get(id)))
                .findFirst()
                .orElseGet(() -> entries.stream()
                        .filter(this::matchesSearch)
                        .map(TechEntry::id)
                        .findFirst()
                        .orElse(selectedTechId));
    }

    private boolean canQueue(TechEntry entry) {
        if (entry == null || unlocked.contains(entry.id()) || queue.contains(entry.id())) {
            return false;
        }
        if (entry.researchMaterials().isEmpty()) {
            return false;
        }
        for (String prereq : entry.prerequisites()) {
            if (!unlocked.contains(prereq) && !queue.contains(prereq)) {
                return false;
            }
        }
        if (!completedTasks.containsAll(entry.requiredTasks())) {
            return false;
        }
        return true;
    }

    private boolean canReorderQueue(int fromIndex, int toIndex) {
        if (fromIndex < 0 || toIndex < 0 || fromIndex >= queue.size() || toIndex >= queue.size() || fromIndex == toIndex) {
            return false;
        }

        List<String> reordered = new ArrayList<>(queue);
        String moved = reordered.remove(fromIndex);
        reordered.add(toIndex, moved);
        return isValidQueueOrder(reordered);
    }

    private boolean isValidQueueOrder(List<String> candidateQueue) {
        for (int i = 0; i < candidateQueue.size(); i++) {
            TechEntry entry = entryById.get(candidateQueue.get(i));
            if (entry == null) {
                return false;
            }
            for (String prereq : entry.prerequisites()) {
                if (unlocked.contains(prereq)) {
                    continue;
                }
                int prereqIndex = candidateQueue.indexOf(prereq);
                if (prereqIndex < 0 || prereqIndex >= i) {
                    return false;
                }
            }
        }
        return true;
    }

    private List<TechEntry> orderedTechListEntries() {
        return entries.stream()
                .filter(entry -> !queue.contains(entry.id()))
                .filter(this::matchesSearch)
                .sorted(Comparator
                        .comparingInt(this::listStateRank)
                        .thenComparingInt(this::queueOrderRank)
                        .thenComparing(TechEntry::title, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(TechEntry::id))
                .toList();
    }

    private int listStateRank(TechEntry entry) {
        if (entry == null) {
            return Integer.MAX_VALUE;
        }
        if (queue.contains(entry.id())) {
            return 0;
        }
        if (canQueue(entry)) {
            return 1;
        }
        if (unlocked.contains(entry.id())) {
            return 3;
        }
        return 2;
    }

    private int queueOrderRank(TechEntry entry) {
        if (entry == null) {
            return Integer.MAX_VALUE;
        }
        int index = queue.indexOf(entry.id());
        return index >= 0 ? index : Integer.MAX_VALUE;
    }

    private TreeViewport treeViewport() {
        int x = guiX + LEFT_PANEL_WIDTH + 9;
        int y = guiY + 21;
        int width = GUI_WIDTH - LEFT_PANEL_WIDTH - 18;
        int height = GUI_HEIGHT - 36;
        return new TreeViewport(x, y, width, height);
    }

    private static @Nullable NodeBounds shifted(@Nullable NodeBounds bounds, int offsetX, int offsetY) {
        if (bounds == null) {
            return null;
        }
        return new NodeBounds(bounds.x() + offsetX, bounds.y() + offsetY, bounds.width(), bounds.height());
    }

    private Component manualTaskLabel(ManualTaskEntry task) {
        boolean done = completedTasks.contains(task.id());
        String prefix = (done && !task.repeatable()) ? "✓ " : "";
        return Component.literal(trimToWidth(prefix + task.title(), 176));
    }

    private ItemStack techIcon(TechEntry entry) {
        if (entry != null) {
            if (!entry.iconItem().isBlank()) {
                ItemStack explicitIcon = itemStackFromId(entry.iconItem());
                if (!explicitIcon.isEmpty()) {
                    return explicitIcon;
                }
            }

            for (TechMaterial material : entry.researchMaterials()) {
                ItemStack materialIcon = itemStackFromId(material.itemId());
                if (!materialIcon.isEmpty()) {
                    return materialIcon;
                }
            }

            for (String taskId : entry.requiredTasks()) {
                ManualTaskEntry task = taskById.get(taskId);
                if (task == null || task.itemId().isBlank()) {
                    continue;
                }

                ItemStack taskIcon = itemStackFromId(task.itemId());
                if (!taskIcon.isEmpty()) {
                    return taskIcon;
                }
            }
        }

        return new ItemStack(Items.BOOK);
    }

    private ItemStack itemStackFromId(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return ItemStack.EMPTY;
        }
        ResourceLocation itemId = ResourceLocation.tryParse(rawId);
        if (itemId == null) {
            return ItemStack.EMPTY;
        }

        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item == null || item == Items.AIR) {
            return ItemStack.EMPTY;
        }

        return new ItemStack(item);
    }

    private ItemStack taskIcon(ManualTaskEntry task) {
        if (task == null || task.itemId().isBlank()) {
            return new ItemStack(Items.PAPER);
        }

        ResourceLocation itemId = ResourceLocation.tryParse(task.itemId());
        if (itemId == null) {
            return new ItemStack(Items.PAPER);
        }

        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item == null || item == Items.AIR) {
            return new ItemStack(Items.PAPER);
        }

        return new ItemStack(item, Math.max(1, task.itemCount()));
    }

    private static int getInt(JsonObject json, String key) {
        return json != null && json.has(key) ? json.get(key).getAsInt() : 0;
    }

    private static void readStringArray(@Nullable JsonArray array, Set<String> out) {
        if (array == null) {
            return;
        }
        for (JsonElement element : array) {
            out.add(element.getAsString());
        }
    }

    private static void readStringArray(@Nullable JsonArray array, List<String> out) {
        if (array == null) {
            return;
        }
        for (JsonElement element : array) {
            out.add(element.getAsString());
        }
    }

    private static List<String> readStringList(@Nullable JsonArray array) {
        List<String> out = new ArrayList<>();
        readStringArray(array, out);
        return out;
    }

    private static List<TechMaterial> readMaterialList(@Nullable JsonArray array) {
        if (array == null) {
            return List.of();
        }

        List<TechMaterial> materials = new ArrayList<>();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject material = element.getAsJsonObject();
            String item = material.has("item") ? material.get("item").getAsString() : "";
            if (item.isBlank()) {
                continue;
            }
            materials.add(new TechMaterial(
                    item,
                    Math.max(1, getInt(material, "count"))
            ));
        }
        return materials;
    }

    private static String shortLabel(String text, int max) {
        if (text == null || text.length() <= max) {
            return text == null ? "" : text;
        }
        if (max <= 3) {
            return text.substring(0, Math.max(1, max));
        }
        return text.substring(0, Math.max(1, max - 3)).trim() + "...";
    }

    private String labelWithState(TechEntry entry) {
        if (unlocked.contains(entry.id())) {
            return entry.title() + " (Unlocked)";
        }
        if (isActiveEntry(entry.id())) {
            return entry.title() + " (Active)";
        }
        if (queue.contains(entry.id())) {
            return entry.title() + " (Queued)";
        }
        if (canQueue(entry)) {
            return entry.title() + " (Available)";
        }
        return entry.title() + " (Locked)";
    }

    private int titleColor(TechEntry entry) {
        if (unlocked.contains(entry.id())) {
            return 0xFF6FDB91;
        }
        if (isActiveEntry(entry.id())) {
            return 0xFF8FD5FF;
        }
        if (queue.contains(entry.id())) {
            return 0xFFE5C46E;
        }
        if (canQueue(entry)) {
            return 0xFFE9D98C;
        }
        return 0xFFE07A7A;
    }

    private String trimToWidth(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        String suffix = "...";
        int suffixWidth = font.width(suffix);
        String body = font.plainSubstrByWidth(text, Math.max(0, maxWidth - suffixWidth));
        return body + suffix;
    }

    private static void drawPanel(GuiGraphics guiGraphics, int x, int y, int width, int height, int fillColor, int borderColor) {
        guiGraphics.fill(x, y, x + width, y + height, fillColor);
        guiGraphics.fill(x, y, x + width, y + 1, borderColor);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, borderColor);
        guiGraphics.fill(x, y, x + 1, y + height, borderColor);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, borderColor);
    }

    private static void drawMiniProgressBar(GuiGraphics guiGraphics, NodeBounds bounds, int progress, int cost, boolean queueCard) {
        if (cost <= 0 || progress <= 0 || progress >= cost) {
            return;
        }
        int barX = bounds.x() + 2;
        int barY = queueCard ? bounds.y() + bounds.height() - 11 : bounds.y() + bounds.height() - 4;
        int barW = bounds.width() - 4;
        int fillW = Math.clamp((progress * barW) / cost, 1, barW);
        guiGraphics.fill(barX, barY, barX + barW, barY + 2, 0xFF20242C);
        guiGraphics.fill(barX, barY, barX + fillW, barY + 2, 0xFF42C86F);
    }

    private static void drawNodeProgressBar(GuiGraphics guiGraphics, NodeBounds bounds, int progress, int cost) {
        if (cost <= 0 || progress <= 0 || progress >= cost) {
            return;
        }
        int barX = bounds.x() + 4;
        int barY = bounds.y() + bounds.height() - 4;
        int barW = bounds.width() - 8;
        int fillW = Math.clamp((progress * barW) / cost, 1, barW);
        guiGraphics.fill(barX, barY, barX + barW, barY + 2, 0xFF20242C);
        guiGraphics.fill(barX, barY, barX + fillW, barY + 2, 0xFF42C86F);
    }

    private static void drawSelectionOutline(GuiGraphics guiGraphics, NodeBounds bounds) {
        drawOutline(guiGraphics, bounds.x() - 1, bounds.y() - 1, bounds.width() + 2, bounds.height() + 2, SELECTION_COLOR);
    }

    private static void drawOutline(GuiGraphics guiGraphics, int x, int y, int width, int height, int color) {
        guiGraphics.fill(x, y, x + width, y + 1, color);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, color);
        guiGraphics.fill(x, y, x + 1, y + height, color);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    private static void drawConnector(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int color) {
        int left = Math.min(x1, x2);
        int right = Math.max(x1, x2);
        int top = Math.min(y1, y2);
        int bottom = Math.max(y1, y2);
        int midX = left + (right - left) / 2;

        guiGraphics.fill(left, y1, midX + 1, y1 + 1, color);
        guiGraphics.fill(midX, top, midX + 1, bottom + 1, color);
        guiGraphics.fill(midX, y2, right + 1, y2 + 1, color);
    }

    private record TechEntry(String id, String title, String description, int cost, String iconItem,
                             int runDurationTicks, List<String> unlocks, List<TechMaterial> researchMaterials, List<String> prerequisites,
                             List<String> requiredTasks) {
    }

    private record TechMaterial(String itemId, int itemCount) {
    }

    private record ManualTaskEntry(String id, String title, String description, String itemId, int itemCount, boolean repeatable) {
    }

    private record NodeBounds(int x, int y, int width, int height) {
        int centerX() {
            return x + width / 2;
        }

        int centerY() {
            return y + height / 2;
        }

        boolean contains(double mx, double my) {
            return mx >= x && mx < x + width && my >= y && my < y + height;
        }
    }

    private record TreeViewport(int x, int y, int width, int height) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + width && my >= y && my < y + height;
        }
    }
}
