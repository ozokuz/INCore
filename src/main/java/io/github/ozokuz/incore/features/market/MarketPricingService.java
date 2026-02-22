package io.github.ozokuz.incore.features.market;

import io.github.ozokuz.incore.Config;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class MarketPricingService {
    private static final long NOON_SALT = 0x4E4F4F4E5F53414CL;
    private static final long BOOTSTRAP_SALT = 0x424F4F5453545241L;
    private static final double NORMAL_MOVE_LOWER_END_EXPONENT = 1.85D;

    private MarketPricingService() {
    }

    public static void tick(MinecraftServer server) {
        if (server == null) {
            return;
        }

        MarketSavedData data = MarketSavedData.get(server);
        ensureAllStates(data);

        ZonedDateTime now = MarketTime.now(server);
        long hourKey = MarketTime.hourKey(now);
        long noonDayKey = MarketTime.noonDayKey(now);
        bootstrapMissingHistory(server, data, hourKey);

        if (data.lastProcessedHourKey() != hourKey) {
            rolloverHour(data, hourKey);
            data.setLastProcessedHourKey(hourKey);
        }

        if (data.lastProcessedNoonDayKey() != noonDayKey) {
            applyDailyNoonUpdate(server, data, hourKey, noonDayKey);
            data.setLastProcessedNoonDayKey(noonDayKey);
        }
    }

    public static TradeResult applyBuy(MinecraftServer server, ResourceLocation itemId, int quantity) {
        return applyTrade(server, itemId, Math.max(1, quantity), true);
    }

    public static TradeResult applySell(MinecraftServer server, ResourceLocation itemId, int quantity) {
        return applyTrade(server, itemId, Math.max(1, quantity), false);
    }

    public static int currentPrice(MinecraftServer server, ResourceLocation itemId) {
        MarketItemDefinition definition = MarketItemManager.get(itemId);
        if (definition == null) {
            return -1;
        }

        MarketSavedData data = MarketSavedData.get(server);
        MarketSavedData.ItemState state = data.stateFor(itemId, definition.basePriceSpur());
        return Math.max(1, state.currentPrice());
    }

    public static List<MarketSavedData.PriceCandle> candles(MinecraftServer server, ResourceLocation itemId) {
        MarketItemDefinition definition = MarketItemManager.get(itemId);
        if (definition == null) {
            return List.of();
        }

        MarketSavedData.ItemState state = MarketSavedData.get(server).stateFor(itemId, definition.basePriceSpur());
        return List.copyOf(state.candles());
    }

    public static void forceRollover(MinecraftServer server) {
        MarketSavedData data = MarketSavedData.get(server);
        ensureAllStates(data);

        ZonedDateTime now = MarketTime.now(server);
        long hourKey = MarketTime.hourKey(now);
        long noonDayKey = MarketTime.noonDayKey(now);
        bootstrapMissingHistory(server, data, hourKey);

        rolloverHour(data, hourKey);
        applyDailyNoonUpdate(server, data, hourKey, noonDayKey);
        data.setLastProcessedHourKey(hourKey);
        data.setLastProcessedNoonDayKey(noonDayKey);
    }

    public static boolean setDemand(MinecraftServer server, ResourceLocation itemId, double demand) {
        MarketItemDefinition definition = MarketItemManager.get(itemId);
        if (definition == null) {
            return false;
        }

        MarketSavedData data = MarketSavedData.get(server);
        MarketSavedData.ItemState state = data.stateFor(itemId, definition.basePriceSpur());
        state.setDemandIndex(demand);
        state.setCurrentPrice(priceFromDemand(definition, demand));
        long hourKey = Math.max(data.lastProcessedHourKey(), MarketTime.hourKey(MarketTime.now(server)));
        updateLatestCandle(state, hourKey, state.currentPrice(), 0, 0);
        data.setDirty();
        return true;
    }

    private static TradeResult applyTrade(MinecraftServer server, ResourceLocation itemId, int quantity, boolean buy) {
        MarketItemDefinition definition = MarketItemManager.get(itemId);
        if (definition == null) {
            return TradeResult.invalid();
        }

        tick(server);
        MarketSavedData data = MarketSavedData.get(server);
        MarketSavedData.ItemState state = data.stateFor(itemId, definition.basePriceSpur());

        double impactPerItem = buy
                ? Config.MARKET_BUY_IMPACT_PER_ITEM.get()
                : Config.MARKET_SELL_IMPACT_PER_ITEM.get();
        double signedImpact = impactPerItem * quantity * definition.volatilityWeight() * (buy ? 1D : -1D);

        double nextDemand = state.demandIndex() + signedImpact;
        int nextPrice = priceFromDemand(definition, nextDemand);
        state.setDemandIndex(nextDemand);
        state.setCurrentPrice(nextPrice);

        long hourKey = data.lastProcessedHourKey();
        updateLatestCandle(state, hourKey, nextPrice, buy ? quantity : 0, buy ? 0 : quantity);

        data.setDirty();
        return new TradeResult(nextPrice, nextDemand);
    }

    private static void ensureAllStates(MarketSavedData data) {
        for (MarketItemDefinition definition : MarketItemManager.all()) {
            data.stateFor(definition.itemId(), definition.basePriceSpur());
        }
    }

    private static void bootstrapMissingHistory(MinecraftServer server, MarketSavedData data, long hourKey) {
        int retentionHours = Math.max(24, Config.MARKET_HISTORY_RETENTION_DAYS.get() * 24);
        boolean changed = false;

        for (MarketItemDefinition definition : MarketItemManager.all()) {
            MarketSavedData.ItemState state = data.stateFor(definition.itemId(), definition.basePriceSpur());
            if (!state.candles().isEmpty()) {
                continue;
            }

            long startHour = hourKey - retentionHours + 1L;
            Random random = deterministicRandom(server, startHour, definition.itemId(), BOOTSTRAP_SALT);
            state.candles().clear();

            int close = Math.max(1, definition.basePriceSpur());
            double runningMultiplier = clampMultiplier(priceMultiplier(definition, close));
            double hourlyNoise = Math.max(0D, Config.MARKET_BOOTSTRAP_HOURLY_NOISE_PCT.get());
            double wickNoise = Math.max(0D, Config.MARKET_BOOTSTRAP_WICK_NOISE_PCT.get());
            double volatility = Math.max(0.1D, definition.volatilityWeight());

            for (long key = startHour; key <= hourKey; key++) {
                int open = close;

                double maxMove = Math.min(0.95D, hourlyNoise * volatility);
                double delta = randomBetween(random, -maxMove, maxMove);
                runningMultiplier = clampMultiplier(runningMultiplier * (1D + delta));
                int nextClose = priceFromMultiplier(definition, runningMultiplier);
                nextClose = applyLowPriceBootstrapTick(definition, open, nextClose, delta, random);

                int high = Math.max(open, nextClose);
                int low = Math.min(open, nextClose);

                if (wickNoise > 0D) {
                    double highPct = randomBetween(random, 0D, wickNoise);
                    double lowPct = randomBetween(random, 0D, wickNoise);
                    high = Math.max(high, priceFromMultiplier(definition, priceMultiplier(definition, high) * (1D + highPct)));
                    low = Math.min(low, priceFromMultiplier(definition, priceMultiplier(definition, low) * (1D - lowPct)));
                }

                state.candles().add(new MarketSavedData.PriceCandle(key, open, high, low, nextClose, 0, 0));
                close = nextClose;
            }
            enforceNonFlatBootstrapHistory(definition, state.candles(), random);
            close = state.candles().isEmpty() ? Math.max(1, definition.basePriceSpur()) : state.candles().getLast().close();

            state.setCurrentPrice(close);
            state.setDemandIndex(demandFromMultiplier(clampMultiplier(priceMultiplier(definition, close))));
            changed = true;
        }

        if (changed) {
            data.setDirty();
        }
    }

    private static void applyDailyNoonUpdate(MinecraftServer server, MarketSavedData data, long hourKey, long noonDayKey) {
        double smallChance = Math.clamp(Config.MARKET_DAILY_SMALL_REVERSION_CHANCE.get(), 0D, 1D);
        double normalChance = Math.clamp(Config.MARKET_DAILY_NORMAL_MOVE_CHANCE.get(), 0D, 1D);
        double radicalChance = Math.clamp(Config.MARKET_DAILY_RADICAL_CHANCE.get(), 0D, 1D);
        double totalChance = smallChance + normalChance + radicalChance;
        if (totalChance <= 0D) {
            smallChance = 0D;
            normalChance = 1D;
            radicalChance = 0D;
            totalChance = 1D;
        }

        double smallThreshold = smallChance;
        double normalThreshold = smallChance + normalChance;
        double reversion = Math.clamp(Config.MARKET_DAILY_MEAN_REVERSION.get(), 0D, 1D);
        double normalMaxChange = Math.max(0D, Config.MARKET_DAILY_NORMAL_MAX_CHANGE_PCT.get());
        double crashBias = Math.clamp(Config.MARKET_DAILY_RADICAL_CRASH_VS_SPIKE_BIAS.get(), 0D, 1D);

        double radicalSpikeMin = Math.max(0D, Config.MARKET_DAILY_RADICAL_SPIKE_MIN_PCT.get());
        double radicalSpikeMax = Math.max(radicalSpikeMin, Config.MARKET_DAILY_RADICAL_SPIKE_MAX_PCT.get());
        double radicalCrashMin = Math.max(0D, Config.MARKET_DAILY_RADICAL_CRASH_MIN_PCT.get());
        double radicalCrashMax = Math.max(radicalCrashMin, Config.MARKET_DAILY_RADICAL_CRASH_MAX_PCT.get());

        for (MarketItemDefinition definition : MarketItemManager.all()) {
            MarketSavedData.ItemState state = data.stateFor(definition.itemId(), definition.basePriceSpur());
            Random random = deterministicRandom(server, noonDayKey, definition.itemId(), NOON_SALT);
            double roll = random.nextDouble() * totalChance;

            double currentMultiplier = clampMultiplier(priceMultiplier(definition, state.currentPrice()));
            double nextMultiplier;

            if (roll < smallThreshold) {
                double nextDemand = state.demandIndex() * (1D - reversion);
                nextMultiplier = 1D + nextDemand;
            } else if (roll < normalThreshold) {
                double normalDelta = sampleLowerEndHeavyNormalDelta(random, normalMaxChange);
                nextMultiplier = currentMultiplier * (1D + normalDelta);
            } else if (random.nextDouble() < crashBias) {
                double crashPct = randomBetween(random, radicalCrashMin, radicalCrashMax);
                nextMultiplier = currentMultiplier * Math.max(0.01D, 1D - crashPct);
            } else {
                double spikePct = randomBetween(random, radicalSpikeMin, radicalSpikeMax);
                nextMultiplier = currentMultiplier * (1D + spikePct);
            }

            int nextPrice = priceFromMultiplier(definition, nextMultiplier);
            double nextDemand = demandFromMultiplier(clampMultiplier(priceMultiplier(definition, nextPrice)));
            state.setDemandIndex(nextDemand);
            state.setCurrentPrice(nextPrice);
            updateLatestCandle(state, hourKey, nextPrice, 0, 0);
        }
        data.setDirty();
    }

    private static void rolloverHour(MarketSavedData data, long hourKey) {
        int retentionHours = Math.max(24, Config.MARKET_HISTORY_RETENTION_DAYS.get() * 24);
        for (Map.Entry<ResourceLocation, MarketSavedData.ItemState> entry : data.states().entrySet()) {
            MarketItemDefinition definition = MarketItemManager.get(entry.getKey());
            if (definition == null) {
                continue;
            }
            MarketSavedData.ItemState state = entry.getValue();
            ensureCandleHour(state, hourKey, Math.max(1, state.currentPrice()));
            trimHistory(state, retentionHours);
        }
        data.setDirty();
    }

    private static int priceFromDemand(MarketItemDefinition definition, double demand) {
        return priceFromMultiplier(definition, 1D + demand);
    }

    private static int priceFromMultiplier(MarketItemDefinition definition, double multiplier) {
        double clampedMultiplier = clampMultiplier(multiplier);
        return Math.max(1, (int) Math.round(definition.basePriceSpur() * clampedMultiplier));
    }

    private static int clampPrice(MarketItemDefinition definition, int rawPrice) {
        return priceFromMultiplier(definition, priceMultiplier(definition, Math.max(1, rawPrice)));
    }

    private static double priceMultiplier(MarketItemDefinition definition, int price) {
        int base = Math.max(1, definition.basePriceSpur());
        return Math.max(0.0001D, (double) Math.max(1, price) / base);
    }

    private static double demandFromMultiplier(double multiplier) {
        return clampMultiplier(multiplier) - 1D;
    }

    private static double clampMultiplier(double rawMultiplier) {
        double minMultiplier = Math.max(0.01D, Config.MARKET_MIN_PRICE_MULTIPLIER.get());
        double maxMultiplier = Math.max(minMultiplier, Config.MARKET_MAX_PRICE_MULTIPLIER.get());
        return Math.clamp(rawMultiplier, minMultiplier, maxMultiplier);
    }

    private static Random deterministicRandom(MinecraftServer server, long key, ResourceLocation itemId, long salt) {
        long worldSeed = server.overworld() == null ? 0L : server.overworld().getSeed();
        long seed = worldSeed ^ mix64(key) ^ mix64(itemId.hashCode()) ^ salt;
        return new Random(mix64(seed));
    }

    private static int applyLowPriceBootstrapTick(
            MarketItemDefinition definition,
            int open,
            int roundedClose,
            double delta,
            Random random
    ) {
        if (roundedClose != open) {
            return roundedClose;
        }

        double expectedStep = Math.abs(delta) * Math.max(1, open);
        if (expectedStep <= 0D) {
            return roundedClose;
        }
        if (random.nextDouble() >= Math.min(1D, expectedStep)) {
            return roundedClose;
        }

        int direction = delta >= 0D ? 1 : -1;
        int nudged = clampPrice(definition, open + direction);
        if (nudged == open) {
            nudged = clampPrice(definition, open - direction);
        }
        return nudged;
    }

    private static void enforceNonFlatBootstrapHistory(
            MarketItemDefinition definition,
            List<MarketSavedData.PriceCandle> candles,
            Random random
    ) {
        if (!isFlatCloseSeries(candles) || candles.size() < 2) {
            return;
        }

        int pivotIndex = 1 + random.nextInt(candles.size() - 1);
        int direction = random.nextBoolean() ? 1 : -1;
        for (int i = pivotIndex; i < candles.size(); i++) {
            MarketSavedData.PriceCandle previous = candles.get(i - 1);
            MarketSavedData.PriceCandle current = candles.get(i);

            int open = previous.close();
            int close = current.close();
            if (i == pivotIndex) {
                close = clampPrice(definition, open + direction);
                if (close == open) {
                    close = clampPrice(definition, open - direction);
                }
            }

            int high = Math.max(open, close);
            int low = Math.min(open, close);
            candles.set(i, new MarketSavedData.PriceCandle(
                    current.hourKey(),
                    open,
                    high,
                    low,
                    close,
                    current.buyVolume(),
                    current.sellVolume()
            ));
        }
    }

    private static boolean isFlatCloseSeries(List<MarketSavedData.PriceCandle> candles) {
        if (candles == null || candles.size() < 2) {
            return true;
        }

        int firstClose = candles.getFirst().close();
        for (int i = 1; i < candles.size(); i++) {
            if (candles.get(i).close() != firstClose) {
                return false;
            }
        }
        return true;
    }

    private static double randomBetween(Random random, double min, double max) {
        if (max <= min) {
            return min;
        }
        return min + (max - min) * random.nextDouble();
    }

    private static double sampleLowerEndHeavyNormalDelta(Random random, double maxAbsChange) {
        if (maxAbsChange <= 0D) {
            return 0D;
        }

        // Skew toward smaller magnitude while keeping up/down direction unbiased.
        double magnitude = maxAbsChange * Math.pow(random.nextDouble(), NORMAL_MOVE_LOWER_END_EXPONENT);
        return random.nextBoolean() ? -magnitude : magnitude;
    }

    private static long mix64(long value) {
        long z = value + 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    private static void ensureCandleHour(MarketSavedData.ItemState state, long hourKey, int fallbackPrice) {
        List<MarketSavedData.PriceCandle> candles = state.candles();
        int startPrice = Math.max(1, fallbackPrice);

        if (candles.isEmpty()) {
            candles.add(new MarketSavedData.PriceCandle(hourKey, startPrice, startPrice, startPrice, startPrice, 0, 0));
            return;
        }

        MarketSavedData.PriceCandle last = candles.getLast();
        if (last.hourKey() == hourKey) {
            return;
        }

        int close = Math.max(1, last.close());
        long next = last.hourKey() + 1L;
        while (next <= hourKey) {
            candles.add(new MarketSavedData.PriceCandle(next, close, close, close, close, 0, 0));
            next++;
        }
    }

    private static void updateLatestCandle(MarketSavedData.ItemState state, long hourKey, int closePrice, int buyVolume, int sellVolume) {
        ensureCandleHour(state, hourKey, closePrice);
        List<MarketSavedData.PriceCandle> candles = state.candles();
        if (candles.isEmpty()) {
            return;
        }

        MarketSavedData.PriceCandle last = candles.getLast();
        candles.set(candles.size() - 1, last.withTrade(closePrice, buyVolume, sellVolume));
    }

    private static void trimHistory(MarketSavedData.ItemState state, int maxHours) {
        if (state.candles().size() <= maxHours) {
            return;
        }

        int removeCount = state.candles().size() - maxHours;
        List<MarketSavedData.PriceCandle> trimmed = new ArrayList<>(state.candles().subList(removeCount, state.candles().size()));
        state.candles().clear();
        state.candles().addAll(trimmed);
    }

    public record TradeResult(int priceSpur, double demandIndex, boolean valid) {
        public static TradeResult invalid() {
            return new TradeResult(-1, 0D, false);
        }

        public TradeResult(int priceSpur, double demandIndex) {
            this(priceSpur, demandIndex, true);
        }
    }
}
