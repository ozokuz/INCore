package io.github.ozokuz.incore.features.research.station;

import io.github.ozokuz.incore.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class PowerInputMenu extends AbstractContainerMenu {
    private final BlockPos blockPos;
    private int familyOrdinal = -1;
    private int powerTier;
    private int primaryValue;
    private int secondaryValue;
    private int detailAValue;
    private int detailBValue;
    private int detailCValue;
    private int availablePower;

    public PowerInputMenu(int containerId, Inventory playerInventory, BlockPos blockPos) {
        super(Registration.POWER_INPUT_MENU.get(), containerId);
        this.blockPos = blockPos.immutable();
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(this.blockPos);
        syncFromBlockEntity(blockEntity);

        addDataSlot(slot(
                () -> blockEntity instanceof IResearchPowerInput input ? input.family().ordinal() : -1,
                value -> familyOrdinal = value
        ));
        addDataSlot(slot(
                () -> blockEntity instanceof IResearchPowerInput input ? input.powerTier() : 0,
                value -> powerTier = Math.max(0, value)
        ));
        addDataSlot(slot(
                () -> switch (blockEntity) {
                    case ElectricPowerInputBlockEntity electric -> electric.energyStored();
                    case MechanicalPowerInputBlockEntity mechanical -> Math.round(Math.abs(mechanical.getSpeed()) * 100.0F);
                    default -> 0;
                },
                value -> primaryValue = Math.max(0, value)
        ));
        addDataSlot(slot(
                () -> switch (blockEntity) {
                    case ElectricPowerInputBlockEntity electric -> electric.energyCapacity();
                    case MechanicalPowerInputBlockEntity mechanical -> Math.round(mechanical.availableRpm() * 100.0F);
                    default -> 0;
                },
                value -> secondaryValue = Math.max(0, value)
        ));
        addDataSlot(slot(
                () -> switch (blockEntity) {
                    case ElectricPowerInputBlockEntity electric -> electric.maxReceive();
                    case MechanicalPowerInputBlockEntity mechanical -> mechanical.isOperational() ? 1 : 0;
                    default -> 0;
                },
                value -> detailAValue = value
        ));
        addDataSlot(slot(
                () -> switch (blockEntity) {
                    case ElectricPowerInputBlockEntity electric -> electric.maxFePerTickLimit();
                    case MechanicalPowerInputBlockEntity mechanical -> mechanical.isOverStressed() ? 1 : 0;
                    default -> 0;
                },
                value -> detailBValue = value
        ));
        addDataSlot(slot(
                () -> switch (blockEntity) {
                    case ElectricPowerInputBlockEntity electric -> electric.maxFePerInputOperationLimit();
                    default -> 0;
                },
                value -> detailCValue = value
        ));
        addDataSlot(slot(
                () -> blockEntity instanceof IResearchPowerInput input ? input.availableResearchPower(null, Integer.MAX_VALUE) : 0,
                value -> availablePower = Math.max(0, value)
        ));
    }

    private static DataSlot slot(IntSupplier getter, IntConsumer setter) {
        return new DataSlot() {
            @Override
            public int get() {
                return getter.getAsInt();
            }

            @Override
            public void set(int value) {
                setter.accept(value);
            }
        };
    }

    private void syncFromBlockEntity(BlockEntity blockEntity) {
        if (blockEntity instanceof IResearchPowerInput input) {
            familyOrdinal = input.family().ordinal();
            powerTier = input.powerTier();
            availablePower = input.availableResearchPower(null, Integer.MAX_VALUE);
        } else {
            familyOrdinal = -1;
            powerTier = 0;
            availablePower = 0;
        }

        if (blockEntity instanceof ElectricPowerInputBlockEntity electric) {
            primaryValue = electric.energyStored();
            secondaryValue = electric.energyCapacity();
            detailAValue = electric.maxReceive();
            detailBValue = electric.maxFePerTickLimit();
            detailCValue = electric.maxFePerInputOperationLimit();
        } else if (blockEntity instanceof MechanicalPowerInputBlockEntity mechanical) {
            primaryValue = Math.round(Math.abs(mechanical.getSpeed()) * 100.0F);
            secondaryValue = Math.round(mechanical.availableRpm() * 100.0F);
            detailAValue = mechanical.isOperational() ? 1 : 0;
            detailBValue = mechanical.isOverStressed() ? 1 : 0;
            detailCValue = 0;
        } else {
            primaryValue = 0;
            secondaryValue = 0;
            detailAValue = 0;
            detailBValue = 0;
            detailCValue = 0;
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (!(player.level().getBlockEntity(blockPos) instanceof IResearchPowerInput)) {
            return false;
        }
        return player.distanceToSqr(blockPos.getX() + 0.5D, blockPos.getY() + 0.5D, blockPos.getZ() + 0.5D) <= 64.0D;
    }

    public ResearchPowerFamily family() {
        return familyOrdinal < 0 || familyOrdinal >= ResearchPowerFamily.values().length
                ? null
                : ResearchPowerFamily.values()[familyOrdinal];
    }

    public int powerTier() {
        return powerTier;
    }

    public int primaryValue() {
        return primaryValue;
    }

    public int secondaryValue() {
        return secondaryValue;
    }

    public int detailAValue() {
        return detailAValue;
    }

    public int detailBValue() {
        return detailBValue;
    }

    public int detailCValue() {
        return detailCValue;
    }

    public int availablePower() {
        return availablePower;
    }

    @FunctionalInterface
    private interface IntSupplier {
        int getAsInt();
    }

    @FunctionalInterface
    private interface IntConsumer {
        void accept(int value);
    }
}
