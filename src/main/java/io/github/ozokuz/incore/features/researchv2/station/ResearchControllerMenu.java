package io.github.ozokuz.incore.features.researchv2.station;

import io.github.ozokuz.incore.Registration;
import io.github.ozokuz.incore.features.researchv2.station.network.StationNetworkService;
import io.github.ozokuz.incore.features.researchv2.station.network.TeamStationNetworkSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;

public class ResearchControllerMenu extends AbstractContainerMenu {
    private final BlockPos blockPos;
    private boolean formed;
    private int stationTier;
    private boolean teamLinked;
    private int connectedPartCount;
    private int inputCount;
    private int powerFamilyOrdinal = -1;
    private int powerInputTier;
    private int availablePower;
    private int outputPortCount;
    private boolean hasLogicHousing;
    private boolean hasResearchDrive;
    private boolean hasMaterialStorage;
    private boolean hasAugmenter;
    private boolean hasLinkPort;
    private boolean stationNetworkLinked;
    private int teamStationNetworkCount;
    private boolean teamStationNetworkValid;

    public ResearchControllerMenu(int containerId, Inventory playerInventory, BlockPos blockPos) {
        super(Registration.RESEARCH_CONTROLLER_MENU.get(), containerId);
        this.blockPos = blockPos.immutable();
        ResearchControllerBlockEntity blockEntity = resolveBlockEntity(playerInventory);
        syncFromBlockEntity(blockEntity);

        addDataSlot(slot(
                () -> blockEntity != null && blockEntity.isFormed() ? 1 : 0,
                value -> formed = value > 0
        ));
        addDataSlot(slot(
                () -> blockEntity == null ? 0 : blockEntity.stationTier(),
                value -> stationTier = Math.max(0, value)
        ));
        addDataSlot(slot(
                () -> blockEntity != null && !blockEntity.teamId().isBlank() ? 1 : 0,
                value -> teamLinked = value > 0
        ));
        addDataSlot(slot(
                () -> blockEntity == null ? 0 : blockEntity.connectedPartCount(),
                value -> connectedPartCount = Math.max(0, value)
        ));
        addDataSlot(slot(
                () -> blockEntity == null ? 0 : blockEntity.powerInputPositions().size(),
                value -> inputCount = Math.max(0, value)
        ));
        addDataSlot(slot(
                () -> blockEntity == null || blockEntity.powerFamily() == null ? -1 : blockEntity.powerFamily().ordinal(),
                value -> powerFamilyOrdinal = value
        ));
        addDataSlot(slot(
                () -> blockEntity == null ? 0 : blockEntity.powerInputTier(),
                value -> powerInputTier = Math.max(0, value)
        ));
        addDataSlot(slot(
                () -> blockEntity == null ? 0 : blockEntity.availableResearchPower(Integer.MAX_VALUE),
                value -> availablePower = Math.max(0, value)
        ));
        addDataSlot(slot(
                () -> blockEntity == null ? 0 : blockEntity.outputPortPositions().size(),
                value -> outputPortCount = Math.max(0, value)
        ));
        addDataSlot(slot(
                () -> blockEntity != null && blockEntity.logicHousingPos() != null ? 1 : 0,
                value -> hasLogicHousing = value > 0
        ));
        addDataSlot(slot(
                () -> blockEntity != null && blockEntity.researchDrivePos() != null ? 1 : 0,
                value -> hasResearchDrive = value > 0
        ));
        addDataSlot(slot(
                () -> blockEntity != null && blockEntity.materialStoragePos() != null ? 1 : 0,
                value -> hasMaterialStorage = value > 0
        ));
        addDataSlot(slot(
                () -> blockEntity != null && blockEntity.augmenterPos() != null ? 1 : 0,
                value -> hasAugmenter = value > 0
        ));
        addDataSlot(slot(
                () -> blockEntity != null && StationNetworkService.hasLinkPort(blockEntity) ? 1 : 0,
                value -> hasLinkPort = value > 0
        ));
        addDataSlot(slot(
                () -> blockEntity != null && !StationNetworkService.stationNetworkId(blockEntity).isBlank()
                        && StationNetworkService.snapshot(blockEntity.getLevel().getServer(), blockEntity.teamId()).linkedStationIds().contains(blockEntity.stationId()) ? 1 : 0,
                value -> stationNetworkLinked = value > 0
        ));
        addDataSlot(slot(
                () -> {
                    TeamStationNetworkSnapshot snapshot = networkSnapshot(blockEntity);
                    return snapshot.stationNetworkCount();
                },
                value -> teamStationNetworkCount = Math.max(0, value)
        ));
        addDataSlot(slot(
                () -> networkSnapshot(blockEntity).stationNetworkValid() ? 1 : 0,
                value -> teamStationNetworkValid = value > 0
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

    private ResearchControllerBlockEntity resolveBlockEntity(Inventory playerInventory) {
        return playerInventory.player.level().getBlockEntity(blockPos) instanceof ResearchControllerBlockEntity controller
                ? controller
                : null;
    }

    private void syncFromBlockEntity(ResearchControllerBlockEntity blockEntity) {
        formed = blockEntity != null && blockEntity.isFormed();
        stationTier = blockEntity == null ? 0 : blockEntity.stationTier();
        teamLinked = blockEntity != null && !blockEntity.teamId().isBlank();
        connectedPartCount = blockEntity == null ? 0 : blockEntity.connectedPartCount();
        inputCount = blockEntity == null ? 0 : blockEntity.powerInputPositions().size();
        powerFamilyOrdinal = blockEntity == null || blockEntity.powerFamily() == null ? -1 : blockEntity.powerFamily().ordinal();
        powerInputTier = blockEntity == null ? 0 : blockEntity.powerInputTier();
        availablePower = blockEntity == null ? 0 : blockEntity.availableResearchPower(Integer.MAX_VALUE);
        outputPortCount = blockEntity == null ? 0 : blockEntity.outputPortPositions().size();
        hasLogicHousing = blockEntity != null && blockEntity.logicHousingPos() != null;
        hasResearchDrive = blockEntity != null && blockEntity.researchDrivePos() != null;
        hasMaterialStorage = blockEntity != null && blockEntity.materialStoragePos() != null;
        hasAugmenter = blockEntity != null && blockEntity.augmenterPos() != null;
        TeamStationNetworkSnapshot snapshot = networkSnapshot(blockEntity);
        hasLinkPort = blockEntity != null && StationNetworkService.hasLinkPort(blockEntity);
        stationNetworkLinked = blockEntity != null && snapshot.linkedStationIds().contains(blockEntity.stationId());
        teamStationNetworkCount = snapshot.stationNetworkCount();
        teamStationNetworkValid = snapshot.stationNetworkValid();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (!(player.level().getBlockEntity(blockPos) instanceof ResearchControllerBlockEntity)) {
            return false;
        }
        return player.distanceToSqr(blockPos.getX() + 0.5D, blockPos.getY() + 0.5D, blockPos.getZ() + 0.5D) <= 64.0D;
    }

    public boolean formed() {
        return formed;
    }

    public int stationTier() {
        return stationTier;
    }

    public boolean teamLinked() {
        return teamLinked;
    }

    public int connectedPartCount() {
        return connectedPartCount;
    }

    public int inputCount() {
        return inputCount;
    }

    public ResearchPowerFamily powerFamily() {
        return powerFamilyOrdinal < 0 || powerFamilyOrdinal >= ResearchPowerFamily.values().length
                ? null
                : ResearchPowerFamily.values()[powerFamilyOrdinal];
    }

    public int powerInputTier() {
        return powerInputTier;
    }

    public int availablePower() {
        return availablePower;
    }

    public int outputPortCount() {
        return outputPortCount;
    }

    public boolean hasLogicHousing() {
        return hasLogicHousing;
    }

    public boolean hasResearchDrive() {
        return hasResearchDrive;
    }

    public boolean hasMaterialStorage() {
        return hasMaterialStorage;
    }

    public boolean hasAugmenter() {
        return hasAugmenter;
    }

    public boolean hasLinkPort() {
        return hasLinkPort;
    }

    public boolean stationNetworkLinked() {
        return stationNetworkLinked;
    }

    public int teamStationNetworkCount() {
        return teamStationNetworkCount;
    }

    public boolean teamStationNetworkValid() {
        return teamStationNetworkValid;
    }

    private static TeamStationNetworkSnapshot networkSnapshot(ResearchControllerBlockEntity blockEntity) {
        if (blockEntity == null || blockEntity.getLevel() == null || blockEntity.getLevel().getServer() == null || blockEntity.teamId().isBlank()) {
            return TeamStationNetworkSnapshot.empty(blockEntity == null ? "" : blockEntity.teamId());
        }
        return StationNetworkService.snapshot(blockEntity.getLevel().getServer(), blockEntity.teamId());
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
