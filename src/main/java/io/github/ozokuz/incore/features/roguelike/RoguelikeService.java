package io.github.ozokuz.incore.features.roguelike;

import io.github.ozokuz.incore.Registration;
import io.github.ozokuz.incore.features.roguelike.content.RoguelikeAltarBlockEntity;
import io.github.ozokuz.incore.features.roguelike.content.RoguelikePortalBlockEntity;
import io.github.ozokuz.incore.features.roguelike.data.AltarOfferingData;
import io.github.ozokuz.incore.features.roguelike.data.AltarOfferingManager;
import io.github.ozokuz.incore.features.roguelike.data.DungeonObjectiveData;
import io.github.ozokuz.incore.features.roguelike.data.DungeonObjectiveManager;
import io.github.ozokuz.incore.features.roguelike.data.DungeonThemeData;
import io.github.ozokuz.incore.features.roguelike.data.DungeonThemeManager;
import io.github.ozokuz.incore.features.roguelike.state.RoguelikeSavedData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class RoguelikeService {
    private static final String DUNGEON_ENTITY_TAG = "incore:roguelike_dungeon_id";
    private static final int MIN_ALTAR_ITEM_VARIANTS = 3;
    private static final int MAX_ALTAR_ITEM_VARIANTS = 5;
    private static final int ALTAR_VARIANT_GROWTH_STEP = 10;
    private static final double ALTAR_COLLECTION_RADIUS = 1.8D;
    private static final int DUNGEON_LAYOUT_RADIUS_CELLS = 2;
    private static final int MIN_DUNGEON_ROOM_COUNT = 4;
    private static final int MAX_DUNGEON_ROOM_COUNT = 9;
    private static final int DUNGEON_LAYOUT_ATTEMPTS = 128;
    private static final Direction[] CARDINAL_DIRECTIONS = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
    private static final Map<UUID, ServerBossEvent> OBJECTIVE_BARS = new HashMap<>();

    private static final List<EntityType<? extends Monster>> MOB_POOL = List.of(
            EntityType.ZOMBIE,
            EntityType.SKELETON,
            EntityType.SPIDER,
            EntityType.HUSK,
            EntityType.STRAY
    );

    private RoguelikeService() {
    }

    public static void tickAltar(ServerLevel level, BlockPos altarPos, RoguelikeAltarBlockEntity altar) {
        UUID ownerId = altar.ownerId();
        if (ownerId == null) {
            syncAltarDisplay(List.of(), altar);
            return;
        }

        MinecraftServer server = level.getServer();
        RoguelikeSavedData data = RoguelikeSavedData.get(server);
        ensureAltarRequirement(server, data, ownerId);

        absorbDroppedItems(level, altarPos, data, ownerId);
        syncAltarDisplay(data.altarRequirements(ownerId), altar);
    }

    public static boolean tryFinalizeAltar(Player player, InteractionHand hand, BlockPos altarPos) {
        if (!(player instanceof ServerPlayer serverPlayer) || serverPlayer.getServer() == null) {
            return false;
        }

        UUID ownerId = resolveAltarOwner(serverPlayer, altarPos);
        if (ownerId == null) {
            return false;
        }

        MinecraftServer server = serverPlayer.getServer();
        RoguelikeSavedData data = RoguelikeSavedData.get(server);
        ensureAltarRequirement(server, data, ownerId);

        ItemStack held = serverPlayer.getItemInHand(hand);
        if (!held.is(Registration.EMPTY_DUNGEON_CRYSTAL_ITEM.get())) {
            showAltarRequirement(serverPlayer, altarPos);
            serverPlayer.sendSystemMessage(Component.translatable("incore.roguelike.altar.need_empty_crystal"));
            return false;
        }

        if (!data.isAltarComplete(ownerId)) {
            showAltarRequirement(serverPlayer, altarPos);
            serverPlayer.sendSystemMessage(Component.translatable("incore.roguelike.altar.not_ready"));
            return false;
        }

        if (!serverPlayer.isCreative()) {
            held.shrink(1);
        }

        ItemStack crystal = new ItemStack(Registration.DUNGEON_CRYSTAL_ITEM.get());
        if (!serverPlayer.addItem(crystal)) {
            serverPlayer.drop(crystal, false);
        }

        data.incrementCrystalsCrafted(ownerId);
        chooseNextAltarRequirement(server, data, ownerId);

        serverPlayer.sendSystemMessage(Component.translatable("incore.roguelike.altar.created_crystal").withStyle(ChatFormatting.AQUA));
        showAltarRequirement(serverPlayer, altarPos);

        BlockEntity blockEntity = serverPlayer.serverLevel().getBlockEntity(altarPos);
        if (blockEntity instanceof RoguelikeAltarBlockEntity altar) {
            syncAltarDisplay(data.altarRequirements(ownerId), altar);
        }

        return true;
    }

    public static void showAltarRequirement(Player player, BlockPos altarPos) {
        if (!(player instanceof ServerPlayer serverPlayer) || serverPlayer.getServer() == null) {
            return;
        }

        UUID ownerId = altarPos == null
                ? serverPlayer.getUUID()
                : resolveAltarOwner(serverPlayer, altarPos);
        if (ownerId == null) {
            return;
        }

        MinecraftServer server = serverPlayer.getServer();
        RoguelikeSavedData data = RoguelikeSavedData.get(server);
        ensureAltarRequirement(server, data, ownerId);

        List<RoguelikeSavedData.AltarRequirement> requirements = data.altarRequirements(ownerId);
        if (requirements.size() < MIN_ALTAR_ITEM_VARIANTS) {
            serverPlayer.sendSystemMessage(Component.translatable("incore.roguelike.altar.no_offerings"));
            return;
        }

        serverPlayer.sendSystemMessage(Component.translatable("incore.roguelike.altar.requirement.header"));

        for (RoguelikeSavedData.AltarRequirement requirement : requirements) {
            AltarOfferingData offering = AltarOfferingManager.OFFERINGS.get(requirement.offeringId());
            if (offering == null) {
                continue;
            }

            serverPlayer.sendSystemMessage(
                    Component.translatable(
                                    "incore.roguelike.altar.requirement.item",
                                    offering.item().getDescription(),
                                    requirement.submittedAmount(),
                                    requirement.requiredAmount()
                            )
                            .withStyle(requirement.isComplete() ? ChatFormatting.GREEN : ChatFormatting.WHITE)
            );
        }

        if (data.isAltarComplete(ownerId)) {
            serverPlayer.sendSystemMessage(Component.translatable("incore.roguelike.altar.ready"));
        } else {
            serverPlayer.sendSystemMessage(Component.translatable("incore.roguelike.altar.drop_items_hint"));
        }
    }

    private static UUID resolveAltarOwner(ServerPlayer player, BlockPos altarPos) {
        BlockEntity blockEntity = player.serverLevel().getBlockEntity(altarPos);
        if (!(blockEntity instanceof RoguelikeAltarBlockEntity altar)) {
            return null;
        }

        UUID ownerId = altar.ownerId();
        if (ownerId != null) {
            return ownerId;
        }

        ownerId = player.getUUID();
        altar.setOwner(ownerId);
        return ownerId;
    }

    public static int getAltarDifficulty(MinecraftServer server, UUID ownerId) {
        return RoguelikeSavedData.get(server).crystalsCrafted(ownerId);
    }

    public static void setAltarDifficulty(MinecraftServer server, UUID ownerId, int value) {
        RoguelikeSavedData data = RoguelikeSavedData.get(server);
        data.setCrystalsCrafted(ownerId, value);
        chooseNextAltarRequirement(server, data, ownerId);
    }

    public static ItemStack createDungeonCrystal(int count, ResourceLocation themeId, ResourceLocation objectiveId) {
        ItemStack stack = new ItemStack(Registration.DUNGEON_CRYSTAL_ITEM.get(), Math.max(1, count));
        if (themeId == null && objectiveId == null) {
            return stack;
        }

        if (themeId != null) {
            stack.set(Registration.DUNGEON_CRYSTAL_THEME.get(), themeId);
        }
        if (objectiveId != null) {
            stack.set(Registration.DUNGEON_CRYSTAL_OBJECTIVE.get(), objectiveId);
        }

        return stack;
    }

    public static boolean onPortalInteracted(Player player, InteractionHand hand, RoguelikePortalBlockEntity portal, BlockPos portalPos) {
        return tryEnterPortal(player, portal, portalPos);
    }

    public static boolean tryActivatePortalFromFrame(Player player, InteractionHand hand, BlockPos clickedPos, Direction clickedFace) {
        if (!(player instanceof ServerPlayer serverPlayer) || serverPlayer.getServer() == null) {
            return false;
        }

        ItemStack crystalStack = serverPlayer.getItemInHand(hand);
        if (!crystalStack.is(Registration.DUNGEON_CRYSTAL_ITEM.get())) {
            return false;
        }

        ServerLevel level = serverPlayer.serverLevel();
        Optional<RoguelikePortalShape> shape = findFrameShape(level, clickedPos, clickedFace);
        if (shape.isEmpty()) {
            serverPlayer.sendSystemMessage(Component.translatable("incore.roguelike.portal.frame_invalid"));
            return false;
        }

        return tryActivatePortal(serverPlayer, shape.get(), crystalStack);
    }

    private static Optional<RoguelikePortalShape> findFrameShape(ServerLevel level, BlockPos clickedPos, Direction clickedFace) {
        BlockPos[] seeds = new BlockPos[]{
                clickedPos,
                clickedPos.relative(clickedFace),
                clickedPos.relative(clickedFace.getOpposite())
        };

        Set<BlockPos> checked = new HashSet<>();
        for (BlockPos seed : seeds) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dy = -3; dy <= 2; dy++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        BlockPos candidate = seed.offset(dx, dy, dz);
                        if (!checked.add(candidate)) {
                            continue;
                        }

                        Optional<RoguelikePortalShape> shape = RoguelikePortalShape.findEmptyPortalShape(level, candidate);
                        if (shape.isPresent()) {
                            return shape;
                        }
                    }
                }
            }
        }

        return Optional.empty();
    }

    public static boolean tryEnterPortal(Player player, RoguelikePortalBlockEntity portal, BlockPos portalPos) {
        if (!(player instanceof ServerPlayer serverPlayer) || serverPlayer.getServer() == null) {
            return false;
        }

        if (!portal.isActivated()) {
            serverPlayer.sendSystemMessage(Component.translatable("incore.roguelike.portal.not_active"));
            return false;
        }

        MinecraftServer server = serverPlayer.getServer();
        RoguelikeSavedData data = RoguelikeSavedData.get(server);
        RoguelikeSavedData.DungeonRecord dungeon = data.getDungeon(portal.dungeonId());
        if (dungeon == null || dungeon.state() != RoguelikeSavedData.State.ACTIVE) {
            clearConnectedPortalBlocks(serverPlayer.serverLevel(), portalPos, portal.dungeonId());
            serverPlayer.sendSystemMessage(Component.translatable("incore.roguelike.portal.no_dungeon"));
            return false;
        }

        UUID playerId = serverPlayer.getUUID();
        Optional<RoguelikeSavedData.ActiveRun> activeRun = data.getRun(playerId);
        if (serverPlayer.serverLevel().dimension().equals(RoguelikeConstants.ROGUELIKE_DIMENSION)) {
            if (activeRun.isPresent() && activeRun.get().dungeonId() == dungeon.dungeonId()) {
                return tryReturnFromDungeon(serverPlayer, data, dungeon, activeRun.get());
            }

            serverPlayer.sendSystemMessage(Component.translatable("incore.roguelike.return_portal.unbound"));
            serverPlayer.setPortalCooldown();
            return false;
        }

        RoguelikeSavedData.PlayerProgress progress = dungeon.progressFor(playerId);
        if (progress.entered()) {
            serverPlayer.sendSystemMessage(Component.translatable("incore.roguelike.portal.already_entered"));
            serverPlayer.setPortalCooldown();
            return false;
        }

        ServerLevel roguelikeLevel = server.getLevel(RoguelikeConstants.ROGUELIKE_DIMENSION);
        if (roguelikeLevel == null) {
            serverPlayer.sendSystemMessage(Component.translatable("incore.roguelike.portal.dimension_missing"));
            return false;
        }

        long gameTime = roguelikeLevel.getGameTime();
        if (dungeon.expiresAtGameTime() <= 0L) {
            dungeon = dungeon.withExpiry(gameTime + RoguelikeConstants.DUNGEON_TIME_LIMIT_TICKS);
            data.putDungeon(dungeon);
        }

        data.startRun(playerId, dungeon.dungeonId(), serverPlayer.serverLevel().dimension(), portalPos);
        dungeon = dungeon.upsertProgress(playerId, progress.withEntered());
        data.putDungeon(dungeon);

        BlockPos entry = dungeonEntryPos(dungeon);
        teleport(serverPlayer, roguelikeLevel, entry);

        DungeonObjectiveData objective = DungeonObjectiveManager.OBJECTIVES.get(dungeon.objectiveId());
        if (objective != null) {
            var objectiveProgress = dungeon.progressFor(playerId);
            updateObjectiveBar(serverPlayer, dungeon.objectiveId(), objective, objectiveProgress);
            serverPlayer.sendSystemMessage(Component.translatable(
                    "incore.roguelike.portal.objective",
                    objectiveDisplayName(dungeon.objectiveId()),
                    objectiveProgress.kills(),
                    objective.target()
            ).withStyle(ChatFormatting.GOLD));
        }

        return true;
    }

    private static boolean tryReturnFromDungeon(ServerPlayer player, RoguelikeSavedData data, RoguelikeSavedData.DungeonRecord dungeon, RoguelikeSavedData.ActiveRun run) {
        if (player.getServer() == null) {
            return false;
        }

        MinecraftServer server = player.getServer();
        ServerLevel returnLevel = server.getLevel(run.returnDimensionKey());
        if (returnLevel == null) {
            player.sendSystemMessage(Component.translatable("incore.roguelike.portal.dimension_missing"));
            return false;
        }

        UUID playerId = player.getUUID();
        RoguelikeSavedData.PlayerProgress progress = dungeon.progressFor(playerId).withEntered().withDeath();
        dungeon = dungeon.upsertProgress(playerId, progress);
        data.putDungeon(dungeon);

        data.clearRun(playerId);
        removeObjectiveBar(playerId);
        failDungeonIfAllEntrantsFailed(server, data, run.dungeonId());

        teleport(player, returnLevel, run.returnPos());
        player.sendSystemMessage(Component.translatable("incore.roguelike.return_portal.used"));
        return true;
    }

    public static void onServerTick(MinecraftServer server) {
        if (server.getTickCount() % 20 != 0) {
            return;
        }

        RoguelikeSavedData data = RoguelikeSavedData.get(server);
        syncObjectiveBars(server, data);
        ServerLevel roguelikeLevel = server.getLevel(RoguelikeConstants.ROGUELIKE_DIMENSION);
        if (roguelikeLevel == null) {
            return;
        }

        long now = roguelikeLevel.getGameTime();
        List<RoguelikeSavedData.DungeonRecord> snapshot = new ArrayList<>(data.dungeons());
        for (RoguelikeSavedData.DungeonRecord dungeon : snapshot) {
            if (dungeon.state() != RoguelikeSavedData.State.ACTIVE || dungeon.expiresAtGameTime() <= 0L) {
                continue;
            }

            long remaining = dungeon.expiresAtGameTime() - now;
            if (remaining <= 0L) {
                expireDungeon(server, data, dungeon);
                continue;
            }

            sendTimerToDungeonPlayers(server, data, dungeon, remaining);
        }
    }

    public static void onPlayerDeath(ServerPlayer player) {
        if (player.getServer() == null) {
            return;
        }

        removeObjectiveBar(player.getUUID());
        RoguelikeSavedData data = RoguelikeSavedData.get(player.getServer());
        data.getRun(player.getUUID()).ifPresent(run -> {
            RoguelikeSavedData.DungeonRecord dungeon = data.getDungeon(run.dungeonId());
            if (dungeon != null) {
                var progress = dungeon.progressFor(player.getUUID()).withEntered().withDeath();
                dungeon = dungeon.upsertProgress(player.getUUID(), progress);
                data.putDungeon(dungeon);
            }

            data.setPendingReturn(player.getUUID(), run.returnDimensionKey(), run.returnPos());
            data.clearRun(player.getUUID());
            failDungeonIfAllEntrantsFailed(player.getServer(), data, run.dungeonId());
        });
    }

    public static void onPlayerRespawn(ServerPlayer player) {
        if (player.getServer() == null) {
            return;
        }

        removeObjectiveBar(player.getUUID());
        RoguelikeSavedData data = RoguelikeSavedData.get(player.getServer());
        data.takePendingReturn(player.getUUID()).ifPresent(target -> {
            ServerLevel level = player.getServer().getLevel(target.dimensionKey());
            if (level == null) {
                return;
            }

            teleport(player, level, target.pos());
        });
    }

    public static void onPlayerLogin(ServerPlayer player) {
        onPlayerRespawn(player);
        if (player.getServer() == null) {
            return;
        }

        RoguelikeSavedData data = RoguelikeSavedData.get(player.getServer());
        data.getRun(player.getUUID()).ifPresentOrElse(run -> {
            RoguelikeSavedData.DungeonRecord dungeon = data.getDungeon(run.dungeonId());
            if (dungeon == null || dungeon.state() != RoguelikeSavedData.State.ACTIVE) {
                removeObjectiveBar(player.getUUID());
                return;
            }

            DungeonObjectiveData objective = DungeonObjectiveManager.OBJECTIVES.get(dungeon.objectiveId());
            if (objective == null) {
                removeObjectiveBar(player.getUUID());
                return;
            }

            updateObjectiveBar(player, dungeon.objectiveId(), objective, dungeon.progressFor(player.getUUID()));
        }, () -> removeObjectiveBar(player.getUUID()));
    }

    public static void onPlayerLogout(ServerPlayer player) {
        removeObjectiveBar(player.getUUID());
    }

    public static void onDungeonMobDeath(LivingEntity killedEntity) {
        if (killedEntity.level().isClientSide() || killedEntity.getServer() == null) {
            return;
        }

        long dungeonId = killedEntity.getPersistentData().getLong(DUNGEON_ENTITY_TAG);
        if (dungeonId <= 0L) {
            return;
        }

        MinecraftServer server = killedEntity.getServer();
        RoguelikeSavedData data = RoguelikeSavedData.get(server);
        RoguelikeSavedData.DungeonRecord dungeon = data.getDungeon(dungeonId);
        if (dungeon == null || dungeon.state() != RoguelikeSavedData.State.ACTIVE) {
            return;
        }

        DungeonObjectiveData objective = DungeonObjectiveManager.OBJECTIVES.get(dungeon.objectiveId());
        if (objective == null || !objective.isKillObjective()) {
            return;
        }

        Set<UUID> activePlayers = data.activePlayersInDungeon(dungeonId);
        if (activePlayers.isEmpty()) {
            return;
        }

        for (UUID playerId : activePlayers) {
            RoguelikeSavedData.PlayerProgress progress = dungeon.progressFor(playerId).withEntered().addKills(1);
            dungeon = dungeon.upsertProgress(playerId, progress);

            ServerPlayer activePlayer = server.getPlayerList().getPlayer(playerId);
            if (activePlayer != null) {
                updateObjectiveBar(activePlayer, dungeon.objectiveId(), objective, progress);
                activePlayer.sendSystemMessage(Component.translatable(
                        "incore.roguelike.portal.objective_progress",
                        progress.kills(),
                        objective.target()
                ));
            }
        }

        boolean completed = false;
        for (UUID playerId : activePlayers) {
            RoguelikeSavedData.PlayerProgress progress = dungeon.progressFor(playerId);
            if (progress.kills() < objective.target() || progress.rewarded()) {
                continue;
            }

            int rewardCount = objective.rewardCrates();
            if (progress.died()) {
                rewardCount = Math.max(1, rewardCount / 2);
            }

            ServerPlayer activePlayer = server.getPlayerList().getPlayer(playerId);
            if (activePlayer != null) {
                ItemStack reward = new ItemStack(Registration.DUNGEON_COMPLETION_CRATE_ITEM.get(), rewardCount);
                if (!activePlayer.addItem(reward)) {
                    activePlayer.drop(reward, false);
                }
                activePlayer.sendSystemMessage(Component.translatable("incore.roguelike.portal.completed", rewardCount));
            }

            dungeon = dungeon.upsertProgress(playerId, progress.withRewarded());
            completed = true;
        }

        data.putDungeon(dungeon);
        if (!completed) {
            return;
        }

        RoguelikeSavedData.DungeonRecord completedDungeon = dungeon.withState(RoguelikeSavedData.State.COMPLETED);
        data.putDungeon(completedDungeon);
        closeDungeon(server, data, completedDungeon, false);
    }

    private static boolean tryActivatePortal(ServerPlayer player, RoguelikePortalShape portalShape, ItemStack crystalStack) {
        if (player.getServer() == null) {
            return false;
        }

        MinecraftServer server = player.getServer();
        ServerLevel roguelikeLevel = server.getLevel(RoguelikeConstants.ROGUELIKE_DIMENSION);
        if (roguelikeLevel == null) {
            player.sendSystemMessage(Component.translatable("incore.roguelike.portal.dimension_missing"));
            return false;
        }

        RandomSource random = roguelikeLevel.random;
        Optional<DungeonThemeManager.PickedTheme> themePick = pickThemeForCrystal(player, crystalStack, random);
        Optional<DungeonObjectiveManager.PickedObjective> objectivePick = pickObjectiveForCrystal(player, crystalStack, random);
        if (themePick.isEmpty() || objectivePick.isEmpty()) {
            if (!themePick.isEmpty() || !objectivePick.isEmpty()) {
                return false;
            }
            player.sendSystemMessage(Component.translatable("incore.roguelike.portal.data_missing"));
            return false;
        }

        RoguelikeSavedData data = RoguelikeSavedData.get(server);
        Optional<ResourceLocation> missingStructure = firstMissingStructureTemplate(roguelikeLevel, themePick.get().data());
        if (missingStructure.isPresent()) {
            player.sendSystemMessage(Component.translatable("incore.roguelike.portal.missing_structure", missingStructure.get().toString()));
            return false;
        }

        int slotIndex;
        BlockPos origin;
        RoguelikeSavedData.DungeonRecord reusable = data.dungeons().stream()
                .filter(RoguelikeSavedData.DungeonRecord::isRecyclable)
                .filter(record -> !data.hasActiveRunInDungeon(record.dungeonId()))
                .findFirst()
                .orElse(null);

        if (reusable != null) {
            slotIndex = reusable.slotIndex();
            origin = reusable.origin();
            deactivatePortal(server, reusable);
            data.removeDungeon(reusable.dungeonId());
        } else {
            slotIndex = data.nextSlotIndex();
            origin = RoguelikeSavedData.slotOrigin(slotIndex);
        }

        long dungeonId = data.nextDungeonId();
        RoguelikeSavedData.DungeonRecord dungeon = RoguelikeSavedData.DungeonRecord.create(
                dungeonId,
                slotIndex,
                themePick.get().id(),
                objectivePick.get().id(),
                origin,
                player.serverLevel().dimension(),
                portalShape.bottomLeft(),
                0L
        );

        if (!generateDungeon(roguelikeLevel, dungeon, themePick.get().data(), objectivePick.get().data())) {
            player.sendSystemMessage(Component.translatable("incore.roguelike.portal.generation_failed"));
            return false;
        }
        data.putDungeon(dungeon);
        portalShape.createPortalBlocks();
        portalShape.forEachPortalBlock(portalPos -> {
            BlockEntity blockEntity = player.serverLevel().getBlockEntity(portalPos);
            if (blockEntity instanceof RoguelikePortalBlockEntity portal) {
                portal.setDungeonId(dungeonId);
                portal.setChanged();
            }
        });

        if (!player.isCreative()) {
            crystalStack.shrink(1);
        }

        player.sendSystemMessage(Component.translatable(
                "incore.roguelike.portal.activated",
                themeDisplayName(themePick.get().id()),
                objectiveDisplayName(objectivePick.get().id())
        ));
        return true;
    }

    private static Optional<DungeonThemeManager.PickedTheme> pickThemeForCrystal(ServerPlayer player, ItemStack crystalStack, RandomSource random) {
        ResourceLocation customThemeId = crystalStack.get(Registration.DUNGEON_CRYSTAL_THEME.get());
        if (customThemeId == null) {
            return DungeonThemeManager.pickRandom(random);
        }

        DungeonThemeData themeData = DungeonThemeManager.THEMES.get(customThemeId);
        if (themeData == null) {
            player.sendSystemMessage(Component.translatable("incore.roguelike.portal.invalid_theme", customThemeId.toString()));
            return Optional.empty();
        }

        return Optional.of(new DungeonThemeManager.PickedTheme(customThemeId, themeData));
    }

    private static Optional<DungeonObjectiveManager.PickedObjective> pickObjectiveForCrystal(ServerPlayer player, ItemStack crystalStack, RandomSource random) {
        ResourceLocation customObjectiveId = crystalStack.get(Registration.DUNGEON_CRYSTAL_OBJECTIVE.get());
        if (customObjectiveId == null) {
            return DungeonObjectiveManager.pickRandom(random);
        }

        DungeonObjectiveData objectiveData = DungeonObjectiveManager.OBJECTIVES.get(customObjectiveId);
        if (objectiveData == null) {
            player.sendSystemMessage(Component.translatable("incore.roguelike.portal.invalid_objective", customObjectiveId.toString()));
            return Optional.empty();
        }

        return Optional.of(new DungeonObjectiveManager.PickedObjective(customObjectiveId, objectiveData));
    }

    private static boolean generateDungeon(ServerLevel level, RoguelikeSavedData.DungeonRecord dungeon, DungeonThemeData theme, DungeonObjectiveData objective) {
        BlockPos anchor = dungeon.origin();
        clearDungeonArea(level, anchor);

        DungeonLayout layout = generateDungeonLayout(level.random, objective.target());
        Map<GridCell, DungeonThemeData.StructureRef> roomStructures = new HashMap<>();
        roomStructures.put(GridCell.START, theme.startingRoomStructure());
        for (GridCell room : layout.rooms()) {
            if (room.equals(GridCell.START)) {
                continue;
            }
            roomStructures.put(room, theme.pickRandomRoom(level.random).structure());
        }

        Map<ResourceLocation, BlockPos> structureSizes = new HashMap<>();
        for (DungeonThemeData.StructureRef ref : roomStructures.values()) {
            BlockPos size = structureSize(level, ref.id());
            if (size == null) {
                return false;
            }
            structureSizes.put(ref.id(), size);
        }

        BlockPos hallwayNsSize = structureSize(level, theme.hallwayNorthSouthStructure().id());
        if (hallwayNsSize == null) {
            return false;
        }
        BlockPos hallwayEwSize = structureSize(level, theme.hallwayEastWestStructure().id());
        if (hallwayEwSize == null) {
            return false;
        }
        structureSizes.put(theme.hallwayNorthSouthStructure().id(), hallwayNsSize);
        structureSizes.put(theme.hallwayEastWestStructure().id(), hallwayEwSize);

        Map<GridCell, RoomPlacement> placedRooms = new HashMap<>();
        List<HallPlacement> placedHallways = new ArrayList<>();

        int baseFloorY = anchor.getY();
        placedRooms.put(GridCell.START, new RoomPlacement(anchor.getX(), anchor.getZ(), roomStructures.get(GridCell.START), structureSizes.get(roomStructures.get(GridCell.START).id())));
        ArrayDeque<GridCell> queue = new ArrayDeque<>();
        queue.add(GridCell.START);

        while (!queue.isEmpty()) {
            GridCell current = queue.removeFirst();
            RoomPlacement currentPlacement = placedRooms.get(current);
            if (currentPlacement == null) {
                continue;
            }

            for (Direction direction : CARDINAL_DIRECTIONS) {
                GridCell hallwayCell = current.step(direction, 1);
                GridCell nextRoomCell = current.step(direction, 2);
                if (!layout.hallways().contains(hallwayCell) || !layout.rooms().contains(nextRoomCell)) {
                    continue;
                }
                if (placedRooms.containsKey(nextRoomCell)) {
                    continue;
                }

                DungeonThemeData.StructureRef nextRef = roomStructures.get(nextRoomCell);
                if (nextRef == null) {
                    return false;
                }
                BlockPos nextSize = structureSizes.get(nextRef.id());
                if (nextSize == null) {
                    return false;
                }

                DungeonThemeData.StructureRef hallwayRef = (direction == Direction.NORTH || direction == Direction.SOUTH)
                        ? theme.hallwayNorthSouthStructure()
                        : theme.hallwayEastWestStructure();
                BlockPos hallwaySize = structureSizes.get(hallwayRef.id());
                if (hallwaySize == null) {
                    return false;
                }

                int currentHalf = halfAlongDirection(currentPlacement.size(), direction);
                int hallwayHalf = halfAlongDirection(hallwaySize, direction);
                int hallwayLength = lengthAlongDirection(hallwaySize, direction);
                int nextHalf = halfAlongDirection(nextSize, direction);

                int centerX = currentPlacement.centerX() + direction.getStepX() * (currentHalf + hallwayLength + nextHalf);
                int centerZ = currentPlacement.centerZ() + direction.getStepZ() * (currentHalf + hallwayLength + nextHalf);
                RoomPlacement nextPlacement = new RoomPlacement(centerX, centerZ, nextRef, nextSize);
                placedRooms.put(nextRoomCell, nextPlacement);
                queue.add(nextRoomCell);

                int hallwayCenterX = currentPlacement.centerX() + direction.getStepX() * (currentHalf + hallwayHalf);
                int hallwayCenterZ = currentPlacement.centerZ() + direction.getStepZ() * (currentHalf + hallwayHalf);
                placedHallways.add(new HallPlacement(hallwayCenterX, hallwayCenterZ, hallwayRef, hallwaySize));
            }
        }

        if (placedRooms.size() != layout.rooms().size()) {
            return false;
        }

        List<RoomArea> roomAreas = new ArrayList<>(placedRooms.size());
        for (RoomPlacement room : placedRooms.values()) {
            BlockPos roomOrigin = originFromCenter(room.centerX(), room.centerZ(), room.size(), baseFloorY - room.ref().floorYFromBottom());
            if (!placeStructureTemplate(level, room.ref().id(), roomOrigin, dungeon.dungeonId())) {
                return false;
            }
            roomAreas.add(new RoomArea(roomOrigin, room.size(), baseFloorY));
        }

        for (HallPlacement hall : placedHallways) {
            BlockPos hallwayOrigin = originFromCenter(hall.centerX(), hall.centerZ(), hall.size(), baseFloorY - hall.ref().floorYFromBottom());
            if (!placeStructureTemplate(level, hall.ref().id(), hallwayOrigin, dungeon.dungeonId())) {
                return false;
            }
        }

        BlockPos entry = new BlockPos(anchor.getX(), baseFloorY + 1, anchor.getZ());
        clearEntrySpace(level, entry);

        int spawnCount = Math.min(256, Math.max(24, objective.target() + 8));
        for (int i = 0; i < spawnCount; i++) {
            spawnDungeonMob(level, dungeon.dungeonId(), roomAreas);
        }

        return true;
    }

    private static DungeonLayout generateDungeonLayout(RandomSource random, int objectiveTarget) {
        int desiredRooms = Math.max(MIN_DUNGEON_ROOM_COUNT, Math.min(MAX_DUNGEON_ROOM_COUNT, 1 + (objectiveTarget / 6)));
        Set<GridCell> rooms = new HashSet<>();
        Set<GridCell> hallways = new HashSet<>();
        List<GridCell> frontier = new ArrayList<>();

        rooms.add(GridCell.START);
        frontier.add(GridCell.START);

        int attempts = 0;
        while (rooms.size() < desiredRooms && attempts < DUNGEON_LAYOUT_ATTEMPTS) {
            attempts++;

            GridCell from = frontier.get(random.nextInt(frontier.size()));
            Direction direction = CARDINAL_DIRECTIONS[random.nextInt(CARDINAL_DIRECTIONS.length)];
            GridCell hallway = from.step(direction, 1);
            GridCell nextRoom = from.step(direction, 2);
            if (!isInsideLayout(nextRoom)) {
                continue;
            }

            if (rooms.add(nextRoom)) {
                hallways.add(hallway);
                frontier.add(nextRoom);
            }
        }

        if (hallways.isEmpty()) {
            Direction direction = CARDINAL_DIRECTIONS[random.nextInt(CARDINAL_DIRECTIONS.length)];
            GridCell hallway = GridCell.START.step(direction, 1);
            GridCell nextRoom = GridCell.START.step(direction, 2);
            if (isInsideLayout(nextRoom)) {
                rooms.add(nextRoom);
                hallways.add(hallway);
            }
        }

        return new DungeonLayout(Set.copyOf(rooms), Set.copyOf(hallways));
    }

    private static boolean isInsideLayout(GridCell roomCell) {
        return Math.abs(roomCell.x()) <= DUNGEON_LAYOUT_RADIUS_CELLS && Math.abs(roomCell.z()) <= DUNGEON_LAYOUT_RADIUS_CELLS;
    }

    private static int lengthAlongDirection(BlockPos size, Direction direction) {
        return direction.getAxis() == Direction.Axis.X ? size.getX() : size.getZ();
    }

    private static int halfAlongDirection(BlockPos size, Direction direction) {
        return lengthAlongDirection(size, direction) / 2;
    }

    private static BlockPos originFromCenter(int centerX, int centerZ, BlockPos size, int originY) {
        int x = centerX - (size.getX() / 2);
        int z = centerZ - (size.getZ() / 2);
        return new BlockPos(x, originY, z);
    }

    private static BlockPos structureSize(ServerLevel level, ResourceLocation structureId) {
        Optional<StructureTemplate> template = level.getStructureManager().get(structureId);
        if (template.isEmpty()) {
            return null;
        }

        return new BlockPos(template.get().getSize().getX(), template.get().getSize().getY(), template.get().getSize().getZ());
    }

    private static void spawnDungeonMob(ServerLevel level, long dungeonId, List<RoomArea> roomAreas) {
        if (roomAreas.isEmpty()) {
            return;
        }

        EntityType<? extends Monster> type = MOB_POOL.get(level.random.nextInt(MOB_POOL.size()));
        Mob mob = type.create(level);
        if (mob == null) {
            return;
        }

        for (int attempt = 0; attempt < 8; attempt++) {
            RoomArea room = roomAreas.get(level.random.nextInt(roomAreas.size()));
            BlockPos roomOrigin = room.origin();
            BlockPos size = room.size();
            int innerMinX = roomOrigin.getX() + 2;
            int innerMaxX = roomOrigin.getX() + size.getX() - 3;
            int innerMinZ = roomOrigin.getZ() + 2;
            int innerMaxZ = roomOrigin.getZ() + size.getZ() - 3;

            double x = innerMinX + level.random.nextDouble() * Math.max(1, innerMaxX - innerMinX);
            double z = innerMinZ + level.random.nextDouble() * Math.max(1, innerMaxZ - innerMinZ);
            BlockPos spawnPos = new BlockPos((int) Math.floor(x), room.floorY() + 1, (int) Math.floor(z));
            int topY = roomOrigin.getY() + size.getY() - 2;
            while (spawnPos.getY() < topY && (!level.getBlockState(spawnPos).isAir() || !level.getBlockState(spawnPos.above()).isAir())) {
                spawnPos = spawnPos.above();
            }

            if (!level.getBlockState(spawnPos).isAir() || !level.getBlockState(spawnPos.above()).isAir()) {
                continue;
            }

            mob.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, level.random.nextFloat() * 360F, 0.0F);
            mob.getPersistentData().putLong(DUNGEON_ENTITY_TAG, dungeonId);
            level.addFreshEntity(mob);
            return;
        }

        RoomArea fallbackRoom = roomAreas.get(level.random.nextInt(roomAreas.size()));
        mob.moveTo(
                fallbackRoom.origin().getX() + (fallbackRoom.size().getX() / 2.0D),
                fallbackRoom.floorY() + 1,
                fallbackRoom.origin().getZ() + (fallbackRoom.size().getZ() / 2.0D),
                level.random.nextFloat() * 360F,
                0.0F
        );
        mob.getPersistentData().putLong(DUNGEON_ENTITY_TAG, dungeonId);
        level.addFreshEntity(mob);
    }

    private static boolean placeStructureTemplate(ServerLevel level, ResourceLocation templateId, BlockPos pos, long dungeonId) {
        Optional<StructureTemplate> template = level.getStructureManager().get(templateId);
        if (template.isEmpty()) {
            return false;
        }

        StructurePlaceSettings settings = new StructurePlaceSettings().setIgnoreEntities(true);
        StructureTemplate value = template.get();
        if (!value.placeInWorld(level, pos, pos, settings, level.random, Block.UPDATE_ALL)) {
            return false;
        }

        replaceReturnPortalPlaceholders(level, value, pos, dungeonId);
        return true;
    }

    private static void replaceReturnPortalPlaceholders(ServerLevel level, StructureTemplate template, BlockPos origin, long dungeonId) {
        BlockPos size = new BlockPos(template.getSize().getX(), template.getSize().getY(), template.getSize().getZ());
        int maxX = origin.getX() + size.getX() - 1;
        int maxY = origin.getY() + size.getY() - 1;
        int maxZ = origin.getZ() + size.getZ() - 1;

        for (int x = origin.getX(); x <= maxX; x++) {
            for (int y = origin.getY(); y <= maxY; y++) {
                for (int z = origin.getZ(); z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!level.getBlockState(pos).is(Registration.DUNGEON_RETURN_PORTAL_BLOCK.get())) {
                        continue;
                    }

                    level.setBlockAndUpdate(pos, Registration.ROGUELIKE_PORTAL_BLOCK.get().defaultBlockState());
                    BlockEntity blockEntity = level.getBlockEntity(pos);
                    if (blockEntity instanceof RoguelikePortalBlockEntity portal) {
                        portal.setDungeonId(dungeonId);
                        portal.setChanged();
                    }
                }
            }
        }
    }

    private static Optional<ResourceLocation> firstMissingStructureTemplate(ServerLevel level, DungeonThemeData theme) {
        if (level.getStructureManager().get(theme.startingRoomStructure().id()).isEmpty()) {
            return Optional.of(theme.startingRoomStructure().id());
        }
        if (level.getStructureManager().get(theme.hallwayNorthSouthStructure().id()).isEmpty()) {
            return Optional.of(theme.hallwayNorthSouthStructure().id());
        }
        if (level.getStructureManager().get(theme.hallwayEastWestStructure().id()).isEmpty()) {
            return Optional.of(theme.hallwayEastWestStructure().id());
        }

        for (DungeonThemeData.RoomStructure room : theme.roomStructures()) {
            if (level.getStructureManager().get(room.structure().id()).isEmpty()) {
                return Optional.of(room.structure().id());
            }
        }

        return Optional.empty();
    }

    private static void clearEntrySpace(ServerLevel level, BlockPos entry) {
        level.setBlockAndUpdate(entry, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(entry.above(), Blocks.AIR.defaultBlockState());
        if (level.getBlockState(entry.below()).isAir()) {
            level.setBlockAndUpdate(entry.below(), Blocks.STONE.defaultBlockState());
        }
    }

    private static void clearDungeonArea(ServerLevel level, BlockPos origin) {
        int radius = 224;
        int minX = origin.getX() - radius;
        int minZ = origin.getZ() - radius;
        int maxX = origin.getX() + radius;
        int maxZ = origin.getZ() + radius;
        int minY = origin.getY() - 24;
        int maxY = origin.getY() + 80;

        for (int x = minX - 1; x <= maxX + 1; x++) {
            for (int z = minZ - 1; z <= maxZ + 1; z++) {
                for (int y = minY; y <= maxY; y++) {
                    level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    private record DungeonLayout(Set<GridCell> rooms, Set<GridCell> hallways) {
    }

    private record RoomPlacement(int centerX, int centerZ, DungeonThemeData.StructureRef ref, BlockPos size) {
    }

    private record HallPlacement(int centerX, int centerZ, DungeonThemeData.StructureRef ref, BlockPos size) {
    }

    private record RoomArea(BlockPos origin, BlockPos size, int floorY) {
    }

    private record GridCell(int x, int z) {
        private static final GridCell START = new GridCell(0, 0);

        private GridCell step(Direction direction, int amount) {
            return switch (direction) {
                case NORTH -> new GridCell(x, z - amount);
                case SOUTH -> new GridCell(x, z + amount);
                case WEST -> new GridCell(x - amount, z);
                case EAST -> new GridCell(x + amount, z);
                default -> this;
            };
        }

        private boolean isEastWestHallway() {
            return Math.abs(x) % 2 == 1;
        }
    }

    private static void ensureAltarRequirement(MinecraftServer server, RoguelikeSavedData data, UUID ownerId) {
        List<RoguelikeSavedData.AltarRequirement> current = data.altarRequirements(ownerId);
        if (current.size() >= MIN_ALTAR_ITEM_VARIANTS && hasOnlyValidOfferings(current)) {
            return;
        }

        chooseNextAltarRequirement(server, data, ownerId);
    }

    private static boolean hasOnlyValidOfferings(List<RoguelikeSavedData.AltarRequirement> requirements) {
        for (RoguelikeSavedData.AltarRequirement requirement : requirements) {
            AltarOfferingData offering = AltarOfferingManager.OFFERINGS.get(requirement.offeringId());
            if (offering == null || requirement.requiredAmount() <= 0) {
                return false;
            }
        }

        return true;
    }

    private static void chooseNextAltarRequirement(MinecraftServer server, RoguelikeSavedData data, UUID ownerId) {
        int uniqueItems = (int) AltarOfferingManager.OFFERINGS.values().stream()
                .map(AltarOfferingData::item)
                .distinct()
                .count();

        if (uniqueItems < MIN_ALTAR_ITEM_VARIANTS) {
            data.setAltarRequirements(ownerId, List.of());
            return;
        }

        int desiredVariants = Math.min(
                Math.min(MAX_ALTAR_ITEM_VARIANTS, uniqueItems),
                MIN_ALTAR_ITEM_VARIANTS + (data.crystalsCrafted(ownerId) / ALTAR_VARIANT_GROWTH_STEP)
        );

        List<AltarOfferingManager.PickedOffering> picked = pickDistinctOfferings(server.overworld().random, desiredVariants);
        if (picked.size() < MIN_ALTAR_ITEM_VARIANTS) {
            data.setAltarRequirements(ownerId, List.of());
            return;
        }

        List<RoguelikeSavedData.AltarRequirement> requirements = new ArrayList<>(picked.size());
        for (AltarOfferingManager.PickedOffering offering : picked) {
            int requiredAmount = offering.data().requiredAmount(data.crystalsCrafted(ownerId));
            requirements.add(new RoguelikeSavedData.AltarRequirement(offering.id(), requiredAmount, 0));
        }

        data.setAltarRequirements(ownerId, requirements);
    }

    private static List<AltarOfferingManager.PickedOffering> pickDistinctOfferings(RandomSource random, int count) {
        List<Map.Entry<ResourceLocation, AltarOfferingData>> pool = new ArrayList<>(AltarOfferingManager.OFFERINGS.entrySet());
        List<AltarOfferingManager.PickedOffering> picked = new ArrayList<>(count);
        Set<Item> usedItems = new HashSet<>();

        while (!pool.isEmpty() && picked.size() < count) {
            int totalWeight = 0;
            for (Map.Entry<ResourceLocation, AltarOfferingData> entry : pool) {
                if (usedItems.contains(entry.getValue().item())) {
                    continue;
                }
                totalWeight += entry.getValue().weight();
            }

            if (totalWeight <= 0) {
                break;
            }

            int roll = random.nextInt(totalWeight);
            Map.Entry<ResourceLocation, AltarOfferingData> choice = null;
            for (Map.Entry<ResourceLocation, AltarOfferingData> entry : pool) {
                if (usedItems.contains(entry.getValue().item())) {
                    continue;
                }

                roll -= entry.getValue().weight();
                if (roll < 0) {
                    choice = entry;
                    break;
                }
            }

            if (choice == null) {
                break;
            }

            picked.add(new AltarOfferingManager.PickedOffering(choice.getKey(), choice.getValue()));
            Item chosenItem = choice.getValue().item();
            usedItems.add(chosenItem);
            pool.removeIf(entry -> entry.getValue().item() == chosenItem);
        }

        return picked;
    }

    private static void absorbDroppedItems(ServerLevel level, BlockPos altarPos, RoguelikeSavedData data, UUID ownerId) {
        if (data.isAltarComplete(ownerId)) {
            return;
        }

        AABB area = new AABB(altarPos).inflate(ALTAR_COLLECTION_RADIUS, 1.0D, ALTAR_COLLECTION_RADIUS);
        List<ItemEntity> itemEntities = level.getEntitiesOfClass(ItemEntity.class, area, itemEntity -> itemEntity.isAlive() && !itemEntity.getItem().isEmpty());

        for (ItemEntity itemEntity : itemEntities) {
            if (data.isAltarComplete(ownerId)) {
                return;
            }

            ItemStack stack = itemEntity.getItem();
            int consumed = consumeStackForRequirements(stack, data, ownerId);
            if (consumed <= 0) {
                continue;
            }

            if (stack.isEmpty()) {
                itemEntity.discard();
            } else {
                itemEntity.setItem(stack);
            }
        }
    }

    private static int consumeStackForRequirements(ItemStack stack, RoguelikeSavedData data, UUID ownerId) {
        if (stack.isEmpty()) {
            return 0;
        }

        for (RoguelikeSavedData.AltarRequirement requirement : data.altarRequirements(ownerId)) {
            if (requirement.isComplete()) {
                continue;
            }

            AltarOfferingData offering = AltarOfferingManager.OFFERINGS.get(requirement.offeringId());
            if (offering == null || offering.item() != stack.getItem()) {
                continue;
            }

            int toConsume = Math.min(requirement.remaining(), stack.getCount());
            int consumed = data.submitOffering(ownerId, requirement.offeringId(), toConsume);
            if (consumed > 0) {
                stack.shrink(consumed);
                return consumed;
            }

            return 0;
        }

        return 0;
    }

    private static void syncAltarDisplay(List<RoguelikeSavedData.AltarRequirement> requirements, RoguelikeAltarBlockEntity altar) {
        List<RoguelikeAltarBlockEntity.DisplayEntry> entries = new ArrayList<>();
        for (RoguelikeSavedData.AltarRequirement requirement : requirements) {
            AltarOfferingData offering = AltarOfferingManager.OFFERINGS.get(requirement.offeringId());
            if (offering == null) {
                continue;
            }

            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(offering.item());
            entries.add(new RoguelikeAltarBlockEntity.DisplayEntry(itemId, requirement.submittedAmount(), requirement.requiredAmount()));
        }

        altar.setDisplayEntries(entries);
    }

    private static void expireDungeon(MinecraftServer server, RoguelikeSavedData data, RoguelikeSavedData.DungeonRecord dungeon) {
        RoguelikeSavedData.DungeonRecord expired = dungeon.withState(RoguelikeSavedData.State.EXPIRED);
        data.putDungeon(expired);

        closeDungeon(server, data, expired, true);
    }

    private static void failDungeonIfAllEntrantsFailed(MinecraftServer server, RoguelikeSavedData data, long dungeonId) {
        RoguelikeSavedData.DungeonRecord dungeon = data.getDungeon(dungeonId);
        if (dungeon == null || dungeon.state() != RoguelikeSavedData.State.ACTIVE || data.hasActiveRunInDungeon(dungeonId)) {
            return;
        }

        boolean anyoneEntered = false;
        for (RoguelikeSavedData.PlayerProgress progress : dungeon.playerProgress().values()) {
            if (!progress.entered()) {
                continue;
            }

            anyoneEntered = true;
            if (!progress.died()) {
                return;
            }
        }

        if (!anyoneEntered) {
            return;
        }

        RoguelikeSavedData.DungeonRecord failed = dungeon.withState(RoguelikeSavedData.State.EXPIRED);
        data.putDungeon(failed);
        closeDungeon(server, data, failed, false);
    }

    private static void closeDungeon(MinecraftServer server, RoguelikeSavedData data, RoguelikeSavedData.DungeonRecord dungeon, boolean killPlayers) {
        deactivatePortal(server, dungeon);
        RoguelikeSavedData.DungeonRecord latestDungeon = dungeon;

        for (UUID playerId : data.activePlayersInDungeon(dungeon.dungeonId())) {
            Optional<RoguelikeSavedData.ActiveRun> run = data.getRun(playerId);
            if (run.isEmpty()) {
                continue;
            }

            removeObjectiveBar(playerId);
            var progress = latestDungeon.progressFor(playerId);
            if (killPlayers) {
                progress = progress.withDeath();
                latestDungeon = latestDungeon.upsertProgress(playerId, progress);
                data.putDungeon(latestDungeon);
            }

            ServerPlayer activePlayer = server.getPlayerList().getPlayer(playerId);
            if (killPlayers) {
                data.setPendingReturn(playerId, run.get().returnDimensionKey(), run.get().returnPos());
                if (activePlayer != null) {
                    activePlayer.kill();
                }
            } else if (activePlayer != null) {
                ServerLevel returnLevel = server.getLevel(run.get().returnDimensionKey());
                if (returnLevel != null) {
                    teleport(activePlayer, returnLevel, run.get().returnPos());
                }
            } else {
                data.setPendingReturn(playerId, run.get().returnDimensionKey(), run.get().returnPos());
            }

            data.clearRun(playerId);
        }
    }

    private static void deactivatePortal(MinecraftServer server, RoguelikeSavedData.DungeonRecord dungeon) {
        ResourceKey<Level> portalDimensionKey = ResourceKey.create(Registries.DIMENSION, dungeon.portalDimension());
        ServerLevel level = server.getLevel(portalDimensionKey);
        if (level == null) {
            return;
        }

        clearConnectedPortalBlocks(level, dungeon.portalPos(), dungeon.dungeonId());
    }

    private static void clearConnectedPortalBlocks(ServerLevel level, BlockPos startPos, long dungeonId) {
        if (dungeonId <= 0) {
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
            if (!(blockEntity instanceof RoguelikePortalBlockEntity portal) || portal.dungeonId() != dungeonId) {
                continue;
            }

            level.setBlockAndUpdate(current, Blocks.AIR.defaultBlockState());
            for (Direction direction : Direction.values()) {
                queue.add(current.relative(direction));
            }
        }
    }

    private static void syncObjectiveBars(MinecraftServer server, RoguelikeSavedData data) {
        Set<UUID> onlineActivePlayers = new HashSet<>();
        for (RoguelikeSavedData.ActiveRun run : data.activeRuns()) {
            UUID playerId = run.playerId();
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player == null) {
                removeObjectiveBar(playerId);
                continue;
            }

            onlineActivePlayers.add(playerId);
            RoguelikeSavedData.DungeonRecord dungeon = data.getDungeon(run.dungeonId());
            if (dungeon == null || dungeon.state() != RoguelikeSavedData.State.ACTIVE) {
                removeObjectiveBar(playerId);
                continue;
            }

            DungeonObjectiveData objective = DungeonObjectiveManager.OBJECTIVES.get(dungeon.objectiveId());
            if (objective == null) {
                removeObjectiveBar(playerId);
                continue;
            }

            updateObjectiveBar(player, dungeon.objectiveId(), objective, dungeon.progressFor(playerId));
        }

        for (UUID playerId : new ArrayList<>(OBJECTIVE_BARS.keySet())) {
            if (!onlineActivePlayers.contains(playerId)) {
                removeObjectiveBar(playerId);
            }
        }
    }

    private static void updateObjectiveBar(ServerPlayer player, ResourceLocation objectiveId, DungeonObjectiveData objective, RoguelikeSavedData.PlayerProgress progress) {
        int target = objective.isKillObjective() ? Math.max(1, objective.target()) : 1;
        int current = objective.isKillObjective() ? progress.kills() : (progress.rewarded() ? 1 : 0);
        int display = Math.min(current, target);
        float fraction = Math.max(0.0F, Math.min(1.0F, display / (float) target));

        Component title = Component.translatable("incore.roguelike.portal.objective", objectiveDisplayName(objectiveId), display, target);
        ServerBossEvent bar = OBJECTIVE_BARS.computeIfAbsent(
                player.getUUID(),
                ignored -> new ServerBossEvent(title, BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS)
        );

        bar.setName(title);
        bar.setProgress(fraction);
        bar.setVisible(true);
        if (!bar.getPlayers().contains(player)) {
            bar.addPlayer(player);
        }
    }

    private static void removeObjectiveBar(UUID playerId) {
        ServerBossEvent bar = OBJECTIVE_BARS.remove(playerId);
        if (bar != null) {
            bar.removeAllPlayers();
        }
    }

    private static void sendTimerToDungeonPlayers(MinecraftServer server, RoguelikeSavedData data, RoguelikeSavedData.DungeonRecord dungeon, long remainingTicks) {
        int totalSeconds = (int) Math.max(0L, remainingTicks / 20L);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        String secondsText = String.format(Locale.ROOT, "%02d", seconds);

        for (UUID playerId : data.activePlayersInDungeon(dungeon.dungeonId())) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player == null) {
                continue;
            }

            player.displayClientMessage(Component.translatable("incore.roguelike.timer", minutes, secondsText), true);
        }
    }

    private static BlockPos dungeonEntryPos(RoguelikeSavedData.DungeonRecord dungeon) {
        return dungeon.origin().offset(0, 1, 0);
    }

    private static void teleport(ServerPlayer player, ServerLevel level, BlockPos pos) {
        player.teleportTo(level, pos.getX() + 0.5D, pos.getY() + 0.1D, pos.getZ() + 0.5D, player.getYRot(), player.getXRot());
    }

    public static Component themeDisplayName(ResourceLocation themeId) {
        return Component.translatable(resourceNameTranslationKey("incore.roguelike.theme", themeId));
    }

    public static Component objectiveDisplayName(ResourceLocation objectiveId) {
        return Component.translatable(resourceNameTranslationKey("incore.roguelike.objective", objectiveId));
    }

    private static String resourceNameTranslationKey(String baseKey, ResourceLocation id) {
        String path = id.getPath().replace('/', '.');
        String suffix = "incore".equals(id.getNamespace())
                ? path
                : id.getNamespace() + "." + path;
        return baseKey + "." + suffix;
    }

    private static MinecraftServer serverFrom(ServerPlayer player) {
        return player.getServer();
    }
}
