package io.github.ozokuz.incore.features.entropy;

public final class EntropyClientCache {
    private static int current;
    private static int cap;
    private static int regenPerTick;
    private static long regenIntervalMillis;
    private static long millisUntilNextIncrease;
    private static long millisUntilFull;
    private static long receivedAtMs;
    private static long boosterAnimationToken;
    private static int boosterAnimationFrom;
    private static int boosterAnimationTo;
    private static int boosterAnimationCap = 1;
    private static int boosterAnimationGain;

    private EntropyClientCache() {
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

    public static synchronized int getSyncedCurrent() {
        return current;
    }

    public static synchronized int getCap() {
        return cap;
    }

    public static synchronized long getReceivedAtMs() {
        return receivedAtMs;
    }

    public static synchronized void recordBoosterGainAnimation(int from, int to, int capValue, int gain) {
        int normalizedCap = Math.max(1, capValue);
        int normalizedFrom = Math.clamp(from, 0, normalizedCap);
        int normalizedTo = Math.clamp(to, normalizedFrom, normalizedCap);
        int normalizedGain = Math.max(0, gain);
        if (normalizedGain <= 0 || normalizedTo <= normalizedFrom) {
            return;
        }

        boosterAnimationFrom = normalizedFrom;
        boosterAnimationTo = normalizedTo;
        boosterAnimationCap = normalizedCap;
        boosterAnimationGain = normalizedGain;
        boosterAnimationToken++;
    }

    public static synchronized long getBoosterAnimationToken() {
        return boosterAnimationToken;
    }

    public static synchronized int getBoosterAnimationFrom() {
        return boosterAnimationFrom;
    }

    public static synchronized int getBoosterAnimationTo() {
        return boosterAnimationTo;
    }

    public static synchronized int getBoosterAnimationCap() {
        return boosterAnimationCap;
    }

    public static synchronized int getBoosterAnimationGain() {
        return boosterAnimationGain;
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
