package ozokuz.incore.integration.ldlib.ui.cards;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.slot.ItemHandlerSlot;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.RectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.inventory.InventorySlots;
import dev.vfyjxf.taffy.style.TaffyPosition;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import ozokuz.incore.client.ui.UIScreenTheme;
import ozokuz.incore.features.cards.DeckStationBlockEntity;
import ozokuz.incore.features.cards.DeckStationItemHandler;
import ozokuz.incore.integration.ldlib.ui.INCoreLdLibUiScaffold;
import ozokuz.incore.integration.ldlib.ui.texture.BeveledRectTexture;

public final class DeckStationLdLibUi {
    public static final int WIDTH = 392;
    public static final int HEIGHT = 238;

    public static final int GRID_COLUMNS = 8;
    public static final int GRID_ROWS = 3;
    public static final int GRID_START_X = 22;
    public static final int GRID_START_Y = 66;
    public static final int GRID_STEP_X = 20;
    public static final int GRID_STEP_Y = 20;

    public static final int CORE_X = GRID_START_X + 3 * GRID_STEP_X;
    public static final int CORE_Y = GRID_START_Y;
    public static final int BOX_X = GRID_START_X + 4 * GRID_STEP_X;
    public static final int BOX_Y = GRID_START_Y;

    public static final int OUTPUT_X = 162;
    public static final int OUTPUT_Y = 34;

    public static final int PLAYER_INV_X = 21;
    public static final int PLAYER_INV_Y = 152;

    private static final int HEADER_Y = 6;
    private static final int HEADER_H = 14;

    private static final int MODULE_PANEL_X = 8;
    private static final int MODULE_PANEL_Y = 24;
    private static final int MODULE_PANEL_W = 186;
    private static final int MODULE_PANEL_H = 106;

    private static final int RIGHT_PANEL_X = 200;
    private static final int RIGHT_PANEL_Y = 24;
    private static final int RIGHT_PANEL_W = 184;
    private static final int RIGHT_PANEL_H = 208;

    private static final int INVENTORY_PANEL_X = 8;
    private static final int INVENTORY_PANEL_Y = 136;
    private static final int INVENTORY_PANEL_W = 186;
    private static final int INVENTORY_PANEL_H = 96;
    private static final IGuiTexture SLOT_TEXTURE = new BeveledRectTexture(
            UIScreenTheme.Crafting.SLOT_FILL,
            UIScreenTheme.Crafting.SLOT_BORDER,
            UIScreenTheme.Crafting.SLOT_BORDER,
            UIScreenTheme.Crafting.SLOT_BORDER,
            1,
            0
    );

    private DeckStationLdLibUi() {
    }

