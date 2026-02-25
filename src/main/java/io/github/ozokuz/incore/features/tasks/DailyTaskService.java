package io.github.ozokuz.incore.features.tasks;

import com.google.gson.Gson;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

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

    public static void ensurePeriod(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        long currentDay = currentDayIndex();
        if (data.getLong(KEY_DAILY_DAY_INDEX) != currentDay) {
            resetDaily(player, currentDay);
        }
    }

    public static void onLogin(ServerPlayer player) {
        ensurePeriod(player);
        CompoundTag data = player.getPersistentData();
        if (!data.getBoolean(KEY_DAILY_LOGIN)) {
            data.putBoolean(KEY_DAILY_LOGIN, true);
        }
    }

    public static void onShopPurchase(ServerPlayer player) {
        ensurePeriod(player);
        CompoundTag data = player.getPersistentData();
        int current = data.getInt(KEY_DAILY_SHOP_PURCHASES);
        data.putInt(KEY_DAILY_SHOP_PURCHASES, current + 1);
    }

    public static void onArenaCompletion(ServerPlayer player) {
        ensurePeriod(player);
        CompoundTag data = player.getPersistentData();
        int current = data.getInt(KEY_DAILY_ARENA_COMPLETIONS);
        data.putInt(KEY_DAILY_ARENA_COMPLETIONS, current + 1);
    }

    public static void onDungeonCompletion(ServerPlayer player) {
        ensurePeriod(player);
        CompoundTag data = player.getPersistentData();
        int current = data.getInt(KEY_DAILY_DUNGEON_COMPLETIONS);
        data.putInt(KEY_DAILY_DUNGEON_COMPLETIONS, current + 1);
    }

    public static void onVendorPurchase(ServerPlayer player) {
        ensurePeriod(player);
        CompoundTag data = player.getPersistentData();
        int current = data.getInt(KEY_DAILY_VENDOR_PURCHASES);
        data.putInt(KEY_DAILY_VENDOR_PURCHASES, current + 1);
    }

    public static void onBuyFromPlayer(ServerPlayer player) {
        ensurePeriod(player);
        CompoundTag data = player.getPersistentData();
        int current = data.getInt(KEY_DAILY_NUMISMATICS_BUYS);
        data.putInt(KEY_DAILY_NUMISMATICS_BUYS, current + 1);
    }

    public static void onSellToPlayer(ServerPlayer player) {
        ensurePeriod(player);
        CompoundTag data = player.getPersistentData();
        int current = data.getInt(KEY_DAILY_NUMISMATICS_SELLS);
        data.putInt(KEY_DAILY_NUMISMATICS_SELLS, current + 1);
    }

    public static int getProgress(ServerPlayer player, DailyTask task) {
        ensurePeriod(player);
        CompoundTag data = player.getPersistentData();
        return switch (task) {
            case LOGIN -> data.getBoolean(KEY_DAILY_LOGIN) ? 1 : 0;
            case SHOP_PURCHASE -> data.getInt(KEY_DAILY_SHOP_PURCHASES);
            case ARENA_COMPLETION -> data.getInt(KEY_DAILY_ARENA_COMPLETIONS);
            case DUNGEON_COMPLETION -> data.getInt(KEY_DAILY_DUNGEON_COMPLETIONS);
            case VENDOR_PURCHASE -> data.getInt(KEY_DAILY_VENDOR_PURCHASES);
            case BUY_FROM_PLAYER -> data.getInt(KEY_DAILY_NUMISMATICS_BUYS);
            case SELL_TO_PLAYER -> data.getInt(KEY_DAILY_NUMISMATICS_SELLS);
        };
    }

    public static boolean isComplete(ServerPlayer player, DailyTask task) {
        return getProgress(player, task) >= task.goal();
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

    public static boolean allCompleted(ServerPlayer player) {
        for (DailyTask task : DailyTask.allTasks()) {
            if (!isComplete(player, task)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isRewardClaimed(ServerPlayer player) {
        ensurePeriod(player);
        return player.getPersistentData().getBoolean(KEY_DAILY_REWARD_CLAIMED);
    }

    public static boolean claimReward(ServerPlayer player) {
        ensurePeriod(player);
        CompoundTag data = player.getPersistentData();
        if (!allCompleted(player) || data.getBoolean(KEY_DAILY_REWARD_CLAIMED)) {
            return false;
        }
        data.putBoolean(KEY_DAILY_REWARD_CLAIMED, true);
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

    private static void resetDaily(ServerPlayer player, long dayIndex) {
        CompoundTag data = player.getPersistentData();
        data.putLong(KEY_DAILY_DAY_INDEX, dayIndex);
        data.putBoolean(KEY_DAILY_LOGIN, false);
        data.putInt(KEY_DAILY_SHOP_PURCHASES, 0);
        data.putInt(KEY_DAILY_ARENA_COMPLETIONS, 0);
        data.putInt(KEY_DAILY_DUNGEON_COMPLETIONS, 0);
        data.putInt(KEY_DAILY_VENDOR_PURCHASES, 0);
        data.putInt(KEY_DAILY_NUMISMATICS_BUYS, 0);
        data.putInt(KEY_DAILY_NUMISMATICS_SELLS, 0);
        data.putBoolean(KEY_DAILY_REWARD_CLAIMED, false);
    }

    private static long currentDayIndex() {
        return LocalDate.now(ZoneOffset.UTC).toEpochDay();
    }

    public static void forceReset(ServerPlayer player) {
        resetDaily(player, currentDayIndex());
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
