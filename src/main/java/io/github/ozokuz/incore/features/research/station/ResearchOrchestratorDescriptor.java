package io.github.ozokuz.incore.features.research.station;

import net.minecraft.core.BlockPos;

import java.util.List;

public record ResearchOrchestratorDescriptor(
        String orchestratorId,
        String teamId,
        String dimensionId,
        BlockPos controllerPos,
        boolean formed,
        ResearchPowerFamily powerFamily,
        int powerInputTier,
        List<BlockPos> powerInputPositions,
        List<BlockPos> linkingPortPositions,
        BlockPos wirelessLinkPos,
        BlockPos orchestrationDrivePos,
        BlockPos augmenterPos
) {
    public ResearchOrchestratorDescriptor {
        orchestratorId = orchestratorId == null ? "" : orchestratorId;
        teamId = teamId == null ? "" : teamId;
        dimensionId = dimensionId == null ? "" : dimensionId;
        powerInputPositions = powerInputPositions == null ? List.of() : List.copyOf(powerInputPositions);
        powerInputTier = Math.max(0, powerInputTier);
    }
}
