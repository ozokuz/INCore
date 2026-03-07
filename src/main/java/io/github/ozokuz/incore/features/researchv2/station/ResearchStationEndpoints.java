package io.github.ozokuz.incore.features.researchv2.station;

import net.minecraft.core.BlockPos;

import java.util.List;

public record ResearchStationEndpoints(
        List<BlockPos> inputs,
        List<BlockPos> inventories
) {
    public ResearchStationEndpoints {
        inputs = inputs == null ? List.of() : List.copyOf(inputs);
        inventories = inventories == null ? List.of() : List.copyOf(inventories);
    }
}
