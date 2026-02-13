package io.github.ozokuz.incore.features.sanity;

public final class SanityClientCache {
    private static int current;
    private static int cap;
    private static int regenPerTick;
    private static long regenIntervalMillis;
    private static long millisUntilNextIncrease;
    private static long millisUntilFull;
    private static long receivedAtMs;

    private SanityClientCache() {
    }

    public static synchronized void update(
            int nextCurrent,
            int nextCap,
            int nextRegenPerTick,
            long nextRegenIntervalMillis,
            long nextMillisUntilNextIncrease,
            long nextMillisUntilFull
    ) {
        current = Math.max(0, nextCurrent);
        cap = Math.max(0, nextCap);
        regenPerTick = Math.max(0, nextRegenPerTick);
        regenIntervalMillis = Math.max(1L, nextRegenIntervalMillis);
        millisUntilNextIncrease = nextMillisUntilNextIncrease;
        millisUntilFull = nextMillisUntilFull;
        receivedAtMs = System.currentTimeMillis();
    }

    public static synchronized int getCurrent() {
        if (cap <= 0) {
            return 0;
        }

        if (regenPerTick <= 0 || current >= cap) {
            return Math.min(current, cap);
        }

        long elapsedTicks = Math.max(0L, (System.currentTimeMillis() - receivedAtMs) / regenIntervalMillis);
        long estimated = (long) current + elapsedTicks * regenPerTick;
        return (int) Math.min(cap, estimated);
    }

    public static synchronized int getCap() {
        return cap;
    }

    public static synchronized long getMillisUntilNextIncrease() {
        if (millisUntilNextIncrease < 0L) {
            return -1L;
        }

        long elapsed = Math.max(0L, System.currentTimeMillis() - receivedAtMs);
        return Math.max(0L, millisUntilNextIncrease - elapsed);
    }

    public static synchronized long getMillisUntilFull() {
        if (millisUntilFull < 0L) {
            return -1L;
        }

        long elapsed = Math.max(0L, System.currentTimeMillis() - receivedAtMs);
        return Math.max(0L, millisUntilFull - elapsed);
    }
}
