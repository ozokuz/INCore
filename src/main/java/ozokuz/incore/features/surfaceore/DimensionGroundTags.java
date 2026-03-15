package ozokuz.incore.features.surfaceore;

import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class DimensionGroundTags {
    private DimensionGroundTags() {}

    public static boolean isNaturalGround(BlockState state, ResourceKey<Level> dimension) {
        DimensionCategory category = DimensionCategory.fromLevel(dimension);
        
        return switch (category) {
            case OVERWORLD -> isOverworldGround(state);
            case NETHER -> isNetherGround(state);
            case END -> isEndGround(state);
        };
    }

    public static boolean isSoftVegetation(BlockState state, ResourceKey<Level> dimension) {
        DimensionCategory category = DimensionCategory.fromLevel(dimension);
        
        return switch (category) {
            case OVERWORLD -> isOverworldVegetation(state);
            case NETHER -> isNetherVegetation(state);
            case END -> false;
        };
    }

    private static boolean isOverworldGround(BlockState state) {
        return state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.DIRT)
                || state.is(BlockTags.SAND)
                || state.is(BlockTags.TERRACOTTA)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.CLAY)
                || state.is(Blocks.SNOW_BLOCK)
                || state.is(Blocks.POWDER_SNOW)
                || state.is(Blocks.STONE)
                || state.is(Blocks.STONE_SLAB);
    }

    private static boolean isNetherGround(BlockState state) {
        return state.is(BlockTags.BASE_STONE_NETHER)
                || state.is(Blocks.NETHERRACK)
                || state.is(Blocks.SOUL_SAND)
                || state.is(Blocks.SOUL_SOIL)
                || state.is(Blocks.BASALT)
                || state.is(Blocks.WARPED_NYLIUM)
                || state.is(Blocks.CRIMSON_NYLIUM)
                || state.is(Blocks.GRAVEL);
    }

    private static boolean isEndGround(BlockState state) {
        return state.is(Blocks.END_STONE);
    }

    private static boolean isOverworldVegetation(BlockState state) {
        return state.is(Blocks.SHORT_GRASS)
                || state.is(Blocks.TALL_GRASS)
                || state.is(Blocks.FERN)
                || state.is(Blocks.LARGE_FERN)
                || state.is(Blocks.DEAD_BUSH)
                || state.is(BlockTags.SMALL_FLOWERS)
                || state.is(BlockTags.TALL_FLOWERS);
    }

    private static boolean isNetherVegetation(BlockState state) {
        return state.is(Blocks.NETHER_SPROUTS)
                || state.is(Blocks.TWISTING_VINES)
                || state.is(Blocks.TWISTING_VINES_PLANT)
                || state.is(Blocks.WEEPING_VINES)
                || state.is(Blocks.WEEPING_VINES_PLANT)
                || state.is(Blocks.CRIMSON_ROOTS)
                || state.is(Blocks.WARPED_ROOTS);
    }
}
