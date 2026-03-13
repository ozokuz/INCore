package io.github.ozokuz.incore.features.research.station;

import io.github.ozokuz.incore.Registration;
import io.github.ozokuz.incore.features.research.station.network.StationNetworkService;
import io.github.ozokuz.incore.features.research.station.network.TeamStationNetworkSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;

public class ResearchOrchestratorControllerMenu extends AbstractContainerMenu {
    private final BlockPos blockPos;
    private boolean formed;
    private boolean teamLinked;
    private int connectedPartCount;
    private int inputCount;
    private int linkingPortCount;
    private boolean hasWirelessLink;
    private boolean hasOrchestrationDrive;
    private boolean hasAugmenter;
    private int powerFamilyOrdinal = -1;
    private int powerInputTier;
    private boolean orchestratorRequired;
    private boolean orchestratorPresent;
    private boolean orchestratorValid;
    private int cableCapacityPerLink;
    private int wirelessCapacity;
    private int wirelessRange;
    private boolean infiniteWireless;
    private boolean interdimensionalWireless;
    private int validWirelessStations;
    private int invalidWirelessStations;
    private int teamStationNetworkCount;
    private boolean teamStationNetworkValid;

    public ResearchOrchestratorControllerMenu(int containerId, Inventory playerInventory, BlockPos blockPos) {
        super(Registration.RESEARCH_ORCHESTRATOR_CONTROLLER_MENU.get(), containerId);
        this.blockPos = blockPos.immutable();
        ResearchOrchestratorControllerBlockEntity blockEntity = resolveBlockEntity(playerInventory);
        syncFromBlockEntity(blockEntity);

        addDataSlot(slot(
                () -> blockEntity != null && blockEntity.isFormed() ? 1 : 0,
                value -> formed = value > 0
        ));
        addDataSlot(slot(
                () -> blockEntity != null && !blockEntity.teamId().isBlank() ? 1 : 0,
                value -> teamLinked = value > 0
        ));
        addDataSlot(slot(
                () -> blockEntity == null ? 0 : blockEntity.connectedParts().size(),
                value -> connectedPartCount = Math.max(0, value)
        ));
        addDataSlot(slot(
                () -> blockEntity == null ? 0 : blockEntity.powerInputPositions().size(),
                value -> inputCount = Math.max(0, value)
        ));
        addDataSlot(slot(
                () -> blockEntity == null ? 0 : blockEntity.linkingPortPositions().size(),
                value -> linkingPortCount = Math.max(0, value)
        ));
        addDataSlot(slot(
                () -> blockEntity != null && blockEntity.wirelessLinkPos() != null ? 1 : 0,
                value -> hasWirelessLink = value > 0
        ));
        addDataSlot(slot(
                () -> blockEntity != null && blockEntity.orchestrationDrivePos() != null ? 1 : 0,
                value -> hasOrchestrationDrive = value > 0
        ));
        addDataSlot(slot(
                () -> blockEntity != null && blockEntity.augmenterPos() != null ? 1 : 0,
                value -> hasAugmenter = value > 0
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
                () -> orchestrationSnapshot(blockEntity).orchestratorRequired() ? 1 : 0,
                value -> orchestratorRequired = value > 0
        ));
        addDataSlot(slot(
                () -> orchestrationSnapshot(blockEntity).orchestratorPresent() ? 1 : 0,
                value -> orchestratorPresent = value > 0
        ));
        addDataSlot(slot(
                () -> orchestrationSnapshot(blockEntity).orchestratorValid() ? 1 : 0,
                value -> orchestratorValid = value > 0
        ));
        addDataSlot(slot(
                () -> orchestrationSnapshot(blockEntity).cableCapacityPerLink(),
                value -> cableCapacityPerLink = Math.max(0, value)
        ));
        addDataSlot(slot(
                () -> orchestrationSnapshot(blockEntity).wirelessCapacity(),
                value -> wirelessCapacity = Math.max(0, value)
        ));
        addDataSlot(slot(
                () -> orchestrationSnapshot(blockEntity).wirelessRange(),
                value -> wirelessRange = Math.max(0, value)
        ));
        addDataSlot(slot(
                () -> orchestrationSnapshot(blockEntity).infiniteWireless() ? 1 : 0,
                value -> infiniteWireless = value > 0
        ));
        addDataSlot(slot(
                () -> orchestrationSnapshot(blockEntity).interdimensionalWireless() ? 1 : 0,
                value -> interdimensionalWireless = value > 0
        ));
        addDataSlot(slot(
                () -> orchestrationSnapshot(blockEntity).validWirelessStationIds().size(),
                value -> validWirelessStations = Math.max(0, value)
        ));
        addDataSlot(slot(
                () -> orchestrationSnapshot(blockEntity).invalidWirelessStationIds().size(),
                value -> invalidWirelessStations = Math.max(0, value)
        ));
        addDataSlot(slot(
                () -> networkSnapshot(blockEntity).stationNetworkCount(),
                value -> teamStationNetworkCount = Math.max(0, value)
        ));
        addDataSlot(slot(
                () -> networkSnapshot(blockEntity).stationNetworkValid() ? 1 : 0,
                value -> teamStationNetworkValid = value > 0
        ));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (!(player.level().getBlockEntity(blockPos) instanceof ResearchOrchestratorControllerBlockEntity)) {
            return false;
        }
        return player.distanceToSqr(blockPos.getX() + 0.5D, blockPos.getY() + 0.5D, blockPos.getZ() + 0.5D) <= 64.0D;
    }

