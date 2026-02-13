package io.github.ozokuz.incore.features.sanity;

public final class SanityClientCache {
    private static final long MILLIS_PER_MINUTE = 60_000L;

    private static int current;
    private static int cap;
    private static int regenPerMinute;
    private static long millisUntilNextIncrease;
    private static long millisUntilFull;
    private static long receivedAtMs;

    private SanityClientCache() {
    }

    public static synchronized void update(
            int nextCurrent,
            int nextCap,
            int nextRegenPerMinute,
            long nextMillisUntilNextIncrease,
            long nextMillisUntilFull
    ) {
        current = Math.max(0, nextCurrent);
        cap = Math.max(0, nextCap);
        regenPerMinute = Math.max(0, nextRegenPerMinute);
        millisUntilNextIncrease = nextMillisUntilNextIncrease;
        millisUntilFull = nextMillisUntilFull;
        receivedAtMs = System.currentTimeMillis();
    }

    public static synchronized int getCurrent() {
        if (cap <= 0) {
            return 0;
        }

        if (regenPerMinute <= 0 || current >= cap) {
            return Math.min(current, cap);
        }

        long elapsedMinutes = Math.max(0L, (System.currentTimeMillis() - receivedAtMs) / MILLIS_PER_MINUTE);
        long estimated = (long) current + elapsedMinutes * regenPerMinute;
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
