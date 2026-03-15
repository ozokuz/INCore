package ozokuz.incore.features.roguelike;

import ozokuz.incore.Registration;
import ozokuz.incore.features.roguelike.content.RoguelikePortalBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Predicate;

public class RoguelikePortalShape {
    private static final TagKey<Block> ROGUELIKE_PORTAL_FRAME_BLOCKS = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.parse("incore:roguelike_portal_frame_blocks")
    );

    private final LevelAccessor level;
    private final Direction.Axis axis;
    private final Direction rightDir;
    private int numPortalBlocks;
    @Nullable
    private BlockPos bottomLeft;
    private int height;
    private final int width;

    public static Optional<RoguelikePortalShape> findEmptyPortalShape(LevelAccessor level, BlockPos pos) {
        return findPortalShape(level, pos, shape -> shape.isValid() && shape.numPortalBlocks == 0, Direction.Axis.X);
    }

    private static Optional<RoguelikePortalShape> findPortalShape(LevelAccessor level, BlockPos pos, Predicate<RoguelikePortalShape> predicate, Direction.Axis axis) {
        Optional<RoguelikePortalShape> shape = Optional.of(new RoguelikePortalShape(level, pos, axis)).filter(predicate);
        if (shape.isPresent()) {
            return shape;
        }

        Direction.Axis other = axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
        return Optional.of(new RoguelikePortalShape(level, pos, other)).filter(predicate);
    }

    public RoguelikePortalShape(LevelAccessor level, BlockPos pos, Direction.Axis axis) {
        this.level = level;
        this.axis = axis;
        this.rightDir = axis == Direction.Axis.X ? Direction.WEST : Direction.SOUTH;
        this.bottomLeft = calculateBottomLeft(pos);

        if (this.bottomLeft == null) {
            this.bottomLeft = pos;
            this.width = 1;
            this.height = 1;
        } else {
            this.width = calculateWidth();
            if (this.width > 0) {
                this.height = calculateHeight();
            }
        }
    }

    public Direction.Axis axis() {
        return axis;
    }

    public BlockPos bottomLeft() {
        return bottomLeft;
    }

    public boolean isComplete() {
        return isValid() && numPortalBlocks == width * height;
    }

    public boolean isValid() {
        return bottomLeft != null && width >= 2 && width <= 21 && height >= 3 && height <= 21;
    }

    public void createPortalBlocks() {
        BlockState state = Registration.ROGUELIKE_PORTAL_BLOCK.get().defaultBlockState().setValue(RoguelikePortalBlock.AXIS, axis);
        BlockPos.betweenClosed(
                bottomLeft,
                bottomLeft.relative(Direction.UP, height - 1).relative(rightDir, width - 1)
        ).forEach(pos -> level.setBlock(pos, state, 18));
    }

    public void forEachPortalBlock(java.util.function.Consumer<BlockPos> consumer) {
        BlockPos.betweenClosed(
                bottomLeft,
                bottomLeft.relative(Direction.UP, height - 1).relative(rightDir, width - 1)
        ).forEach(consumer);
    }

    @Nullable
    private BlockPos calculateBottomLeft(BlockPos pos) {
        int minY = Math.max(level.getMinBuildHeight(), pos.getY() - 21);

        while (pos.getY() > minY && isEmpty(level.getBlockState(pos.below()))) {
            pos = pos.below();
        }

        Direction direction = rightDir.getOpposite();
        int distance = getDistanceUntilEdgeAboveFrame(pos, direction) - 1;
        return distance < 0 ? null : pos.relative(direction, distance);
    }

    private int calculateWidth() {
        int distance = getDistanceUntilEdgeAboveFrame(bottomLeft, rightDir);
        return distance >= 2 && distance <= 21 ? distance : 0;
    }

    private int getDistanceUntilEdgeAboveFrame(BlockPos pos, Direction direction) {
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (int i = 0; i <= 21; i++) {
            mutablePos.set(pos).move(direction, i);
            BlockState state = level.getBlockState(mutablePos);
            if (!isEmpty(state)) {
                if (isFrame(mutablePos)) {
                    return i;
                }
                break;
            }

            BlockPos.MutableBlockPos below = mutablePos.mutable().move(Direction.DOWN);
            if (!isFrame(below)) {
                break;
            }
        }

        return 0;
    }

    private int calculateHeight() {
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        int distance = getDistanceUntilTop(mutablePos);
        return distance >= 3 && distance <= 21 && hasTopFrame(mutablePos, distance) ? distance : 0;
    }

    private boolean hasTopFrame(BlockPos.MutableBlockPos mutablePos, int distanceToTop) {
        for (int i = 0; i < width; i++) {
            mutablePos.set(bottomLeft).move(Direction.UP, distanceToTop).move(rightDir, i);
            if (!isFrame(mutablePos)) {
                return false;
            }
        }

        return true;
    }

    private int getDistanceUntilTop(BlockPos.MutableBlockPos mutablePos) {
        for (int i = 0; i < 21; i++) {
            mutablePos.set(bottomLeft).move(Direction.UP, i).move(rightDir, -1);
            if (!isFrame(mutablePos)) {
                return i;
            }

            mutablePos.set(bottomLeft).move(Direction.UP, i).move(rightDir, width);
            if (!isFrame(mutablePos)) {
                return i;
            }

            for (int j = 0; j < width; j++) {
                mutablePos.set(bottomLeft).move(Direction.UP, i).move(rightDir, j);
                BlockState state = level.getBlockState(mutablePos);
                if (!isEmpty(state)) {
                    return i;
                }

                if (state.is(Registration.ROGUELIKE_PORTAL_BLOCK.get())) {
                    numPortalBlocks++;
                }
            }
        }

        return 21;
    }

    private boolean isFrame(BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(ROGUELIKE_PORTAL_FRAME_BLOCKS);
    }

    private static boolean isEmpty(BlockState state) {
        return state.isAir() || state.is(BlockTags.FIRE) || state.is(Registration.ROGUELIKE_PORTAL_BLOCK.get());
    }
}
