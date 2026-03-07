package io.github.ozokuz.incore.features.researchv2.station;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashSet;
import java.util.Set;

public final class ResearchStationMultiblockOrchestrator {
    private static final int SEARCH_RADIUS = 2;

    private ResearchStationMultiblockOrchestrator() {
    }

    public static void onBlockChanged(Level level, BlockPos changedPos) {
        if (level == null || level.isClientSide || changedPos == null) {
            return;
        }

        Set<BlockPos> controllerPositions = new HashSet<>();
        BlockPos min = changedPos.offset(-SEARCH_RADIUS, -SEARCH_RADIUS, -SEARCH_RADIUS);
        BlockPos max = changedPos.offset(SEARCH_RADIUS, SEARCH_RADIUS, SEARCH_RADIUS);

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ResearchControllerBlockEntity controller) {
                controllerPositions.add(controller.getBlockPos().immutable());
            }
        }

        for (BlockPos controllerPos : controllerPositions) {
            BlockEntity blockEntity = level.getBlockEntity(controllerPos);
            if (blockEntity instanceof ResearchControllerBlockEntity controller) {
                controller.revalidateStructure();
            }
        }
    }
}
