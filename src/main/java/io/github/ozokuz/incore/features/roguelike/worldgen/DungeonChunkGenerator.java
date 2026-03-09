package io.github.ozokuz.incore.features.roguelike.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.ozokuz.incore.Registration;
import io.github.ozokuz.incore.features.encounter_spawner.EncounterSpawnerBE;
import io.github.ozokuz.incore.features.roguelike.RoguelikeConstants;
import io.github.ozokuz.incore.features.roguelike.data.DungeonThemeData;
import io.github.ozokuz.incore.features.roguelike.data.DungeonThemeManager;
import io.github.ozokuz.incore.features.roguelike.instance.DungeonInstanceData;
import io.github.ozokuz.incore.features.roguelike.instance.DungeonInstanceManager;
import io.github.ozokuz.incore.features.roguelike.state.RoguelikeSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.List;
import java.util.Optional;

public class DungeonChunkGenerator extends FlatLevelSource {
    public static final MapCodec<DungeonChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Biome.CODEC.fieldOf("biome").forGetter(DungeonChunkGenerator::biome)
    ).apply(instance, DungeonChunkGenerator::new));

    private final Holder<Biome> biome;

    public DungeonChunkGenerator(Holder<Biome> biome) {
        super(createSettings(biome));
        this.biome = biome;
    }

    @Override
    protected MapCodec<? extends net.minecraft.world.level.chunk.ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager, RandomState random, ChunkAccess chunk) {
        if (!level.getLevel().dimension().equals(RoguelikeConstants.DUNGEON_DIMENSION)) {
            return;
        }

        if (level.getServer() == null) {
            return;
        }

        ChunkPos chunkPos = chunk.getPos();
        RoguelikeSavedData data = RoguelikeSavedData.get(level.getServer());
        DungeonInstanceData instance = data.getWorldgenInstanceForChunk(chunkPos.x, chunkPos.z);
        if (instance == null) {
            return;
        }

        DungeonThemeData theme = DungeonThemeManager.THEMES.get(instance.themeId());
        if (theme == null) {
            return;
        }

        DungeonWorldPlan plan = DungeonWorldPlanner.plan(level.getLevel(), instance, theme);
        if (plan == null) {
            return;
        }

        BoundingBox chunkBox = new BoundingBox(
                chunkPos.getMinBlockX(),
                level.getMinBuildHeight(),
                chunkPos.getMinBlockZ(),
                chunkPos.getMaxBlockX(),
                level.getMaxBuildHeight() - 1,
                chunkPos.getMaxBlockZ()
        );

        for (DungeonWorldPlan.PlacedTemplate placedTemplate : plan.templates()) {
            if (!placedTemplate.intersectsChunk(chunkPos.x, chunkPos.z)) {
                continue;
            }

            Optional<StructureTemplate> templateOptional = level.getLevel().getStructureManager().get(placedTemplate.id());
            if (templateOptional.isEmpty()) {
                continue;
            }

            StructureTemplate template = templateOptional.get();
            StructurePlaceSettings settings = new StructurePlaceSettings()
                    .setIgnoreEntities(true)
                    .setBoundingBox(chunkBox);

            template.placeInWorld(level, placedTemplate.origin(), placedTemplate.origin(), settings, level.getRandom(), Block.UPDATE_ALL);
            replaceReturnPortalPlaceholders(level, template, placedTemplate.origin(), settings, instance.id().value());
        }

        placeEncounterSpawners(level, plan, chunkPos);

        if ((plan.entryPos().getX() >> 4) == chunkPos.x && (plan.entryPos().getZ() >> 4) == chunkPos.z) {
            clearEntrySpace(level, plan.entryPos());
        }

        DungeonInstanceManager.ensureObjectiveBlocksGenerated(level.getLevel(), instance, chunkPos);

        chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
    }

    private static void placeEncounterSpawners(WorldGenRegion level, DungeonWorldPlan plan, ChunkPos chunkPos) {
        for (DungeonWorldPlan.EncounterSpawnerPlacement placement : plan.encounterSpawners()) {
            if (!placement.isInChunk(chunkPos.x, chunkPos.z)) {
                continue;
            }

            level.setBlock(placement.pos(), Registration.ENCOUNTER_SPAWNER_BLOCK.get().defaultBlockState(), Block.UPDATE_ALL);
            BlockEntity blockEntity = level.getBlockEntity(placement.pos());
            if (blockEntity instanceof EncounterSpawnerBE spawner) {
                spawner.setEncounterId(placement.encounterId().toString());
                spawner.setSpawnOffset(placement.spawnOffset());
                spawner.setEncounterStrengthMultipliers(placement.mobHealthMultiplier(), placement.mobDamageMultiplier());
                spawner.setChanged();
            }
        }
    }

    private static void replaceReturnPortalPlaceholders(
            WorldGenRegion level,
            StructureTemplate template,
            BlockPos origin,
            StructurePlaceSettings settings,
            long instanceId
    ) {
        List<StructureTemplate.StructureBlockInfo> placeholders = template.filterBlocks(
                origin,
                settings,
                Registration.DUNGEON_RETURN_PORTAL_BLOCK.get()
        );

        for (StructureTemplate.StructureBlockInfo placeholder : placeholders) {
            BlockPos pos = placeholder.pos();
            level.setBlock(pos, Registration.ROGUELIKE_PORTAL_BLOCK.get().defaultBlockState(), Block.UPDATE_ALL);
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof io.github.ozokuz.incore.features.roguelike.content.RoguelikePortalBlockEntity portal) {
                portal.setInstanceId(instanceId);
                portal.setChanged();
            }
        }
    }

    private static void clearEntrySpace(WorldGenRegion level, BlockPos entry) {
        level.setBlock(entry, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(entry.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        if (level.getBlockState(entry.below()).isAir()) {
            level.setBlock(entry.below(), Blocks.BEDROCK.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    private Holder<Biome> biome() {
        return biome;
    }

    private static FlatLevelGeneratorSettings createSettings(Holder<Biome> biome) {
        FlatLevelGeneratorSettings settings = new FlatLevelGeneratorSettings(Optional.empty(), biome, List.of());
        settings.getLayersInfo().add(new FlatLayerInfo(81, Blocks.BEDROCK));
        settings.getLayersInfo().add(new FlatLayerInfo(1, Blocks.AIR));
        settings.updateLayers();
        return settings;
    }
}
