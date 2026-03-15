package ozokuz.incore.features.research.station;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.WrenchItem;
import ozokuz.incore.features.research.station.network.StationNetworkService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ResearchLinkCableBlock extends Block {
    public static final MapCodec<ResearchLinkCableBlock> CODEC = simpleCodec(ResearchLinkCableBlock::new);
    public static final BooleanProperty NORTH = net.minecraft.world.level.block.state.properties.BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = net.minecraft.world.level.block.state.properties.BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = net.minecraft.world.level.block.state.properties.BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = net.minecraft.world.level.block.state.properties.BlockStateProperties.WEST;
    public static final BooleanProperty UP = net.minecraft.world.level.block.state.properties.BlockStateProperties.UP;
    public static final BooleanProperty DOWN = net.minecraft.world.level.block.state.properties.BlockStateProperties.DOWN;
    private static final BooleanProperty CUT_NORTH = BooleanProperty.create("cut_north");
    private static final BooleanProperty CUT_EAST = BooleanProperty.create("cut_east");
    private static final BooleanProperty CUT_SOUTH = BooleanProperty.create("cut_south");
    private static final BooleanProperty CUT_WEST = BooleanProperty.create("cut_west");
    private static final BooleanProperty CUT_UP = BooleanProperty.create("cut_up");
    private static final BooleanProperty CUT_DOWN = BooleanProperty.create("cut_down");

    private static final VoxelShape CORE_SHAPE = Block.box(5.0D, 5.0D, 5.0D, 11.0D, 11.0D, 11.0D);
    private static final VoxelShape NORTH_SHAPE = Block.box(5.0D, 5.0D, 0.0D, 11.0D, 11.0D, 5.0D);
    private static final VoxelShape EAST_SHAPE = Block.box(11.0D, 5.0D, 5.0D, 16.0D, 11.0D, 11.0D);
    private static final VoxelShape SOUTH_SHAPE = Block.box(5.0D, 5.0D, 11.0D, 11.0D, 11.0D, 16.0D);
    private static final VoxelShape WEST_SHAPE = Block.box(0.0D, 5.0D, 5.0D, 5.0D, 11.0D, 11.0D);
    private static final VoxelShape UP_SHAPE = Block.box(5.0D, 11.0D, 5.0D, 11.0D, 16.0D, 11.0D);
    private static final VoxelShape DOWN_SHAPE = Block.box(5.0D, 0.0D, 5.0D, 11.0D, 5.0D, 11.0D);

    public ResearchLinkCableBlock() {
        this(Properties.of().mapColor(MapColor.METAL).strength(2.0F).sound(SoundType.CHAIN).noOcclusion());
    }

    public ResearchLinkCableBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(NORTH, false)
                .setValue(EAST, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false)
                .setValue(UP, false)
                .setValue(DOWN, false)
                .setValue(CUT_NORTH, false)
                .setValue(CUT_EAST, false)
                .setValue(CUT_SOUTH, false)
                .setValue(CUT_WEST, false)
                .setValue(CUT_UP, false)
                .setValue(CUT_DOWN, false));
    }

    @Override
    protected @NotNull MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN, CUT_NORTH, CUT_EAST, CUT_SOUTH, CUT_WEST, CUT_UP, CUT_DOWN);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return updateConnections(defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override
    protected @NotNull BlockState updateShape(
            @NotNull BlockState state,
            @NotNull Direction direction,
            @NotNull BlockState neighborState,
            @NotNull LevelAccessor level,
            @NotNull BlockPos currentPos,
            @NotNull BlockPos neighborPos
    ) {
        return state.setValue(propertyFor(direction), canConnect(state, direction, level, neighborPos));
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(
            @NotNull ItemStack stack,
            @NotNull BlockState state,
            @NotNull Level level,
            @NotNull BlockPos pos,
            @NotNull Player player,
            @NotNull InteractionHand hand,
            @NotNull net.minecraft.world.phys.BlockHitResult hitResult
    ) {
        if (!(stack.getItem() instanceof WrenchItem)) {
            return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
        }

        Direction direction = resolveInteractedDirection(state, level, pos, hitResult);
        if (direction == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }

        boolean nextCut = !state.getValue(cutPropertyFor(direction));
        BlockState updatedState = withCutState(state, level, pos, direction, nextCut);
        if (updatedState.equals(state)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        level.setBlock(pos, updatedState, Block.UPDATE_ALL);
        BlockPos neighborPos = pos.relative(direction);
        BlockState neighborState = level.getBlockState(neighborPos);
        if (neighborState.getBlock() instanceof ResearchLinkCableBlock) {
            level.setBlock(neighborPos, withCutState(neighborState, level, neighborPos, direction.getOpposite(), nextCut), Block.UPDATE_ALL);
        }
        StationNetworkService.onTopologyChanged(level);
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return shapeFor(state);
    }

    @Override
    protected @NotNull VoxelShape getCollisionShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return shapeFor(state);
    }

    @Override
    protected boolean useShapeForLightOcclusion(@NotNull BlockState state) {
        return true;
    }

    @Override
    protected void onPlace(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && !state.is(oldState.getBlock())) {
            BlockState updatedState = updateConnections(state, level, pos);
            if (!updatedState.equals(state)) {
                level.setBlock(pos, updatedState, Block.UPDATE_ALL);
            }
            StationNetworkService.onTopologyChanged(level);
        }
    }

    @Override
    protected void onRemove(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            StationNetworkService.onTopologyChanged(level);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private BlockState updateConnections(BlockState state, LevelReader level, BlockPos pos) {
        return state
                .setValue(NORTH, canConnect(state, Direction.NORTH, level, pos.north()))
                .setValue(EAST, canConnect(state, Direction.EAST, level, pos.east()))
                .setValue(SOUTH, canConnect(state, Direction.SOUTH, level, pos.south()))
                .setValue(WEST, canConnect(state, Direction.WEST, level, pos.west()))
                .setValue(UP, canConnect(state, Direction.UP, level, pos.above()))
                .setValue(DOWN, canConnect(state, Direction.DOWN, level, pos.below()));
    }

    private static BooleanProperty propertyFor(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH;
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case UP -> UP;
            case DOWN -> DOWN;
        };
    }

    private static BooleanProperty cutPropertyFor(Direction direction) {
        return switch (direction) {
            case NORTH -> CUT_NORTH;
            case EAST -> CUT_EAST;
            case SOUTH -> CUT_SOUTH;
            case WEST -> CUT_WEST;
            case UP -> CUT_UP;
            case DOWN -> CUT_DOWN;
        };
    }

    public static boolean hasOpenConnection(BlockState state, Direction direction) {
        return state.getBlock() instanceof ResearchLinkCableBlock
                && state.getValue(propertyFor(direction))
                && !state.getValue(cutPropertyFor(direction));
    }

    private boolean connectsTo(LevelReader level, BlockPos pos, Direction incomingDirection) {
        BlockState neighbor = level.getBlockState(pos);
        if (neighbor.getBlock() instanceof ResearchLinkCableBlock) {
            return !neighbor.getValue(cutPropertyFor(incomingDirection));
        }
        return neighbor.getBlock() instanceof LinkingPortBlock;
    }

    private boolean canConnect(BlockState state, Direction direction, LevelReader level, BlockPos neighborPos) {
        return !state.getValue(cutPropertyFor(direction)) && connectsTo(level, neighborPos, direction.getOpposite());
    }

    private BlockState withCutState(BlockState state, LevelReader level, BlockPos pos, Direction direction, boolean cut) {
        BlockState updated = state.setValue(cutPropertyFor(direction), cut);
        return updated.setValue(propertyFor(direction), canConnect(updated, direction, level, pos.relative(direction)));
    }

    private @Nullable Direction resolveInteractedDirection(BlockState state, LevelReader level, BlockPos pos, net.minecraft.world.phys.BlockHitResult hitResult) {
        Vec3 localHit = hitResult.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
        Direction bestDirection = null;
        double bestDistance = Double.MAX_VALUE;
        for (Direction direction : Direction.values()) {
            if (!(state.getValue(propertyFor(direction))
                    || state.getValue(cutPropertyFor(direction))
                    || connectsTo(level, pos.relative(direction), direction.getOpposite()))) {
                continue;
            }
            if (!hitWithinArm(localHit, direction)) {
                continue;
            }
            double distance = distanceToArmCenter(localHit, direction);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestDirection = direction;
            }
        }
        if (bestDirection != null) {
            return bestDirection;
        }

        Direction fallback = hitResult.getDirection();
        if (state.getValue(propertyFor(fallback))
                || state.getValue(cutPropertyFor(fallback))
                || connectsTo(level, pos.relative(fallback), fallback.getOpposite())) {
            return fallback;
        }
        return null;
    }

    private static boolean hitWithinArm(Vec3 localHit, Direction direction) {
        double x = localHit.x;
        double y = localHit.y;
        double z = localHit.z;
        return switch (direction) {
            case NORTH -> between(x, 5.0D / 16.0D, 11.0D / 16.0D) && between(y, 5.0D / 16.0D, 11.0D / 16.0D) && between(z, 0.0D, 5.0D / 16.0D);
            case EAST -> between(x, 11.0D / 16.0D, 1.0D) && between(y, 5.0D / 16.0D, 11.0D / 16.0D) && between(z, 5.0D / 16.0D, 11.0D / 16.0D);
            case SOUTH -> between(x, 5.0D / 16.0D, 11.0D / 16.0D) && between(y, 5.0D / 16.0D, 11.0D / 16.0D) && between(z, 11.0D / 16.0D, 1.0D);
            case WEST -> between(x, 0.0D, 5.0D / 16.0D) && between(y, 5.0D / 16.0D, 11.0D / 16.0D) && between(z, 5.0D / 16.0D, 11.0D / 16.0D);
            case UP -> between(x, 5.0D / 16.0D, 11.0D / 16.0D) && between(y, 11.0D / 16.0D, 1.0D) && between(z, 5.0D / 16.0D, 11.0D / 16.0D);
            case DOWN -> between(x, 5.0D / 16.0D, 11.0D / 16.0D) && between(y, 0.0D, 5.0D / 16.0D) && between(z, 5.0D / 16.0D, 11.0D / 16.0D);
        };
    }

    private static double distanceToArmCenter(Vec3 localHit, Direction direction) {
        Vec3 center = switch (direction) {
            case NORTH -> new Vec3(0.5D, 0.5D, 2.5D / 16.0D);
            case EAST -> new Vec3(13.5D / 16.0D, 0.5D, 0.5D);
            case SOUTH -> new Vec3(0.5D, 0.5D, 13.5D / 16.0D);
            case WEST -> new Vec3(2.5D / 16.0D, 0.5D, 0.5D);
            case UP -> new Vec3(0.5D, 13.5D / 16.0D, 0.5D);
            case DOWN -> new Vec3(0.5D, 2.5D / 16.0D, 0.5D);
        };
        return localHit.distanceToSqr(center);
    }

    private static boolean between(double value, double minInclusive, double maxInclusive) {
        return value >= minInclusive && value <= maxInclusive;
    }

    private VoxelShape shapeFor(BlockState state) {
        VoxelShape shape = CORE_SHAPE;
        if (state.getValue(NORTH)) {
            shape = Shapes.or(shape, NORTH_SHAPE);
        }
        if (state.getValue(EAST)) {
            shape = Shapes.or(shape, EAST_SHAPE);
        }
        if (state.getValue(SOUTH)) {
            shape = Shapes.or(shape, SOUTH_SHAPE);
        }
        if (state.getValue(WEST)) {
            shape = Shapes.or(shape, WEST_SHAPE);
        }
        if (state.getValue(UP)) {
            shape = Shapes.or(shape, UP_SHAPE);
        }
        if (state.getValue(DOWN)) {
            shape = Shapes.or(shape, DOWN_SHAPE);
        }
        return shape;
    }
}
