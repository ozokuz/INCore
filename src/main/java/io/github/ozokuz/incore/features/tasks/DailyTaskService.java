package io.github.ozokuz.incore.features.tasks;

import com.google.gson.Gson;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class DailyTaskService {
    private static final Gson GSON = new Gson();
    private static final String KEY_DAILY_DAY_INDEX = "incore:daily_day_index";
    private static final String KEY_DAILY_LOGIN = "incore:daily_login";
    private static final String KEY_DAILY_SHOP_PURCHASES = "incore:daily_shop_purchases";
    private static final String KEY_DAILY_ARENA_COMPLETIONS = "incore:daily_arena_completions";
    private static final String KEY_DAILY_DUNGEON_COMPLETIONS = "incore:daily_dungeon_completions";
    private static final String KEY_DAILY_VENDOR_PURCHASES = "incore:daily_vendor_purchases";
    private static final String KEY_DAILY_NUMISMATICS_BUYS = "incore:daily_numismatics_buys";
    private static final String KEY_DAILY_NUMISMATICS_SELLS = "incore:daily_numismatics_sells";
    private static final String KEY_DAILY_REWARD_CLAIMED = "incore:daily_reward_claimed";

    private DailyTaskService() {
    }

    public static void tick(ServerPlayer player) {
        ensurePeriod(player);
    }

    public static long currentDayIndex() {
        return LocalDate.now(ZoneOffset.UTC).toEpochDay();
    }

    public static void ensurePeriod(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        ensurePeriod(server, player.getUUID());
        syncFromSavedData(player);
    }

    public static void ensurePeriod(MinecraftServer server, UUID playerId) {
        DailyTaskSavedData savedData = DailyTaskSavedData.get(server);
        long currentDay = currentDayIndex();
        savedData.setCurrentDayIndex(currentDay);

        DailyTaskSavedData.PlayerDailyData playerData = savedData.getOrCreatePlayerData(playerId);
        if (playerData.getDayIndex() != currentDay) {
            playerData.reset(currentDay);
            savedData.markDirty();
        }
    }

    private static void syncFromSavedData(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        DailyTaskSavedData savedData = DailyTaskSavedData.get(server);
        DailyTaskSavedData.PlayerDailyData playerData = savedData.getPlayerData(player.getUUID());
        if (playerData == null) return;

        CompoundTag data = player.getPersistentData();
        data.putLong(KEY_DAILY_DAY_INDEX, playerData.getDayIndex());
        data.putBoolean(KEY_DAILY_LOGIN, playerData.isLogin());
        data.putInt(KEY_DAILY_SHOP_PURCHASES, playerData.getShopPurchases());
        data.putInt(KEY_DAILY_ARENA_COMPLETIONS, playerData.getArenaCompletions());
        data.putInt(KEY_DAILY_DUNGEON_COMPLETIONS, playerData.getDungeonCompletions());
        data.putInt(KEY_DAILY_VENDOR_PURCHASES, playerData.getVendorPurchases());
        data.putInt(KEY_DAILY_NUMISMATICS_BUYS, playerData.getNumismaticsBuys());
        data.putInt(KEY_DAILY_NUMISMATICS_SELLS, playerData.getNumismaticsSells());
        data.putBoolean(KEY_DAILY_REWARD_CLAIMED, playerData.isRewardClaimed());
    }

    public static void onLogin(ServerPlayer player) {
        ensurePeriod(player);
        MinecraftServer server = player.getServer();
        if (server == null) return;

        DailyTaskSavedData savedData = DailyTaskSavedData.get(server);
        DailyTaskSavedData.PlayerDailyData playerData = savedData.getOrCreatePlayerData(player.getUUID());
        if (!playerData.isLogin()) {
            playerData.setLogin(true);
            savedData.markDirty();
            player.getPersistentData().putBoolean(KEY_DAILY_LOGIN, true);
        }
    }

    public static void onShopPurchase(ServerPlayer player) {
        ensurePeriod(player);
        MinecraftServer server = player.getServer();
        if (server == null) return;

        DailyTaskSavedData savedData = DailyTaskSavedData.get(server);
        DailyTaskSavedData.PlayerDailyData playerData = savedData.getOrCreatePlayerData(player.getUUID());
        playerData.setShopPurchases(playerData.getShopPurchases() + 1);
        savedData.markDirty();
        player.getPersistentData().putInt(KEY_DAILY_SHOP_PURCHASES, playerData.getShopPurchases());
    }

    public static void onArenaCompletion(ServerPlayer player) {
        ensurePeriod(player);
        MinecraftServer server = player.getServer();
        if (server == null) return;

        DailyTaskSavedData savedData = DailyTaskSavedData.get(server);
        DailyTaskSavedData.PlayerDailyData playerData = savedData.getOrCreatePlayerData(player.getUUID());
        playerData.setArenaCompletions(playerData.getArenaCompletions() + 1);
        savedData.markDirty();
        player.getPersistentData().putInt(KEY_DAILY_ARENA_COMPLETIONS, playerData.getArenaCompletions());
    }

    public static void onDungeonCompletion(ServerPlayer player) {
        ensurePeriod(player);
        MinecraftServer server = player.getServer();
        if (server == null) return;

        DailyTaskSavedData savedData = DailyTaskSavedData.get(server);
        DailyTaskSavedData.PlayerDailyData playerData = savedData.getOrCreatePlayerData(player.getUUID());
        playerData.setDungeonCompletions(playerData.getDungeonCompletions() + 1);
        savedData.markDirty();
        player.getPersistentData().putInt(KEY_DAILY_DUNGEON_COMPLETIONS, playerData.getDungeonCompletions());
    }

    public static void onVendorPurchase(ServerPlayer player) {
        ensurePeriod(player);
        MinecraftServer server = player.getServer();
        if (server == null) return;

        DailyTaskSavedData savedData = DailyTaskSavedData.get(server);
        DailyTaskSavedData.PlayerDailyData playerData = savedData.getOrCreatePlayerData(player.getUUID());
        playerData.setVendorPurchases(playerData.getVendorPurchases() + 1);
        savedData.markDirty();
        player.getPersistentData().putInt(KEY_DAILY_VENDOR_PURCHASES, playerData.getVendorPurchases());
    }

    public static void onBuyFromPlayer(ServerPlayer player) {
        ensurePeriod(player);
        MinecraftServer server = player.getServer();
        if (server == null) return;

        onBuyFromPlayer(server, player.getUUID());
        player.getPersistentData().putInt(KEY_DAILY_NUMISMATICS_BUYS, 
            DailyTaskSavedData.get(server).getPlayerData(player.getUUID()).getNumismaticsBuys());
    }

    public static void onBuyFromPlayer(MinecraftServer server, UUID playerId) {
        ensurePeriod(server, playerId);
        DailyTaskSavedData savedData = DailyTaskSavedData.get(server);
        DailyTaskSavedData.PlayerDailyData playerData = savedData.getOrCreatePlayerData(playerId);
        playerData.setNumismaticsBuys(playerData.getNumismaticsBuys() + 1);
        savedData.markDirty();
    }

    public static void onSellToPlayer(ServerPlayer player) {
        ensurePeriod(player);
        MinecraftServer server = player.getServer();
        if (server == null) return;

        onSellToPlayer(server, player.getUUID());
        player.getPersistentData().putInt(KEY_DAILY_NUMISMATICS_SELLS,
            DailyTaskSavedData.get(server).getPlayerData(player.getUUID()).getNumismaticsSells());
    }

    public static void onSellToPlayer(MinecraftServer server, UUID playerId) {
        ensurePeriod(server, playerId);
        DailyTaskSavedData savedData = DailyTaskSavedData.get(server);
        DailyTaskSavedData.PlayerDailyData playerData = savedData.getOrCreatePlayerData(playerId);
        playerData.setNumismaticsSells(playerData.getNumismaticsSells() + 1);
        savedData.markDirty();
    }

    public static int getProgress(ServerPlayer player, DailyTask task) {
        ensurePeriod(player);
        MinecraftServer server = player.getServer();
        if (server == null) return 0;

        return getProgress(server, player.getUUID(), task);
    }

    public static int getProgress(MinecraftServer server, UUID playerId, DailyTask task) {
        ensurePeriod(server, playerId);
        DailyTaskSavedData savedData = DailyTaskSavedData.get(server);
        DailyTaskSavedData.PlayerDailyData playerData = savedData.getPlayerData(playerId);
        if (playerData == null) return 0;
        return playerData.getProgress(task);
    }

    public static boolean isComplete(ServerPlayer player, DailyTask task) {
        return getProgress(player, task) >= task.goal();
    }

    public static boolean isComplete(MinecraftServer server, UUID playerId, DailyTask task) {
        return getProgress(server, playerId, task) >= task.goal();
    }

    public static int countCompleted(ServerPlayer player) {
        ensurePeriod(player);
        int count = 0;
        for (DailyTask task : DailyTask.allTasks()) {
            if (isComplete(player, task)) {
                count++;
            }
        }
        return count;
    }

    public static int countCompleted(MinecraftServer server, UUID playerId) {
        ensurePeriod(server, playerId);
        int count = 0;
        for (DailyTask task : DailyTask.allTasks()) {
            if (isComplete(server, playerId, task)) {
                count++;
            }
        }
        return count;
    }

    public static boolean allCompleted(ServerPlayer player) {
        for (DailyTask task : DailyTask.allTasks()) {
            if (!isComplete(player, task)) {
                return false;
            }
        }
        return true;
    }

    public static boolean allCompleted(MinecraftServer server, UUID playerId) {
        for (DailyTask task : DailyTask.allTasks()) {
            if (!isComplete(server, playerId, task)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isRewardClaimed(ServerPlayer player) {
        ensurePeriod(player);
        MinecraftServer server = player.getServer();
        if (server == null) return false;

        DailyTaskSavedData savedData = DailyTaskSavedData.get(server);
        DailyTaskSavedData.PlayerDailyData playerData = savedData.getPlayerData(player.getUUID());
        return playerData != null && playerData.isRewardClaimed();
    }

    public static boolean claimReward(ServerPlayer player) {
        ensurePeriod(player);
        MinecraftServer server = player.getServer();
        if (server == null) return false;

        DailyTaskSavedData savedData = DailyTaskSavedData.get(server);
        DailyTaskSavedData.PlayerDailyData playerData = savedData.getOrCreatePlayerData(player.getUUID());

        if (!allCompleted(player) || playerData.isRewardClaimed()) {
            return false;
        }

        playerData.setRewardClaimed(true);
        savedData.markDirty();
        player.getPersistentData().putBoolean(KEY_DAILY_REWARD_CLAIMED, true);
        return true;
    }

    public static List<DailyTaskView> buildViews(ServerPlayer player) {
        ensurePeriod(player);
        List<DailyTaskView> views = new ArrayList<>();
        for (DailyTask task : DailyTask.allTasks()) {
            int progress = getProgress(player, task);
            views.add(new DailyTaskView(task.name(), task.title(), task.goal(), progress));
        }
        return views;
    }

    public static String buildSyncJson(ServerPlayer player) {
        ensurePeriod(player);
        return GSON.toJson(new DailySyncView(
                buildViews(player),
                countCompleted(player),
                allCompleted(player),
                isRewardClaimed(player)
        ));
    }

    public static void forceReset(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        long currentDay = currentDayIndex();
        DailyTaskSavedData savedData = DailyTaskSavedData.get(server);
        DailyTaskSavedData.PlayerDailyData playerData = savedData.getOrCreatePlayerData(player.getUUID());
        playerData.reset(currentDay);
        savedData.markDirty();
        syncFromSavedData(player);
    }

    public record DailyTaskView(String id, String title, int goal, int progress) {
    }

    public record DailySyncView(
            List<DailyTaskView> tasks,
            int completedCount,
            boolean allCompleted,
            boolean rewardClaimed
    ) {
    }
}
