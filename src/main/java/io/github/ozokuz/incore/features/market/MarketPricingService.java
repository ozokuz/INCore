package io.github.ozokuz.incore.features.market;

import io.github.ozokuz.incore.Config;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class MarketPricingService {
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

        if (data.lastProcessedHourKey() != hourKey) {
            rolloverHour(data, hourKey);
            data.setLastProcessedHourKey(hourKey);
        }

        if (data.lastProcessedNoonDayKey() != noonDayKey) {
            applyDailyMeanReversion(data, hourKey);
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

        rolloverHour(data, hourKey);
        applyDailyMeanReversion(data, hourKey);
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

    private static void applyDailyMeanReversion(MarketSavedData data, long hourKey) {
        double reversion = Math.clamp(Config.MARKET_DAILY_MEAN_REVERSION.get(), 0D, 1D);
        for (MarketItemDefinition definition : MarketItemManager.all()) {
            MarketSavedData.ItemState state = data.stateFor(definition.itemId(), definition.basePriceSpur());
            double nextDemand = state.demandIndex() * (1D - reversion);
            state.setDemandIndex(nextDemand);
            state.setCurrentPrice(priceFromDemand(definition, nextDemand));
            updateLatestCandle(state, hourKey, state.currentPrice(), 0, 0);
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
        double rawMultiplier = 1D + demand;
        double minMultiplier = Math.max(0.01D, Config.MARKET_MIN_PRICE_MULTIPLIER.get());
        double maxMultiplier = Math.max(minMultiplier, Config.MARKET_MAX_PRICE_MULTIPLIER.get());
        double clampedMultiplier = Math.clamp(rawMultiplier, minMultiplier, maxMultiplier);
        return Math.max(1, (int) Math.round(definition.basePriceSpur() * clampedMultiplier));
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
