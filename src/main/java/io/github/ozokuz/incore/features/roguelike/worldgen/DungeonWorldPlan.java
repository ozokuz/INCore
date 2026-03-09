package io.github.ozokuz.incore.features.roguelike.worldgen;

import io.github.ozokuz.incore.features.roguelike.data.DungeonThemeData;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.List;

public record DungeonWorldPlan(
        BlockPos startRoomOrigin,
        BlockPos entryPos,
        List<PlacedTemplate> templates,
        List<EncounterSpawnerPlacement> encounterSpawners
) {
    public DungeonWorldPlan {
        startRoomOrigin = startRoomOrigin == null ? BlockPos.ZERO : startRoomOrigin.immutable();
        entryPos = entryPos == null ? BlockPos.ZERO : entryPos.immutable();
        templates = templates == null ? List.of() : List.copyOf(templates);
        encounterSpawners = encounterSpawners == null ? List.of() : List.copyOf(encounterSpawners);
    }

    public List<BlockPos> encounterSpawnerPositions() {
        return encounterSpawners.stream()
                .map(EncounterSpawnerPlacement::pos)
                .toList();
    }

    public record PlacedTemplate(
            DungeonThemeData.TemplateRef templateRef,
            BlockPos origin,
            BoundingBox bounds
    ) {
        public PlacedTemplate {
            origin = origin == null ? BlockPos.ZERO : origin.immutable();
        }

        public ResourceLocation id() {
            return templateRef.id();
        }

        public boolean intersectsChunk(int chunkX, int chunkZ) {
            int minX = chunkX << 4;
            int minZ = chunkZ << 4;
            return bounds != null && bounds.intersects(minX, minZ, minX + 15, minZ + 15);
        }
    }

    public record EncounterSpawnerPlacement(
            BlockPos pos,
            ResourceLocation encounterId,
            BlockPos spawnOffset,
            double mobHealthMultiplier,
            double mobDamageMultiplier
    ) {
        public EncounterSpawnerPlacement {
            pos = pos == null ? BlockPos.ZERO : pos.immutable();
            spawnOffset = spawnOffset == null ? BlockPos.ZERO : spawnOffset.immutable();
        }

        public boolean isInChunk(int chunkX, int chunkZ) {
            return (pos.getX() >> 4) == chunkX && (pos.getZ() >> 4) == chunkZ;
        }
    }
}
