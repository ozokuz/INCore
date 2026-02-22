package io.github.ozokuz.incore.features.market;

import dev.ithundxr.createnumismatics.content.backend.BankAccount;
import dev.ithundxr.createnumismatics.content.bank.CardItem;
import dev.ithundxr.createnumismatics.content.bank.IDCardItem;
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
import java.util.UUID;

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
        MarketNetworking.openMarketScreen(player, buildScreenData(player.getServer(), false, null, null));
    }

    public static void openTerminalScreen(ServerPlayer player, MarketTerminalBlockEntity terminal) {
        if (player.getServer() == null) {
            return;
        }
        boolean canTrade = terminal.canTrade(player);
        BlockPos pos = terminal.getBlockPos();
        MarketNetworking.openMarketScreen(player, buildScreenData(player.getServer(), canTrade, pos, terminal));
    }

    public static void requestRefresh(ServerPlayer player, BlockPos terminalPos) {
        if (player.getServer() == null) {
            return;
        }

        MarketTerminalBlockEntity terminal = terminalAt(player, terminalPos);
        boolean canTrade = terminal != null && terminal.canTrade(player);
        MarketNetworking.openMarketScreen(player, buildScreenData(player.getServer(), canTrade, terminal == null ? null : terminalPos, terminal));
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

        BankAccount account = MarketBanking.resolveManualAccount(player, findFirstBoundCard(player));
        if (account == null || !MarketBanking.withdraw(account, cost)) {
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

        BankAccount account = MarketBanking.resolveManualAccount(player, findFirstBoundCard(player));
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

    public static boolean addTrustedFromHeldIdCard(ServerPlayer player, BlockPos terminalPos) {
        MarketTerminalBlockEntity terminal = terminalAt(player, terminalPos);
        if (terminal == null || !terminal.canManageTrust(player.getUUID())) {
            return false;
        }

        ItemStack held = player.getMainHandItem();
        if (held.isEmpty() || !IDCardItem.isBound(held)) {
            return false;
        }

        UUID trusted = IDCardItem.get(held);
        if (trusted == null) {
            return false;
        }

        terminal.addTrusted(trusted);
        return true;
    }

    public static boolean removeTrustedFromHeldIdCard(ServerPlayer player, BlockPos terminalPos) {
        MarketTerminalBlockEntity terminal = terminalAt(player, terminalPos);
        if (terminal == null || !terminal.canManageTrust(player.getUUID())) {
            return false;
        }

        ItemStack held = player.getMainHandItem();
        if (held.isEmpty() || !IDCardItem.isBound(held)) {
            return false;
        }

        UUID trusted = IDCardItem.get(held);
        if (trusted == null) {
            return false;
        }

        terminal.removeTrusted(trusted);
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

    private static ItemStack findFirstBoundCard(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && CardItem.isBound(stack)) {
                return stack;
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (!stack.isEmpty() && CardItem.isBound(stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
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

    public static ScreenData buildScreenData(MinecraftServer server, boolean canTrade, BlockPos terminalPos, MarketTerminalBlockEntity terminal) {
        MarketPricingService.tick(server);

        List<ItemView> items = new ArrayList<>();
        for (MarketItemDefinition definition : MarketItemManager.all()) {
            int price = MarketPricingService.currentPrice(server, definition.itemId());
            List<CandleView> candles = MarketPricingService.candles(server, definition.itemId()).stream()
                    .map(c -> new CandleView(c.hourKey(), c.open(), c.high(), c.low(), c.close(), c.buyVolume(), c.sellVolume()))
                    .toList();

            double demand = MarketSavedData.get(server)
                    .stateFor(definition.itemId(), definition.basePriceSpur())
                    .demandIndex();

            items.add(new ItemView(
                    definition.itemId().toString(),
                    definition.displayName(),
                    definition.basePriceSpur(),
                    price,
                    demand,
                    candles
            ));
        }

        List<String> trusted = terminal == null
                ? List.of()
                : terminal.trustedPlayers().stream().map(UUID::toString).toList();

        return new ScreenData(
                canTrade,
                terminalPos == null ? null : terminalPos.asLong(),
                items,
                trusted
        );
    }

    public record ScreenData(
            boolean canTrade,
            Long terminalPos,
            List<ItemView> items,
            List<String> trustedPlayers
    ) {
    }

    public record ItemView(
            String itemId,
            String displayName,
            int basePriceSpur,
            int currentPriceSpur,
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
