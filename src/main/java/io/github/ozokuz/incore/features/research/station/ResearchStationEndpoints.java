package io.github.ozokuz.incore.features.research.station;

import java.util.List;
import net.minecraft.core.BlockPos;

public record ResearchStationEndpoints(
        List<BlockPos> inputs,
        List<BlockPos> linkingPorts,
        List<BlockPos> wirelessLinks,
        List<BlockPos> inventories,
        BlockPos logicHousing,
        BlockPos researchDrive,
        BlockPos materialStorage,
        List<BlockPos> outputPorts,
        BlockPos augmenter
) {
    public ResearchStationEndpoints {
        inputs = inputs == null ? List.of() : List.copyOf(inputs);
        linkingPorts = linkingPorts == null ? List.of() : List.copyOf(linkingPorts);
        wirelessLinks = wirelessLinks == null ? List.of() : List.copyOf(wirelessLinks);
        inventories = inventories == null ? List.of() : List.copyOf(inventories);
        outputPorts = outputPorts == null ? List.of() : List.copyOf(outputPorts);
    }
}
