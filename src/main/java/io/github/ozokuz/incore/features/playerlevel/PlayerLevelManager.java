package io.github.ozokuz.incore.features.playerlevel;

import io.github.ozokuz.incore.Config;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public final class PlayerLevelManager {
    private static final String KEY_LEVEL = "incore:player_level";
    private static final String KEY_EXPERIENCE = "incore:player_level_experience";
    private static final String KEY_HIGHEST_REWARDED_LEVEL = "incore:player_level_highest_rewarded";

    private PlayerLevelManager() {
    }

    public static void initialize(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();

        int level = Math.max(0, data.getInt(KEY_LEVEL));
        int currentExperience = Math.max(0, data.getInt(KEY_EXPERIENCE));
        int highestRewarded = Math.max(0, data.getInt(KEY_HIGHEST_REWARDED_LEVEL));

        int maxProgress = Math.max(0, getExperienceToNextLevel(level) - 1);
        int clampedExperience = Math.min(maxProgress, currentExperience);

        data.putInt(KEY_LEVEL, level);
        data.putInt(KEY_EXPERIENCE, clampedExperience);
        data.putInt(KEY_HIGHEST_REWARDED_LEVEL, highestRewarded);
        PlayerFeatureUnlockService.reconcileUpToLevel(player, level);
    }

    public static int getLevel(ServerPlayer player) {
        initialize(player);
        return player.getPersistentData().getInt(KEY_LEVEL);
    }

    public static int getCurrentExperience(ServerPlayer player) {
        initialize(player);
        return player.getPersistentData().getInt(KEY_EXPERIENCE);
    }

    public static int getExperienceToNextLevel(ServerPlayer player) {
        return getExperienceToNextLevel(getLevel(player));
    }

    public static int getExperienceToNextLevel(int currentLevel) {
        int level = Math.max(0, currentLevel);
        long base = Config.PLAYER_LEVEL_BASE_XP_COST.get();
        long increase = Config.PLAYER_LEVEL_XP_COST_INCREASE.get();
        long total = base + increase * level;
        return (int) Math.max(1L, Math.min((long) Integer.MAX_VALUE, total));
    }

    public static void setLevel(ServerPlayer player, int level, boolean grantRewards) {
        initialize(player);

        CompoundTag data = player.getPersistentData();
        int currentLevel = data.getInt(KEY_LEVEL);
        int nextLevel = Math.max(0, level);
        data.putInt(KEY_LEVEL, nextLevel);

        int maxProgress = Math.max(0, getExperienceToNextLevel(nextLevel) - 1);
        data.putInt(KEY_EXPERIENCE, Math.min(maxProgress, data.getInt(KEY_EXPERIENCE)));

        if (grantRewards && nextLevel > currentLevel) {
            grantRewardsUpTo(player, currentLevel + 1, nextLevel);
        } else if (!grantRewards && nextLevel > currentLevel) {
            int highestRewarded = data.getInt(KEY_HIGHEST_REWARDED_LEVEL);
            data.putInt(KEY_HIGHEST_REWARDED_LEVEL, Math.max(highestRewarded, nextLevel));
        }

        PlayerFeatureUnlockService.reconcileUpToLevel(player, nextLevel);
    }

    public static void addLevels(ServerPlayer player, int delta, boolean grantRewards) {
        int currentLevel = getLevel(player);
        long next = (long) currentLevel + delta;
        int clamped = (int) Math.max(0L, Math.min((long) Integer.MAX_VALUE, next));
        setLevel(player, clamped, grantRewards);
    }

    public static void setCurrentExperience(ServerPlayer player, int experience) {
        initialize(player);
        CompoundTag data = player.getPersistentData();

        int level = data.getInt(KEY_LEVEL);
        int maxProgress = Math.max(0, getExperienceToNextLevel(level) - 1);
        int clamped = Math.clamp(experience, 0, maxProgress);
        data.putInt(KEY_EXPERIENCE, clamped);
    }

    public static void addExperience(ServerPlayer player, int amount) {
        if (amount == 0) {
            return;
        }

        initialize(player);
        if (amount > 0) {
            addPositiveExperience(player, amount);
            return;
        }

        removeExperience(player, -amount);
    }

    public static void grantPendingRewards(ServerPlayer player) {
        initialize(player);

        CompoundTag data = player.getPersistentData();
        int level = data.getInt(KEY_LEVEL);
        int highestRewarded = data.getInt(KEY_HIGHEST_REWARDED_LEVEL);
        if (level <= highestRewarded) {
            return;
        }

        grantRewardsUpTo(player, highestRewarded + 1, level);
    }

    public static void copyData(ServerPlayer from, ServerPlayer to) {
        CompoundTag oldData = from.getPersistentData();
        CompoundTag newData = to.getPersistentData();

        if (oldData.contains(KEY_LEVEL)) {
            newData.putInt(KEY_LEVEL, oldData.getInt(KEY_LEVEL));
        }

        if (oldData.contains(KEY_EXPERIENCE)) {
            newData.putInt(KEY_EXPERIENCE, oldData.getInt(KEY_EXPERIENCE));
        }

        if (oldData.contains(KEY_HIGHEST_REWARDED_LEVEL)) {
            newData.putInt(KEY_HIGHEST_REWARDED_LEVEL, oldData.getInt(KEY_HIGHEST_REWARDED_LEVEL));
        }

        PlayerFeatureUnlockService.copyData(from, to);
    }

    private static void addPositiveExperience(ServerPlayer player, int amount) {
        CompoundTag data = player.getPersistentData();
        int level = data.getInt(KEY_LEVEL);
        int currentExperience = data.getInt(KEY_EXPERIENCE);
        int oldLevel = level;
        long remaining = amount;

        while (remaining > 0L) {
            int cost = getExperienceToNextLevel(level);
            int required = cost - currentExperience;
            if (remaining < required) {
                currentExperience += (int) remaining;
                remaining = 0L;
            } else {
                remaining -= required;
                level++;
                currentExperience = 0;
            }
        }

        data.putInt(KEY_LEVEL, level);
        data.putInt(KEY_EXPERIENCE, currentExperience);

        if (level > oldLevel) {
            grantRewardsUpTo(player, oldLevel + 1, level);
        }

        PlayerFeatureUnlockService.reconcileUpToLevel(player, level);
    }

    private static void removeExperience(ServerPlayer player, int amount) {
        CompoundTag data = player.getPersistentData();
        int level = data.getInt(KEY_LEVEL);
        int currentExperience = data.getInt(KEY_EXPERIENCE);
        long remaining = amount;

        while (remaining > 0L) {
            if (currentExperience >= remaining) {
                currentExperience -= (int) remaining;
                break;
            }

            remaining -= currentExperience;
            if (level <= 0) {
                currentExperience = 0;
                break;
            }

            level--;
            currentExperience = Math.max(0, getExperienceToNextLevel(level) - 1);
            if (remaining > 0L) {
                remaining--;
            }
        }

        data.putInt(KEY_LEVEL, Math.max(0, level));
        data.putInt(KEY_EXPERIENCE, Math.max(0, currentExperience));
    }

    private static void grantRewardsUpTo(ServerPlayer player, int fromLevelInclusive, int toLevelInclusive) {
        if (toLevelInclusive < fromLevelInclusive) {
            return;
        }

        CompoundTag data = player.getPersistentData();
        int highestRewarded = data.getInt(KEY_HIGHEST_REWARDED_LEVEL);
        int start = Math.max(fromLevelInclusive, highestRewarded + 1);

        for (int level = start; level <= toLevelInclusive; level++) {
            List<PlayerLevelReward> rewards = PlayerLevelRewardManager.getRewardsForLevel(level);
            if (rewards.isEmpty()) {
                data.putInt(KEY_HIGHEST_REWARDED_LEVEL, level);
                continue;
            }

            for (PlayerLevelReward reward : rewards) {
                reward.grant(player);
            }

            data.putInt(KEY_HIGHEST_REWARDED_LEVEL, level);
        }
    }
}
