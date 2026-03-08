package io.github.ozokuz.incore.features.researchv2.station;

import net.minecraft.core.BlockPos;

import java.util.List;

public record ResearchStationEndpoints(
        List<BlockPos> inputs,
        List<BlockPos> inventories,
        BlockPos logicHousing,
        BlockPos researchDrive,
        BlockPos materialStorage,
        List<BlockPos> outputPorts,
        BlockPos augmenter
) {
    public ResearchStationEndpoints {
        inputs = inputs == null ? List.of() : List.copyOf(inputs);
        inventories = inventories == null ? List.of() : List.copyOf(inventories);
        outputPorts = outputPorts == null ? List.of() : List.copyOf(outputPorts);
    }
}
