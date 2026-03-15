package ozokuz.incore.features.research.station;

import ozokuz.incore.Registration;
import ozokuz.incore.features.machines.multiblock.MachinePowerFamily;
import ozokuz.incore.features.research.ResearchManager;
import ozokuz.incore.features.research.state.ActiveResearchRun;
import ozokuz.incore.features.research.state.ResearchQueueEntry;
import ozokuz.incore.features.research.state.ResearchQueueStatus;
import ozokuz.incore.features.research.station.network.StationNetworkService;
import ozokuz.incore.features.research.station.network.TeamStationNetworkSnapshot;
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
    private int runTickProgress;
    private int runTickRequired = 1;
    private int completedRuns;
    private int requiredRuns = 1;
    private int queueStatusOrdinal = -1;

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
        addDataSlot(slot(
                () -> displayRun(blockEntity) == null ? 0 : displayRun(blockEntity).runTickProgress(),
                value -> runTickProgress = Math.max(0, value)
        ));
        addDataSlot(slot(
                () -> displayRun(blockEntity) == null ? 1 : Math.max(1, displayRun(blockEntity).runTickRequired()),
                value -> runTickRequired = Math.max(1, value)
        ));
        addDataSlot(slot(
                () -> queueHead(blockEntity) == null ? 0 : queueHead(blockEntity).completedRuns(),
                value -> completedRuns = Math.max(0, value)
        ));
        addDataSlot(slot(
                () -> queueHead(blockEntity) == null ? 1 : Math.max(1, queueHead(blockEntity).requiredRuns()),
                value -> requiredRuns = Math.max(1, value)
        ));
        addDataSlot(slot(
                () -> queueHead(blockEntity) == null ? -1 : queueHead(blockEntity).status().ordinal(),
                value -> queueStatusOrdinal = value
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
        ResearchQueueEntry head = queueHead(blockEntity);
        ActiveResearchRun displayRun = displayRun(blockEntity);
        runTickProgress = displayRun == null ? (head == null ? 0 : Math.max(0, head.runTickProgress())) : Math.max(0, displayRun.runTickProgress());
        runTickRequired = displayRun == null ? (head == null ? 1 : Math.max(1, head.runTickRequired())) : Math.max(1, displayRun.runTickRequired());
        completedRuns = head == null ? 0 : Math.max(0, head.completedRuns());
        requiredRuns = head == null ? 1 : Math.max(1, head.requiredRuns());
        queueStatusOrdinal = head == null ? -1 : head.status().ordinal();
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

    public MachinePowerFamily powerFamily() {
        return powerFamilyOrdinal < 0 || powerFamilyOrdinal >= MachinePowerFamily.values().length
                ? null
                : MachinePowerFamily.values()[powerFamilyOrdinal];
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

    public int runTickProgress() {
        return runTickProgress;
    }

    public int runTickRequired() {
        return runTickRequired;
    }

    public int completedRuns() {
        return completedRuns;
    }

    public int requiredRuns() {
        return requiredRuns;
    }

    public boolean hasActiveRun() {
        return queueStatusOrdinal >= 0;
    }

    public ResearchQueueStatus queueStatus() {
        return queueStatusOrdinal < 0 || queueStatusOrdinal >= ResearchQueueStatus.values().length
                ? null
                : ResearchQueueStatus.values()[queueStatusOrdinal];
    }

    public int runProgressScaled(int width) {
        int total = Math.max(1, runTickRequired());
        return Math.clamp((runTickProgress() * width) / total, 0, width);
    }

    private static TeamStationNetworkSnapshot networkSnapshot(ResearchControllerBlockEntity blockEntity) {
        if (blockEntity == null || blockEntity.getLevel() == null || blockEntity.getLevel().getServer() == null || blockEntity.teamId().isBlank()) {
            return TeamStationNetworkSnapshot.empty(blockEntity == null ? "" : blockEntity.teamId());
        }
        return StationNetworkService.snapshot(blockEntity.getLevel().getServer(), blockEntity.teamId());
    }

    private static ResearchQueueEntry queueHead(ResearchControllerBlockEntity blockEntity) {
        if (blockEntity == null || blockEntity.getLevel() == null || blockEntity.getLevel().getServer() == null || blockEntity.teamId().isBlank()) {
            return null;
        }
        var state = ResearchManager.ensureTeamState(blockEntity.getLevel().getServer(), blockEntity.teamId());
        return state.researchQueue().isEmpty() ? null : state.researchQueue().get(0);
    }

    private static ActiveResearchRun displayRun(ResearchControllerBlockEntity blockEntity) {
        ResearchQueueEntry head = queueHead(blockEntity);
        return head == null || blockEntity == null ? null : head.activeRun(blockEntity.stationId());
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
