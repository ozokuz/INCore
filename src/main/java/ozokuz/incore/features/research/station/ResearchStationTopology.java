package ozokuz.incore.features.research.station;

import ozokuz.incore.features.machines.multiblock.MachinePowerFamily;
import java.util.List;
import net.minecraft.core.BlockPos;

public record ResearchStationTopology(
        boolean formed,
        List<BlockPos> connectedParts,
        List<BlockPos> inputPositions,
        List<BlockPos> linkingPortPositions,
        List<BlockPos> wirelessLinkPositions,
        BlockPos logicHousingPos,
        BlockPos researchDrivePos,
        BlockPos materialStoragePos,
        List<BlockPos> outputPortPositions,
        BlockPos augmenterPos,
        MachinePowerFamily powerFamily,
        int powerInputTier
) {
    public ResearchStationTopology {
        connectedParts = connectedParts == null ? List.of() : List.copyOf(connectedParts);
        inputPositions = inputPositions == null ? List.of() : List.copyOf(inputPositions);
        linkingPortPositions = linkingPortPositions == null ? List.of() : List.copyOf(linkingPortPositions);
        wirelessLinkPositions = wirelessLinkPositions == null ? List.of() : List.copyOf(wirelessLinkPositions);
        outputPortPositions = outputPortPositions == null ? List.of() : List.copyOf(outputPortPositions);
        powerInputTier = Math.max(0, powerInputTier);
    }

    public static ResearchStationTopology unformed() {
        return new ResearchStationTopology(false, List.of(), List.of(), List.of(), List.of(), null, null, null, List.of(), null, null, 0);
    }
}
