package io.github.ozokuz.incore.features.roguelike.instance;

import io.github.ozokuz.incore.INCore;
import io.github.ozokuz.incore.Registration;
import io.github.ozokuz.incore.features.roguelike.RoguelikeConstants;
import io.github.ozokuz.incore.features.roguelike.RoguelikePortalShape;
import io.github.ozokuz.incore.features.roguelike.RoguelikeService;
import io.github.ozokuz.incore.features.roguelike.content.RoguelikePortalBlockEntity;
import io.github.ozokuz.incore.features.roguelike.data.DungeonObjectiveData;
import io.github.ozokuz.incore.features.roguelike.data.DungeonObjectiveManager;
import io.github.ozokuz.incore.features.roguelike.data.DungeonThemeData;
import io.github.ozokuz.incore.features.roguelike.state.RoguelikeSavedData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class DungeonInstanceManager {
    private static final String REGION_FILE_PATTERN = "r.%d.%d.mca";

    private DungeonInstanceManager() {
    }

    public static boolean activatePortal(
            ServerPlayer player,
            RoguelikePortalShape portalShape,
            ItemStack crystalStack,
            ResourceLocation themeId,
            ResourceLocation objectiveId,
            DungeonThemeData themeData
    ) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return false;
        }

        ServerLevel dungeonLevel = server.getLevel(RoguelikeConstants.DUNGEON_DIMENSION);
        if (dungeonLevel == null) {
            player.sendSystemMessage(Component.translatable("incore.roguelike.portal.dimension_missing"));
            return false;
        }

        Optional<ResourceLocation> missingStructure = firstMissingStructureTemplate(dungeonLevel, themeData);
        if (missingStructure.isPresent()) {
            player.sendSystemMessage(Component.translatable("incore.roguelike.portal.missing_structure", missingStructure.get().toString()));
            return false;
        }

        RoguelikeSavedData data = RoguelikeSavedData.get(server);
        int slotIndex = data.allocateSlot();
        RoguelikeSavedData.SlotOriginChunks slotOrigin = RoguelikeSavedData.slotOriginChunks(slotIndex);

        DungeonInstanceId instanceId = data.nextInstanceId();
        DungeonInstanceData instance = new DungeonInstanceData(
                instanceId,
                slotIndex,
                slotOrigin.chunkX(),
                slotOrigin.chunkZ(),
                themeId,
                objectiveId,
                List.of(),
                0L,
                DungeonInstanceData.State.CREATED,
                DungeonInstanceData.CleanupStage.NONE,
                DungeonInstanceData.CleanupMode.NONE,
                player.serverLevel().dimension().location(),
                portalShape.bottomLeft(),
                BlockPos.ZERO,
                BlockPos.ZERO
        );

        PlacementResult placement = placeStartRoom(dungeonLevel, instance, themeData);
        if (placement == null) {
            data.freeSlot(slotIndex);
            player.sendSystemMessage(Component.translatable("incore.roguelike.portal.generation_failed"));
            return false;
        }

        instance = instance
                .withPlacement(placement.startRoomOrigin(), placement.entryPos())
                .withState(DungeonInstanceData.State.ACTIVE);
        data.putInstance(instance);

        portalShape.createPortalBlocks();
        portalShape.forEachPortalBlock(portalPos -> {
            BlockEntity blockEntity = player.serverLevel().getBlockEntity(portalPos);
            if (blockEntity instanceof RoguelikePortalBlockEntity portal) {
                portal.setInstanceId(instanceId.value());
                portal.setChanged();
            }
        });

        if (!player.isCreative()) {
            crystalStack.shrink(1);
        }

        player.sendSystemMessage(Component.translatable(
                "incore.roguelike.portal.activated",
                RoguelikeService.themeDisplayName(themeId),
                RoguelikeService.objectiveDisplayName(objectiveId)
        ));
        return true;
    }

    public static boolean tryEnterPortal(ServerPlayer player, RoguelikePortalBlockEntity portal, BlockPos portalPos) {
        if (!portal.isActivated()) {
            player.sendSystemMessage(Component.translatable("incore.roguelike.portal.not_active"));
            return false;
        }

        MinecraftServer server = player.getServer();
        if (server == null) {
            return false;
        }

        RoguelikeSavedData data = RoguelikeSavedData.get(server);
        DungeonInstanceData instance = data.getInstance(portal.instanceId());
        if (instance == null) {
            clearConnectedPortalBlocks(player.serverLevel(), portalPos, portal.instanceId());
            player.sendSystemMessage(Component.translatable("incore.roguelike.portal.no_dungeon"));
            return false;
        }

        if (player.serverLevel().dimension().equals(RoguelikeConstants.DUNGEON_DIMENSION)) {
            RoguelikeSavedData.ActiveRun run = data.getRun(player.getUUID()).orElse(null);
            if (run != null && run.instanceId().equals(instance.id())) {
                return returnPlayerFromDungeon(server, data, instance, run, player);
            }

            player.sendSystemMessage(Component.translatable("incore.roguelike.return_portal.unbound"));
            player.setPortalCooldown();
            return false;
        }

        if (instance.state() != DungeonInstanceData.State.ACTIVE) {
            player.sendSystemMessage(Component.translatable("incore.roguelike.portal.not_active"));
            return false;
        }

        UUID playerId = player.getUUID();
        if (data.getRun(playerId).isPresent()) {
            player.sendSystemMessage(Component.translatable("incore.roguelike.portal.already_entered"));
            player.setPortalCooldown();
            return false;
        }

        ServerLevel dungeonLevel = server.getLevel(RoguelikeConstants.DUNGEON_DIMENSION);
        if (dungeonLevel == null) {
            player.sendSystemMessage(Component.translatable("incore.roguelike.portal.dimension_missing"));
            return false;
        }

        if (instance.endGameTime() <= 0L) {
            instance = instance.withEndGameTime(dungeonLevel.getGameTime() + RoguelikeConstants.DUNGEON_TIME_LIMIT_TICKS);
            data.putInstance(instance);
        }

        data.startRun(playerId, instance.id(), player.serverLevel().dimension(), portalPos);
        teleport(player, dungeonLevel, instance.entryPos());

        DungeonObjectiveData objective = DungeonObjectiveManager.OBJECTIVES.get(instance.objectiveId());
        if (objective != null) {
            player.sendSystemMessage(Component.translatable(
                    "incore.roguelike.portal.objective",
                    RoguelikeService.objectiveDisplayName(instance.objectiveId()),
                    0,
                    objective.target()
            ).withStyle(ChatFormatting.GOLD));
        }

        return true;
    }

    private static boolean returnPlayerFromDungeon(
            MinecraftServer server,
            RoguelikeSavedData data,
            DungeonInstanceData instance,
            RoguelikeSavedData.ActiveRun run,
            ServerPlayer player
    ) {
        ServerLevel returnLevel = server.getLevel(run.returnDimensionKey());
        if (returnLevel == null) {
            player.sendSystemMessage(Component.translatable("incore.roguelike.portal.dimension_missing"));
            return false;
        }

        data.clearRun(player.getUUID());
        teleport(player, returnLevel, run.returnPos());

        if (instance.state() == DungeonInstanceData.State.ACTIVE) {
            data.putInstance(instance
                    .withState(DungeonInstanceData.State.COMPLETED)
                    .withCleanup(DungeonInstanceData.CleanupStage.DENY_ENTRY, DungeonInstanceData.CleanupMode.EVICT));
        }

        player.sendSystemMessage(Component.translatable("incore.roguelike.return_portal.used"));
        return true;
    }

    public static boolean tryUseReturnPlaceholder(Player player, BlockPos interactionPos) {
        if (!(player instanceof ServerPlayer serverPlayer) || serverPlayer.getServer() == null) {
            return false;
        }

        if (!serverPlayer.serverLevel().dimension().equals(RoguelikeConstants.DUNGEON_DIMENSION)) {
            serverPlayer.sendSystemMessage(Component.translatable("incore.roguelike.return_portal.unbound"));
            return false;
        }

        MinecraftServer server = serverPlayer.getServer();
        RoguelikeSavedData data = RoguelikeSavedData.get(server);
        RoguelikeSavedData.ActiveRun run = data.getRun(serverPlayer.getUUID()).orElse(null);
        if (run == null) {
            serverPlayer.sendSystemMessage(Component.translatable("incore.roguelike.return_portal.unbound"));
            return false;
        }

        DungeonInstanceData instance = data.getInstance(run.instanceId());
        if (instance == null || !instance.containsBlock(interactionPos)) {
            serverPlayer.sendSystemMessage(Component.translatable("incore.roguelike.return_portal.unbound"));
            return false;
        }

        ServerLevel returnLevel = server.getLevel(run.returnDimensionKey());
        if (returnLevel == null) {
            serverPlayer.sendSystemMessage(Component.translatable("incore.roguelike.portal.dimension_missing"));
            return false;
        }

        data.clearRun(serverPlayer.getUUID());
        teleport(serverPlayer, returnLevel, run.returnPos());

        if (instance.state() == DungeonInstanceData.State.ACTIVE) {
            data.putInstance(instance
                    .withState(DungeonInstanceData.State.COMPLETED)
                    .withCleanup(DungeonInstanceData.CleanupStage.DENY_ENTRY, DungeonInstanceData.CleanupMode.EVICT));
        }

        serverPlayer.sendSystemMessage(Component.translatable("incore.roguelike.return_portal.used"));
        return true;
    }

    public static void onServerTick(MinecraftServer server) {
        if (server.getTickCount() % RoguelikeConstants.MANAGER_TICK_INTERVAL != 0) {
            return;
        }

        RoguelikeSavedData data = RoguelikeSavedData.get(server);
        ServerLevel dungeonLevel = server.getLevel(RoguelikeConstants.DUNGEON_DIMENSION);
        if (dungeonLevel == null) {
            return;
        }

        long now = dungeonLevel.getGameTime();
        List<DungeonInstanceData> snapshot = new ArrayList<>(data.instances());
        for (DungeonInstanceData instance : snapshot) {
            if (instance.state() == DungeonInstanceData.State.ACTIVE) {
                long remaining = instance.endGameTime() > 0L ? instance.endGameTime() - now : -1L;
                if (instance.endGameTime() > 0L && remaining <= 0L) {
                    data.putInstance(instance
                            .withState(DungeonInstanceData.State.EXPIRED)
                            .withCleanup(DungeonInstanceData.CleanupStage.DENY_ENTRY, DungeonInstanceData.CleanupMode.KILL));
                    continue;
                }

                if (remaining > 0L) {
                    sendTimerToDungeonPlayers(server, data, instance, remaining);
                }
                continue;
            }

            if (instance.state() == DungeonInstanceData.State.COMPLETED) {
                data.putInstance(instance
                        .withState(DungeonInstanceData.State.CLEANING)
                        .withCleanup(DungeonInstanceData.CleanupStage.DENY_ENTRY, DungeonInstanceData.CleanupMode.EVICT));
                continue;
            }

            if (instance.state() == DungeonInstanceData.State.EXPIRED) {
                data.putInstance(instance
                        .withState(DungeonInstanceData.State.CLEANING)
                        .withCleanup(DungeonInstanceData.CleanupStage.DENY_ENTRY, DungeonInstanceData.CleanupMode.KILL));
                continue;
            }

            if (instance.state() == DungeonInstanceData.State.CLEANING) {
                processCleaning(server, dungeonLevel, data, instance);
            }
        }
    }

    private static void processCleaning(MinecraftServer server, ServerLevel dungeonLevel, RoguelikeSavedData data, DungeonInstanceData instance) {
        switch (instance.cleanupStage()) {
            case NONE, DENY_ENTRY -> {
                deactivatePortal(server, instance);
                data.putInstance(instance.withCleanupStage(DungeonInstanceData.CleanupStage.EVICT_PLAYERS));
            }
            case EVICT_PLAYERS -> {
                evictPlayers(server, data, instance);
                data.putInstance(instance.withCleanupStage(DungeonInstanceData.CleanupStage.WAIT_UNLOAD));
            }
            case WAIT_UNLOAD -> {
                if (!hasLoadedChunks(dungeonLevel, instance)) {
                    data.putInstance(instance.withCleanupStage(DungeonInstanceData.CleanupStage.DELETE_FILES));
                }
            }
            case DELETE_FILES -> {
                if (deleteInstanceRegionFiles(server, instance)) {
                    data.putInstance(instance.withCleanupStage(DungeonInstanceData.CleanupStage.REMOVE_METADATA));
                }
            }
            case REMOVE_METADATA -> {
                data.freeSlot(instance.slotIndex());
                data.removeInstance(instance.id());
            }
        }
    }

    private static void evictPlayers(MinecraftServer server, RoguelikeSavedData data, DungeonInstanceData instance) {
        for (RoguelikeSavedData.ActiveRun run : data.activeRunsForInstance(instance.id())) {
            UUID playerId = run.playerId();
            ServerPlayer activePlayer = server.getPlayerList().getPlayer(playerId);
            data.setPendingReturn(playerId, run.returnDimensionKey(), run.returnPos());
            data.clearRun(playerId);

            if (activePlayer == null) {
                continue;
            }

            if (instance.cleanupMode() == DungeonInstanceData.CleanupMode.KILL) {
                if (activePlayer.serverLevel().dimension().equals(RoguelikeConstants.DUNGEON_DIMENSION)) {
                    activePlayer.kill();
                }
                continue;
            }

            ServerLevel returnLevel = server.getLevel(run.returnDimensionKey());
            if (returnLevel != null) {
                teleport(activePlayer, returnLevel, run.returnPos());
                data.takePendingReturn(playerId);
            }
        }
    }

    private static boolean hasLoadedChunks(ServerLevel level, DungeonInstanceData instance) {
        for (int chunkX = instance.originChunkX(); chunkX <= instance.maxChunkX(); chunkX++) {
            for (int chunkZ = instance.originChunkZ(); chunkZ <= instance.maxChunkZ(); chunkZ++) {
                if (level.hasChunk(chunkX, chunkZ)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean deleteInstanceRegionFiles(MinecraftServer server, DungeonInstanceData instance) {
        Path dimensionPath = dimensionPath(server, RoguelikeConstants.DUNGEON_DIMENSION);
        if (dimensionPath == null) {
            return false;
        }

        for (String folder : List.of("region", "poi", "entities")) {
            Path folderPath = dimensionPath.resolve(folder);
            for (int regionX = instance.minRegionX(); regionX <= instance.maxRegionX(); regionX++) {
                for (int regionZ = instance.minRegionZ(); regionZ <= instance.maxRegionZ(); regionZ++) {
                    Path regionFile = folderPath.resolve(REGION_FILE_PATTERN.formatted(regionX, regionZ));
                    try {
                        Files.deleteIfExists(regionFile);
                    } catch (IOException exception) {
                        INCore.LOGGER.warn("Failed deleting dungeon instance region file {}", regionFile, exception);
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private static Path dimensionPath(MinecraftServer server, ResourceKey<Level> dimension) {
        Path root = server.getWorldPath(LevelResource.ROOT);
        if (dimension == Level.OVERWORLD) {
            return root;
        }
        if (dimension == Level.NETHER) {
            return root.resolve("DIM-1");
        }
        if (dimension == Level.END) {
            return root.resolve("DIM1");
        }

        ResourceLocation id = dimension.location();
        return root.resolve("dimensions").resolve(id.getNamespace()).resolve(id.getPath());
    }

    public static void onPlayerDeath(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }

        RoguelikeSavedData data = RoguelikeSavedData.get(server);
        data.getRun(player.getUUID()).ifPresent(run -> {
            data.setPendingReturn(player.getUUID(), run.returnDimensionKey(), run.returnPos());
            data.clearRun(player.getUUID());
        });
    }

    public static void onPlayerRespawn(ServerPlayer player) {
        applyPendingReturn(player);
    }

    public static void onPlayerLogin(ServerPlayer player) {
        applyPendingReturn(player);
    }

    public static void onPlayerLogout(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null || !player.serverLevel().dimension().equals(RoguelikeConstants.DUNGEON_DIMENSION)) {
            return;
        }

        RoguelikeSavedData data = RoguelikeSavedData.get(server);
        data.getRun(player.getUUID()).ifPresent(run -> {
            data.setPendingReturn(player.getUUID(), run.returnDimensionKey(), run.returnPos());
            data.clearRun(player.getUUID());
        });
    }

    private static void applyPendingReturn(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }

        RoguelikeSavedData data = RoguelikeSavedData.get(server);
        data.takePendingReturn(player.getUUID()).ifPresent(target -> {
            ServerLevel level = server.getLevel(target.dimensionKey());
            if (level != null) {
                teleport(player, level, target.pos());
            }
        });
    }

    public static void onDungeonMobDeath() {
        // Objective mob tracking is removed in phase 1.
    }

    public static void onSpawnPositionCheck(MobSpawnEvent.PositionCheck event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (!level.dimension().equals(RoguelikeConstants.DUNGEON_DIMENSION)) {
            return;
        }

        MobSpawnType spawnType = event.getSpawnType();
        boolean allowed = spawnType == MobSpawnType.SPAWNER
                || spawnType == MobSpawnType.TRIAL_SPAWNER
                || spawnType == MobSpawnType.COMMAND;

        if (!allowed) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
        }
    }

    private static PlacementResult placeStartRoom(ServerLevel level, DungeonInstanceData instance, DungeonThemeData theme) {
        Optional<StructureTemplate> templateOptional = level.getStructureManager().get(theme.startingRoomStructure().id());
        if (templateOptional.isEmpty()) {
            return null;
        }

        StructureTemplate template = templateOptional.get();
        var size = template.getSize();
        int slotSpanBlocks = RoguelikeConstants.INSTANCE_SIZE_CHUNKS * 16;

        int minBlockX = instance.originChunkX() * 16;
        int minBlockZ = instance.originChunkZ() * 16;

        int originX = minBlockX + Math.max(0, (slotSpanBlocks - size.getX()) / 2);
        int originZ = minBlockZ + Math.max(0, (slotSpanBlocks - size.getZ()) / 2);
        int floorY = RoguelikeConstants.DUNGEON_FLOOR_Y;
        int originY = floorY - Math.max(0, theme.startingRoomStructure().floorYFromBottom());

        if (originY < 0 || originY + size.getY() - 1 > 80) {
            return null;
        }

        BlockPos origin = new BlockPos(originX, originY, originZ);
        StructurePlaceSettings settings = new StructurePlaceSettings().setIgnoreEntities(true);
        if (!template.placeInWorld(level, origin, origin, settings, level.random, Block.UPDATE_ALL)) {
            return null;
        }
        replaceReturnPortalPlaceholders(level, template, origin, settings, instance.id().value());

        BlockPos entry = new BlockPos(
                originX + Math.max(1, size.getX() / 2),
                Math.min(80, floorY + 1),
                originZ + Math.max(1, size.getZ() / 2)
        );
        clearEntrySpace(level, entry);
        return new PlacementResult(origin, entry);
    }

    private static void clearEntrySpace(ServerLevel level, BlockPos entry) {
        level.setBlockAndUpdate(entry, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(entry.above(), Blocks.AIR.defaultBlockState());
        if (level.getBlockState(entry.below()).isAir()) {
            level.setBlockAndUpdate(entry.below(), Blocks.BEDROCK.defaultBlockState());
        }
    }

    private static Optional<ResourceLocation> firstMissingStructureTemplate(ServerLevel level, DungeonThemeData theme) {
        if (level.getStructureManager().get(theme.startingRoomStructure().id()).isEmpty()) {
            return Optional.of(theme.startingRoomStructure().id());
        }

        return Optional.empty();
    }

    private static void replaceReturnPortalPlaceholders(
            ServerLevel level,
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
            level.setBlockAndUpdate(pos, Registration.ROGUELIKE_PORTAL_BLOCK.get().defaultBlockState());
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof RoguelikePortalBlockEntity portal) {
                portal.setInstanceId(instanceId);
                portal.setChanged();
            }
        }
    }

    private static void deactivatePortal(MinecraftServer server, DungeonInstanceData instance) {
        ResourceKey<Level> portalDimensionKey = ResourceKey.create(Registries.DIMENSION, instance.portalDimension());
        ServerLevel level = server.getLevel(portalDimensionKey);
        if (level == null) {
            return;
        }

        clearConnectedPortalBlocks(level, instance.portalPos(), instance.id().value());
    }

    private static void clearConnectedPortalBlocks(ServerLevel level, BlockPos startPos, long instanceId) {
        if (instanceId <= 0L) {
            return;
        }

        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(startPos.immutable());

        while (!queue.isEmpty()) {
            BlockPos current = queue.removeFirst();
            if (!visited.add(current)) {
                continue;
            }

            BlockEntity blockEntity = level.getBlockEntity(current);
            if (!(blockEntity instanceof RoguelikePortalBlockEntity portal) || portal.instanceId() != instanceId) {
                continue;
            }

            level.setBlockAndUpdate(current, Blocks.AIR.defaultBlockState());
            for (Direction direction : Direction.values()) {
                queue.add(current.relative(direction));
            }
        }
    }

    private static void sendTimerToDungeonPlayers(MinecraftServer server, RoguelikeSavedData data, DungeonInstanceData instance, long remainingTicks) {
        int totalSeconds = (int) Math.max(0L, remainingTicks / 20L);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        String secondsText = String.format(Locale.ROOT, "%02d", seconds);

        for (RoguelikeSavedData.ActiveRun run : data.activeRunsForInstance(instance.id())) {
            ServerPlayer player = server.getPlayerList().getPlayer(run.playerId());
            if (player != null) {
                player.displayClientMessage(Component.translatable("incore.roguelike.timer", minutes, secondsText), true);
            }
        }
    }

    private static void teleport(ServerPlayer player, ServerLevel level, BlockPos pos) {
        player.teleportTo(level, pos.getX() + 0.5D, pos.getY() + 0.1D, pos.getZ() + 0.5D, player.getYRot(), player.getXRot());
    }

    private record PlacementResult(BlockPos startRoomOrigin, BlockPos entryPos) {
    }
}
