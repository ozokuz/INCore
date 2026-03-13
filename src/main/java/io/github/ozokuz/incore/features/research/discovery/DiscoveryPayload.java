package io.github.ozokuz.incore.features.research.discovery;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record DiscoveryPayload(
        List<ResourceLocation> nodeIds,
        String sourceType,
        String sourceId,
        String displayName,
        String originTeamId
) {
    public DiscoveryPayload {
        nodeIds = List.copyOf(nodeIds == null ? List.of() : nodeIds);
        sourceType = sourceType == null ? "" : sourceType.strip();
        sourceId = sourceId == null ? "" : sourceId.strip();
        displayName = displayName == null ? "" : displayName.strip();
        originTeamId = originTeamId == null ? "" : originTeamId.strip();
    }
}
