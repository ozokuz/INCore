package io.github.ozokuz.incore.features.arena;

import com.google.gson.Gson;
import dev.shadowsoffire.gateways.entity.GatewayEntity;
import dev.shadowsoffire.gateways.gate.Gateway;
import dev.shadowsoffire.gateways.gate.GatewayRegistry;
import io.github.ozokuz.incore.Registration;
import io.github.ozokuz.incore.features.arena.content.ArenaRewardCrateData;
import io.github.ozokuz.incore.features.arena.data.ArenaCatalogEntry;
import io.github.ozokuz.incore.features.arena.data.ArenaCatalogManager;
import io.github.ozokuz.incore.features.arena.network.ArenaNetworking;
import io.github.ozokuz.incore.features.arena.state.ArenaSavedData;
import io.github.ozokuz.incore.features.tasks.DailyTaskEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class ArenaService {
    private static final Gson GSON = new Gson();

    private ArenaService() {
    }

    public static void openCombatCatalog(ServerPlayer player) {
        List<ArenaCatalogEntry> entries = ArenaCatalogManager.all();
        if (entries.isEmpty()) {
            player.sendSystemMessage(Component.translatable("incore.arena.catalog.empty"));
            return;
        }

        ArenaNetworking.openCatalog(player, GSON.toJson(toScreenData(entries)));
    }

    public static void startRun(ServerPlayer player, ResourceLocation entryId) {
        if (player.getServer() == null) {
            return;
        }

        ArenaCatalogEntry entry = ArenaCatalogManager.get(entryId);
        if (entry == null) {
            player.sendSystemMessage(Component.translatable("incore.arena.catalog.entry_missing", entryId.toString()));
            return;
        }

        MinecraftServer server = player.getServer();
        ArenaSavedData data = ArenaSavedData.get(server);

        Optional<ArenaSavedData.RunRecord> currentRun = data.getRun(player.getUUID());
        if (currentRun.isPresent() && !currentRun.get().state().hasEnded()) {
            player.sendSystemMessage(Component.translatable("incore.arena.run.already_active"));
            return;
        }
        if (currentRun.isPresent() && currentRun.get().state().hasEnded()) {
            data.clearRun(player.getUUID());
        }

        ServerLevel arenaLevel = server.getLevel(ArenaConstants.ARENA_DIMENSION);
        if (arenaLevel == null) {
            player.sendSystemMessage(Component.translatable("incore.arena.dimension_missing"));
            return;
        }

        int slotIndex = data.getOrAssignSlot(player.getUUID());
        BlockPos origin = ArenaSavedData.slotOrigin(slotIndex);

        buildArena(arenaLevel, origin);

        ArenaSavedData.RunRecord run = new ArenaSavedData.RunRecord(
                player.getUUID(),
                slotIndex,
                origin,
                entry.id(),
                entry.gatewayId(),
                null,
                ArenaSavedData.RunState.PREPARED,
                player.serverLevel().dimension().location(),
                player.blockPosition()
        );
        data.putRun(run);

        teleport(player, arenaLevel, origin.offset(0, 2, 0));
        player.sendSystemMessage(Component.translatable("incore.arena.run.teleported", entry.categoryName(), entry.difficultyName()));
    }

    public static boolean onOrbInteracted(ServerPlayer player, BlockPos orbPos) {
        if (player.getServer() == null) {
            return false;
        }

        ArenaSavedData data = ArenaSavedData.get(player.getServer());
        ArenaSavedData.RunRecord run = data.getRun(player.getUUID()).orElse(null);
        if (run == null) {
            player.sendSystemMessage(Component.translatable("incore.arena.run.none"));
            return false;
        }

        BlockPos expectedOrbPos = orbPosForRun(run);
        if (!expectedOrbPos.equals(orbPos) || !player.serverLevel().dimension().equals(ArenaConstants.ARENA_DIMENSION)) {
            player.sendSystemMessage(Component.translatable("incore.arena.orb.not_bound"));
            return false;
        }

        if (run.state() == ArenaSavedData.RunState.PREPARED) {
            return startGateway(player, data, run);
        }

        if (run.state() == ArenaSavedData.RunState.ACTIVE) {
            player.sendSystemMessage(Component.translatable("incore.arena.orb.active"));
            return false;
        }

        if (run.state().hasEnded()) {
            return returnPlayerToOrigin(player, data, run, true);
        }

        return false;
    }

    public static void onGatewayCompleted(GatewayEntity gateway) {
        if (!(gateway.level() instanceof ServerLevel level) || level.getServer() == null) {
            return;
        }

        ArenaSavedData data = ArenaSavedData.get(level.getServer());
        ArenaSavedData.RunRecord run = data.findRunByGateway(gateway.getUUID()).orElse(null);
        if (run == null || run.state() != ArenaSavedData.RunState.ACTIVE) {
            return;
        }

        ArenaCatalogEntry entry = ArenaCatalogManager.get(run.entryId());
        if (entry == null) {
            data.putRun(run.withState(ArenaSavedData.RunState.ENDED_FAIL));
            showOrb(level, run);
            return;
        }

        data.putRun(run.withState(ArenaSavedData.RunState.ENDED_SUCCESS));
        showOrb(level, run);
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(run.playerId());
        if (player != null) {
            ItemStack crate = ArenaRewardCrateData.createCrateStack(entry);
            if (!player.addItem(crate)) {
                player.drop(crate, false);
            }

            player.sendSystemMessage(Component.translatable("incore.arena.run.success", entry.difficultyName()));
            player.sendSystemMessage(Component.translatable("incore.arena.orb.return_ready"));
            DailyTaskEvents.onArenaCompletion(player);
        }
    }

    public static void onGatewayFailed(GatewayEntity gateway) {
        if (!(gateway.level() instanceof ServerLevel level) || level.getServer() == null) {
            return;
        }

        ArenaSavedData data = ArenaSavedData.get(level.getServer());
        ArenaSavedData.RunRecord run = data.findRunByGateway(gateway.getUUID()).orElse(null);
        if (run == null || run.state() != ArenaSavedData.RunState.ACTIVE) {
            return;
        }

        data.putRun(run.withState(ArenaSavedData.RunState.ENDED_FAIL));
        showOrb(level, run);
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(run.playerId());
        if (player != null) {
            player.sendSystemMessage(Component.translatable("incore.arena.run.failed"));
            player.sendSystemMessage(Component.translatable("incore.arena.orb.return_ready"));
        }
    }

    public static void onPlayerDeath(ServerPlayer player) {
        if (player.getServer() == null) {
            return;
        }

        ArenaSavedData data = ArenaSavedData.get(player.getServer());
        ArenaSavedData.RunRecord run = data.getRun(player.getUUID()).orElse(null);
        if (run == null || !player.serverLevel().dimension().equals(ArenaConstants.ARENA_DIMENSION)) {
            return;
        }

        data.setPendingReturn(player.getUUID(), run.returnDimensionKey(), run.returnPos());
        data.clearRun(player.getUUID());
    }

    public static void onPlayerRespawn(ServerPlayer player) {
        applyPendingReturn(player);
    }

    public static void onPlayerLogin(ServerPlayer player) {
        applyPendingReturn(player);
    }

    public static void onPlayerLogout(ServerPlayer player) {
        if (player.getServer() == null) {
            return;
        }

        if (!player.serverLevel().dimension().equals(ArenaConstants.ARENA_DIMENSION)) {
            return;
        }

        ArenaSavedData data = ArenaSavedData.get(player.getServer());
        ArenaSavedData.RunRecord run = data.getRun(player.getUUID()).orElse(null);
        if (run == null) {
            return;
        }

        data.setPendingReturn(player.getUUID(), run.returnDimensionKey(), run.returnPos());
        data.clearRun(player.getUUID());
    }

    public static boolean isArenaGateway(UUID gatewayEntityId) {
        if (gatewayEntityId == null) {
            return false;
        }

        MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return false;
        }

        ArenaSavedData data = ArenaSavedData.get(server);
        return data.findRunByGateway(gatewayEntityId)
                .map(run -> run.state() == ArenaSavedData.RunState.ACTIVE)
                .orElse(false);
    }

    private static void applyPendingReturn(ServerPlayer player) {
        if (player.getServer() == null) {
            return;
        }

        ArenaSavedData data = ArenaSavedData.get(player.getServer());
        data.takePendingReturn(player.getUUID()).ifPresent(target -> {
            ServerLevel level = player.getServer().getLevel(target.dimensionKey());
            if (level == null) {
                return;
            }

            teleport(player, level, target.pos());
        });
    }

    private static boolean startGateway(ServerPlayer player, ArenaSavedData data, ArenaSavedData.RunRecord run) {
        ResourceLocation gatewayId = run.gatewayId();
        var holder = GatewayRegistry.INSTANCE.holder(gatewayId);
        if (!holder.isBound()) {
            player.sendSystemMessage(Component.translatable("incore.arena.gateway.missing", gatewayId.toString()));
            return false;
        }

        Gateway gateway = holder.get();
        GatewayEntity entity = gateway.createEntity(player.serverLevel(), player);
        entity.setPos(run.origin().getX() + 0.5D, run.origin().getY() + 3D, run.origin().getZ() + 0.5D);

        if (!player.serverLevel().noCollision(entity)) {
            player.sendSystemMessage(Component.translatable("incore.arena.gateway.no_space"));
            return false;
        }

        hideOrb(player.serverLevel(), run);
        player.serverLevel().addFreshEntity(entity);
        entity.onGateCreated();

        data.putRun(run.withGatewayEntity(entity.getUUID(), ArenaSavedData.RunState.ACTIVE));
        player.sendSystemMessage(Component.translatable("incore.arena.gateway.started"));
        return true;
    }

    private static boolean returnPlayerToOrigin(ServerPlayer player, ArenaSavedData data, ArenaSavedData.RunRecord run, boolean notify) {
        if (player.getServer() == null) {
            return false;
        }

        ServerLevel returnLevel = player.getServer().getLevel(run.returnDimensionKey());
        if (returnLevel == null) {
            player.sendSystemMessage(Component.translatable("incore.arena.dimension_missing"));
            return false;
        }

        teleport(player, returnLevel, run.returnPos());
        data.clearRun(player.getUUID());
        if (notify) {
            player.sendSystemMessage(Component.translatable("incore.arena.run.returned"));
        }

        return true;
    }

    private static void teleport(ServerPlayer player, ServerLevel level, BlockPos pos) {
        player.teleportTo(level, pos.getX() + 0.5D, pos.getY() + 0.1D, pos.getZ() + 0.5D, player.getYRot(), player.getXRot());
    }

    private static ScreenData toScreenData(List<ArenaCatalogEntry> entries) {
        Map<String, String> categoryNames = new LinkedHashMap<>();
        for (ArenaCatalogEntry entry : entries) {
            categoryNames.putIfAbsent(entry.categoryId(), entry.categoryName());
        }

        List<ScreenEntry> screenEntries = entries.stream().map(entry -> new ScreenEntry(
                entry.id().toString(),
                entry.categoryId(),
                entry.categoryName(),
                entry.difficultyId(),
                entry.difficultyName(),
                entry.gatewayId().toString(),
                entry.rewardSanityCost(),
                entry.rewardItems().stream()
                        .map(stack -> new RewardView(stack.itemId().toString(), stack.count()))
                        .toList(),
                entry.rewardSummary()
        )).toList();

        return new ScreenData(
                categoryNames.entrySet().stream().map(e -> new CategoryView(e.getKey(), e.getValue())).toList(),
                screenEntries
        );
    }

    private static void buildArena(ServerLevel level, BlockPos origin) {
        int radius = ArenaConstants.ARENA_RADIUS;
        int floorDepth = ArenaConstants.ARENA_FLOOR_DEPTH;
        int wallHeight = ArenaConstants.ARENA_WALL_HEIGHT;

        int minX = origin.getX() - radius - 2;
        int maxX = origin.getX() + radius + 2;
        int minZ = origin.getZ() - radius - 2;
        int maxZ = origin.getZ() + radius + 2;
        int minY = origin.getY() - floorDepth - 2;
        int maxY = origin.getY() + wallHeight + 5;

        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    mutable.set(x, y, z);
                    level.setBlockAndUpdate(mutable, Blocks.AIR.defaultBlockState());
                }
            }
        }

        for (int x = origin.getX() - radius; x <= origin.getX() + radius; x++) {
            for (int z = origin.getZ() - radius; z <= origin.getZ() + radius; z++) {
                for (int depth = 0; depth < floorDepth; depth++) {
                    int y = origin.getY() - depth;
                    mutable.set(x, y, z);

                    if (depth >= floorDepth - 2) {
                        level.setBlockAndUpdate(mutable, Blocks.BEDROCK.defaultBlockState());
                    } else if (depth == 0) {
                        level.setBlockAndUpdate(mutable, Blocks.SMOOTH_STONE.defaultBlockState());
                    } else {
                        level.setBlockAndUpdate(mutable, Blocks.STONE.defaultBlockState());
                    }
                }
            }
        }

        int minWallX = origin.getX() - radius;
        int maxWallX = origin.getX() + radius;
        int minWallZ = origin.getZ() - radius;
        int maxWallZ = origin.getZ() + radius;
        int minWallY = origin.getY() + 1;
        int maxWallY = origin.getY() + wallHeight;

        for (int y = minWallY; y <= maxWallY; y++) {
            for (int x = minWallX; x <= maxWallX; x++) {
                level.setBlockAndUpdate(new BlockPos(x, y, minWallZ), Blocks.BARRIER.defaultBlockState());
                level.setBlockAndUpdate(new BlockPos(x, y, maxWallZ), Blocks.BARRIER.defaultBlockState());
            }
            for (int z = minWallZ; z <= maxWallZ; z++) {
                level.setBlockAndUpdate(new BlockPos(minWallX, y, z), Blocks.BARRIER.defaultBlockState());
                level.setBlockAndUpdate(new BlockPos(maxWallX, y, z), Blocks.BARRIER.defaultBlockState());
            }
        }

        int ceilingY = maxWallY + 1;
        for (int x = minWallX; x <= maxWallX; x++) {
            for (int z = minWallZ; z <= maxWallZ; z++) {
                mutable.set(x, ceilingY, z);
                level.setBlockAndUpdate(mutable, Blocks.BARRIER.defaultBlockState());
            }
        }

        BlockPos orbPos = orbPosForOrigin(origin);
        level.setBlockAndUpdate(orbPos, Registration.ARENA_ORB_BLOCK.get().defaultBlockState());

        for (int y = orbPos.getY() + 1; y <= orbPos.getY() + 3; y++) {
            level.setBlockAndUpdate(new BlockPos(origin.getX(), y, origin.getZ()), Blocks.AIR.defaultBlockState());
        }
    }

    private static BlockPos orbPosForRun(ArenaSavedData.RunRecord run) {
        return orbPosForOrigin(run.origin());
    }

    private static BlockPos orbPosForOrigin(BlockPos origin) {
        return origin.above(2);
    }

    private static void hideOrb(ServerLevel level, ArenaSavedData.RunRecord run) {
        BlockPos orbPos = orbPosForRun(run);
        if (level.getBlockState(orbPos).getBlock() == Registration.ARENA_ORB_BLOCK.get()) {
            level.setBlockAndUpdate(orbPos, Blocks.AIR.defaultBlockState());
        }
    }

    private static void showOrb(ServerLevel level, ArenaSavedData.RunRecord run) {
        BlockPos orbPos = orbPosForRun(run);
        level.setBlockAndUpdate(orbPos, Registration.ARENA_ORB_BLOCK.get().defaultBlockState());
    }

    public record ScreenData(List<CategoryView> categories, List<ScreenEntry> entries) {
    }

    public record CategoryView(String id, String name) {
    }

    public record ScreenEntry(
            String id,
            String categoryId,
            String categoryName,
            String difficultyId,
            String difficultyName,
            String gatewayId,
            int rewardSanityCost,
            List<RewardView> rewardItems,
            String rewardSummary
    ) {
    }

    public record RewardView(String itemId, int count) {
    }
}
