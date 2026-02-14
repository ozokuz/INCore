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
            clearDungeonArea(roguelikeLevel, origin);
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

        generateDungeon(roguelikeLevel, dungeon, themePick.get().data(), objectivePick.get().data());
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

    private static void generateDungeon(ServerLevel level, RoguelikeSavedData.DungeonRecord dungeon, DungeonThemeData theme, DungeonObjectiveData objective) {
        BlockPos origin = dungeon.origin();
        int size = RoguelikeConstants.DUNGEON_ROOM_SIZE;
        int height = RoguelikeConstants.DUNGEON_ROOM_HEIGHT;
        int maxX = origin.getX() + size - 1;
        int maxY = origin.getY() + height - 1;
        int maxZ = origin.getZ() + size - 1;

        Block floor = theme.floorBlock();
        Block wall = theme.wallBlock();
        Block ceiling = theme.ceilingBlock();

        for (int x = origin.getX(); x <= maxX; x++) {
            for (int z = origin.getZ(); z <= maxZ; z++) {
                for (int y = origin.getY(); y <= maxY; y++) {
                    boolean boundary = x == origin.getX() || x == maxX || z == origin.getZ() || z == maxZ || y == origin.getY() || y == maxY;
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!boundary) {
                        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                        continue;
                    }

                    if (y == origin.getY()) {
                        level.setBlockAndUpdate(pos, floor.defaultBlockState());
                    } else if (y == maxY) {
                        level.setBlockAndUpdate(pos, ceiling.defaultBlockState());
                    } else {
                        level.setBlockAndUpdate(pos, wall.defaultBlockState());
                    }
                }
            }
        }

        BlockPos entry = dungeonEntryPos(dungeon);
        level.setBlockAndUpdate(entry.below(), floor.defaultBlockState());
        level.setBlockAndUpdate(entry, Blocks.AIR.defaultBlockState());

        int spawnCount = Math.min(256, Math.max(24, objective.target() + 8));
        for (int i = 0; i < spawnCount; i++) {
            spawnDungeonMob(level, dungeon.dungeonId(), origin, size);
        }
    }

    private static void spawnDungeonMob(ServerLevel level, long dungeonId, BlockPos origin, int size) {
        EntityType<? extends Monster> type = MOB_POOL.get(level.random.nextInt(MOB_POOL.size()));
        Mob mob = type.create(level);
        if (mob == null) {
            return;
        }

        int innerMinX = origin.getX() + 2;
        int innerMaxX = origin.getX() + size - 3;
        int innerMinZ = origin.getZ() + 2;
        int innerMaxZ = origin.getZ() + size - 3;

        double x = innerMinX + level.random.nextDouble() * Math.max(1, innerMaxX - innerMinX);
        double z = innerMinZ + level.random.nextDouble() * Math.max(1, innerMaxZ - innerMinZ);
        double y = origin.getY() + 1;

        mob.moveTo(x + 0.5D, y, z + 0.5D, level.random.nextFloat() * 360F, 0.0F);
        mob.getPersistentData().putLong(DUNGEON_ENTITY_TAG, dungeonId);

        level.addFreshEntity(mob);
    }

    private static void clearDungeonArea(ServerLevel level, BlockPos origin) {
        int size = RoguelikeConstants.DUNGEON_ROOM_SIZE;
        int height = RoguelikeConstants.DUNGEON_ROOM_HEIGHT;
        int maxX = origin.getX() + size - 1;
        int maxY = origin.getY() + height - 1;
        int maxZ = origin.getZ() + size - 1;

        for (int x = origin.getX() - 1; x <= maxX + 1; x++) {
            for (int z = origin.getZ() - 1; z <= maxZ + 1; z++) {
                for (int y = origin.getY() - 1; y <= maxY + 1; y++) {
                    level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
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
        int half = RoguelikeConstants.DUNGEON_ROOM_SIZE / 2;
        return dungeon.origin().offset(half, 1, half);
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
