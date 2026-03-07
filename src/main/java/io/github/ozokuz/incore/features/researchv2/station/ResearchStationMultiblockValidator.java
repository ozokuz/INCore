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

    public static ValidationResult validate(Level level, BlockPos controllerPos) {
        if (level == null || controllerPos == null) {
            return ValidationResult.unformed();
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
                        List<BlockPos> connected = validateCandidate(level, controllerPos, minCorner, sizeX, MIN_HEIGHT, sizeZ, casingBlock);
                        if (!connected.isEmpty()) {
                            candidates.add(new Candidate(minCorner, connected));
                        }
                    }
                }
            }
        }

        if (candidates.isEmpty()) {
            return ValidationResult.unformed();
        }

        candidates.sort(Comparator
                .comparingInt((Candidate candidate) -> candidate.minCorner().getX())
                .thenComparingInt(candidate -> candidate.minCorner().getY())
                .thenComparingInt(candidate -> candidate.minCorner().getZ()));

        return new ValidationResult(true, List.copyOf(candidates.get(0).connectedParts()));
    }

    private static List<BlockPos> validateCandidate(
            Level level,
            BlockPos controllerPos,
            BlockPos minCorner,
            int sizeX,
            int sizeY,
            int sizeZ,
            Block casingBlock
    ) {
        List<BlockPos> connected = new ArrayList<>(sizeX * sizeY * sizeZ);

        for (int dx = 0; dx < sizeX; dx++) {
            for (int dy = 0; dy < sizeY; dy++) {
                for (int dz = 0; dz < sizeZ; dz++) {
                    BlockPos pos = minCorner.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);

                    if (pos.equals(controllerPos)) {
                        if (!(state.getBlock() instanceof AbstractResearchControllerBlock)) {
                            return List.of();
                        }
                    } else if (state.getBlock() != casingBlock) {
                        return List.of();
                    }

                    connected.add(pos.immutable());
                }
            }
        }

        connected.sort(Comparator.comparingLong(BlockPos::asLong));
        return connected;
    }

    private record Candidate(BlockPos minCorner, List<BlockPos> connectedParts) {
    }

    public record ValidationResult(boolean formed, List<BlockPos> connectedParts) {
        public static ValidationResult unformed() {
            return new ValidationResult(false, List.of());
        }
    }
}
