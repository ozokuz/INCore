package ozokuz.incore.features.research.station;

import ozokuz.incore.features.machines.multiblock.MachinePowerFamily;
import java.util.List;
import net.minecraft.core.BlockPos;

public record ResearchOrchestratorTopology(
        boolean formed,
        List<BlockPos> connectedParts,
        List<BlockPos> powerInputPositions,
        List<BlockPos> linkingPortPositions,
        BlockPos wirelessLinkPos,
        BlockPos orchestrationDrivePos,
        BlockPos augmenterPos,
        MachinePowerFamily powerFamily,
        int powerInputTier
) {
    public ResearchOrchestratorTopology {
        connectedParts = connectedParts == null ? List.of() : List.copyOf(connectedParts);
        powerInputPositions = powerInputPositions == null ? List.of() : List.copyOf(powerInputPositions);
        linkingPortPositions = linkingPortPositions == null ? List.of() : List.copyOf(linkingPortPositions);
        powerInputTier = Math.max(0, powerInputTier);
    }

    public static ResearchOrchestratorTopology unformed() {
        return new ResearchOrchestratorTopology(false, List.of(), List.of(), List.of(), null, null, null, null, 0);
    }
}
