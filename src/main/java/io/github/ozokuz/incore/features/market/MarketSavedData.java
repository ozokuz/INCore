package io.github.ozokuz.incore.features.market;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MarketSavedData extends SavedData {
    private static final String DATA_NAME = "incore_market";

    private final Map<ResourceLocation, ItemState> states = new HashMap<>();
    private long lastProcessedHourKey = Long.MIN_VALUE;
    private long lastProcessedNoonDayKey = Long.MIN_VALUE;

    public static MarketSavedData get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(MarketSavedData::new, MarketSavedData::load, DataFixTypes.LEVEL),
                DATA_NAME
        );
    }

    public static MarketSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        MarketSavedData data = new MarketSavedData();
        data.lastProcessedHourKey = tag.contains("lastProcessedHourKey", Tag.TAG_LONG)
                ? tag.getLong("lastProcessedHourKey")
                : Long.MIN_VALUE;
        data.lastProcessedNoonDayKey = tag.contains("lastProcessedNoonDayKey", Tag.TAG_LONG)
                ? tag.getLong("lastProcessedNoonDayKey")
                : Long.MIN_VALUE;

        ListTag stateList = tag.getList("states", Tag.TAG_COMPOUND);
        for (Tag stateTag : stateList) {
            CompoundTag row = (CompoundTag) stateTag;
            ResourceLocation itemId = ResourceLocation.tryParse(row.getString("item"));
            if (itemId == null) {
                continue;
            }

            ItemState state = new ItemState();
            state.demandIndex = row.getDouble("demandIndex");
            state.currentPrice = Math.max(1, row.getInt("currentPrice"));

            ListTag candlesTag = row.getList("candles", Tag.TAG_COMPOUND);
            for (Tag candleTag : candlesTag) {
                CompoundTag candleRow = (CompoundTag) candleTag;
                state.candles.add(new PriceCandle(
                        candleRow.getLong("hourKey"),
                        Math.max(1, candleRow.getInt("open")),
                        Math.max(1, candleRow.getInt("high")),
                        Math.max(1, candleRow.getInt("low")),
                        Math.max(1, candleRow.getInt("close")),
                        Math.max(0, candleRow.getInt("buyVolume")),
                        Math.max(0, candleRow.getInt("sellVolume"))
                ));
            }

            data.states.put(itemId, state);
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLong("lastProcessedHourKey", lastProcessedHourKey);
        tag.putLong("lastProcessedNoonDayKey", lastProcessedNoonDayKey);

        ListTag stateList = new ListTag();
        for (Map.Entry<ResourceLocation, ItemState> entry : states.entrySet()) {
            CompoundTag row = new CompoundTag();
            row.putString("item", entry.getKey().toString());
            row.putDouble("demandIndex", entry.getValue().demandIndex);
            row.putInt("currentPrice", Math.max(1, entry.getValue().currentPrice));

            ListTag candlesTag = new ListTag();
            for (PriceCandle candle : entry.getValue().candles) {
                CompoundTag candleRow = new CompoundTag();
                candleRow.putLong("hourKey", candle.hourKey());
                candleRow.putInt("open", candle.open());
                candleRow.putInt("high", candle.high());
                candleRow.putInt("low", candle.low());
                candleRow.putInt("close", candle.close());
                candleRow.putInt("buyVolume", candle.buyVolume());
                candleRow.putInt("sellVolume", candle.sellVolume());
                candlesTag.add(candleRow);
            }
            row.put("candles", candlesTag);
            stateList.add(row);
        }
        tag.put("states", stateList);
        return tag;
    }

    public ItemState stateFor(ResourceLocation itemId, int fallbackPrice) {
        return states.computeIfAbsent(itemId, id -> {
            ItemState state = new ItemState();
            state.currentPrice = Math.max(1, fallbackPrice);
            return state;
        });
    }

    public Map<ResourceLocation, ItemState> states() {
        return states;
    }

    public long lastProcessedHourKey() {
        return lastProcessedHourKey;
    }

    public void setLastProcessedHourKey(long key) {
        if (this.lastProcessedHourKey != key) {
            this.lastProcessedHourKey = key;
            setDirty();
        }
    }

    public long lastProcessedNoonDayKey() {
        return lastProcessedNoonDayKey;
    }

    public void setLastProcessedNoonDayKey(long key) {
        if (this.lastProcessedNoonDayKey != key) {
            this.lastProcessedNoonDayKey = key;
            setDirty();
        }
    }

    public static final class ItemState {
        private double demandIndex;
        private int currentPrice = 1;
        private final List<PriceCandle> candles = new ArrayList<>();

        public double demandIndex() {
            return demandIndex;
        }

        public void setDemandIndex(double demandIndex) {
            this.demandIndex = demandIndex;
        }

        public int currentPrice() {
            return currentPrice;
        }

        public void setCurrentPrice(int currentPrice) {
            this.currentPrice = Math.max(1, currentPrice);
        }

        public List<PriceCandle> candles() {
            return candles;
        }
    }

    public record PriceCandle(
            long hourKey,
            int open,
            int high,
            int low,
            int close,
            int buyVolume,
            int sellVolume
    ) {
        public PriceCandle {
            open = Math.max(1, open);
            high = Math.max(open, high);
            low = Math.max(1, Math.min(low, high));
            close = Math.max(1, close);
            buyVolume = Math.max(0, buyVolume);
            sellVolume = Math.max(0, sellVolume);
        }

        public PriceCandle withClose(int nextClose) {
            int closePrice = Math.max(1, nextClose);
            return new PriceCandle(
                    hourKey,
                    open,
                    Math.max(high, closePrice),
                    Math.min(low, closePrice),
                    closePrice,
                    buyVolume,
                    sellVolume
            );
        }

        public PriceCandle withTrade(int closePrice, int buys, int sells) {
            int normalizedClose = Math.max(1, closePrice);
            return new PriceCandle(
                    hourKey,
                    open,
                    Math.max(high, normalizedClose),
                    Math.min(low, normalizedClose),
                    normalizedClose,
                    buyVolume + Math.max(0, buys),
                    sellVolume + Math.max(0, sells)
            );
        }
    }
}
