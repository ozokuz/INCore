package io.github.ozokuz.incore.features.market;

import dev.ithundxr.createnumismatics.content.backend.BankAccount;
import io.github.ozokuz.incore.Config;
import io.github.ozokuz.incore.features.battlepass.BattlePassTaskHooks;
import io.github.ozokuz.incore.features.market.content.AbstractMarketTerminalBlockEntity;
import io.github.ozokuz.incore.features.market.content.MarketTerminalMeBlockEntity;
import io.github.ozokuz.incore.features.market.network.MarketNetworking;
import io.github.ozokuz.incore.integration.ae2.Ae2StorageAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MarketService {
    private static final Map<UUID, ViewerSession> ACTIVE_VIEWERS = new ConcurrentHashMap<>();

    private MarketService() {
    }

    public static void onServerTick(MinecraftServer server) {
        if (MarketPricingService.tick(server)) {
            syncActiveViewers(server);
        }
    }

    public static void openReadOnlyScreen(ServerPlayer player) {
        openReadOnlyScreen(player, null);
    }

    public static void openReadOnlyScreen(ServerPlayer player, @Nullable ResourceLocation detailItemId) {
        if (player.getServer() == null) {
            return;
        }
        MarketNetworking.openMarketScreen(player, buildScreenData(player, player.getServer(), false, null, detailItemId));
    }

    public static void openTerminalScreen(ServerPlayer player, AbstractMarketTerminalBlockEntity terminal) {
        if (player.getServer() == null) {
            return;
        }
        boolean canTrade = terminal.canTrade(player);
        BlockPos pos = terminal.getBlockPos();
        MarketNetworking.openMarketScreen(player, buildScreenData(player, player.getServer(), canTrade, pos, null));
    }

    public static void requestRefresh(ServerPlayer player, BlockPos terminalPos) {
        requestRefresh(player, terminalPos, null);
    }

    public static void requestRefresh(ServerPlayer player, BlockPos terminalPos, @Nullable ResourceLocation detailItemId) {
        if (player.getServer() == null) {
            return;
        }

        AbstractMarketTerminalBlockEntity terminal = terminalAt(player, terminalPos);
        boolean canTrade = terminal != null && terminal.canTrade(player);
        MarketNetworking.openMarketScreen(player, buildScreenData(player, player.getServer(), canTrade, terminal == null ? null : terminalPos, detailItemId));
    }

    public static void subscribeViewer(ServerPlayer player, @Nullable Long terminalPosLong, @Nullable ResourceLocation detailItemId) {
        ACTIVE_VIEWERS.put(player.getUUID(), new ViewerSession(terminalPosLong, detailItemId));
        syncViewer(player, ACTIVE_VIEWERS.get(player.getUUID()));
    }

    public static void unsubscribeViewer(ServerPlayer player) {
        ACTIVE_VIEWERS.remove(player.getUUID());
    }

    public static void syncActiveViewers(@Nullable MinecraftServer server) {
        if (server == null) {
            return;
        }

        List<UUID> staleViewers = new ArrayList<>();
        for (Map.Entry<UUID, ViewerSession> entry : ACTIVE_VIEWERS.entrySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                staleViewers.add(entry.getKey());
                continue;
            }
            syncViewer(player, entry.getValue());
        }
        for (UUID viewerId : staleViewers) {
            ACTIVE_VIEWERS.remove(viewerId);
        }
    }

    public static boolean buyFromMarket(ServerPlayer player, BlockPos terminalPos, ResourceLocation itemId, int quantity) {
        if (player.getServer() == null) {
            return false;
        }

        AbstractMarketTerminalBlockEntity terminal = terminalAt(player, terminalPos);
        if (terminal == null || !terminal.canTrade(player)) {
            player.sendSystemMessage(Component.translatable("incore.market.not_allowed"));
            return false;
        }

        if (terminal instanceof MarketTerminalMeBlockEntity meTerminal) {
            return buyFromMarketMeTerminal(player, meTerminal, itemId, quantity);
        }
        return buyFromMarketInventoryTerminal(player, terminal, itemId, quantity);
    }

    private static boolean buyFromMarketInventoryTerminal(ServerPlayer player, AbstractMarketTerminalBlockEntity terminal, ResourceLocation itemId, int quantity) {
        if (player.getServer() == null) {
            return false;
        }

        MarketItemDefinition definition = MarketItemManager.get(itemId);
        if (definition == null) {
            return false;
        }

        int stackCount = Math.max(1, quantity);
        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item == null || item == net.minecraft.world.item.Items.AIR) {
            return false;
        }
        int stackUnitSize = stackUnitSize(item);
        int totalItems = toItemCount(stackCount, stackUnitSize);
        if (totalItems <= 0) {
            return false;
        }

        int unitPrice = MarketPricingService.currentPrice(player.getServer(), itemId);
        if (unitPrice <= 0) {
            return false;
        }

        long costLong = (long) unitPrice * stackCount;
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

        giveItems(player, item, totalItems);

        MarketPricingService.applyBuy(player.getServer(), itemId, stackCount);
        BattlePassTaskHooks.onMarketBuy(player, totalItems);
        syncActiveViewers(player.getServer());
        return true;
    }

    private static boolean buyFromMarketMeTerminal(ServerPlayer player, MarketTerminalMeBlockEntity terminal, ResourceLocation itemId, int quantity) {
        if (player.getServer() == null) {
            return false;
        }

        MarketItemDefinition definition = MarketItemManager.get(itemId);
        if (definition == null) {
            return false;
        }

        int stackCount = Math.max(1, quantity);
        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item == null || item == net.minecraft.world.item.Items.AIR) {
            return false;
        }
        int stackUnitSize = stackUnitSize(item);
        int totalItems = toItemCount(stackCount, stackUnitSize);
        if (totalItems <= 0) {
            return false;
        }

        int unitPrice = MarketPricingService.currentPrice(player.getServer(), itemId);
        if (unitPrice <= 0) {
            return false;
        }

        int cost = (int) Math.min(Integer.MAX_VALUE, (long) unitPrice * stackCount);
        BankAccount account = MarketBanking.resolveManualAccount(player, terminal.cardStack());
        if (account == null) {
            player.sendSystemMessage(Component.translatable("incore.market.no_account"));
            return false;
        }
        if (!MarketBanking.withdraw(account, cost)) {
            player.sendSystemMessage(Component.translatable("incore.market.insufficient_funds"));
            return false;
        }

        ItemStack bought = new ItemStack(item, totalItems);
        if (terminal.ae2Online()) {
            var insertResult = Ae2StorageAccess.insert(terminal.grid(), terminal.actionSource(), bought);
            if (!insertResult.remainder().isEmpty()) {
                giveItems(player, item, insertResult.remainder().getCount());
            }
        } else {
            giveItems(player, item, totalItems);
        }

        MarketPricingService.applyBuy(player.getServer(), itemId, stackCount);
        BattlePassTaskHooks.onMarketBuy(player, totalItems);
        syncActiveViewers(player.getServer());
        return true;
    }

    public static boolean sellToMarket(ServerPlayer player, BlockPos terminalPos, ResourceLocation itemId, int quantity) {
        if (player.getServer() == null) {
            return false;
        }

        AbstractMarketTerminalBlockEntity terminal = terminalAt(player, terminalPos);
        if (terminal == null || !terminal.canTrade(player)) {
            player.sendSystemMessage(Component.translatable("incore.market.not_allowed"));
            return false;
        }

        if (terminal instanceof MarketTerminalMeBlockEntity meTerminal) {
            return sellToMarketMeTerminal(player, meTerminal, itemId, quantity);
        }
        return sellToMarketInventoryTerminal(player, terminal, itemId, quantity);
    }

    private static boolean sellToMarketInventoryTerminal(ServerPlayer player, AbstractMarketTerminalBlockEntity terminal, ResourceLocation itemId, int quantity) {
        if (player.getServer() == null) {
            return false;
        }

        MarketItemDefinition definition = MarketItemManager.get(itemId);
        if (definition == null) {
            return false;
        }

        int stackCount = Math.max(1, quantity);
        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item == null || item == net.minecraft.world.item.Items.AIR) {
            return false;
        }
        int stackUnitSize = stackUnitSize(item);
        int requestedItems = toItemCount(stackCount, stackUnitSize);
        if (requestedItems <= 0) {
            return false;
        }

        int available = countInInventory(player, itemId);
        if (available < requestedItems) {
            player.sendSystemMessage(Component.translatable("incore.market.no_items_to_sell"));
            return false;
        }

        SaleQuote quote = quoteSale(player.getServer(), itemId, stackCount);
        if (!quote.valid()) {
            return false;
        }

        ItemStack card = terminal.cardStack();
        BankAccount account = MarketBanking.resolveManualAccount(player, card);
        if (account == null) {
            player.sendSystemMessage(Component.translatable("incore.market.no_account"));
            return false;
        }

        if (!removeItems(player, itemId, requestedItems)) {
            return false;
        }

        MarketBanking.deposit(account, quote.netPayoutSpur());
        MarketPricingService.applySell(player.getServer(), itemId, stackCount);
        BattlePassTaskHooks.onMarketSell(player, requestedItems);
        syncActiveViewers(player.getServer());
        return true;
    }

    private static boolean sellToMarketMeTerminal(ServerPlayer player, MarketTerminalMeBlockEntity terminal, ResourceLocation itemId, int quantity) {
        if (player.getServer() == null) {
            return false;
        }

        MarketItemDefinition definition = MarketItemManager.get(itemId);
        if (definition == null) {
            return false;
        }

        int stackCount = Math.max(1, quantity);
        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item == null || item == net.minecraft.world.item.Items.AIR) {
            return false;
        }
        int stackUnitSize = stackUnitSize(item);
        int requestedItems = toItemCount(stackCount, stackUnitSize);
        if (requestedItems <= 0) {
            return false;
        }

        int inventoryAvailable = countInInventory(player, itemId);
        long meAvailable = terminal.ae2Online() ? Ae2StorageAccess.count(terminal.grid(), new ItemStack(item)) : 0L;
        if (inventoryAvailable + meAvailable < requestedItems) {
            player.sendSystemMessage(Component.translatable("incore.market.no_items_to_sell"));
            return false;
        }

        SaleQuote quote = quoteSale(player.getServer(), itemId, stackCount);
        if (!quote.valid()) {
            return false;
        }

        BankAccount account = MarketBanking.resolveManualAccount(player, terminal.cardStack());
        if (account == null) {
            player.sendSystemMessage(Component.translatable("incore.market.no_account"));
            return false;
        }

        int remaining = requestedItems;
        if (!removeItems(player, itemId, remaining)) {
            return false;
        }
        if (terminal.ae2Online()) {
            long meRemoved = Ae2StorageAccess.extract(terminal.grid(), terminal.actionSource(), new ItemStack(item), remaining);
            if (meRemoved < remaining) {
                int stillNeeded = (int) (remaining - meRemoved);
                if (!removeItems(player, itemId, stillNeeded)) {
                    return false;
                }
            }
        }

        MarketBanking.deposit(account, quote.netPayoutSpur());
        MarketPricingService.applySell(player.getServer(), itemId, stackCount);
        BattlePassTaskHooks.onMarketSell(player, requestedItems);
        syncActiveViewers(player.getServer());
        return true;
    }

    public static SaleQuote quoteSale(MinecraftServer server, ResourceLocation itemId, int stackCount) {
        int unitPrice = MarketPricingService.currentPrice(server, itemId);
        return quoteSale(unitPrice, stackCount);
    }

    public static SaleQuote quoteSale(int unitPrice, int stackCount) {
        int normalizedUnitPrice = Math.max(0, unitPrice);
        int normalizedStackCount = Math.max(1, stackCount);
        if (normalizedUnitPrice <= 0) {
            return SaleQuote.invalid();
        }

        long grossLong = (long) normalizedUnitPrice * normalizedStackCount;
        int gross = (int) Math.min(Integer.MAX_VALUE, grossLong);
        int tax = (int) Math.min(Integer.MAX_VALUE,
                Math.floor(gross * Math.max(0D, Math.min(1D, Config.MARKET_SELL_TAX_RATE.get()))));
        int net = Math.max(0, gross - tax);
        return new SaleQuote(normalizedUnitPrice, normalizedStackCount, gross, tax, net, true);
    }

    private static void giveItems(ServerPlayer player, Item item, int totalItems) {
        int remaining = totalItems;
        int maxStack = stackUnitSize(item);
        while (remaining > 0) {
            int giving = Math.min(remaining, maxStack);
            ItemStack stack = new ItemStack(item, giving);
            if (!player.addItem(stack)) {
                player.drop(stack, false);
            }
            remaining -= giving;
        }
    }

    private static AbstractMarketTerminalBlockEntity terminalAt(ServerPlayer player, BlockPos pos) {
        if (pos == null) {
            return null;
        }
        if (!(player.level().getBlockEntity(pos) instanceof AbstractMarketTerminalBlockEntity terminal)) {
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

    private static int stackUnitSize(Item item) {
        return Math.max(1, item.getDefaultMaxStackSize());
    }

    private static int toItemCount(int stackCount, int stackUnitSize) {
        long total = (long) Math.max(1, stackCount) * Math.max(1, stackUnitSize);
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    private static void syncViewer(ServerPlayer player, ViewerSession session) {
        if (player.getServer() == null || session == null) {
            return;
        }

        BlockPos terminalPos = session.terminalPos() == null ? null : BlockPos.of(session.terminalPos());
        AbstractMarketTerminalBlockEntity terminal = terminalPos == null ? null : terminalAt(player, terminalPos);
        boolean canTrade = terminal != null && terminal.canTrade(player);
        BlockPos syncedTerminalPos = terminal == null ? null : terminalPos;
        MarketNetworking.syncMarketSnapshot(
                player,
                buildScreenData(player, player.getServer(), canTrade, syncedTerminalPos, session.detailItemId())
        );
    }

    public static ScreenData buildScreenData(ServerPlayer player, MinecraftServer server, boolean canTrade, BlockPos terminalPos) {
        return buildScreenData(player, server, canTrade, terminalPos, null);
    }

    public static ScreenData buildScreenData(ServerPlayer player, MinecraftServer server, boolean canTrade, BlockPos terminalPos, @Nullable ResourceLocation detailItemId) {
        MarketPricingService.tick(server);

        BankAccount account = MarketBanking.resolveManualAccount(player, ItemStack.EMPTY);
        int balanceSpur = MarketBanking.balanceSpur(account);
        boolean ae2Linked = terminalPos != null && player.level().getBlockEntity(terminalPos) instanceof MarketTerminalMeBlockEntity meTerminal && meTerminal.ae2Linked();
        boolean ae2Online = terminalPos != null && player.level().getBlockEntity(terminalPos) instanceof MarketTerminalMeBlockEntity meTerminalOnline && meTerminalOnline.ae2Online();

        List<ItemView> items = new ArrayList<>();
        MarketTerminalMeBlockEntity meTerminal = terminalPos == null ? null
                : player.level().getBlockEntity(terminalPos) instanceof MarketTerminalMeBlockEntity found ? found : null;
        for (MarketItemDefinition definition : MarketItemManager.all()) {
            int price = MarketPricingService.currentPrice(server, definition.itemId());
            List<MarketSavedData.PriceCandle> sourceCandles = MarketPricingService.candles(server, definition.itemId());
            List<CandleView> candles = List.of();
            if (detailItemId != null && detailItemId.equals(definition.itemId())) {
                candles = sourceCandles.stream()
                        .map(c -> new CandleView(c.hourKey(), c.open(), c.high(), c.low(), c.close(), c.buyVolume(), c.sellVolume()))
                        .toList();
            }
            double dayChangePercent = dayChangePercent(sourceCandles, price);

            double demand = MarketSavedData.get(server)
                    .stateFor(definition.itemId(), definition.basePriceSpur())
                    .demandIndex();

            int inventoryCount = countInInventory(player, definition.itemId());
            int availableCount = inventoryCount;
            if (meTerminal != null && meTerminal.ae2Online()) {
                Item item = BuiltInRegistries.ITEM.get(definition.itemId());
                if (item != null && item != net.minecraft.world.item.Items.AIR) {
                    long meCount = Ae2StorageAccess.count(meTerminal.grid(), new ItemStack(item));
                    availableCount = (int) Math.min(Integer.MAX_VALUE, meCount + inventoryCount);
                }
            }

            items.add(new ItemView(
                    definition.itemId().toString(),
                    definition.displayName(),
                    definition.basePriceSpur(),
                    price,
                    dayChangePercent,
                    demand,
                    candles,
                    inventoryCount,
                    availableCount
            ));
        }

        return new ScreenData(
                canTrade,
                terminalPos == null ? null : terminalPos.asLong(),
                items,
                balanceSpur,
                ae2Linked,
                ae2Online
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
            List<ItemView> items,
            int balanceSpur,
            boolean ae2Linked,
            boolean ae2Online
    ) {
    }

    public record ItemView(
            String itemId,
            String displayName,
            int basePriceSpur,
            int currentPriceSpur,
            double dayChangePercent,
            double demandIndex,
            List<CandleView> candles,
            int inventoryCount,
            int availableCount
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

    private record ViewerSession(@Nullable Long terminalPos, @Nullable ResourceLocation detailItemId) {
    }

    public record SaleQuote(
            int unitPriceSpur,
            int stackCount,
            int grossPayoutSpur,
            int taxSpur,
            int netPayoutSpur,
            boolean valid
    ) {
        public static SaleQuote invalid() {
            return new SaleQuote(0, 0, 0, 0, 0, false);
        }
    }
}
