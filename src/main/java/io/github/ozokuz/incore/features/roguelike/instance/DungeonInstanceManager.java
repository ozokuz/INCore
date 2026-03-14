package io.github.ozokuz.incore.features.roguelike.instance;

import io.github.ozokuz.incore.INCore;
import io.github.ozokuz.incore.Registration;
import io.github.ozokuz.incore.features.market.MarketBanking;
import io.github.ozokuz.incore.features.roguelike.DungeonDeathDifficulty;
import io.github.ozokuz.incore.features.roguelike.RoguelikeConstants;
import io.github.ozokuz.incore.features.roguelike.RoguelikePortalShape;
import io.github.ozokuz.incore.features.roguelike.RoguelikeService;
import io.github.ozokuz.incore.features.roguelike.content.RoguelikePortalBlockEntity;
import io.github.ozokuz.incore.features.roguelike.data.DungeonObjectiveData;
import io.github.ozokuz.incore.features.roguelike.data.DungeonObjectiveManager;
import io.github.ozokuz.incore.features.roguelike.data.DungeonModifierData;
import io.github.ozokuz.incore.features.roguelike.data.DungeonModifierManager;
import io.github.ozokuz.incore.features.roguelike.data.DungeonThemeData;
import io.github.ozokuz.incore.features.roguelike.layout.DungeonLayoutGenerator;
import io.github.ozokuz.incore.features.roguelike.network.RoguelikeMinimapPartyPayload;
import io.github.ozokuz.incore.features.roguelike.network.RoguelikeNetworking;
import io.github.ozokuz.incore.features.roguelike.state.RoguelikeSavedData;
import io.github.ozokuz.incore.features.roguelike.worldgen.DungeonWorldPlan;
import io.github.ozokuz.incore.features.roguelike.worldgen.DungeonWorldPlanner;
import io.github.ozokuz.incore.features.party.PartyService;
import io.github.ozokuz.incore.features.tasks.DailyTaskEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class DungeonInstanceManager {
    private static final String REGION_FILE_PATTERN = "r.%d.%d.mca";
    private static final int ROOM_SIZE_CHUNKS = DungeonWorldPlanner.ROOM_SIZE_CHUNKS;
    private static final int CHUNK_STRIDE = DungeonWorldPlanner.CHUNK_STRIDE;
    private static final int START_CHUNK_OFFSET = DungeonWorldPlanner.START_CHUNK_OFFSET;
    private static final int MINIMAP_GRID_SIZE = DungeonLayoutGenerator.GRID_SIZE;
    private static final int MINIMAP_CENTER_CELL = DungeonLayoutGenerator.CENTER_CELL;
    private static final Map<Long, Map<UUID, Set<Integer>>> REVEALED_ROOMS = new HashMap<>();
    private static final Map<UUID, Long> LAST_GRAPH_SYNC_INSTANCE = new HashMap<>();
    private static final Map<UUID, PendingDeathOutcome> PENDING_DEATH_OUTCOMES = new HashMap<>();

    private DungeonInstanceManager() {
    }

    public static boolean activatePortal(
            ServerPlayer player,
            RoguelikePortalShape portalShape,
            ItemStack crystalStack,
            ResourceLocation themeId,
            ResourceLocation objectiveId,
            List<ResourceLocation> modifierIds,
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
        long boundPartyId = PartyService.getPartyIdForPlayer(server, player.getUUID());

        DungeonInstanceId instanceId = data.nextInstanceId();
        DungeonInstanceData instance = new DungeonInstanceData(
                instanceId,
                slotIndex,
                slotOrigin.chunkX(),
                slotOrigin.chunkZ(),
                themeId,
                objectiveId,
                modifierIds == null ? List.of() : List.copyOf(modifierIds),
                0L,
                DungeonInstanceData.State.CREATED,
                DungeonInstanceData.CleanupStage.NONE,
                DungeonInstanceData.CleanupMode.NONE,
                player.serverLevel().dimension().location(),
                portalShape.bottomLeft(),
                BlockPos.ZERO,
                BlockPos.ZERO,
                boundPartyId,
                player.getUUID(),
                Set.of()
        );

        DungeonWorldPlan plan = DungeonWorldPlanner.plan(dungeonLevel, instance, themeData);
        if (plan == null) {
            data.freeSlot(slotIndex);
            player.sendSystemMessage(Component.translatable("incore.roguelike.portal.generation_failed"));
            return false;
        }

        instance = instance
                .withPlacement(plan.startRoomOrigin(), plan.entryPos())
                .withState(DungeonInstanceData.State.ACTIVE);
        data.putInstance(instance);
        initializeObjectiveState(data, instance, plan);

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
        MinecraftServer server = player.getServer();
        if (server == null) {
            return false;
        }

        RoguelikeSavedData data = RoguelikeSavedData.get(server);

        if (player.serverLevel().dimension().equals(RoguelikeConstants.DUNGEON_DIMENSION)) {
            RoguelikeSavedData.ActiveRun run = data.getRun(player.getUUID()).orElse(null);
            if (run != null) {
                DungeonInstanceData activeInstance = data.getInstance(run.instanceId());
                if (activeInstance != null) {
                    return returnPlayerFromDungeon(server, data, activeInstance, run, player);
                }

                data.clearRun(player.getUUID());
                ServerLevel returnLevel = server.getLevel(run.returnDimensionKey());
                if (returnLevel == null) {
                    player.sendSystemMessage(Component.translatable("incore.roguelike.portal.dimension_missing"));
                    return false;
                }

                teleport(player, returnLevel, run.returnPos());
                player.sendSystemMessage(Component.translatable("incore.roguelike.return_portal.used"));
                return true;
            }

            player.sendSystemMessage(Component.translatable("incore.roguelike.return_portal.unbound"));
            player.setPortalCooldown();
            return false;
        }

        if (!portal.isActivated()) {
            player.sendSystemMessage(Component.translatable("incore.roguelike.portal.not_active"));
            return false;
        }

        DungeonInstanceData instance = data.getInstance(portal.instanceId());
        if (instance == null) {
            clearConnectedPortalBlocks(player.serverLevel(), portalPos, portal.instanceId());
            player.sendSystemMessage(Component.translatable("incore.roguelike.portal.no_dungeon"));
            return false;
        }

        if (instance.state() != DungeonInstanceData.State.ACTIVE) {
            player.sendSystemMessage(Component.translatable("incore.roguelike.portal.not_active"));
            return false;
        }

        UUID playerId = player.getUUID();
        if (!PartyService.canEnterDungeonInstance(server, instance, playerId)) {
            player.sendSystemMessage(Component.translatable("incore.roguelike.portal.party_locked"));
            player.setPortalCooldown();
            return false;
        }

        if (instance.hasPlayerEntered(playerId)) {
            player.sendSystemMessage(Component.translatable("incore.roguelike.portal.already_entered"));
            player.setPortalCooldown();
            return false;
        }

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
            instance = instance.withEndGameTime(dungeonLevel.getGameTime() + timerLimitTicks(instance));
        }

        instance = instance.withPlayerEntered(playerId);
        data.putInstance(instance);

        data.startRun(playerId, instance.id(), player.serverLevel().dimension(), portalPos);
        teleport(player, dungeonLevel, instance.entryPos());
        syncMinimapGraphIfNeeded(player, instance);

        ensurePlayerObjectiveProgress(data, instance, playerId);

        DungeonObjectiveData objective = DungeonObjectiveManager.getObjective(instance.objectiveId());
        if (objective != null) {
            RoguelikeSavedData.ObjectiveStateRecord state = data.objectiveState(instance.id());
            RoguelikeSavedData.PlayerObjectiveProgress playerProgress = state == null
                    ? RoguelikeSavedData.PlayerObjectiveProgress.EMPTY
                    : state.progress(playerId);
            int progress = playerProgress.progress();
            int target = state == null ? objective.target() : state.target();
            player.sendSystemMessage(Component.translatable(
                    "incore.roguelike.portal.objective",
                    RoguelikeService.objectiveDisplayName(instance.objectiveId()),
                    progress,
                    target
            ).withStyle(ChatFormatting.GOLD));
            if (state != null) {
                String hintKey = switch (state.type()) {
                    case "signal_emission" -> "incore.roguelike.objective.signal.hint";
                    case "scavenger_hunt" -> "incore.roguelike.objective.scavenger.hint";
                    default -> "incore.roguelike.objective.essence.hint";
                };
                player.sendSystemMessage(Component.translatable(hintKey, progress, target).withStyle(ChatFormatting.GRAY));
            }
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

        markPlayerAbandonedIfIncomplete(data, instance, player.getUUID());
        updateInstanceResolution(data, instance);

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

        markPlayerAbandonedIfIncomplete(data, instance, serverPlayer.getUUID());
        updateInstanceResolution(data, instance);

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
        syncXaeroMapSuppression(server);

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
                syncMinimap(server, data, instance);
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
                REVEALED_ROOMS.remove(instance.id().value());
                data.removeObjectiveState(instance.id());
                LAST_GRAPH_SYNC_INSTANCE.entrySet().removeIf(entry -> entry.getValue().equals(instance.id().value()));
            }
            default -> {
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
        LAST_GRAPH_SYNC_INSTANCE.remove(player.getUUID());

        RoguelikeSavedData data = RoguelikeSavedData.get(server);
        data.getRun(player.getUUID()).ifPresent(run -> {
            DungeonDeathDifficulty difficulty = data.dungeonDeathDifficulty(player.getUUID());
            PENDING_DEATH_OUTCOMES.put(
                    player.getUUID(),
                    new PendingDeathOutcome(difficulty, Math.clamp(player.getInventory().selected, 0, 8))
            );

            data.setPendingReturn(player.getUUID(), run.returnDimensionKey(), run.returnPos());
            data.clearRun(player.getUUID());

            DungeonInstanceData instance = data.getInstance(run.instanceId());
            if (instance != null) {
                markPlayerFailed(data, instance, player.getUUID());
                updateInstanceResolution(data, instance);
            }
        });
    }

    public static void onPlayerDrops(ServerPlayer player, LivingDropsEvent event) {
        PendingDeathOutcome outcome = PENDING_DEATH_OUTCOMES.remove(player.getUUID());
        if (outcome == null || player.getServer() == null) {
            return;
        }

        RoguelikeSavedData data = RoguelikeSavedData.get(player.getServer());
        applyDeathOutcome(data, player, outcome, captureDropSnapshot(player, event));
        event.getDrops().clear();
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
        LAST_GRAPH_SYNC_INSTANCE.remove(player.getUUID());
        data.getRun(player.getUUID()).ifPresent(run -> {
            data.setPendingReturn(player.getUUID(), run.returnDimensionKey(), run.returnPos());
            data.clearRun(player.getUUID());

            DungeonInstanceData instance = data.getInstance(run.instanceId());
            if (instance != null) {
                markPlayerAbandonedIfIncomplete(data, instance, player.getUUID());
                updateInstanceResolution(data, instance);
            }
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
        data.takePendingInventoryRestore(player.getUUID()).ifPresent(restore -> restoreInventory(player, restore));
        data.takePendingRecoveryDelivery(player.getUUID()).ifPresent(recoveryId -> deliverRecoveryItems(player, recoveryId));
    }

    public static void onDungeonMobDeath(net.minecraft.world.entity.LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level) || !level.dimension().equals(RoguelikeConstants.DUNGEON_DIMENSION)) {
            return;
        }

        MinecraftServer server = level.getServer();
        if (server == null) {
            return;
        }

        RoguelikeSavedData data = RoguelikeSavedData.get(server);
        DungeonInstanceData instance = findInstanceForPos(data, entity.blockPosition());
        if (instance == null) {
            return;
        }

        RoguelikeSavedData.ObjectiveStateRecord state = data.objectiveState(instance.id());
        if (state == null) {
            return;
        }

        boolean encounterCompletion = isEncounterCompletion(entity, level);
        boolean eliteKill = isEliteKill(entity);
        if ("essence_gathering".equals(state.type())) {
            if (encounterCompletion || eliteKill) {
                creditActivePlayers(server, data, instance, 1, null);
            }
            return;
        }

        if ("scavenger_hunt".equals(state.type()) && (encounterCompletion || eliteKill)) {
            ServerPlayer killer = killerPlayer(entity);
            if (killer != null && data.getRun(killer.getUUID()).isPresent()) {
                giveScavengerToken(killer);
            }
        }
    }

    public static void onDungeonBlockInteracted(ServerPlayer player, BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        if (!player.serverLevel().dimension().equals(RoguelikeConstants.DUNGEON_DIMENSION)) {
            return;
        }

        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }

        RoguelikeSavedData data = RoguelikeSavedData.get(server);
        RoguelikeSavedData.ActiveRun run = data.getRun(player.getUUID()).orElse(null);
        if (run == null) {
            return;
        }

        DungeonInstanceData instance = data.getInstance(run.instanceId());
        if (instance == null || !instance.containsBlock(pos)) {
            return;
        }

        RoguelikeSavedData.ObjectiveStateRecord objectiveState = data.objectiveState(instance.id());
        if (objectiveState == null) {
            return;
        }

        if ("signal_emission".equals(objectiveState.type()) && state.is(Registration.ENCOUNTER_SPAWNER_BLOCK.get())) {
            if (tryActivatePylonForPlayer(data, instance, player.getUUID(), pos)) {
                creditActivePlayers(server, data, instance, 1, player.getUUID());
            }
            return;
        }

        if (isChestLike(state) && markChestOpened(data, instance, pos)) {
            if ("essence_gathering".equals(objectiveState.type())) {
                creditActivePlayers(server, data, instance, 1, player.getUUID());
            } else if ("scavenger_hunt".equals(objectiveState.type())) {
                giveScavengerToken(player);
            }
        }
    }

    public static boolean trySubmitScavengerToken(Player player, @org.jetbrains.annotations.Nullable net.minecraft.world.InteractionHand hand, BlockPos altarPos) {
        if (!(player instanceof ServerPlayer serverPlayer) || serverPlayer.getServer() == null) {
            return false;
        }
        if (!serverPlayer.serverLevel().dimension().equals(RoguelikeConstants.DUNGEON_DIMENSION)) {
            return false;
        }

        MinecraftServer server = serverPlayer.getServer();
        RoguelikeSavedData data = RoguelikeSavedData.get(server);
        RoguelikeSavedData.ActiveRun run = data.getRun(serverPlayer.getUUID()).orElse(null);
        if (run == null) {
            return false;
        }

        DungeonInstanceData instance = data.getInstance(run.instanceId());
        if (instance == null || !instance.containsBlock(altarPos)) {
            return false;
        }

        RoguelikeSavedData.ObjectiveStateRecord objectiveState = data.objectiveState(instance.id());
        if (objectiveState == null || !"scavenger_hunt".equals(objectiveState.type())) {
            return false;
        }
        if (!isScavengerAltar(objectiveState, altarPos)) {
            return false;
        }

        if (hand == null) {
            RoguelikeSavedData.PlayerObjectiveProgress progress = objectiveState.progress(serverPlayer.getUUID());
            serverPlayer.sendSystemMessage(Component.translatable("incore.roguelike.objective.scavenger.hint", progress.progress(), objectiveState.target()));
            return true;
        }

        ItemStack held = serverPlayer.getItemInHand(hand);
        if (!held.is(Registration.DUNGEON_SCAVENGER_TOKEN_ITEM.get())) {
            return false;
        }

        held.shrink(1);
        creditActivePlayers(server, data, instance, 1, serverPlayer.getUUID());
        return true;
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

    private static Optional<ResourceLocation> firstMissingStructureTemplate(ServerLevel level, DungeonThemeData theme) {
        List<ResourceLocation> ids = new ArrayList<>(theme.allTemplateIds());
        ids.sort(Comparator.comparing(ResourceLocation::toString));
        for (ResourceLocation id : ids) {
            if (level.getStructureManager().get(id).isEmpty()) {
                return Optional.of(id);
            }
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

    private static void checkInstanceEmptyAndCleanup(RoguelikeSavedData data, DungeonInstanceData instance) {
        if (instance.state() != DungeonInstanceData.State.ACTIVE) {
            return;
        }

        List<RoguelikeSavedData.ActiveRun> remainingRuns = data.activeRunsForInstance(instance.id());
        if (remainingRuns.isEmpty()) {
            data.putInstance(instance
                    .withState(DungeonInstanceData.State.COMPLETED)
                    .withCleanup(DungeonInstanceData.CleanupStage.DENY_ENTRY, DungeonInstanceData.CleanupMode.EVICT));
        }
    }

    private static long timerLimitTicks(DungeonInstanceData instance) {
        long adjusted = RoguelikeConstants.DUNGEON_TIME_LIMIT_TICKS;
        for (ResourceLocation modifierId : instance.modifiers()) {
            DungeonModifierData modifier = DungeonModifierManager.MODIFIERS.get(modifierId);
            if (modifier == null) {
                continue;
            }
            adjusted += modifier.timerTicksDelta();
        }
        return Math.max(20L, adjusted);
    }

    public static void ensureObjectiveBlocksGenerated(ServerLevel dungeonLevel, DungeonInstanceData instance, ChunkPos chunkPos) {
        MinecraftServer server = dungeonLevel.getServer();
        if (server == null) {
            return;
        }

        RoguelikeSavedData data = RoguelikeSavedData.get(server);
        RoguelikeSavedData.ObjectiveStateRecord objectiveState = data.objectiveState(instance.id());
        if (objectiveState == null) {
            return;
        }

        if (!"scavenger_hunt".equals(objectiveState.type()) || !objectiveState.objectiveAltars().isEmpty()) {
            return;
        }

        if ((instance.entryPos().getX() >> 4) != chunkPos.x || (instance.entryPos().getZ() >> 4) != chunkPos.z) {
            return;
        }

        BlockPos scavengerAltarPos = findScavengerAltarPos(dungeonLevel, instance.entryPos());
        data.putObjectiveState(objectiveState.withObjectiveAltars(List.of(scavengerAltarPos)));
        dungeonLevel.setBlockAndUpdate(scavengerAltarPos, Registration.DUNGEON_OBJECTIVE_ALTAR_BLOCK.get().defaultBlockState());
    }

    private static void initializeObjectiveState(RoguelikeSavedData data, DungeonInstanceData instance, DungeonWorldPlan plan) {
        DungeonObjectiveData objectiveData = DungeonObjectiveManager.getObjective(instance.objectiveId());
        if (objectiveData == null) {
            data.removeObjectiveState(instance.id());
            return;
        }

        String type = objectiveData.type();
        int target = Math.max(1, objectiveData.target());
        Set<BlockPos> pylons = new HashSet<>();
        List<BlockPos> objectiveAltars = plan.featurePositions("objective_altar");

        if ("signal_emission".equals(type)) {
            for (BlockPos pos : plan.encounterSpawnerPositions()) {
                pylons.add(pos.immutable());
                if (pylons.size() >= target) {
                    break;
                }
            }
            if (pylons.isEmpty()) {
                pylons.add(instance.entryPos().immutable());
            }
            target = Math.max(1, Math.min(target, pylons.size()));
        }

        data.putObjectiveState(new RoguelikeSavedData.ObjectiveStateRecord(
                instance.id(),
                type,
                target,
                objectiveData.rewardCrates(),
                pylons,
                objectiveAltars,
                Set.of(),
                Map.of()
        ));
    }

    private static BlockPos findScavengerAltarPos(ServerLevel level, BlockPos entryPos) {
        BlockPos[] candidates = new BlockPos[]{
                entryPos.offset(2, 0, 0),
                entryPos.offset(-2, 0, 0),
                entryPos.offset(0, 0, 2),
                entryPos.offset(0, 0, -2),
                entryPos.offset(2, 1, 0)
        };
        for (BlockPos candidate : candidates) {
            if (level.getBlockState(candidate).isAir() && level.getBlockState(candidate.above()).isAir()) {
                return candidate;
            }
        }
        return entryPos.offset(2, 1, 0);
    }

    private static void creditActivePlayers(
            MinecraftServer server,
            RoguelikeSavedData data,
            DungeonInstanceData instance,
            int amount,
            UUID sourcePlayerId
    ) {
        if (amount <= 0) {
            return;
        }

        RoguelikeSavedData.ObjectiveStateRecord state = data.objectiveState(instance.id());
        if (state == null) {
            return;
        }

        List<UUID> creditedPlayers = new ArrayList<>();
        for (RoguelikeSavedData.ActiveRun run : data.activeRunsForInstance(instance.id())) {
            UUID playerId = run.playerId();
            ensurePlayerObjectiveProgress(data, instance, playerId);
            state = data.objectiveState(instance.id());
            RoguelikeSavedData.PlayerObjectiveProgress progress = state.progress(playerId);
            if (progress.resolved()) {
                continue;
            }

            int nextProgress = Math.min(state.target(), progress.progress() + amount);
            if (nextProgress == progress.progress()) {
                continue;
            }

            data.putObjectiveState(state.withPlayerProgress(
                    playerId,
                    new RoguelikeSavedData.PlayerObjectiveProgress(nextProgress, progress.resolution(), progress.activatedPylons())
            ));
            state = data.objectiveState(instance.id());
            creditedPlayers.add(playerId);
            if (nextProgress >= state.target()) {
                completeObjectiveForPlayer(server, data, instance, playerId);
                state = data.objectiveState(instance.id());
            }
        }

        if (!creditedPlayers.isEmpty()) {
            broadcastObjectiveProgress(server, data, instance, creditedPlayers);
        }
        updateInstanceResolution(data, instance);
    }

    private static void broadcastObjectiveProgress(
            MinecraftServer server,
            RoguelikeSavedData data,
            DungeonInstanceData instance,
            List<UUID> playerIds
    ) {
        RoguelikeSavedData.ObjectiveStateRecord state = data.objectiveState(instance.id());
        if (state == null) {
            return;
        }

        String hintKey = switch (state.type()) {
            case "signal_emission" -> "incore.roguelike.objective.signal.hint";
            case "scavenger_hunt" -> "incore.roguelike.objective.scavenger.hint";
            default -> "incore.roguelike.objective.essence.hint";
        };

        for (UUID playerId : playerIds) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player == null) {
                continue;
            }

            RoguelikeSavedData.PlayerObjectiveProgress progressState = state.progress(playerId);
            Component progress = Component.translatable("incore.roguelike.objective.progress", progressState.progress(), state.target()).withStyle(ChatFormatting.GOLD);
            Component hint = Component.translatable(hintKey, progressState.progress(), state.target()).withStyle(ChatFormatting.GRAY);
            player.sendSystemMessage(progress);
            player.sendSystemMessage(hint);
        }
    }

    private static void completeObjectiveForPlayer(
            MinecraftServer server,
            RoguelikeSavedData data,
            DungeonInstanceData instance,
            UUID playerId
    ) {
        RoguelikeSavedData.ObjectiveStateRecord state = data.objectiveState(instance.id());
        if (state == null) {
            return;
        }

        RoguelikeSavedData.PlayerObjectiveProgress progress = state.progress(playerId);
        if (progress.resolved()) {
            return;
        }

        data.putObjectiveState(state.withPlayerProgress(
                playerId,
                new RoguelikeSavedData.PlayerObjectiveProgress(state.target(), RoguelikeSavedData.ObjectiveResolution.COMPLETED, progress.activatedPylons())
        ));

        int crateCount = Math.max(1, state.rewardCrates() + extraRewardCrates(instance));
        ItemStack crateStack = new ItemStack(Registration.DUNGEON_COMPLETION_CRATE_ITEM.get());
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player != null) {
            for (int i = 0; i < crateCount; i++) {
                ItemStack reward = crateStack.copy();
                if (!player.addItem(reward)) {
                    player.drop(reward, false);
                }
            }
            player.sendSystemMessage(Component.translatable("incore.roguelike.objective.complete", crateCount).withStyle(ChatFormatting.GREEN));
            DailyTaskEvents.onDungeonCompletion(player);
        }
    }

    private static int extraRewardCrates(DungeonInstanceData instance) {
        int extra = 0;
        for (ResourceLocation modifierId : instance.modifiers()) {
            DungeonModifierData modifier = DungeonModifierManager.MODIFIERS.get(modifierId);
            if (modifier != null) {
                extra += modifier.bonusCrates();
            }
        }
        return Math.max(0, extra);
    }

    private static DungeonInstanceData findInstanceForPos(RoguelikeSavedData data, BlockPos pos) {
        for (DungeonInstanceData instance : data.instances()) {
            if (instance.state() == DungeonInstanceData.State.ACTIVE && instance.containsBlock(pos)) {
                return instance;
            }
        }
        return null;
    }

    private static boolean isChestLike(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(Blocks.CHEST) || state.is(Blocks.TRAPPED_CHEST) || state.is(Blocks.BARREL) || state.is(Blocks.ENDER_CHEST);
    }

    private static boolean isEncounterCompletion(net.minecraft.world.entity.LivingEntity entity, ServerLevel level) {
        String groupId = entity.getPersistentData().getString("incore:encounter_group");
        if (groupId.isBlank()) {
            return false;
        }
        AABB area = new AABB(entity.blockPosition()).inflate(48.0D);
        List<Mob> remaining = level.getEntitiesOfClass(Mob.class, area, mob -> mob.isAlive() && mob != entity && groupId.equals(mob.getPersistentData().getString("incore:encounter_group")));
        return remaining.isEmpty();
    }

    private static boolean isEliteKill(net.minecraft.world.entity.LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        return data.contains("apoth.elite")
                || data.contains("apoth_affix")
                || data.contains("apotheosis:affix")
                || data.contains("apotheosis_affix")
                || entity.getTags().contains("apotheosis_elite");
    }

    private static ServerPlayer killerPlayer(net.minecraft.world.entity.LivingEntity entity) {
        if (entity.getKillCredit() instanceof ServerPlayer player) {
            return player;
        }
        return null;
    }

    private static void giveScavengerToken(ServerPlayer player) {
        ItemStack token = new ItemStack(Registration.DUNGEON_SCAVENGER_TOKEN_ITEM.get());
        if (!player.addItem(token)) {
            player.drop(token, false);
        }
    }

    private static void teleport(ServerPlayer player, ServerLevel level, BlockPos pos) {
        player.teleportTo(level, pos.getX() + 0.5D, pos.getY() + 0.1D, pos.getZ() + 0.5D, player.getYRot(), player.getXRot());
    }

    private static void syncXaeroMapSuppression(MinecraftServer server) {
        Holder<MobEffect> minimapEffect = resolveXaeroEffect(
                ResourceLocation.parse("xaerominimap:no_minimap"),
                ResourceLocation.parse("xaerominimap:no_minimap_ui")
        );
        Holder<MobEffect> worldMapEffect = resolveXaeroEffect(
                ResourceLocation.parse("xaeroworldmap:no_world_map"),
                ResourceLocation.parse("xaeroworldmap:no_worldmap")
        );

        if (minimapEffect == null && worldMapEffect == null) {
            return;
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            boolean shouldSuppress = player.serverLevel().dimension().equals(RoguelikeConstants.DUNGEON_DIMENSION) && !player.isCreative();
            if (shouldSuppress) {
                if (minimapEffect != null) {
                    player.addEffect(new MobEffectInstance(minimapEffect, 220, 0, true, false, false));
                }
                if (worldMapEffect != null) {
                    player.addEffect(new MobEffectInstance(worldMapEffect, 220, 0, true, false, false));
                }
            } else {
                if (minimapEffect != null) {
                    player.removeEffect(minimapEffect);
                }
                if (worldMapEffect != null) {
                    player.removeEffect(worldMapEffect);
                }
            }
        }
    }

    private static boolean tryActivatePylonForPlayer(RoguelikeSavedData data, DungeonInstanceData instance, UUID playerId, BlockPos pos) {
        RoguelikeSavedData.ObjectiveStateRecord state = data.objectiveState(instance.id());
        if (state == null || !state.requiredPylons().contains(pos)) {
            return false;
        }
        RoguelikeSavedData.PlayerObjectiveProgress progress = state.progress(playerId);
        if (progress.resolved() || progress.activatedPylons().contains(pos)) {
            return false;
        }
        Set<BlockPos> activated = new HashSet<>(progress.activatedPylons());
        activated.add(pos.immutable());
        data.putObjectiveState(state.withPlayerProgress(
                playerId,
                new RoguelikeSavedData.PlayerObjectiveProgress(progress.progress(), progress.resolution(), activated)
        ));
        return true;
    }

    private static boolean markChestOpened(RoguelikeSavedData data, DungeonInstanceData instance, BlockPos pos) {
        RoguelikeSavedData.ObjectiveStateRecord state = data.objectiveState(instance.id());
        if (state == null || state.openedChests().contains(pos.asLong())) {
            return false;
        }
        data.putObjectiveState(state.withOpenedChest(pos));
        return true;
    }

    private static boolean isScavengerAltar(RoguelikeSavedData.ObjectiveStateRecord state, BlockPos pos) {
        return state.objectiveAltars().contains(pos);
    }

    private static void ensurePlayerObjectiveProgress(RoguelikeSavedData data, DungeonInstanceData instance, UUID playerId) {
        RoguelikeSavedData.ObjectiveStateRecord state = data.objectiveState(instance.id());
        if (state == null || state.playerProgress().containsKey(playerId)) {
            return;
        }
        data.putObjectiveState(state.withPlayerProgress(playerId, RoguelikeSavedData.PlayerObjectiveProgress.EMPTY));
    }

    private static void markPlayerFailed(RoguelikeSavedData data, DungeonInstanceData instance, UUID playerId) {
        markPlayerResolution(data, instance, playerId, RoguelikeSavedData.ObjectiveResolution.FAILED, false);
    }

    private static void markPlayerAbandonedIfIncomplete(RoguelikeSavedData data, DungeonInstanceData instance, UUID playerId) {
        markPlayerResolution(data, instance, playerId, RoguelikeSavedData.ObjectiveResolution.ABANDONED, true);
    }

    private static void markPlayerResolution(
            RoguelikeSavedData data,
            DungeonInstanceData instance,
            UUID playerId,
            RoguelikeSavedData.ObjectiveResolution resolution,
            boolean ignoreCompleted
    ) {
        RoguelikeSavedData.ObjectiveStateRecord state = data.objectiveState(instance.id());
        if (state == null) {
            return;
        }
        RoguelikeSavedData.PlayerObjectiveProgress progress = state.progress(playerId);
        if (progress.resolution() == RoguelikeSavedData.ObjectiveResolution.COMPLETED && ignoreCompleted) {
            return;
        }
        if (progress.resolved() && progress.resolution() != RoguelikeSavedData.ObjectiveResolution.ACTIVE) {
            if (ignoreCompleted || progress.resolution() == resolution) {
                return;
            }
        }
        data.putObjectiveState(state.withPlayerProgress(
                playerId,
                new RoguelikeSavedData.PlayerObjectiveProgress(progress.progress(), resolution, progress.activatedPylons())
        ));
    }

    private static void applyDeathOutcome(
            RoguelikeSavedData data,
            ServerPlayer player,
            PendingDeathOutcome outcome,
            List<RoguelikeSavedData.ItemStackRecord> capturedInventory
    ) {
        DungeonDeathDifficulty effectiveDifficulty = outcome == null || outcome.difficulty() == null
                ? DungeonDeathDifficulty.SOFTCORE
                : outcome.difficulty();
        switch (effectiveDifficulty) {
            case HARDCORE -> player.sendSystemMessage(Component.translatable("incore.roguelike.death.hardcore").withStyle(ChatFormatting.RED));
            case MEDIUMCORE -> {
                UUID recoveryId = UUID.randomUUID();
                data.putRecoveryStrongbox(new RoguelikeSavedData.RecoveryStrongboxRecord(recoveryId, player.getUUID(), capturedInventory, false));
                data.setPendingRecoveryDelivery(player.getUUID(), recoveryId);
                player.sendSystemMessage(Component.translatable("incore.roguelike.death.mediumcore").withStyle(ChatFormatting.GOLD));
            }
            case SOFTCORE -> {
                data.setPendingInventoryRestore(new RoguelikeSavedData.PendingInventoryRestore(
                        player.getUUID(),
                        outcome == null ? 0 : outcome.selectedSlot(),
                        capturedInventory
                ));
                deductSoftcorePenalty(player);
                player.sendSystemMessage(Component.translatable("incore.roguelike.death.softcore").withStyle(ChatFormatting.YELLOW));
            }
            default -> {
            }
        }
    }

    private static void deductSoftcorePenalty(ServerPlayer player) {
        var account = MarketBanking.resolveManualAccount(player, ItemStack.EMPTY);
        int currentBalance = MarketBanking.balanceSpur(account);
        int penalty = Math.max(0, (int) Math.floor(currentBalance * 0.20D));
        if (penalty > 0) {
            MarketBanking.withdraw(account, penalty);
            player.sendSystemMessage(Component.translatable("incore.roguelike.death.softcore_penalty", penalty).withStyle(ChatFormatting.GRAY));
        }
    }

    private static List<RoguelikeSavedData.ItemStackRecord> captureDropSnapshot(ServerPlayer player, LivingDropsEvent event) {
        List<RoguelikeSavedData.ItemStackRecord> captured = new ArrayList<>();
        for (var itemEntity : event.getDrops()) {
            ItemStack stack = itemEntity.getItem();
            if (!stack.isEmpty()) {
                captured.add(RoguelikeSavedData.ItemStackRecord.of(-1, stack, player.registryAccess()));
            }
        }
        return List.copyOf(captured);
    }

    private static void restoreInventory(ServerPlayer player, RoguelikeSavedData.PendingInventoryRestore restore) {
        for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
            player.getInventory().items.set(slot, ItemStack.EMPTY);
        }
        for (int slot = 0; slot < player.getInventory().armor.size(); slot++) {
            player.getInventory().armor.set(slot, ItemStack.EMPTY);
        }
        for (int slot = 0; slot < player.getInventory().offhand.size(); slot++) {
            player.getInventory().offhand.set(slot, ItemStack.EMPTY);
        }
        for (RoguelikeSavedData.ItemStackRecord record : restore.contents()) {
            ItemStack stack = record.toStack(player.registryAccess());
            if (stack.isEmpty()) {
                continue;
            }
            int slot = record.slot();
            if (slot >= 0 && slot < player.getInventory().items.size()) {
                player.getInventory().items.set(slot, stack);
            } else if (slot >= 100 && slot < 100 + player.getInventory().armor.size()) {
                player.getInventory().armor.set(slot - 100, stack);
            } else if (slot >= 150 && slot < 150 + player.getInventory().offhand.size()) {
                player.getInventory().offhand.set(slot - 150, stack);
            } else if (!player.addItem(stack)) {
                player.drop(stack, false);
            }
        }
        player.getInventory().selected = Math.clamp(restore.selectedSlot(), 0, 8);
        player.containerMenu.broadcastChanges();
    }

    private static void deliverRecoveryItems(ServerPlayer player, UUID recoveryId) {
        MinecraftServer server = player.getServer();
        if (server == null || recoveryId == null) {
            return;
        }

        RoguelikeSavedData data = RoguelikeSavedData.get(server);
        RoguelikeSavedData.RecoveryStrongboxRecord record = data.recoveryStrongbox(recoveryId);
        if (record == null || record.opened()) {
            return;
        }

        ItemStack strongbox = new ItemStack(Registration.LOCKED_RECOVERY_STRONGBOX_BLOCK_ITEM.get());
        strongbox.set(Registration.RECOVERY_STRONGBOX_ID.get(), recoveryId.toString());
        ItemStack key = new ItemStack(Registration.RECOVERY_STRONGBOX_KEY_ITEM.get());
        key.set(Registration.RECOVERY_STRONGBOX_ID.get(), recoveryId.toString());

        if (!player.addItem(strongbox)) {
            player.drop(strongbox, false);
        }
        if (!player.addItem(key)) {
            player.drop(key, false);
        }

        player.sendSystemMessage(Component.translatable("incore.roguelike.recovery.delivered").withStyle(ChatFormatting.GOLD));
    }

    private static void updateInstanceResolution(RoguelikeSavedData data, DungeonInstanceData instance) {
        if (instance.state() != DungeonInstanceData.State.ACTIVE) {
            return;
        }

        RoguelikeSavedData.ObjectiveStateRecord state = data.objectiveState(instance.id());
        if (state == null) {
            checkInstanceEmptyAndCleanup(data, instance);
            return;
        }

        for (UUID playerId : instance.enteredPlayers()) {
            RoguelikeSavedData.PlayerObjectiveProgress progress = state.progress(playerId);
            if (!progress.resolved()) {
                return;
            }
        }

        data.putInstance(instance
                .withState(DungeonInstanceData.State.COMPLETED)
                .withCleanup(DungeonInstanceData.CleanupStage.DENY_ENTRY, DungeonInstanceData.CleanupMode.EVICT));
    }

    private static Holder<MobEffect> resolveXaeroEffect(ResourceLocation... ids) {
        for (ResourceLocation id : ids) {
            Optional<Holder.Reference<MobEffect>> holder = BuiltInRegistries.MOB_EFFECT.getHolder(id);
            if (holder.isPresent()) {
                return holder.get();
            }
        }
        return null;
    }

    private static void syncMinimap(MinecraftServer server, RoguelikeSavedData data, DungeonInstanceData instance) {
        List<RoguelikeSavedData.ActiveRun> runs = data.activeRunsForInstance(instance.id());
        if (runs.isEmpty()) {
            return;
        }

        Map<UUID, Set<Integer>> revealedByPlayer = REVEALED_ROOMS.computeIfAbsent(instance.id().value(), ignored -> new HashMap<>());
        List<RoguelikeMinimapPartyPayload.Marker> markers = new ArrayList<>();

        for (RoguelikeSavedData.ActiveRun run : runs) {
            ServerPlayer player = server.getPlayerList().getPlayer(run.playerId());
            if (player == null || !player.serverLevel().dimension().equals(RoguelikeConstants.DUNGEON_DIMENSION)) {
                continue;
            }

            syncMinimapGraphIfNeeded(player, instance);
            int roomId = roomIdForPosition(instance, player.blockPosition());
            if (roomId >= 0) {
                Set<Integer> revealed = revealedByPlayer.computeIfAbsent(player.getUUID(), ignored -> new HashSet<>());
                if (revealed.add(roomId)) {
                    RoguelikeNetworking.revealRoom(player, instance.id().value(), roomId);
                }
                markers.add(new RoguelikeMinimapPartyPayload.Marker(player.getUUID(), roomId));
            }
        }

        if (markers.isEmpty()) {
            return;
        }

        for (RoguelikeSavedData.ActiveRun run : runs) {
            ServerPlayer player = server.getPlayerList().getPlayer(run.playerId());
            if (player == null || !player.serverLevel().dimension().equals(RoguelikeConstants.DUNGEON_DIMENSION)) {
                continue;
            }
            RoguelikeNetworking.syncParty(player, instance.id().value(), markers);
        }
    }

    private static void syncMinimapGraphIfNeeded(ServerPlayer player, DungeonInstanceData instance) {
        Long lastSynced = LAST_GRAPH_SYNC_INSTANCE.get(player.getUUID());
        if (lastSynced != null && lastSynced == instance.id().value()) {
            return;
        }
        RoguelikeNetworking.syncGraph(player, instance.id().value(), instance.originChunkX(), instance.originChunkZ());
        LAST_GRAPH_SYNC_INSTANCE.put(player.getUUID(), instance.id().value());
    }

    private static int roomIdForPosition(DungeonInstanceData instance, BlockPos pos) {
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        int relativeX = chunkX - instance.originChunkX();
        int relativeZ = chunkZ - instance.originChunkZ();

        if (relativeX == START_CHUNK_OFFSET && relativeZ == START_CHUNK_OFFSET) {
            return MINIMAP_CENTER_CELL + (MINIMAP_CENTER_CELL * MINIMAP_GRID_SIZE);
        }

        for (int cellZ = 0; cellZ < MINIMAP_GRID_SIZE; cellZ++) {
            for (int cellX = 0; cellX < MINIMAP_GRID_SIZE; cellX++) {
                if (cellX == MINIMAP_CENTER_CELL && cellZ == MINIMAP_CENTER_CELL) {
                    continue;
                }
                int minChunkX = cellX * CHUNK_STRIDE;
                int minChunkZ = cellZ * CHUNK_STRIDE;
                int maxChunkX = minChunkX + ROOM_SIZE_CHUNKS - 1;
                int maxChunkZ = minChunkZ + ROOM_SIZE_CHUNKS - 1;
                if (relativeX >= minChunkX && relativeX <= maxChunkX && relativeZ >= minChunkZ && relativeZ <= maxChunkZ) {
                    return cellX + (cellZ * MINIMAP_GRID_SIZE);
                }
            }
        }
        return -1;
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

    private record EncounterModifierProfile(double spawnChance, double mobHealthMultiplier, double mobDamageMultiplier) {
    }

    private record PendingDeathOutcome(DungeonDeathDifficulty difficulty, int selectedSlot) {
    }
}
