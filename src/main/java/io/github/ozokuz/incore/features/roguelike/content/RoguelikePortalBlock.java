package io.github.ozokuz.incore.features.roguelike.content;

import io.github.ozokuz.incore.Registration;
import io.github.ozokuz.incore.features.roguelike.RoguelikePortalShape;
import io.github.ozokuz.incore.features.roguelike.RoguelikeConstants;
import io.github.ozokuz.incore.features.roguelike.RoguelikeService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RoguelikePortalBlock extends Block implements EntityBlock {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    private static final VoxelShape X_SHAPE = Block.box(0.0D, 0.0D, 6.0D, 16.0D, 16.0D, 10.0D);
    private static final VoxelShape Z_SHAPE = Block.box(6.0D, 0.0D, 0.0D, 10.0D, 16.0D, 16.0D);

    public RoguelikePortalBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_CYAN)
                .strength(5.0F)
                .sound(SoundType.GLASS)
                .lightLevel(state -> 11)
                .noCollission()
                .noOcclusion());
        this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos blockPos, @NotNull BlockState blockState) {
        return new RoguelikePortalBlockEntity(blockPos, blockState);
    }

    @Override
    protected @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return state.getValue(AXIS) == Direction.Axis.X ? X_SHAPE : Z_SHAPE;
    }

    @Override
    protected @NotNull BlockState updateShape(@NotNull BlockState state, @NotNull Direction direction, @NotNull BlockState neighborState,
                                              @NotNull LevelAccessor level, @NotNull BlockPos currentPos, @NotNull BlockPos neighborPos) {
        if (level instanceof Level world && world.dimension().equals(RoguelikeConstants.DUNGEON_DIMENSION)) {
            return super.updateShape(state, direction, neighborState, level, currentPos, neighborPos);
        }

        Direction.Axis directionAxis = direction.getAxis();
        Direction.Axis portalAxis = state.getValue(AXIS);
        boolean isPerpendicularHorizontal = directionAxis.isHorizontal() && directionAxis != portalAxis;
        boolean neighborIsPortal = neighborState.is(Registration.ROGUELIKE_PORTAL_BLOCK.get());
        boolean frameValid = new RoguelikePortalShape(level, currentPos, portalAxis).isComplete();
        if (isPerpendicularHorizontal && !neighborIsPortal && !frameValid) {
            return Blocks.AIR.defaultBlockState();
        }

        return super.updateShape(state, direction, neighborState, level, currentPos, neighborPos);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(ItemStack stack, @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                                                       @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hitResult) {
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }

        var be = level.getBlockEntity(pos);
        if (!(be instanceof RoguelikePortalBlockEntity portal)) {
            return ItemInteractionResult.FAIL;
        }

        boolean handled = RoguelikeService.onPortalInteracted(player, hand, portal, pos);
        return handled ? ItemInteractionResult.SUCCESS : ItemInteractionResult.FAIL;
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                                                        @NotNull Player player, @NotNull BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        var be = level.getBlockEntity(pos);
        if (!(be instanceof RoguelikePortalBlockEntity portal)) {
            return InteractionResult.FAIL;
        }

        boolean entered = RoguelikeService.tryEnterPortal(player, portal, pos);
        return entered ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    @Override
    protected void entityInside(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Entity entity) {
        if (level.isClientSide) {
            return;
        }

        if (!(entity instanceof ServerPlayer player) || player.isOnPortalCooldown()) {
            return;
        }

        var be = level.getBlockEntity(pos);
        if (!(be instanceof RoguelikePortalBlockEntity portal)) {
            return;
        }

        if (RoguelikeService.tryEnterPortal(player, portal, pos)) {
            player.setPortalCooldown();
        }
    }
}
