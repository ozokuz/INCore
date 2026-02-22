package io.github.ozokuz.incore.features.market;

import dev.ithundxr.createnumismatics.content.backend.BankAccount;
import io.github.ozokuz.incore.features.market.content.MarketTerminalBlockEntity;
import io.github.ozokuz.incore.features.market.network.MarketNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class MarketService {
    private MarketService() {
    }

    public static void onServerTick(MinecraftServer server) {
        MarketPricingService.tick(server);
    }

    public static void openReadOnlyScreen(ServerPlayer player) {
        if (player.getServer() == null) {
            return;
        }
        MarketNetworking.openMarketScreen(player, buildScreenData(player.getServer(), false, null));
    }

    public static void openTerminalScreen(ServerPlayer player, MarketTerminalBlockEntity terminal) {
        if (player.getServer() == null) {
            return;
        }
        boolean canTrade = terminal.canTrade(player);
        BlockPos pos = terminal.getBlockPos();
        MarketNetworking.openMarketScreen(player, buildScreenData(player.getServer(), canTrade, pos));
    }

    public static void requestRefresh(ServerPlayer player, BlockPos terminalPos) {
        if (player.getServer() == null) {
            return;
        }

        MarketTerminalBlockEntity terminal = terminalAt(player, terminalPos);
        boolean canTrade = terminal != null && terminal.canTrade(player);
        MarketNetworking.openMarketScreen(player, buildScreenData(player.getServer(), canTrade, terminal == null ? null : terminalPos));
    }

    public static boolean buyFromMarket(ServerPlayer player, BlockPos terminalPos, ResourceLocation itemId, int quantity) {
        if (player.getServer() == null) {
            return false;
        }

        MarketTerminalBlockEntity terminal = terminalAt(player, terminalPos);
        if (terminal == null || !terminal.canTrade(player)) {
            player.sendSystemMessage(Component.translatable("incore.market.not_allowed"));
            return false;
        }

        MarketItemDefinition definition = MarketItemManager.get(itemId);
        if (definition == null) {
            return false;
        }

        int qty = Math.max(1, quantity);
        int unitPrice = MarketPricingService.currentPrice(player.getServer(), itemId);
        if (unitPrice <= 0) {
            return false;
        }

        long costLong = (long) unitPrice * qty;
        int cost = (int) Math.min(Integer.MAX_VALUE, costLong);

        ItemStack card = terminal.cardStack();
        BankAccount account = MarketBanking.resolveManualAccount(player, card);
        if (account == null) {
            player.sendSystemMessage(Component.translatable("incore.market.no_account"));
            return false;
        }
        if (!MarketBanking.withdraw(account, cost)) {
            player.sendSystemMessage(Component.translatable("incore.market.insufficient_funds"));
            return false;
        }

        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item == null || item == net.minecraft.world.item.Items.AIR) {
            return false;
        }

        ItemStack stack = new ItemStack(item, qty);
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }

        MarketPricingService.applyBuy(player.getServer(), itemId, qty);
        return true;
    }

    public static boolean sellToMarket(ServerPlayer player, BlockPos terminalPos, ResourceLocation itemId, int quantity) {
        if (player.getServer() == null) {
            return false;
        }

        MarketTerminalBlockEntity terminal = terminalAt(player, terminalPos);
        if (terminal == null || !terminal.canTrade(player)) {
            player.sendSystemMessage(Component.translatable("incore.market.not_allowed"));
            return false;
        }

        MarketItemDefinition definition = MarketItemManager.get(itemId);
        if (definition == null) {
            return false;
        }

        int qty = Math.max(1, quantity);
        int available = countInInventory(player, itemId);
        if (available <= 0) {
            player.sendSystemMessage(Component.translatable("incore.market.no_items_to_sell"));
            return false;
        }

        int selling = Math.min(available, qty);
        int unitPrice = MarketPricingService.currentPrice(player.getServer(), itemId);
        if (unitPrice <= 0) {
            return false;
        }

        ItemStack card = terminal.cardStack();
        BankAccount account = MarketBanking.resolveManualAccount(player, card);
        if (account == null) {
            player.sendSystemMessage(Component.translatable("incore.market.no_account"));
            return false;
        }

        if (!removeItems(player, itemId, selling)) {
            return false;
        }

        long payoutLong = (long) unitPrice * selling;
        int payout = (int) Math.min(Integer.MAX_VALUE, payoutLong);
        MarketBanking.deposit(account, payout);
        MarketPricingService.applySell(player.getServer(), itemId, selling);
        return true;
    }

    private static MarketTerminalBlockEntity terminalAt(ServerPlayer player, BlockPos pos) {
        if (pos == null) {
            return null;
        }
        if (!(player.level().getBlockEntity(pos) instanceof MarketTerminalBlockEntity terminal)) {
            return null;
        }
        return terminal;
    }

    private static int countInInventory(ServerPlayer player, ResourceLocation itemId) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.isEmpty()) {
                continue;
            }
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (itemId.equals(id)) {
                total += stack.getCount();
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.isEmpty()) {
                continue;
            }
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (itemId.equals(id)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static boolean removeItems(ServerPlayer player, ResourceLocation itemId, int amount) {
        int remaining = amount;
        for (ItemStack stack : player.getInventory().items) {
            if (remaining <= 0) {
                break;
            }
            if (stack.isEmpty()) {
                continue;
            }
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (!itemId.equals(id)) {
                continue;
            }
            int remove = Math.min(remaining, stack.getCount());
            stack.shrink(remove);
            remaining -= remove;
        }

        for (ItemStack stack : player.getInventory().offhand) {
            if (remaining <= 0) {
                break;
            }
            if (stack.isEmpty()) {
                continue;
            }
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (!itemId.equals(id)) {
                continue;
            }
            int remove = Math.min(remaining, stack.getCount());
            stack.shrink(remove);
            remaining -= remove;
        }

        return remaining <= 0;
    }

    public static ScreenData buildScreenData(MinecraftServer server, boolean canTrade, BlockPos terminalPos) {
        MarketPricingService.tick(server);

        List<ItemView> items = new ArrayList<>();
        for (MarketItemDefinition definition : MarketItemManager.all()) {
            int price = MarketPricingService.currentPrice(server, definition.itemId());
            List<MarketSavedData.PriceCandle> sourceCandles = MarketPricingService.candles(server, definition.itemId());
            List<CandleView> candles = sourceCandles.stream()
                    .map(c -> new CandleView(c.hourKey(), c.open(), c.high(), c.low(), c.close(), c.buyVolume(), c.sellVolume()))
                    .toList();
            double dayChangePercent = dayChangePercent(sourceCandles, price);

            double demand = MarketSavedData.get(server)
                    .stateFor(definition.itemId(), definition.basePriceSpur())
                    .demandIndex();

            items.add(new ItemView(
                    definition.itemId().toString(),
                    definition.displayName(),
                    definition.basePriceSpur(),
                    price,
                    dayChangePercent,
                    demand,
                    candles
            ));
        }

        return new ScreenData(
                canTrade,
                terminalPos == null ? null : terminalPos.asLong(),
                items
        );
    }

    private static double dayChangePercent(List<MarketSavedData.PriceCandle> candles, int fallbackPrice) {
        if (candles == null || candles.isEmpty()) {
            return 0D;
        }

        MarketSavedData.PriceCandle latest = candles.getLast();
        int latestClose = Math.max(1, latest.close());
        long targetHourKey = latest.hourKey() - 24L;

        int baseline = Math.max(1, candles.getFirst().close());
        for (int i = candles.size() - 1; i >= 0; i--) {
            MarketSavedData.PriceCandle candle = candles.get(i);
            if (candle.hourKey() <= targetHourKey) {
                baseline = Math.max(1, candle.close());
                break;
            }
        }

        if (baseline <= 0) {
            baseline = Math.max(1, fallbackPrice);
        }

        return ((latestClose - baseline) * 100D) / baseline;
    }

    public record ScreenData(
            boolean canTrade,
            Long terminalPos,
            List<ItemView> items
    ) {
    }

    public record ItemView(
            String itemId,
            String displayName,
            int basePriceSpur,
            int currentPriceSpur,
            double dayChangePercent,
            double demandIndex,
            List<CandleView> candles
    ) {
    }

    public record CandleView(
            long hourKey,
            int open,
            int high,
            int low,
            int close,
            int buyVolume,
            int sellVolume
    ) {
    }
}
