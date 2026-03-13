package io.github.ozokuz.incore.features.research.station;

import io.github.ozokuz.incore.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        List<BlockPos> linkingPorts = new ArrayList<>();
        List<BlockPos> wirelessLinks = new ArrayList<>();
        ResearchPowerFamily powerFamily = null;
        int powerInputTier = 0;
        int controllerCount = 0;
        Block inputBlockType = null;
        BlockPos logicHousingPos = null;
        BlockPos researchDrivePos = null;
        BlockPos materialStoragePos = null;
        List<BlockPos> outputPortPositions = new ArrayList<>(2);
        BlockPos augmenterPos = null;

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
                    } else if (state.getBlock() instanceof LogicHousingBlock) {
                        if (logicHousingPos != null) {
                            return ResearchStationTopology.unformed();
                        }
                        logicHousingPos = pos.immutable();
                    } else if (state.getBlock() instanceof ResearchDriveBlock) {
                        if (researchDrivePos != null) {
                            return ResearchStationTopology.unformed();
                        }
                        researchDrivePos = pos.immutable();
                    } else if (state.getBlock() instanceof MaterialStorageBlock) {
                        if (materialStoragePos != null) {
                            return ResearchStationTopology.unformed();
                        }
                        materialStoragePos = pos.immutable();
                    } else if (state.getBlock() instanceof OutputPortBlock) {
                        if (outputPortPositions.size() >= 2) {
                            return ResearchStationTopology.unformed();
                        }
                        outputPortPositions.add(pos.immutable());
                    } else if (state.getBlock() instanceof WirelessLinkBlock) {
                        if (wirelessLinks.size() >= 1) {
                            return ResearchStationTopology.unformed();
                        }
                        wirelessLinks.add(pos.immutable());
                    } else if (state.getBlock() instanceof AugmenterBlock) {
                        if (augmenterPos != null) {
                            return ResearchStationTopology.unformed();
                        }
                        augmenterPos = pos.immutable();
                    } else if (state.getBlock() instanceof LinkingPortBlock) {
                        linkingPorts.add(pos.immutable());
                    } else if (state.getBlock() != casingBlock) {
                        return ResearchStationTopology.unformed();
                    }

                    connected.add(pos.immutable());
                }
            }
        }

        if (controllerCount != 1
                || inputs.isEmpty()
                || logicHousingPos == null
                || researchDrivePos == null
                || materialStoragePos == null) {
            return ResearchStationTopology.unformed();
        }

        if (!isValidControllerPlacement(level.getBlockState(controllerPos), controllerPos, minCorner, sizeX, sizeY, sizeZ)) {
            return ResearchStationTopology.unformed();
        }

        if (sharesPartsWithAnotherStation(level, controllerPos, connected)) {
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
        linkingPorts.sort(Comparator.comparingLong(BlockPos::asLong));
        wirelessLinks.sort(Comparator.comparingLong(BlockPos::asLong));
        outputPortPositions.sort(Comparator.comparingLong(BlockPos::asLong));
        return new ResearchStationTopology(
                true,
                connected,
                inputs,
                linkingPorts,
                wirelessLinks,
                logicHousingPos,
                researchDrivePos,
                materialStoragePos,
                outputPortPositions,
                augmenterPos,
                powerFamily,
                powerInputTier
        );
    }

    private static boolean sharesPartsWithAnotherStation(Level level, BlockPos controllerPos, List<BlockPos> connectedParts) {
        if (!(level instanceof ServerLevel serverLevel) || connectedParts.isEmpty()) {
            return false;
        }

        Set<BlockPos> claimedParts = new HashSet<>(connectedParts);
        for (ResearchControllerBlockEntity otherController : ResearchMultiblockStationRegistry.controllersForLevel(serverLevel)) {
            if (otherController == null || otherController.getBlockPos().equals(controllerPos)) {
                continue;
            }
            for (BlockPos otherPart : otherController.connectedParts()) {
                if (claimedParts.contains(otherPart)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isValidControllerPlacement(
            BlockState controllerState,
            BlockPos controllerPos,
            BlockPos minCorner,
            int sizeX,
            int sizeY,
            int sizeZ
    ) {
        if (!(controllerState.getBlock() instanceof AbstractResearchControllerBlock)
                || !controllerState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return false;
        }

        Direction facing = controllerState.getValue(BlockStateProperties.HORIZONTAL_FACING);
        int minX = minCorner.getX();
        int maxX = minX + sizeX - 1;
        int minY = minCorner.getY();
        int maxY = minY + sizeY - 1;
        int minZ = minCorner.getZ();
        int maxZ = minZ + sizeZ - 1;

        if (controllerPos.getY() < minY || controllerPos.getY() > maxY) {
            return false;
        }

        if (sizeX == 3) {
            int centerX = minX + 1;
            if (controllerPos.getX() != centerX) {
                return false;
            }
            if (controllerPos.getZ() == minZ) {
                return facing == Direction.NORTH;
            }
            if (controllerPos.getZ() == maxZ) {
                return facing == Direction.SOUTH;
            }
            return false;
        }

        if (sizeZ == 3) {
            int centerZ = minZ + 1;
            if (controllerPos.getZ() != centerZ) {
                return false;
            }
            if (controllerPos.getX() == minX) {
                return facing == Direction.WEST;
            }
            if (controllerPos.getX() == maxX) {
                return facing == Direction.EAST;
            }
            return false;
        }

        return false;
    }

    private record Candidate(BlockPos minCorner, ResearchStationTopology topology) {
    }
}
