package ozokuz.incore.features.machines.multiblock;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public abstract class AbstractMachineInventoryMenu extends AbstractContainerMenu {
    private static final int SCREEN_WIDTH = 176;
    private static final int MIN_MACHINE_SECTION_WIDTH = 68;
    protected final AbstractMachineInventoryPartBlockEntity blockEntity;
    private final ContainerLevelAccess access;
    private final int machineSlotCount;
    private final int machineRows;
    private final int machineColumns;
    private final int machineSectionLeft;
    private final int machineSectionWidth;
    private final int machineSlotStartY;

    protected AbstractMachineInventoryMenu(net.minecraft.world.inventory.MenuType<?> type, int containerId, Inventory playerInventory, AbstractMachineInventoryPartBlockEntity blockEntity, int columns) {
        this(type, containerId, playerInventory, blockEntity, columns, 18);
    }

    protected AbstractMachineInventoryMenu(net.minecraft.world.inventory.MenuType<?> type, int containerId, Inventory playerInventory, AbstractMachineInventoryPartBlockEntity blockEntity, int columns, int machineSlotStartY) {
        super(type, containerId);
        this.blockEntity = blockEntity;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        this.machineSlotCount = Math.max(1, blockEntity.menuSlotCount());
        this.machineColumns = Math.max(1, Math.min(machineSlotCount, columns));
        this.machineRows = Math.max(1, (int) Math.ceil(machineSlotCount / (double) machineColumns));
        this.machineSlotStartY = machineSlotStartY;
        int slotGroupWidth = machineColumns * 18;
        this.machineSectionWidth = Math.max(MIN_MACHINE_SECTION_WIDTH, slotGroupWidth + 14);
        this.machineSectionLeft = (SCREEN_WIDTH - machineSectionWidth) / 2;

        int startX = machineSectionLeft + ((machineSectionWidth - slotGroupWidth) / 2);
        for (int slot = 0; slot < machineSlotCount; slot++) {
            int row = slot / machineColumns;
            int col = slot % machineColumns;
            addSlot(new ActiveStationSlot(blockEntity, slot, startX + col * 18, machineSlotStartY + row * 18));
        }

        int playerInvY = machineSlotStartY + machineRows * 18 + 14;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, playerInvY + row * 18));
            }
        }

        int hotbarY = playerInvY + 58;
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, hotbarY));
        }
    }

    public int machineRows() {
        return machineRows;
    }

    public int machineSlotCount() {
        return machineSlotCount;
    }

    public int machineSectionLeft() {
        return machineSectionLeft;
    }

    public int machineSectionWidth() {
        return machineSectionWidth;
    }

    public int machineSlotStartY() {
        return machineSlotStartY;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }

        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < machineSlotCount) {
            if (!moveItemStackTo(stack, machineSlotCount, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, machineSlotCount, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, stack);
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return access.evaluate((level, pos) -> {
            if (level.getBlockEntity(pos) != blockEntity) {
                return false;
            }
            double centerX = pos.getX() + 0.5D;
            double centerY = pos.getY() + 0.5D;
            double centerZ = pos.getZ() + 0.5D;
            return player.distanceToSqr(centerX, centerY, centerZ) <= 64.0D;
        }, true);
    }

    private static final class ActiveStationSlot extends SlotItemHandler {
        private final AbstractMachineInventoryPartBlockEntity blockEntity;
        private final int machineSlot;

        private ActiveStationSlot(AbstractMachineInventoryPartBlockEntity blockEntity, int slot, int xPosition, int yPosition) {
            super(blockEntity.rawItemHandler(), slot, xPosition, yPosition);
            this.blockEntity = blockEntity;
            this.machineSlot = slot;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return blockEntity.isSlotActive(machineSlot) && super.mayPlace(stack);
        }

        @Override
        public boolean mayPickup(Player player) {
            return blockEntity.isSlotActive(machineSlot) && super.mayPickup(player);
        }

        @Override
        public boolean isActive() {
            return blockEntity.isSlotActive(machineSlot);
        }
    }
}
