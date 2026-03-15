package ozokuz.incore.features.roguelike.worldgen;

import ozokuz.incore.features.roguelike.RoguelikeConstants;
import ozokuz.incore.features.roguelike.data.DungeonModifierData;
import ozokuz.incore.features.roguelike.data.DungeonModifierManager;
import ozokuz.incore.features.roguelike.data.DungeonSocketData;
import ozokuz.incore.features.roguelike.data.DungeonSocketManager;
import ozokuz.incore.features.roguelike.data.DungeonThemeData;
import ozokuz.incore.features.roguelike.instance.DungeonInstanceData;
import ozokuz.incore.features.roguelike.layout.DungeonLayoutGenerator;
import ozokuz.incore.features.roguelike.layout.DungeonLayoutPlan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class DungeonWorldPlanner {
    public static final int ROOM_MIDDLE_FLOOR_WORLD_Y = 26;
    public static final int ROOM_SIZE_CHUNKS = 3;
    public static final int ROOM_GAP_CHUNKS = 2;
    public static final int CHUNK_STRIDE = ROOM_SIZE_CHUNKS + ROOM_GAP_CHUNKS;
    public static final int START_CHUNK_OFFSET = RoguelikeConstants.INSTANCE_SIZE_CHUNKS / 2;

    private DungeonWorldPlanner() {
    }

    @Nullable
    public static DungeonWorldPlan plan(ServerLevel level, DungeonInstanceData instance, DungeonThemeData theme) {
        long seed = level.getSeed() ^ instance.id().value() ^ ((long) instance.slotIndex() << 32);
        RandomSource layoutRandom = RandomSource.create(seed);
        RandomSource encounterRandom = RandomSource.create(seed ^ 0x9E3779B97F4A7C15L);
        DungeonLayoutPlan layoutPlan = DungeonLayoutGenerator.generate(theme, layoutRandom);

        List<DungeonLayoutPlan.RoomPlacement> orderedRooms = new ArrayList<>(layoutPlan.rooms());
        orderedRooms.sort(Comparator
                .comparing(DungeonLayoutPlan.RoomPlacement::startRoom).reversed()
                .thenComparingInt(DungeonLayoutPlan.RoomPlacement::cellZ)
                .thenComparingInt(DungeonLayoutPlan.RoomPlacement::cellX));

        Map<Long, PlacedRoom> placedRooms = new HashMap<>();
        Set<Long> occupiedChunks = new HashSet<>();
        List<DungeonWorldPlan.PlacedTemplate> templates = new ArrayList<>();
        List<DungeonWorldPlan.FeaturePlacement> features = new ArrayList<>();
        BlockPos startRoomOrigin = null;
        BlockPos entryPos = null;
        EncounterModifierProfile encounterProfile = encounterModifierProfile(instance);

        for (DungeonLayoutPlan.RoomPlacement room : orderedRooms) {
            int footprintChunkX;
            int footprintChunkZ;
            int footprintWidth;
            int footprintDepth;
            if (room.startRoom()) {
                footprintChunkX = instance.originChunkX() + START_CHUNK_OFFSET;
                footprintChunkZ = instance.originChunkZ() + START_CHUNK_OFFSET;
                footprintWidth = 1;
                footprintDepth = 1;
            } else {
                footprintChunkX = instance.originChunkX() + room.cellX() * CHUNK_STRIDE;
                footprintChunkZ = instance.originChunkZ() + room.cellZ() * CHUNK_STRIDE;
                footprintWidth = ROOM_SIZE_CHUNKS;
                footprintDepth = ROOM_SIZE_CHUNKS;
            }

            if (!isFootprintInBounds(instance, footprintChunkX, footprintChunkZ, footprintWidth, footprintDepth)) {
                return null;
            }

            PlannedTemplate placed = planTemplate(level, room.template(), footprintChunkX, footprintChunkZ, footprintWidth, footprintDepth);
            if (placed == null) {
                return null;
            }

            templates.add(placed.template());
            features.addAll(planSocketFeatures(room.template().id(), placed.origin(), encounterProfile, encounterRandom));
            markOccupied(occupiedChunks, footprintChunkX, footprintChunkZ, footprintWidth, footprintDepth);

            if (room.startRoom()) {
                startRoomOrigin = placed.origin();
                entryPos = resolveStartEntryPosition(room.template().id(), placed.origin(), placed.structureTemplate());
            }

            placedRooms.put(cellKey(room.cellX(), room.cellZ()), new PlacedRoom(room, footprintChunkX, footprintChunkZ, placed.origin()));
        }

        List<DungeonLayoutPlan.HallwayPlacement> orderedHallways = new ArrayList<>(layoutPlan.hallways());
        orderedHallways.sort(Comparator
                .comparingInt(DungeonLayoutPlan.HallwayPlacement::fromCellZ)
                .thenComparingInt(DungeonLayoutPlan.HallwayPlacement::fromCellX)
                .thenComparingInt(DungeonLayoutPlan.HallwayPlacement::toCellZ)
                .thenComparingInt(DungeonLayoutPlan.HallwayPlacement::toCellX));

        for (DungeonLayoutPlan.HallwayPlacement hallway : orderedHallways) {
            HallwayFootprint footprint = hallwayFootprint(instance, hallway, placedRooms);
            if (footprint == null || !isFootprintInBounds(instance, footprint.chunkX(), footprint.chunkZ(), footprint.widthChunks(), footprint.depthChunks())) {
                return null;
            }

            PlannedTemplate placed = planTemplate(
                    level,
                    hallway.template(),
                    footprint.chunkX(),
                    footprint.chunkZ(),
                    footprint.widthChunks(),
                    footprint.depthChunks()
            );
            if (placed == null) {
                return null;
            }

            templates.add(placed.template());
            features.addAll(planSocketFeatures(hallway.template().id(), placed.origin(), encounterProfile, encounterRandom));
            markOccupied(occupiedChunks, footprint.chunkX(), footprint.chunkZ(), footprint.widthChunks(), footprint.depthChunks());
        }

        planSecretRooms(level, instance, theme, placedRooms, occupiedChunks, layoutRandom, encounterRandom, encounterProfile, templates, features);

        if (startRoomOrigin == null || entryPos == null) {
            return null;
        }

        return new DungeonWorldPlan(startRoomOrigin, entryPos, templates, features);
    }

    private static void planSecretRooms(
            ServerLevel level,
            DungeonInstanceData instance,
            DungeonThemeData theme,
            Map<Long, PlacedRoom> placedRooms,
            Set<Long> occupiedChunks,
            RandomSource layoutRandom,
            RandomSource encounterRandom,
            EncounterModifierProfile encounterProfile,
            List<DungeonWorldPlan.PlacedTemplate> templates,
            List<DungeonWorldPlan.FeaturePlacement> features
    ) {
        if (theme.secretRooms().isEmpty()) {
            return;
        }

        List<PlacedRoom> orderedRooms = placedRooms.values().stream()
                .filter(room -> !room.room().startRoom())
                .sorted(Comparator.comparingInt((PlacedRoom room) -> room.room().cellZ())
                        .thenComparingInt(room -> room.room().cellX()))
                .toList();

        for (PlacedRoom room : orderedRooms) {
            DungeonSocketData socketData = DungeonSocketManager.SOCKETS.getOrDefault(room.room().template().id(), DungeonSocketData.EMPTY);
            if (socketData.secretSockets().isEmpty()) {
                continue;
            }

            DungeonThemeData.TemplateRef secretTemplate = theme.secretRooms().pick(layoutRandom).orElse(null);
            if (secretTemplate == null) {
                continue;
            }

            for (DungeonSocketData.SecretSocket secretSocket : socketData.secretSockets()) {
                int secretChunkX = room.footprintChunkX() + secretSocket.chunkOffsetX();
                int secretChunkZ = room.footprintChunkZ() + secretSocket.chunkOffsetZ();
                if (!isFootprintInBounds(instance, secretChunkX, secretChunkZ, 1, 1)) {
                    continue;
                }

                long key = chunkKey(secretChunkX, secretChunkZ);
                if (occupiedChunks.contains(key)) {
                    continue;
                }

                PlannedTemplate placedSecret = planTemplate(level, secretTemplate, secretChunkX, secretChunkZ, 1, 1);
                if (placedSecret == null) {
                    continue;
                }

                templates.add(placedSecret.template());
                features.addAll(planSocketFeatures(secretTemplate.id(), placedSecret.origin(), encounterProfile, encounterRandom));
                occupiedChunks.add(key);
                break;
            }
        }
    }

    @Nullable
    private static PlannedTemplate planTemplate(
            ServerLevel level,
            DungeonThemeData.TemplateRef templateRef,
            int footprintChunkX,
            int footprintChunkZ,
            int footprintWidthChunks,
            int footprintDepthChunks
    ) {
        Optional<StructureTemplate> templateOptional = level.getStructureManager().get(templateRef.id());
        if (templateOptional.isEmpty()) {
            return null;
        }

        StructureTemplate template = templateOptional.get();
        Vec3i size = template.getSize();
        int footprintWidthBlocks = footprintWidthChunks * 16;
        int footprintDepthBlocks = footprintDepthChunks * 16;
        if (size.getX() > footprintWidthBlocks || size.getZ() > footprintDepthBlocks) {
            return null;
        }

        int originX = footprintChunkX * 16 + Math.max(0, (footprintWidthBlocks - size.getX()) / 2);
        int originZ = footprintChunkZ * 16 + Math.max(0, (footprintDepthBlocks - size.getZ()) / 2);
        int originY = templateRef.originYForMiddleFloor(ROOM_MIDDLE_FLOOR_WORLD_Y);
        if (originY < level.getMinBuildHeight() || originY + size.getY() > level.getMaxBuildHeight()) {
            return null;
        }

        BlockPos origin = new BlockPos(originX, originY, originZ);
        BoundingBox bounds = BoundingBox.fromCorners(origin, origin.offset(size.getX() - 1, size.getY() - 1, size.getZ() - 1));
        return new PlannedTemplate(new DungeonWorldPlan.PlacedTemplate(templateRef, origin, bounds), origin, template);
    }

    private static List<DungeonWorldPlan.FeaturePlacement> planSocketFeatures(
            ResourceLocation templateId,
            BlockPos templateOrigin,
            EncounterModifierProfile profile,
            RandomSource random
    ) {
        DungeonSocketData socketData = DungeonSocketManager.SOCKETS.getOrDefault(templateId, DungeonSocketData.EMPTY);
        List<DungeonWorldPlan.FeaturePlacement> result = new ArrayList<>();
        for (DungeonSocketData.FeatureSocket feature : socketData.featureSockets()) {
            String type = feature.type();
            Vec3i rawOffset = feature.spawnOffset() == null ? Vec3i.ZERO : feature.spawnOffset();
            BlockPos pos = templateOrigin.offset(feature.pos());
            if ("encounter_spawner".equals(type)) {
                if (feature.encounterId() == null) {
                    continue;
                }
                if (profile.spawnChance() < 1.0D && random.nextDouble() > profile.spawnChance()) {
                    continue;
                }
                result.add(new DungeonWorldPlan.FeaturePlacement(
                        type,
                        pos,
                        feature.markerId(),
                        feature.encounterId(),
                        new BlockPos(rawOffset.getX(), rawOffset.getY(), rawOffset.getZ()),
                        profile.mobHealthMultiplier(),
                        profile.mobDamageMultiplier(),
                        feature.blockEntityData()
                ));
                continue;
            }

            result.add(new DungeonWorldPlan.FeaturePlacement(
                    type,
                    pos,
                    feature.markerId(),
                    feature.encounterId(),
                    new BlockPos(rawOffset.getX(), rawOffset.getY(), rawOffset.getZ()),
                    1.0D,
                    1.0D,
                    feature.blockEntityData()
            ));
        }
        return result;
    }

    private static BlockPos resolveStartEntryPosition(ResourceLocation templateId, BlockPos templateOrigin, StructureTemplate template) {
        DungeonSocketData socketData = DungeonSocketManager.SOCKETS.getOrDefault(templateId, DungeonSocketData.EMPTY);
        if (!socketData.entrySockets().isEmpty()) {
            DungeonSocketData.EntrySocket chosen = socketData.entrySockets().stream()
                    .filter(socket -> "spawn".equalsIgnoreCase(socket.id()))
                    .findFirst()
                    .orElse(socketData.entrySockets().getFirst());
            return templateOrigin.offset(chosen.pos());
        }

        Vec3i size = template.getSize();
        return templateOrigin.offset(Math.max(1, size.getX() / 2), 1, Math.max(1, size.getZ() / 2));
    }

    @Nullable
    private static HallwayFootprint hallwayFootprint(
            DungeonInstanceData instance,
            DungeonLayoutPlan.HallwayPlacement hallway,
            Map<Long, PlacedRoom> placedRooms
    ) {
        PlacedRoom from = placedRooms.get(cellKey(hallway.fromCellX(), hallway.fromCellZ()));
        PlacedRoom to = placedRooms.get(cellKey(hallway.toCellX(), hallway.toCellZ()));
        if (from == null || to == null) {
            return null;
        }

        if (from.room().startRoom() || to.room().startRoom()) {
            return new HallwayFootprint(
                    instance.originChunkX() + START_CHUNK_OFFSET,
                    instance.originChunkZ() + START_CHUNK_OFFSET + ROOM_GAP_CHUNKS,
                    1,
                    2
            );
        }

        if (hallway.orientation() == DungeonLayoutPlan.Orientation.EAST_WEST) {
            int minCellX = Math.min(hallway.fromCellX(), hallway.toCellX());
            int rowCellZ = hallway.fromCellZ();
            int chunkX = instance.originChunkX() + minCellX * CHUNK_STRIDE + 3;
            int chunkZ = instance.originChunkZ() + rowCellZ * CHUNK_STRIDE + 1;
            return new HallwayFootprint(chunkX, chunkZ, 2, 1);
        }

        int minCellZ = Math.min(hallway.fromCellZ(), hallway.toCellZ());
        int colCellX = hallway.fromCellX();
        int chunkX = instance.originChunkX() + colCellX * CHUNK_STRIDE + 1;
        int chunkZ = instance.originChunkZ() + minCellZ * CHUNK_STRIDE + 3;
        return new HallwayFootprint(chunkX, chunkZ, 1, 2);
    }

    private static boolean isFootprintInBounds(DungeonInstanceData instance, int chunkX, int chunkZ, int widthChunks, int depthChunks) {
        int maxChunkX = chunkX + widthChunks - 1;
        int maxChunkZ = chunkZ + depthChunks - 1;
        return chunkX >= instance.originChunkX()
                && chunkZ >= instance.originChunkZ()
                && maxChunkX <= instance.maxChunkX()
                && maxChunkZ <= instance.maxChunkZ();
    }

    private static void markOccupied(Set<Long> occupiedChunks, int chunkX, int chunkZ, int widthChunks, int depthChunks) {
        for (int x = chunkX; x < chunkX + widthChunks; x++) {
            for (int z = chunkZ; z < chunkZ + depthChunks; z++) {
                occupiedChunks.add(chunkKey(x, z));
            }
        }
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) ^ (chunkZ & 0xffffffffL);
    }

    private static long cellKey(int cellX, int cellZ) {
        return ((long) cellX << 32) ^ (cellZ & 0xffffffffL);
    }

    private static EncounterModifierProfile encounterModifierProfile(DungeonInstanceData instance) {
        int maxReductionPercent = 0;
        double healthMultiplier = 1.0D;
        double damageMultiplier = 1.0D;

        for (ResourceLocation modifierId : instance.modifiers()) {
            DungeonModifierData modifier = DungeonModifierManager.MODIFIERS.get(modifierId);
            if (modifier == null) {
                continue;
            }
            maxReductionPercent = Math.max(maxReductionPercent, modifier.encounterReductionPercent());
            healthMultiplier = Math.max(healthMultiplier, modifier.mobHealthMultiplier());
            damageMultiplier = Math.max(damageMultiplier, modifier.mobDamageMultiplier());
        }

        double spawnChance = 1.0D - (Math.clamp(maxReductionPercent, 0, 95) / 100.0D);
        return new EncounterModifierProfile(Math.max(0.05D, spawnChance), healthMultiplier, damageMultiplier);
    }

    private record PlannedTemplate(DungeonWorldPlan.PlacedTemplate template, BlockPos origin, StructureTemplate structureTemplate) {
    }

    private record PlacedRoom(
            DungeonLayoutPlan.RoomPlacement room,
            int footprintChunkX,
            int footprintChunkZ,
            BlockPos origin
    ) {
    }

    private record HallwayFootprint(int chunkX, int chunkZ, int widthChunks, int depthChunks) {
    }

    private record EncounterModifierProfile(double spawnChance, double mobHealthMultiplier, double mobDamageMultiplier) {
    }
}
