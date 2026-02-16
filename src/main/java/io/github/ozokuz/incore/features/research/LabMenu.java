package io.github.ozokuz.incore.features.research;

import io.github.ozokuz.incore.Registration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

public class LabMenu extends AbstractContainerMenu {
    public static final int LAB_SLOT_COLUMNS = 3;
    public static final int LAB_SLOT_ROWS = 3;
    public static final int LAB_SLOT_COUNT = LAB_SLOT_COLUMNS * LAB_SLOT_ROWS;

    public static final int LAB_SLOT_X = 234;
    public static final int LAB_SLOT_Y = 62;
    public static final int PLAYER_INVENTORY_X = 79;
    public static final int PLAYER_INVENTORY_Y = 132;
    public static final int HOTBAR_Y = 190;

    private final LabBlockEntity blockEntity;
    private final ContainerData data;

    public LabMenu(int containerId, Inventory playerInventory, LabBlockEntity blockEntity) {
        super(Registration.RESEARCH_LAB_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.data = blockEntity.data;
        addDataSlot(DataSlot.forContainer(data, 0));
        addDataSlot(DataSlot.forContainer(data, 1));
        addDataSlot(DataSlot.forContainer(data, 2));
        addDataSlot(DataSlot.forContainer(data, 3));
        addDataSlot(DataSlot.forContainer(data, 4));
        addDataSlot(DataSlot.forContainer(data, 5));

        LabSingleSlotContainer labInventory = new LabSingleSlotContainer(blockEntity);
        for (int row = 0; row < LAB_SLOT_ROWS; row++) {
            for (int col = 0; col < LAB_SLOT_COLUMNS; col++) {
                addSlot(new Slot(labInventory, col + row * LAB_SLOT_COLUMNS, LAB_SLOT_X + col * 18, LAB_SLOT_Y + row * 18));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, PLAYER_INVENTORY_X + col * 18, PLAYER_INVENTORY_Y + row * 18));
            }
        }

        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(playerInventory, i, PLAYER_INVENTORY_X + i * 18, HOTBAR_Y));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < LAB_SLOT_COUNT) {
            if (!moveItemStackTo(stack, LAB_SLOT_COUNT, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, LAB_SLOT_COUNT, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity.getLevel() != null && player.distanceToSqr(blockEntity.getBlockPos().getCenter()) <= 64;
    }

    public int progress() {
        return data.get(0);
    }

    public int maxProgress() {
        return data.get(1);
    }

    public int progressScaled(int width) {
        return maxProgress() > 0 ? (progress() * width) / maxProgress() : 0;
    }

    public @Nullable ResourceLocation activeResearchId() {
        int index = data.get(2);
        List<ResourceLocation> ids = sortedResearchIds();
        if (index < 0 || index >= ids.size()) {
            return null;
        }
        return ids.get(index);
    }

    public boolean hasActiveResearch() {
        return activeResearchId() != null;
    }

    public boolean hasVisibleProcessing() {
        return hasActiveResearch() || maxProgress() > 0;
    }

    public int labStatus() {
        return data.get(3);
    }

    public boolean isWorking() {
        return labStatus() == LabBlockEntity.STATUS_WORKING;
    }

    public int overallProgress() {
        return data.get(4);
    }

    public int overallMaxProgress() {
        return data.get(5);
    }

    public int overallProgressScaled(int width) {
        return overallMaxProgress() > 0 ? (overallProgress() * width) / overallMaxProgress() : 0;
    }

    public int displayMaxProgress() {
        int max = maxProgress();
        if (max > 0) {
            return max;
        }
        ResourceLocation activeId = activeResearchId();
        if (activeId == null) {
            return 0;
        }
        ResearchEntryData entry = ResearchEntryManager.all().get(activeId);
        if (entry != null) {
            return Math.max(0, entry.runDurationTicks());
        }
        return 0;
    }

    public String activeResearchTitle() {
        ResourceLocation activeId = activeResearchId();
        if (activeId == null) {
            return "-";
        }
        ResearchEntryData entry = ResearchEntryManager.all().get(activeId);
        return entry == null ? activeId.toString() : entry.title();
    }

    public ItemStack activeResearchIcon() {
        ResourceLocation activeId = activeResearchId();
        if (activeId == null) {
            return ItemStack.EMPTY;
        }

        ResearchEntryData entry = ResearchEntryManager.all().get(activeId);
        if (entry == null) {
            return ItemStack.EMPTY;
        }

        if (entry.iconItem() != null) {
            Item explicitIcon = BuiltInRegistries.ITEM.get(entry.iconItem());
            if (explicitIcon != null && explicitIcon != Items.AIR) {
                return new ItemStack(explicitIcon);
            }
        }

        for (ResearchMaterialData material : entry.researchMaterials()) {
            if (material.itemId() == null) {
                continue;
            }
            Item item = BuiltInRegistries.ITEM.get(material.itemId());
            if (item != null && item != Items.AIR) {
                return new ItemStack(item);
            }
        }

        return new ItemStack(Items.BOOK);
    }

    private static List<ResourceLocation> sortedResearchIds() {
        return ResearchEntryManager.all().keySet().stream()
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .toList();
    }
}
