package ozokuz.incore.features.research.station;

import ozokuz.incore.Registration;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class CrudeResearchStationMenu extends AbstractContainerMenu {
    public static final int FUEL_X = 44;
    public static final int FUEL_Y = 30;
    public static final int LOGIC_X = 80;
    public static final int LOGIC_Y = 30;
    public static final int DRIVE_X = 116;
    public static final int DRIVE_Y = 30;

    public static final int PLAYER_INV_X = 8;
    public static final int PLAYER_INV_Y = 84;
    public static final int HOTBAR_Y = PLAYER_INV_Y + 58;

    private final CrudeResearchStationBlockEntity blockEntity;
    private final ContainerLevelAccess access;

    public CrudeResearchStationMenu(int containerId, Inventory playerInventory, CrudeResearchStationBlockEntity blockEntity) {
        super(Registration.CRUDE_RESEARCH_STATION_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());

        for (int i = 0; i < blockEntity.data.getCount(); i++) {
            addDataSlot(DataSlot.forContainer(blockEntity.data, i));
        }

        addSlot(new FuelSlot(blockEntity, CrudeResearchStationBlockEntity.FUEL_SLOT, FUEL_X, FUEL_Y));
        addSlot(new LogicSlot(blockEntity, CrudeResearchStationBlockEntity.LOGIC_SLOT, LOGIC_X, LOGIC_Y));
        addSlot(new DriveSlot(blockEntity, CrudeResearchStationBlockEntity.DRIVE_SLOT, DRIVE_X, DRIVE_Y));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, PLAYER_INV_X + col * 18, PLAYER_INV_Y + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, PLAYER_INV_X + col * 18, HOTBAR_Y));
        }
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

        int machineSlots = CrudeResearchStationBlockEntity.SLOT_COUNT;
        if (index < machineSlots) {
            if (!moveItemStackTo(stack, machineSlots, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getBurnTime(null) > 0) {
            if (!moveItemStackTo(stack, CrudeResearchStationBlockEntity.FUEL_SLOT, CrudeResearchStationBlockEntity.FUEL_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(Registration.BASIC_LOGIC_MODULE_ITEM.get())) {
            if (!moveItemStackTo(stack, CrudeResearchStationBlockEntity.LOGIC_SLOT, CrudeResearchStationBlockEntity.LOGIC_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(Registration.STARTER_DATA_ITEM.get())) {
            if (!moveItemStackTo(stack, CrudeResearchStationBlockEntity.DRIVE_SLOT, CrudeResearchStationBlockEntity.DRIVE_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else {
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
        return stillValid(access, player, Registration.CRUDE_RESEARCH_STATION_BLOCK.get());
    }

    public int researchPowerBuffer() {
        return blockEntity.data.get(0);
    }

    public int burnTime() {
        return blockEntity.data.get(1);
    }

    public int burnTotal() {
        return Math.max(0, blockEntity.data.get(2));
    }

    public int burnProgressScaled(int width) {
        int total = burnTotal();
        if (total <= 0) {
            return 0;
        }
        return Math.clamp((burnTime() * width) / total, 0, width);
    }

    public boolean hasTeamBinding() {
        return blockEntity.data.get(3) > 0;
    }

    public int runTickProgress() {
        return Math.max(0, blockEntity.data.get(4));
    }

    public int runTickRequired() {
        return Math.max(1, blockEntity.data.get(5));
    }

    public int completedRuns() {
        return Math.max(0, blockEntity.data.get(6));
    }

    public int requiredRuns() {
        return Math.max(1, blockEntity.data.get(7));
    }

    public int queueStatusOrdinal() {
        return blockEntity.data.get(8);
    }

    public int runProgressScaled(int width) {
        int total = runTickRequired();
        if (total <= 0) {
            return 0;
        }
        return Math.clamp((runTickProgress() * width) / total, 0, width);
    }

    private static class FuelSlot extends Slot {
        private FuelSlot(CrudeResearchStationBlockEntity blockEntity, int slot, int x, int y) {
            super(blockEntity, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getBurnTime(null) > 0;
        }
    }

    private static class LogicSlot extends Slot {
        private LogicSlot(CrudeResearchStationBlockEntity blockEntity, int slot, int x, int y) {
            super(blockEntity, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.is(Registration.BASIC_LOGIC_MODULE_ITEM.get());
        }
    }

    private static class DriveSlot extends Slot {
        private DriveSlot(CrudeResearchStationBlockEntity blockEntity, int slot, int x, int y) {
            super(blockEntity, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.is(Registration.STARTER_DATA_ITEM.get());
        }
    }
}
