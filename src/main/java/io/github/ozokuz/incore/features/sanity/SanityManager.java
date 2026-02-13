package io.github.ozokuz.incore.features.sanity;

import io.github.ozokuz.incore.Config;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

public final class SanityManager {
    private static final String KEY_CURRENT = "incore:sanity_current";
    private static final String KEY_CAP_BONUS = "incore:sanity_cap_bonus";
    private static final String KEY_LAST_UPDATE_MS = "incore:sanity_last_update_ms";

    private SanityManager() {
    }

    public static void update(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        long now = System.currentTimeMillis();

        if (!data.contains(KEY_LAST_UPDATE_MS)) {
            data.putLong(KEY_LAST_UPDATE_MS, now);
            if (!data.contains(KEY_CURRENT)) {
                data.putInt(KEY_CURRENT, getSanityCap(player));
            }
            clampToCap(player);
            return;
        }

        int cap = getSanityCap(player);
        int current = data.getInt(KEY_CURRENT);
        long lastUpdate = data.getLong(KEY_LAST_UPDATE_MS);
        long regenIntervalMillis = getRegenIntervalMillis();

        long elapsed = Math.max(0L, now - lastUpdate);
        long elapsedTicks = elapsed / regenIntervalMillis;
        int regenPerTick = Config.SANITY_REGEN_PER_MINUTE.get();

        if (elapsedTicks > 0 && regenPerTick > 0 && current < cap) {
            long totalRegen = elapsedTicks * regenPerTick;
            current = (int) Math.min(cap, current + totalRegen);
            data.putInt(KEY_CURRENT, current);
        }

        if (current > cap) {
            data.putInt(KEY_CURRENT, cap);
        }

        if (elapsedTicks > 0) {
            data.putLong(KEY_LAST_UPDATE_MS, lastUpdate + elapsedTicks * regenIntervalMillis);
        }
    }

    public static int getCurrentSanity(ServerPlayer player) {
        update(player);
        return player.getPersistentData().getInt(KEY_CURRENT);
    }

    public static void setCurrentSanity(ServerPlayer player, int value) {
        update(player);

        CompoundTag data = player.getPersistentData();
        int cap = getSanityCap(player);
        int clamped = (int) Math.max(0L, Math.min((long) cap, value));
        data.putInt(KEY_CURRENT, clamped);
    }

    public static void addSanity(ServerPlayer player, int delta) {
        if (delta == 0) {
            return;
        }

        update(player);

        CompoundTag data = player.getPersistentData();
        int cap = getSanityCap(player);
        long next = (long) data.getInt(KEY_CURRENT) + delta;
        int clamped = (int) Math.max(0L, Math.min((long) cap, next));
        data.putInt(KEY_CURRENT, clamped);
    }

    public static int getSanityCap(ServerPlayer player) {
        int baseCap = Config.SANITY_BASE_CAP.get();
        int bonus = Math.max(0, player.getPersistentData().getInt(KEY_CAP_BONUS));
        long total = (long) baseCap + bonus;
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, total));
    }

    public static int getSanityCapBonus(ServerPlayer player) {
        return Math.max(0, player.getPersistentData().getInt(KEY_CAP_BONUS));
    }

    public static void setSanityCapBonus(ServerPlayer player, int bonus) {
        player.getPersistentData().putInt(KEY_CAP_BONUS, Math.max(0, bonus));
        clampToCap(player);
    }

    public static void addSanityCapBonus(ServerPlayer player, int extra) {
        if (extra <= 0) {
            return;
        }

        int currentBonus = getSanityCapBonus(player);
        long nextBonus = (long) currentBonus + extra;
        setSanityCapBonus(player, (int) Math.min(Integer.MAX_VALUE, nextBonus));
    }

    public static void adjustSanityCapBonus(ServerPlayer player, int delta) {
        if (delta == 0) {
            return;
        }

        int currentBonus = getSanityCapBonus(player);
        long next = (long) currentBonus + delta;
        int clamped = (int) Math.max(0L, Math.min((long) Integer.MAX_VALUE, next));
        setSanityCapBonus(player, clamped);
    }

    public static boolean tryConsume(ServerPlayer player, int amount) {
        if (amount <= 0) {
            return true;
        }

        update(player);

        CompoundTag data = player.getPersistentData();
        int current = data.getInt(KEY_CURRENT);
        if (current < amount) {
            return false;
        }

        data.putInt(KEY_CURRENT, current - amount);
        return true;
    }

    public static long getMillisUntilNextIncrease(ServerPlayer player) {
        update(player);

        CompoundTag data = player.getPersistentData();
        int regenPerTick = Config.SANITY_REGEN_PER_MINUTE.get();
        int current = data.getInt(KEY_CURRENT);
        int cap = getSanityCap(player);

        if (current >= cap) {
            return 0L;
        }

        if (regenPerTick <= 0) {
            return -1L;
        }

        long now = System.currentTimeMillis();
        long lastUpdate = data.getLong(KEY_LAST_UPDATE_MS);
        long regenIntervalMillis = getRegenIntervalMillis();
        long elapsed = Math.max(0L, now - lastUpdate);
        long remainder = elapsed % regenIntervalMillis;

        return remainder == 0L ? regenIntervalMillis : regenIntervalMillis - remainder;
    }

    public static long getMillisUntilFull(ServerPlayer player) {
        update(player);

        CompoundTag data = player.getPersistentData();
        int regenPerTick = Config.SANITY_REGEN_PER_MINUTE.get();
        int current = data.getInt(KEY_CURRENT);
        int cap = getSanityCap(player);

        if (current >= cap) {
            return 0L;
        }

        if (regenPerTick <= 0) {
            return -1L;
        }

        int missing = cap - current;
        long ticksToFull = (missing + (long) regenPerTick - 1L) / regenPerTick;
        long nextIncrease = getMillisUntilNextIncrease(player);
        if (nextIncrease < 0L) {
            return -1L;
        }

        return nextIncrease + Math.max(0L, ticksToFull - 1L) * getRegenIntervalMillis();
    }

    public static long getRegenIntervalMillis() {
        return Math.max(1L, Config.SANITY_REGEN_INTERVAL_SECONDS.get()) * 1000L;
    }

    public static void clampToCap(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        int cap = getSanityCap(player);
        int current = data.getInt(KEY_CURRENT);

        if (current > cap) {
            data.putInt(KEY_CURRENT, cap);
        }

        if (!data.contains(KEY_LAST_UPDATE_MS)) {
            data.putLong(KEY_LAST_UPDATE_MS, System.currentTimeMillis());
        }
    }

    public static void copyData(ServerPlayer from, ServerPlayer to) {
        CompoundTag oldData = from.getPersistentData();
        CompoundTag newData = to.getPersistentData();

        if (oldData.contains(KEY_CURRENT)) {
            newData.putInt(KEY_CURRENT, oldData.getInt(KEY_CURRENT));
        }

        if (oldData.contains(KEY_CAP_BONUS)) {
            newData.putInt(KEY_CAP_BONUS, oldData.getInt(KEY_CAP_BONUS));
        }

        if (oldData.contains(KEY_LAST_UPDATE_MS)) {
            newData.putLong(KEY_LAST_UPDATE_MS, oldData.getLong(KEY_LAST_UPDATE_MS));
        }
    }
}
