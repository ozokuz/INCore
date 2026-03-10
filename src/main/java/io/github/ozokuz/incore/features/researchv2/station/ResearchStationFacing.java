package io.github.ozokuz.incore.features.researchv2.station;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

public final class ResearchStationFacing {
    private ResearchStationFacing() {
    }

    public static BlockState stateForPlacement(BlockState defaultState, BlockPlaceContext context) {
        return defaultState.setValue(BlockStateProperties.FACING, context.getNearestLookingDirection().getOpposite());
    }

    public static BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(BlockStateProperties.FACING, rotation.rotate(state.getValue(BlockStateProperties.FACING)));
    }

    public static BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(BlockStateProperties.FACING)));
    }

    public static Direction frontFace(BlockState state) {
        return state.getValue(BlockStateProperties.FACING);
    }

    public static boolean isFrontFace(BlockState state, @Nullable Direction side) {
        return side != null && side == frontFace(state);
    }
}