    public boolean formed() {
        return formed;
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

    public int linkingPortCount() {
        return linkingPortCount;
    }

    public boolean hasWirelessLink() {
        return hasWirelessLink;
    }

    public boolean hasOrchestrationDrive() {
        return hasOrchestrationDrive;
    }

    public boolean hasAugmenter() {
        return hasAugmenter;
    }

    public ResearchPowerFamily powerFamily() {
        return powerFamilyOrdinal < 0 || powerFamilyOrdinal >= ResearchPowerFamily.values().length
                ? null
                : ResearchPowerFamily.values()[powerFamilyOrdinal];
    }

    public int powerInputTier() {
        return powerInputTier;
    }

    public boolean orchestratorRequired() {
        return orchestratorRequired;
    }

    public boolean orchestratorPresent() {
        return orchestratorPresent;
    }

    public boolean orchestratorValid() {
        return orchestratorValid;
    }

    public int cableCapacityPerLink() {
        return cableCapacityPerLink;
    }

    public int wirelessCapacity() {
        return wirelessCapacity;
    }

    public int wirelessRange() {
        return wirelessRange;
    }

    public boolean infiniteWireless() {
        return infiniteWireless;
    }

    public boolean interdimensionalWireless() {
        return interdimensionalWireless;
    }

    public int validWirelessStations() {
        return validWirelessStations;
    }

    public int invalidWirelessStations() {
        return invalidWirelessStations;
    }

    public int teamStationNetworkCount() {
        return teamStationNetworkCount;
    }

    public boolean teamStationNetworkValid() {
        return teamStationNetworkValid;
    }

    private ResearchOrchestratorControllerBlockEntity resolveBlockEntity(Inventory playerInventory) {
        return playerInventory.player.level().getBlockEntity(blockPos) instanceof ResearchOrchestratorControllerBlockEntity orchestrator
                ? orchestrator
                : null;
    }

    private void syncFromBlockEntity(ResearchOrchestratorControllerBlockEntity blockEntity) {
        formed = blockEntity != null && blockEntity.isFormed();
        teamLinked = blockEntity != null && !blockEntity.teamId().isBlank();
        connectedPartCount = blockEntity == null ? 0 : blockEntity.connectedParts().size();
        inputCount = blockEntity == null ? 0 : blockEntity.powerInputPositions().size();
        linkingPortCount = blockEntity == null ? 0 : blockEntity.linkingPortPositions().size();
        hasWirelessLink = blockEntity != null && blockEntity.wirelessLinkPos() != null;
        hasOrchestrationDrive = blockEntity != null && blockEntity.orchestrationDrivePos() != null;
        hasAugmenter = blockEntity != null && blockEntity.augmenterPos() != null;
        powerFamilyOrdinal = blockEntity == null || blockEntity.powerFamily() == null ? -1 : blockEntity.powerFamily().ordinal();
        powerInputTier = blockEntity == null ? 0 : blockEntity.powerInputTier();
        TeamResearchOrchestrationSnapshot orchestrationSnapshot = orchestrationSnapshot(blockEntity);
        orchestratorRequired = orchestrationSnapshot.orchestratorRequired();
        orchestratorPresent = orchestrationSnapshot.orchestratorPresent();
        orchestratorValid = orchestrationSnapshot.orchestratorValid();
        cableCapacityPerLink = orchestrationSnapshot.cableCapacityPerLink();
        wirelessCapacity = orchestrationSnapshot.wirelessCapacity();
        wirelessRange = orchestrationSnapshot.wirelessRange();
        infiniteWireless = orchestrationSnapshot.infiniteWireless();
        interdimensionalWireless = orchestrationSnapshot.interdimensionalWireless();
        validWirelessStations = orchestrationSnapshot.validWirelessStationIds().size();
        invalidWirelessStations = orchestrationSnapshot.invalidWirelessStationIds().size();
        TeamStationNetworkSnapshot networkSnapshot = networkSnapshot(blockEntity);
        teamStationNetworkCount = networkSnapshot.stationNetworkCount();
        teamStationNetworkValid = networkSnapshot.stationNetworkValid();
    }

    private static TeamResearchOrchestrationSnapshot orchestrationSnapshot(ResearchOrchestratorControllerBlockEntity blockEntity) {
        if (blockEntity == null || blockEntity.getLevel() == null || blockEntity.getLevel().getServer() == null || blockEntity.teamId().isBlank()) {
            return TeamResearchOrchestrationSnapshot.notRequired(blockEntity == null ? "" : blockEntity.teamId());
        }
        return ResearchOrchestrationService.snapshot(blockEntity.getLevel().getServer(), blockEntity.teamId());
    }

    private static TeamStationNetworkSnapshot networkSnapshot(ResearchOrchestratorControllerBlockEntity blockEntity) {
        if (blockEntity == null || blockEntity.getLevel() == null || blockEntity.getLevel().getServer() == null || blockEntity.teamId().isBlank()) {
            return TeamStationNetworkSnapshot.empty(blockEntity == null ? "" : blockEntity.teamId());
        }
        return StationNetworkService.snapshot(blockEntity.getLevel().getServer(), blockEntity.teamId());
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

    @FunctionalInterface
    private interface IntSupplier {
        int getAsInt();
    }

    @FunctionalInterface
    private interface IntConsumer {
        void accept(int value);
    }
}
