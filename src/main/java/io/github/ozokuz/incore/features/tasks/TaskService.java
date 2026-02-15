package io.github.ozokuz.incore.features.tasks;

import com.google.gson.Gson;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public final class TaskService {
    private static final Gson GSON = new Gson();
    private static final String KEY_DAY_INDEX = "incore:tasks_day";
    private static final String KEY_WEEK_INDEX = "incore:tasks_week";
    private static final String KEY_DAILY_IDS = "incore:tasks_daily_ids";
    private static final String KEY_WEEKLY_IDS = "incore:tasks_weekly_ids";
    private static final String KEY_DAILY_PROGRESS = "incore:tasks_daily_progress";
    private static final String KEY_WEEKLY_PROGRESS = "incore:tasks_weekly_progress";
    private static final String KEY_DAILY_COMPLETED = "incore:tasks_daily_completed";
    private static final String KEY_WEEKLY_POINTS = "incore:tasks_weekly_points";
    private static final String KEY_WEEKLY_TIER_CLAIMS = "incore:tasks_weekly_tier_claims";

    private TaskService() {
    }

    public static void tick(ServerPlayer player) {
        ensurePeriods(player);
        refreshItemCollectionProgress(player);
        resolveCompletionsAndRewards(player);
    }

    public static void onMobKill(ServerPlayer player, LivingEntity victim) {
        ensurePeriods(player);
        incrementMobKillProgress(player, victim.getType());
        resolveCompletionsAndRewards(player);
    }

    public static String buildSyncJson(ServerPlayer player) {
        ensurePeriods(player);

        CompoundTag data = player.getPersistentData();
        List<TaskView> daily = buildViews(player, readIdList(data, KEY_DAILY_IDS), KEY_DAILY_PROGRESS);
        List<TaskView> weekly = buildViews(player, readIdList(data, KEY_WEEKLY_IDS), KEY_WEEKLY_PROGRESS);

        int weeklyPoints = data.getInt(KEY_WEEKLY_POINTS);
        List<TierView> tiers = new ArrayList<>();
        int claimMask = data.getInt(KEY_WEEKLY_TIER_CLAIMS);
        for (int tier = 1; tier <= 5; tier++) {
            int required = tier * 2;
            boolean unlocked = weeklyPoints >= required;
            boolean claimed = (claimMask & (1 << (tier - 1))) != 0;
            tiers.add(new TierView(tier, required, unlocked, claimed));
        }

        return GSON.toJson(new TaskSyncView(daily, weekly, weeklyPoints, data.getBoolean(KEY_DAILY_COMPLETED), tiers));
    }

    private static void ensurePeriods(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();

        long dayIndex = currentDayIndex();
        if (data.getLong(KEY_DAY_INDEX) != dayIndex) {
            data.putLong(KEY_DAY_INDEX, dayIndex);
            writeIdList(data, KEY_DAILY_IDS, pickTasks(TaskDataManager.dailyTasks(), 3, player.getUUID(), dayIndex));
            data.put(KEY_DAILY_PROGRESS, new CompoundTag());
            data.putBoolean(KEY_DAILY_COMPLETED, false);
        }

        long weekIndex = currentWeekIndex();
        if (data.getLong(KEY_WEEK_INDEX) != weekIndex) {
            data.putLong(KEY_WEEK_INDEX, weekIndex);
            writeIdList(data, KEY_WEEKLY_IDS, pickTasks(TaskDataManager.weeklyTasks(), 5, player.getUUID(), weekIndex));
            data.put(KEY_WEEKLY_PROGRESS, new CompoundTag());
            data.putInt(KEY_WEEKLY_POINTS, 0);
            data.putInt(KEY_WEEKLY_TIER_CLAIMS, 0);
        }
    }

    private static List<String> pickTasks(List<TaskDefinition> source, int count, UUID uuid, long index) {
        if (source.isEmpty()) {
            return List.of();
        }

        List<TaskDefinition> shuffled = new ArrayList<>(source);
        long seed = uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits() ^ (index * 31L);
        Collections.shuffle(shuffled, new Random(seed));

        int resultSize = Math.min(count, shuffled.size());
        List<String> ids = new ArrayList<>(resultSize);
        for (int i = 0; i < resultSize; i++) {
            ids.add(shuffled.get(i).id().toString());
        }
        return ids;
    }

    private static void refreshItemCollectionProgress(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        CompoundTag dailyProgress = data.getCompound(KEY_DAILY_PROGRESS);
        for (String id : readIdList(data, KEY_DAILY_IDS)) {
            TaskDefinition definition = findTask(id, TaskDefinition.Period.DAILY);
            if (definition != null && definition.type() == TaskDefinition.TaskType.ITEM_COLLECTION) {
                dailyProgress.putInt(id, countItem(player, definition.target()));
            }
        }
        data.put(KEY_DAILY_PROGRESS, dailyProgress);

        CompoundTag weeklyProgress = data.getCompound(KEY_WEEKLY_PROGRESS);
        for (String id : readIdList(data, KEY_WEEKLY_IDS)) {
            TaskDefinition definition = findTask(id, TaskDefinition.Period.WEEKLY);
            if (definition != null && definition.type() == TaskDefinition.TaskType.ITEM_COLLECTION) {
                weeklyProgress.putInt(id, countItem(player, definition.target()));
            }
        }
        data.put(KEY_WEEKLY_PROGRESS, weeklyProgress);
    }

    private static int countItem(ServerPlayer player, net.minecraft.resources.ResourceLocation itemId) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getItemHolder().unwrapKey().isPresent() && stack.getItemHolder().unwrapKey().get().location().equals(itemId)) {
                total += stack.getCount();
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (!stack.isEmpty() && stack.getItemHolder().unwrapKey().isPresent() && stack.getItemHolder().unwrapKey().get().location().equals(itemId)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static void incrementMobKillProgress(ServerPlayer player, EntityType<?> victimType) {
        CompoundTag data = player.getPersistentData();

        CompoundTag dailyProgress = data.getCompound(KEY_DAILY_PROGRESS);
        for (String id : readIdList(data, KEY_DAILY_IDS)) {
            TaskDefinition definition = findTask(id, TaskDefinition.Period.DAILY);
            if (definition != null && definition.type() == TaskDefinition.TaskType.MOB_KILL && victimType.builtInRegistryHolder().key().location().equals(definition.target())) {
                dailyProgress.putInt(id, dailyProgress.getInt(id) + 1);
            }
        }
        data.put(KEY_DAILY_PROGRESS, dailyProgress);

        CompoundTag weeklyProgress = data.getCompound(KEY_WEEKLY_PROGRESS);
        for (String id : readIdList(data, KEY_WEEKLY_IDS)) {
            TaskDefinition definition = findTask(id, TaskDefinition.Period.WEEKLY);
            if (definition != null && definition.type() == TaskDefinition.TaskType.MOB_KILL && victimType.builtInRegistryHolder().key().location().equals(definition.target())) {
                weeklyProgress.putInt(id, weeklyProgress.getInt(id) + 1);
            }
        }
        data.put(KEY_WEEKLY_PROGRESS, weeklyProgress);
    }

    private static void resolveCompletionsAndRewards(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();

        if (!data.getBoolean(KEY_DAILY_COMPLETED)) {
            CompoundTag dailyProgress = data.getCompound(KEY_DAILY_PROGRESS);
            for (String id : readIdList(data, KEY_DAILY_IDS)) {
                TaskDefinition definition = findTask(id, TaskDefinition.Period.DAILY);
                if (definition != null && dailyProgress.getInt(id) >= definition.goal()) {
                    data.putBoolean(KEY_DAILY_COMPLETED, true);
                    grantRewards(player, "daily_completion");
                    break;
                }
            }
        }

        CompoundTag weeklyProgress = data.getCompound(KEY_WEEKLY_PROGRESS);
        int points = 0;
        for (String id : readIdList(data, KEY_WEEKLY_IDS)) {
            TaskDefinition definition = findTask(id, TaskDefinition.Period.WEEKLY);
            if (definition != null && weeklyProgress.getInt(id) >= definition.goal()) {
                points += definition.difficulty().points();
            }
        }
        data.putInt(KEY_WEEKLY_POINTS, points);

        int claims = data.getInt(KEY_WEEKLY_TIER_CLAIMS);
        for (int tier = 1; tier <= 5; tier++) {
            int required = tier * 2;
            int bit = 1 << (tier - 1);
            if (points >= required && (claims & bit) == 0) {
                grantRewards(player, "weekly_tier_" + tier);
                claims |= bit;
            }
        }
        data.putInt(KEY_WEEKLY_TIER_CLAIMS, claims);
    }

    private static void grantRewards(ServerPlayer player, String pool) {
        for (TaskReward reward : TaskDataManager.rewardsForPool(pool)) {
            if (reward instanceof TaskReward.ItemReward itemReward) {
                player.addItem(itemReward.item().getDefaultInstance().copyWithCount(itemReward.count()));
            } else if (reward instanceof TaskReward.CommandReward commandReward) {
                player.server.getCommands().performPrefixedCommand(
                        player.server.createCommandSourceStack().withPermission(2).withEntity(player),
                        commandReward.command().replace("{player}", player.getScoreboardName())
                );
            } else if (reward instanceof TaskReward.SanityReward sanityReward) {
                io.github.ozokuz.incore.features.sanity.SanityManager.addSanity(player, sanityReward.amount());
            }
        }
    }

    private static TaskDefinition findTask(String id, TaskDefinition.Period period) {
        List<TaskDefinition> tasks = period == TaskDefinition.Period.DAILY ? TaskDataManager.dailyTasks() : TaskDataManager.weeklyTasks();
        for (TaskDefinition task : tasks) {
            if (task.id().toString().equals(id)) {
                return task;
            }
        }
        return null;
    }

    private static List<TaskView> buildViews(ServerPlayer player, List<String> ids, String progressKey) {
        CompoundTag progressTag = player.getPersistentData().getCompound(progressKey);
        List<TaskView> result = new ArrayList<>();
        for (String id : ids) {
            TaskDefinition definition = findTask(id, progressKey.equals(KEY_DAILY_PROGRESS) ? TaskDefinition.Period.DAILY : TaskDefinition.Period.WEEKLY);
            if (definition == null) {
                continue;
            }
            int progress = progressTag.getInt(id);
            result.add(new TaskView(
                    definition.title(),
                    definition.description(),
                    definition.goal(),
                    progress,
                    definition.period() == TaskDefinition.Period.WEEKLY ? definition.difficulty().name().toLowerCase() : "daily",
                    definition.period() == TaskDefinition.Period.WEEKLY ? definition.difficulty().points() : 0
            ));
        }
        return result;
    }

    private static List<String> readIdList(CompoundTag data, String key) {
        ListTag tag = data.getList(key, StringTag.valueOf("").getId());
        List<String> ids = new ArrayList<>(tag.size());
        for (int i = 0; i < tag.size(); i++) {
            ids.add(tag.getString(i));
        }
        return ids;
    }

    private static void writeIdList(CompoundTag data, String key, List<String> ids) {
        ListTag tag = new ListTag();
        ids.forEach(id -> tag.add(StringTag.valueOf(id)));
        data.put(key, tag);
    }

    private static long currentDayIndex() {
        return LocalDate.now(ZoneOffset.UTC).toEpochDay();
    }

    private static long currentWeekIndex() {
        return currentDayIndex() / 7L;
    }

    private record TaskSyncView(List<TaskView> daily, List<TaskView> weekly, int weeklyPoints, boolean dailyCompleted, List<TierView> tiers) {
    }

    public record TaskView(String title, String description, int goal, int progress, String difficulty, int points) {
    }

    public record TierView(int tier, int requiredPoints, boolean unlocked, boolean claimed) {
    }
}
