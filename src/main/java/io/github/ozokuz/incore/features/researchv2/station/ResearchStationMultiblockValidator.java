package io.github.ozokuz.incore.features.researchv2.station;

import io.github.ozokuz.incore.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ResearchStationMultiblockValidator {
    private static final int MIN_HEIGHT = 2;
    private static final int[][] FOOTPRINTS = {
            {3, 2},
            {2, 3}
    };

    private ResearchStationMultiblockValidator() {
    }

    public static ResearchStationTopology validate(Level level, BlockPos controllerPos) {
        if (level == null || controllerPos == null) {
            return ResearchStationTopology.unformed();
        }

        Block casingBlock = Registration.RESEARCH_STATION_CASING_BLOCK.get();
        List<Candidate> candidates = new ArrayList<>();

        for (int[] footprint : FOOTPRINTS) {
            int sizeX = footprint[0];
            int sizeZ = footprint[1];

            for (int minX = controllerPos.getX() - sizeX + 1; minX <= controllerPos.getX(); minX++) {
                for (int minY = controllerPos.getY() - MIN_HEIGHT + 1; minY <= controllerPos.getY(); minY++) {
                    for (int minZ = controllerPos.getZ() - sizeZ + 1; minZ <= controllerPos.getZ(); minZ++) {
                        BlockPos minCorner = new BlockPos(minX, minY, minZ);
                        ResearchStationTopology topology = validateCandidate(level, controllerPos, minCorner, sizeX, MIN_HEIGHT, sizeZ, casingBlock);
                        if (topology.formed()) {
                            candidates.add(new Candidate(minCorner, topology));
                        }
                    }
                }
            }
        }

        if (candidates.isEmpty()) {
            return ResearchStationTopology.unformed();
        }

        candidates.sort(Comparator
                .comparingInt((Candidate candidate) -> candidate.minCorner().getX())
                .thenComparingInt(candidate -> candidate.minCorner().getY())
                .thenComparingInt(candidate -> candidate.minCorner().getZ()));

        return candidates.get(0).topology();
    }

    private static ResearchStationTopology validateCandidate(
            Level level,
            BlockPos controllerPos,
            BlockPos minCorner,
            int sizeX,
            int sizeY,
            int sizeZ,
            Block casingBlock
    ) {
        List<BlockPos> connected = new ArrayList<>(sizeX * sizeY * sizeZ);
        List<BlockPos> inputs = new ArrayList<>();
        ResearchPowerFamily powerFamily = null;
        int powerInputTier = 0;
        int controllerCount = 0;
        Block inputBlockType = null;

        for (int dx = 0; dx < sizeX; dx++) {
            for (int dy = 0; dy < sizeY; dy++) {
                for (int dz = 0; dz < sizeZ; dz++) {
                    BlockPos pos = minCorner.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);

                    if (pos.equals(controllerPos)) {
                        if (!(state.getBlock() instanceof AbstractResearchControllerBlock)) {
                            return ResearchStationTopology.unformed();
                        }
                        controllerCount++;
                    } else if (state.getBlock() instanceof ResearchPowerInputBlockProvider inputBlock) {
                        inputs.add(pos.immutable());
                        if (inputBlockType == null) {
                            inputBlockType = state.getBlock();
                            powerFamily = inputBlock.family();
                            powerInputTier = inputBlock.powerTier();
                        } else if (state.getBlock() != inputBlockType
                                || inputBlock.family() != powerFamily
                                || inputBlock.powerTier() != powerInputTier) {
                            return ResearchStationTopology.unformed();
                        }
                    } else if (state.getBlock() != casingBlock) {
                        return ResearchStationTopology.unformed();
                    }

                    connected.add(pos.immutable());
                }
            }
        }

        if (controllerCount != 1 || inputs.isEmpty()) {
            return ResearchStationTopology.unformed();
        }

        for (BlockPos inputPos : inputs) {
            BlockState inputState = level.getBlockState(inputPos);
            if (!(inputState.getBlock() instanceof ResearchPowerInputBlockProvider inputBlock)
                    || inputState.getBlock() != inputBlockType
                    || inputBlock.family() != powerFamily
                    || inputBlock.powerTier() != powerInputTier) {
                return ResearchStationTopology.unformed();
            }
        }

        connected.sort(Comparator.comparingLong(BlockPos::asLong));
        inputs.sort(Comparator.comparingLong(BlockPos::asLong));
        return new ResearchStationTopology(true, connected, inputs, powerFamily, powerInputTier);
    }

    private record Candidate(BlockPos minCorner, ResearchStationTopology topology) {
    }
}
