package ozokuz.incore.features.research.station;

import ozokuz.incore.Registration;
import ozokuz.incore.features.machines.multiblock.MachinePowerFamily;
import ozokuz.incore.features.machines.multiblock.MachinePowerInputBlockProvider;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class ResearchOrchestratorMultiblockValidator {
    private static final int MIN_HEIGHT = 2;
    private static final int[][] FOOTPRINTS = {
            {3, 2},
            {2, 3}
    };

    private ResearchOrchestratorMultiblockValidator() {
    }

    public static ResearchOrchestratorTopology validate(Level level, BlockPos controllerPos) {
        if (level == null || controllerPos == null) {
            return ResearchOrchestratorTopology.unformed();
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
                        ResearchOrchestratorTopology topology = validateCandidate(level, controllerPos, minCorner, sizeX, MIN_HEIGHT, sizeZ, casingBlock);
                        if (topology.formed()) {
                            candidates.add(new Candidate(minCorner, topology));
                        }
                    }
                }
            }
        }

        if (candidates.isEmpty()) {
            return ResearchOrchestratorTopology.unformed();
        }

        candidates.sort(Comparator
                .comparingInt((Candidate c) -> c.minCorner().getX())
                .thenComparingInt(c -> c.minCorner().getY())
                .thenComparingInt(c -> c.minCorner().getZ()));
        return candidates.get(0).topology();
    }

    private static ResearchOrchestratorTopology validateCandidate(
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
        MachinePowerFamily powerFamily = null;
        int powerInputTier = 0;
        Block inputBlockType = null;
        int controllerCount = 0;
        BlockPos wirelessLinkPos = null;
        BlockPos orchestrationDrivePos = null;
        BlockPos augmenterPos = null;

        for (int dx = 0; dx < sizeX; dx++) {
            for (int dy = 0; dy < sizeY; dy++) {
                for (int dz = 0; dz < sizeZ; dz++) {
                    BlockPos pos = minCorner.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);

                    if (pos.equals(controllerPos)) {
                        if (!state.is(Registration.RESEARCH_ORCHESTRATOR_CONTROLLER_BLOCK.get())) {
                            return ResearchOrchestratorTopology.unformed();
                        }
                        controllerCount++;
                    } else if (state.getBlock() instanceof MachinePowerInputBlockProvider inputBlock) {
                        inputs.add(pos.immutable());
                        if (inputBlockType == null) {
                            inputBlockType = state.getBlock();
                            powerFamily = inputBlock.family();
                            powerInputTier = inputBlock.powerTier();
                        } else if (state.getBlock() != inputBlockType
                                || inputBlock.family() != powerFamily
                                || inputBlock.powerTier() != powerInputTier) {
                            return ResearchOrchestratorTopology.unformed();
                        }
                    } else if (state.is(Registration.LINKING_PORT_BLOCK.get())) {
                        linkingPorts.add(pos.immutable());
                    } else if (state.is(Registration.WIRELESS_LINK_BLOCK.get())) {
                        if (wirelessLinkPos != null) {
                            return ResearchOrchestratorTopology.unformed();
                        }
                        wirelessLinkPos = pos.immutable();
                    } else if (state.is(Registration.ORCHESTRATION_DRIVE_BLOCK.get())) {
                        if (orchestrationDrivePos != null) {
                            return ResearchOrchestratorTopology.unformed();
                        }
                        orchestrationDrivePos = pos.immutable();
                    } else if (state.is(Registration.AUGMENTER_BLOCK.get())) {
                        if (augmenterPos != null) {
                            return ResearchOrchestratorTopology.unformed();
                        }
                        augmenterPos = pos.immutable();
                    } else if (state.getBlock() != casingBlock) {
                        return ResearchOrchestratorTopology.unformed();
                    }
                    connected.add(pos.immutable());
                }
            }
        }

        if (controllerCount != 1 || inputs.isEmpty() || orchestrationDrivePos == null || (linkingPorts.isEmpty() && wirelessLinkPos == null)) {
            return ResearchOrchestratorTopology.unformed();
        }
        if (!isValidControllerPlacement(level.getBlockState(controllerPos), controllerPos, minCorner, sizeX, sizeY, sizeZ)) {
            return ResearchOrchestratorTopology.unformed();
        }
        if (sharesPartsWithOtherMultiblocks(level, controllerPos, connected)) {
            return ResearchOrchestratorTopology.unformed();
        }

        connected.sort(Comparator.comparingLong(BlockPos::asLong));
        inputs.sort(Comparator.comparingLong(BlockPos::asLong));
        linkingPorts.sort(Comparator.comparingLong(BlockPos::asLong));
        return new ResearchOrchestratorTopology(true, connected, inputs, linkingPorts, wirelessLinkPos, orchestrationDrivePos, augmenterPos, powerFamily, powerInputTier);
    }

    private static boolean sharesPartsWithOtherMultiblocks(Level level, BlockPos controllerPos, List<BlockPos> connectedParts) {
        if (!(level instanceof ServerLevel serverLevel) || connectedParts.isEmpty()) {
            return false;
        }
        Set<BlockPos> claimed = new HashSet<>(connectedParts);
        for (ResearchControllerBlockEntity station : ResearchMultiblockStationRegistry.controllersForLevel(serverLevel)) {
            if (station == null || station.getBlockPos().equals(controllerPos)) {
                continue;
            }
            for (BlockPos pos : station.connectedParts()) {
                if (claimed.contains(pos)) {
                    return true;
                }
            }
        }
        for (ResearchOrchestratorControllerBlockEntity orchestrator : ResearchOrchestratorRegistry.orchestratorsForLevel(serverLevel)) {
            if (orchestrator == null || orchestrator.getBlockPos().equals(controllerPos)) {
                continue;
            }
            for (BlockPos pos : orchestrator.connectedParts()) {
                if (claimed.contains(pos)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isValidControllerPlacement(BlockState controllerState, BlockPos controllerPos, BlockPos minCorner, int sizeX, int sizeY, int sizeZ) {
        if (!controllerState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
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

    private record Candidate(BlockPos minCorner, ResearchOrchestratorTopology topology) {
    }
}
