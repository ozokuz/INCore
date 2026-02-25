package io.github.ozokuz.incore.features.surfaceore;

import io.github.ozokuz.incore.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SurfaceOrePatchFeature extends Feature<NoneFeatureConfiguration> {
    private static final int MIN_SPOTS_PER_PATCH = 2;
    private static final int MAX_SPOTS_PER_PATCH = 6;
    private static final int MIN_MINES_PER_SPOT = 400;
    private static final int MAX_MINES_PER_SPOT = 1200;
    private static final int RICHNESS_NEAR_MIN = 400;
    private static final int RICHNESS_NEAR_MAX = 700;
    private static final int RICHNESS_FAR_MIN = 900;
    private static final int RICHNESS_FAR_MAX = 1200;
    private static final int MAX_RICHNESS_DISTANCE_BLOCKS = 12000;
    private static final int MIN_SPOT_SPACING = 3;
    private static final int SURFACE_COVERAGE_PADDING = 2;
    private static final int SURFACE_FILL_DEPTH = 2;
    private static final int VEGETATION_CLEAR_HEIGHT = 2;
    private static final int CHUNK_SPACING_DIVISOR = 50;

    public SurfaceOrePatchFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        if (!isSelectedChunk(level, origin)) {
            return false;
        }

        ResourceKey<Level> dimension = level.getLevel().dimension();
        DimensionCategory dimensionCategory = DimensionCategory.fromLevel(dimension);

        int targetSpots = Mth.nextInt(random, MIN_SPOTS_PER_PATCH, MAX_SPOTS_PER_PATCH);
        int sharedMines = rollMinesByDistance(level, random, origin);
        SurfaceOreType oreType = SurfaceOreType.random(random, dimensionCategory);

        if (oreType == null) {
            return false;
        }

        List<BlockPos> placedSpots = new ArrayList<>();
        Set<Long> occupied = new HashSet<>();
        int radius = 2 + random.nextInt(3);
        int maxAttempts = targetSpots * 36;

        for (int attempt = 0; attempt < maxAttempts && placedSpots.size() < targetSpots; attempt++) {
            int x = origin.getX() + random.nextInt(radius * 2 + 1) - radius;
            int z = origin.getZ() + random.nextInt(radius * 2 + 1) - radius;
            BlockPos groundPos = findGroundPos(level, x, z);
            if (groundPos == null) {
                continue;
            }

            BlockPos spotPos = groundPos.above();
            if (occupied.contains(spotPos.asLong())) {
                continue;
            }
            if (!isFarEnoughFromOtherSpots(placedSpots, spotPos)) {
                continue;
            }

            BlockState groundState = level.getBlockState(groundPos);
            if (!groundState.isFaceSturdy(level, groundPos, Direction.UP)) {
                continue;
            }

            if (!level.getFluidState(spotPos).isEmpty()) {
                continue;
            }

            BlockState existingState = level.getBlockState(spotPos);
            if (!existingState.canBeReplaced()) {
                continue;
            }
            if (!occupied.add(spotPos.asLong())) {
                continue;
            }

            BlockState oreState = blockForType(oreType).defaultBlockState();
            if (!level.setBlock(spotPos, oreState, Block.UPDATE_ALL)) {
                occupied.remove(spotPos.asLong());
                continue;
            }

            SurfaceOreSpotBlockEntity be = getOrCreateSpotBlockEntity(level, spotPos, oreState);
            if (be == null) {
                level.setBlock(spotPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                occupied.remove(spotPos.asLong());
                continue;
            }

            be.initializeMines(sharedMines);
            placedSpots.add(spotPos.immutable());
        }

        if (placedSpots.size() >= MIN_SPOTS_PER_PATCH) {
            if (coverPatchSurface(level, random, oreType, placedSpots, dimension)) {
                SurfaceOrePatchSavedData.get(level.getLevel()).recordPatch(centerOf(placedSpots));
                return true;
            }

            for (BlockPos placedSpot : placedSpots) {
                level.setBlock(placedSpot, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
            return false;
        }

        for (BlockPos placedSpot : placedSpots) {
            level.setBlock(placedSpot, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        return false;
    }

    private static BlockPos centerOf(List<BlockPos> positions) {
        long sumX = 0L;
        long sumY = 0L;
        long sumZ = 0L;
        int count = positions.size();
        for (BlockPos pos : positions) {
            sumX += pos.getX();
            sumY += pos.getY();
            sumZ += pos.getZ();
        }
        return new BlockPos(
                Mth.floor(sumX / (double) count),
                Mth.floor(sumY / (double) count),
                Mth.floor(sumZ / (double) count)
        );
    }

    private static boolean isFarEnoughFromOtherSpots(List<BlockPos> existingSpots, BlockPos candidatePos) {
        int minDistanceSq = MIN_SPOT_SPACING * MIN_SPOT_SPACING;
        for (BlockPos existingSpot : existingSpots) {
            int dx = existingSpot.getX() - candidatePos.getX();
            int dz = existingSpot.getZ() - candidatePos.getZ();
            if (dx * dx + dz * dz < minDistanceSq) {
                return false;
            }
        }
        return true;
    }

    private static boolean coverPatchSurface(WorldGenLevel level, RandomSource random, SurfaceOreType oreType, List<BlockPos> placedSpots, ResourceKey<Level> dimension) {
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        Set<Long> spotColumns = new HashSet<>();
        for (BlockPos spotPos : placedSpots) {
            minX = Math.min(minX, spotPos.getX());
            maxX = Math.max(maxX, spotPos.getX());
            minZ = Math.min(minZ, spotPos.getZ());
            maxZ = Math.max(maxZ, spotPos.getZ());
            spotColumns.add(BlockPos.asLong(spotPos.getX(), 0, spotPos.getZ()));
        }

        double centerX = (minX + maxX) * 0.5D;
        double centerZ = (minZ + maxZ) * 0.5D;
        double radiusX = Math.max(1.0D, (maxX - minX) * 0.5D + SURFACE_COVERAGE_PADDING);
        double radiusZ = Math.max(1.0D, (maxZ - minZ) * 0.5D + SURFACE_COVERAGE_PADDING);

        Map<BlockPos, BlockState> plannedCoverage = new HashMap<>();
        Set<BlockPos> vegetationClearBases = new HashSet<>();

        for (int x = minX - SURFACE_COVERAGE_PADDING; x <= maxX + SURFACE_COVERAGE_PADDING; x++) {
            for (int z = minZ - SURFACE_COVERAGE_PADDING; z <= maxZ + SURFACE_COVERAGE_PADDING; z++) {
                if (!isInsideOval(x, z, centerX, centerZ, radiusX, radiusZ)) {
                    continue;
                }
                if (spotColumns.contains(BlockPos.asLong(x, 0, z))) {
                    continue;
                }

                int groundY = findGroundY(level, x, z);
                if (groundY <= level.getMinBuildHeight() + 1) {
                    return false;
                }

                BlockPos groundPos = new BlockPos(x, groundY, z);
                BlockPos abovePos = groundPos.above();
                BlockState groundState = level.getBlockState(groundPos);
                BlockState aboveState = level.getBlockState(abovePos);
                if (groundState.getBlock() instanceof SurfaceOreSpotBlock || aboveState.getBlock() instanceof SurfaceOreSpotBlock) {
                    return false;
                }
                if (!level.getFluidState(groundPos).isEmpty() || !level.getFluidState(abovePos).isEmpty()) {
                    return false;
                }
                if (!DimensionGroundTags.isNaturalGround(groundState, dimension)) {
                    return false;
                }
                if (!canClearAboveForPatch(aboveState)) {
                    return false;
                }

                plannedCoverage.put(groundPos, chooseCoverageState(random, oreType));
                vegetationClearBases.add(groundPos);

                for (int depth = 1; depth <= SURFACE_FILL_DEPTH; depth++) {
                    BlockPos belowPos = groundPos.below(depth);
                    BlockState belowState = level.getBlockState(belowPos);
                    if (!canReplaceSubsurfaceGround(belowState, dimension)) {
                        return false;
                    }
                    if (!level.getFluidState(belowPos).isEmpty()) {
                        return false;
                    }
                    plannedCoverage.put(belowPos, chooseSolidCoverageState(random, oreType));
                }
            }
        }

        for (Map.Entry<BlockPos, BlockState> entry : plannedCoverage.entrySet()) {
            level.setBlock(entry.getKey(), entry.getValue(), Block.UPDATE_ALL);
        }
        clearSoftVegetationAbove(level, vegetationClearBases, dimension);
        clearSoftVegetationAbove(level, placedSpots, dimension);
        return true;
    }

    private static boolean isInsideOval(int x, int z, double centerX, double centerZ, double radiusX, double radiusZ) {
        double normX = (x - centerX) / radiusX;
        double normZ = (z - centerZ) / radiusZ;
        return normX * normX + normZ * normZ <= 1.0D;
    }

    private static BlockState chooseCoverageState(RandomSource random, SurfaceOreType oreType) {
        int roll = random.nextInt(100);
        if (roll < 38) {
            return Blocks.STONE.defaultBlockState();
        }
        if (roll < 75) {
            return oreType.oreStoneState();
        }
        if (roll < 88) {
            return slabBottom(Blocks.STONE_SLAB.defaultBlockState());
        }
        return slabBottom(oreType.oreStoneSlabState());
    }

    private static BlockState chooseSolidCoverageState(RandomSource random, SurfaceOreType oreType) {
        if (random.nextInt(100) < 52) {
            return Blocks.STONE.defaultBlockState();
        }
        return oreType.oreStoneState();
    }

    private static int rollMinesByDistance(WorldGenLevel level, RandomSource random, BlockPos origin) {
        BlockPos spawn = level.getLevel().getSharedSpawnPos();
        double dx = origin.getX() - spawn.getX();
        double dz = origin.getZ() - spawn.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        double t = Mth.clamp(distance / MAX_RICHNESS_DISTANCE_BLOCKS, 0.0D, 1.0D);
        int dynamicMin = Mth.clamp(
                Mth.floor(Mth.lerp(t, RICHNESS_NEAR_MIN, RICHNESS_FAR_MIN)),
                MIN_MINES_PER_SPOT,
                MAX_MINES_PER_SPOT
        );
        int dynamicMax = Mth.clamp(
                Mth.ceil(Mth.lerp(t, RICHNESS_NEAR_MAX, RICHNESS_FAR_MAX)),
                dynamicMin,
                MAX_MINES_PER_SPOT
        );
        return Mth.nextInt(random, dynamicMin, dynamicMax);
    }

    private static boolean canClearAboveForPatch(BlockState state) {
        return state.isAir() || state.is(BlockTags.REPLACEABLE);
    }

    private static boolean canReplaceSubsurfaceGround(BlockState state, ResourceKey<Level> dimension) {
        return DimensionGroundTags.isNaturalGround(state, dimension) || state.isAir() || state.is(BlockTags.REPLACEABLE);
    }

    private static void clearSoftVegetationAbove(WorldGenLevel level, Iterable<BlockPos> bases, ResourceKey<Level> dimension) {
        for (BlockPos base : bases) {
            for (int dy = 1; dy <= VEGETATION_CLEAR_HEIGHT; dy++) {
                BlockPos clearPos = base.above(dy);
                BlockState clearState = level.getBlockState(clearPos);
                if (DimensionGroundTags.isSoftVegetation(clearState, dimension)) {
                    level.setBlock(clearPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
    }

    private static BlockState slabBottom(BlockState slabState) {
        if (!(slabState.getBlock() instanceof SlabBlock) || !slabState.hasProperty(SlabBlock.TYPE)) {
            return Blocks.STONE_SLAB.defaultBlockState();
        }
        return slabState.setValue(SlabBlock.TYPE, SlabType.BOTTOM);
    }

    private static boolean isSelectedChunk(WorldGenLevel level, BlockPos origin) {
        ChunkPos chunkPos = new ChunkPos(origin);
        long mixed = mixChunkSeed(level.getSeed(), chunkPos.x, chunkPos.z);
        return Math.floorMod(mixed, CHUNK_SPACING_DIVISOR) == 0;
    }

    private static long mixChunkSeed(long worldSeed, int chunkX, int chunkZ) {
        long mixed = worldSeed;
        mixed ^= (long) chunkX * 341873128712L;
        mixed ^= (long) chunkZ * 132897987541L;
        mixed ^= 0x9E3779B97F4A7C15L;
        mixed = (mixed ^ (mixed >>> 30)) * 0xBF58476D1CE4E5B9L;
        mixed = (mixed ^ (mixed >>> 27)) * 0x94D049BB133111EBL;
        return mixed ^ (mixed >>> 31);
    }

    private static SurfaceOreSpotBlockEntity getOrCreateSpotBlockEntity(WorldGenLevel level, BlockPos pos, BlockState state) {
        BlockEntity existing = level.getBlockEntity(pos);
        if (existing instanceof SurfaceOreSpotBlockEntity spot) {
            return spot;
        }

        if (!(state.getBlock() instanceof EntityBlock entityBlock)) {
            return null;
        }

        BlockEntity created = entityBlock.newBlockEntity(pos, state);
        if (!(created instanceof SurfaceOreSpotBlockEntity spot)) {
            return null;
        }

        ChunkAccess chunk = level.getChunk(pos);
        chunk.setBlockEntity(spot);
        return spot;
    }

    private static Block blockForType(SurfaceOreType oreType) {
        return switch (oreType) {
            case CRIMSITE -> Registration.CRIMSITE_SURFACE_ORE_SPOT_BLOCK.get();
            case VERIDIUM -> Registration.VERIDIUM_SURFACE_ORE_SPOT_BLOCK.get();
            case ASURINE -> Registration.ASURINE_SURFACE_ORE_SPOT_BLOCK.get();
            case OCHRUM -> Registration.OCHRUM_SURFACE_ORE_SPOT_BLOCK.get();
            case CINNABAR -> Registration.CINNABAR_SURFACE_ORE_SPOT_BLOCK.get();
            case MIXED_METALS -> Registration.MIXED_METALS_SURFACE_ORE_SPOT_BLOCK.get();
            case GEM_CLUSTERS -> Registration.GEM_CLUSTERS_SURFACE_ORE_SPOT_BLOCK.get();
        };
    }

    private static BlockPos findGroundPos(WorldGenLevel level, int x, int z) {
        int groundY = findGroundY(level, x, z);
        if (groundY <= level.getMinBuildHeight() + 1) {
            return null;
        }
        return new BlockPos(x, groundY, z);
    }

    private static int findGroundY(WorldGenLevel level, int x, int z) {
        DimensionCategory category = DimensionCategory.fromLevel(level.getLevel().dimension());
        
        if (category == DimensionCategory.NETHER) {
            return findGroundYFromBottom(level, x, z);
        }
        
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
    }

    private static int findGroundYFromBottom(WorldGenLevel level, int x, int z) {
        int minBuildHeight = level.getMinBuildHeight();
        int maxBuildHeight = level.getMaxBuildHeight();
        
        for (int y = minBuildHeight + 1; y < maxBuildHeight - 1; y++) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.BEDROCK)) {
                continue;
            }
            if (!state.isAir() && !state.is(BlockTags.REPLACEABLE)) {
                if (state.isFaceSturdy(level, pos, Direction.UP)) {
                    BlockPos abovePos = pos.above();
                    BlockState aboveState = level.getBlockState(abovePos);
                    if (aboveState.canBeReplaced() && level.getFluidState(abovePos).isEmpty()) {
                        return y;
                    }
                }
            }
        }
        
        return minBuildHeight;
    }
}