    public static ModularUI create(BlockUIMenuType.BlockUIHolder holder) {
        BlockEntity blockEntity = holder.player.level().getBlockEntity(holder.pos);
        if (!(blockEntity instanceof DeckStationBlockEntity station)) {
            return INCoreLdLibUiScaffold.build(holder.player, missingRoot());
        }

        station.refreshPreview();
        DeckStationItemHandler itemHandler = new DeckStationItemHandler(station);

        UIElement root = new UIElement().layout(layout -> {
            layout.width(WIDTH);
            layout.height(HEIGHT);
        });

        root.addChild(panel(0, 0, WIDTH, HEIGHT, UIScreenTheme.Crafting.WINDOW_FILL));
        root.addChild(panel(5, HEADER_Y, WIDTH - 10, HEADER_H, UIScreenTheme.Crafting.HEADER_FILL));
        root.addChild(panel(MODULE_PANEL_X, MODULE_PANEL_Y, MODULE_PANEL_W, MODULE_PANEL_H, UIScreenTheme.Crafting.PANEL_FILL));
        root.addChild(panel(RIGHT_PANEL_X, RIGHT_PANEL_Y, RIGHT_PANEL_W, RIGHT_PANEL_H, UIScreenTheme.Crafting.PANEL_FILL));
        root.addChild(panel(INVENTORY_PANEL_X, INVENTORY_PANEL_Y, INVENTORY_PANEL_W, INVENTORY_PANEL_H, UIScreenTheme.Crafting.PANEL_FILL));

        root.addChild(titleLabel(Component.translatable("block.incore.deck_station"), 10, HEADER_Y + 3, UIScreenTheme.Crafting.TITLE_TEXT));
        root.addChild(sectionLabel("Modules", MODULE_PANEL_X + 6, MODULE_PANEL_Y + 6, UIScreenTheme.Crafting.BODY_TEXT));
        root.addChild(centeredLabel("Core", CORE_X + 6, CORE_Y - 12, UIScreenTheme.Crafting.ACCENT_TEXT));
        root.addChild(centeredLabel("Box", BOX_X + 10, BOX_Y - 12, UIScreenTheme.Crafting.ACCENT_TEXT));
        root.addChild(centeredLabel("Output", OUTPUT_X - 20, OUTPUT_Y + 4, UIScreenTheme.Crafting.ACCENT_TEXT));
        root.addChild(sectionLabel("Deck Preview", RIGHT_PANEL_X + 6, RIGHT_PANEL_Y + 4, UIScreenTheme.Crafting.TITLE_TEXT));
        root.addChild(divider(RIGHT_PANEL_X + 4, RIGHT_PANEL_Y + 15, RIGHT_PANEL_W - 8, UIScreenTheme.Crafting.PANEL_BORDER));
        root.addChild(sectionLabel("Inventory", PLAYER_INV_X, INVENTORY_PANEL_Y + 4, UIScreenTheme.Crafting.BODY_TEXT));

        for (ItemSlot slot : stationSlots(itemHandler)) {
            root.addChild(slot);
        }

        int infoX = RIGHT_PANEL_X + 6;
        int infoY = RIGHT_PANEL_Y + 20;
        root.addChild(boundLabel(() -> Component.literal("Modules: " + station.moduleCount()), infoX, infoY, RIGHT_PANEL_W - 12, UIScreenTheme.Crafting.BODY_TEXT));
        root.addChild(boundLabel(() -> Component.literal("Points: " + station.usedPoints() + "/" + station.capacity()).withColor(pointsColor(station)), infoX, infoY + 12, RIGHT_PANEL_W - 12, UIScreenTheme.Crafting.BODY_TEXT));
        root.addChild(boundLabel(() -> Component.literal("Max Integrity: " + station.maxIntegrity()), infoX, infoY + 24, RIGHT_PANEL_W - 12, UIScreenTheme.Crafting.BODY_TEXT));
        root.addChild(boundLabel(
                () -> {
                    int statusColor = station.isValidPreview() ? UIScreenTheme.Crafting.SUCCESS_TEXT : UIScreenTheme.Crafting.DANGER_TEXT;
                    return station.isValidPreview()
                            ? Component.literal("Status: Valid").withColor(statusColor)
                            : Component.literal("Status: Invalid").withColor(statusColor);
                },
                infoX,
                infoY + 36,
                RIGHT_PANEL_W - 12,
                UIScreenTheme.Crafting.BODY_TEXT
        ));
        root.addChild(boundWrappedLabel(
                () -> station.isValidPreview()
                        ? Component.empty()
                        : Component.translatable(station.failureKey().isBlank() ? "incore.cards.deck.missing_core" : station.failureKey()),
                infoX,
                infoY + 48,
                RIGHT_PANEL_W - 12,
                UIScreenTheme.Crafting.REASON_TEXT
        ));
        root.addChild(sectionLabel("Modifiers", infoX, infoY + 68, UIScreenTheme.Crafting.MODIFIER_LABEL_TEXT));

        int modifierBaseY = infoY + 80;
        for (int i = 0; i < 5; i++) {
            int lineIndex = i;
            root.addChild(boundLabel(
                    () -> {
                        List<String> lines = station.previewModifierLines();
                        if (lines.isEmpty() && lineIndex == 0) {
                            return Component.literal("No active modifiers.");
                        }
                        return lineIndex < lines.size() ? Component.literal(lines.get(lineIndex)) : Component.empty();
                    },
                    infoX,
                    modifierBaseY + i * 10,
                    RIGHT_PANEL_W - 12,
                    modifierColor(station, lineIndex)
            ));
        }

        InventorySlots inventorySlots = new InventorySlots();
        inventorySlots.layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.left(PLAYER_INV_X);
            layout.top(PLAYER_INV_Y);
        });
        inventorySlots.apply(slot -> {
            slot.style(style -> style.backgroundTexture(SLOT_TEXTURE));
            slot.slotStyle(slotStyle -> slotStyle.acceptQuickMove(true).showItemTooltips(true));
        });
        root.addChild(inventorySlots);

        return INCoreLdLibUiScaffold.build(holder.player, root);
    }

    private static List<ItemSlot> stationSlots(DeckStationItemHandler itemHandler) {
        List<ItemSlot> slots = new ArrayList<>();

        int moduleSlot = 0;
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLUMNS; col++) {
                if (row == 0 && col >= 2 && col <= 5) {
                    continue;
                }
                slots.add(stationSlot(itemHandler, moduleSlot++, GRID_START_X + col * GRID_STEP_X, GRID_START_Y + row * GRID_STEP_Y, true));
            }
        }

        slots.add(stationSlot(itemHandler, DeckStationBlockEntity.CORE_SLOT, CORE_X, CORE_Y, true));
        slots.add(stationSlot(itemHandler, DeckStationBlockEntity.BOX_SLOT, BOX_X, BOX_Y, true));
        slots.add(stationSlot(itemHandler, DeckStationBlockEntity.OUTPUT_SLOT, OUTPUT_X, OUTPUT_Y, false));
        return slots;
    }

    private static ItemSlot stationSlot(DeckStationItemHandler itemHandler, int slotIndex, int x, int y, boolean canPlace) {
        ItemHandlerSlot slot = new ItemHandlerSlot(itemHandler, slotIndex)
                .setCanPlace(stack -> canPlace && itemHandler.isItemValid(slotIndex, stack))
                .setCanTake(player -> slotIndex != DeckStationBlockEntity.OUTPUT_SLOT || !itemHandler.extractItem(slotIndex, 1, true).isEmpty())
                .addChangeListener(() -> {
                });

        ItemSlot itemSlot = new ItemSlot(slot);
        itemSlot.style(style -> style.backgroundTexture(SLOT_TEXTURE));
        itemSlot.layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.left(x);
            layout.top(y);
        });
        itemSlot.slotStyle(style -> style
                .showItemTooltips(true)
                .acceptQuickMove(true)
                .quickMovePriority(slotIndex == DeckStationBlockEntity.OUTPUT_SLOT ? 3 : 1)
        );
        return itemSlot;
    }

    private static UIElement panel(int x, int y, int width, int height, int fillColor) {
        return new UIElement().layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.left(x);
            layout.top(y);
            layout.width(width);
            layout.height(height);
        }).style(style -> style.backgroundTexture(RectTexture.of(fillColor)));
    }

    private static UIElement divider(int x, int y, int width, int color) {
        return new UIElement().layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.left(x);
            layout.top(y);
            layout.width(width);
            layout.height(1);
        }).style(style -> style.backgroundTexture(RectTexture.of(color)));
    }

    private static Label titleLabel(Component text, int x, int y, int color) {
        Label label = new Label();
        label.setText(text);
        label.layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.left(x);
            layout.top(y);
        });
        label.textStyle(style -> style.textColor(color).adaptiveWidth(true).textWrap(TextWrap.HIDE));
        return label;
    }

    private static Label sectionLabel(String text, int x, int y, int color) {
        return titleLabel(Component.literal(text), x, y, color);
    }

    private static Label centeredLabel(String text, int centerX, int y, int color) {
        Label label = new Label();
        label.setText(Component.literal(text));
        label.layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.left(centerX - 20);
            layout.top(y);
            layout.width(40);
        });
        label.textStyle(style -> style
                .adaptiveWidth(false)
                .textWrap(TextWrap.HIDE)
                .textColor(color)
                .textAlignHorizontal(Horizontal.CENTER));
        return label;
    }

    private static Label boundLabel(ComponentSupplier supplier, int x, int y, int width, int color) {
        Label label = new Label();
        label.layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.left(x);
            layout.top(y);
            layout.width(width);
        });
        label.textStyle(style -> style.adaptiveWidth(false).textWrap(TextWrap.HIDE).textColor(color));
        label.bind(DataBindingBuilder.componentS2C(supplier::get).build());
        return label;
    }

    private static Label boundWrappedLabel(ComponentSupplier supplier, int x, int y, int width, int color) {
        Label label = new Label();
        label.layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.left(x);
            layout.top(y);
            layout.width(width);
        });
        label.textStyle(style -> style
                .adaptiveWidth(false)
                .adaptiveHeight(true)
                .textWrap(TextWrap.WRAP)
                .textColor(color));
        label.bind(DataBindingBuilder.componentS2C(supplier::get).build());
        return label;
    }

    private static int pointsColor(DeckStationBlockEntity station) {
        return station.capacity() <= 0
                ? UIScreenTheme.Crafting.MUTED_TEXT
                : (station.usedPoints() > station.capacity() ? UIScreenTheme.Crafting.DANGER_TEXT : UIScreenTheme.Crafting.SUCCESS_TEXT);
    }

    private static int modifierColor(DeckStationBlockEntity station, int lineIndex) {
        List<String> lines = station.previewModifierLines();
        if (lines.isEmpty()) {
            return UIScreenTheme.Crafting.MUTED_TEXT;
        }
        if (lineIndex >= lines.size()) {
            return UIScreenTheme.Crafting.BODY_TEXT;
        }
        String line = lines.get(lineIndex);
        if (line.startsWith("-")) {
            return UIScreenTheme.Crafting.DANGER_TEXT;
        }
        if (line.startsWith("Undecrypted Cryptics:")) {
            return UIScreenTheme.Crafting.WARNING_TEXT;
        }
        return UIScreenTheme.Crafting.SUCCESS_TEXT;
    }

    private static UIElement missingRoot() {
        UIElement root = new UIElement().layout(layout -> {
            layout.width(220);
            layout.height(90);
        });
        root.addChild(panel(0, 0, 220, 90, UIScreenTheme.Crafting.PANEL_FILL));
        root.addChild(titleLabel(Component.literal("Deck Station"), 8, 8, UIScreenTheme.Crafting.TITLE_TEXT));
        root.addChild(boundWrappedLabel(
                () -> Component.literal("Deck station block entity was unavailable."),
                8,
                28,
                204,
                UIScreenTheme.Crafting.DANGER_TEXT
        ));
        return root;
    }

    @FunctionalInterface
    private interface ComponentSupplier {
        Component get();
    }
}