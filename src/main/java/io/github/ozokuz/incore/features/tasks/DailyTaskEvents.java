package io.github.ozokuz.incore.features.tasks;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public final class DailyTaskEvents {
    private DailyTaskEvents() {
    }

    public static void onLogin(ServerPlayer player) {
        DailyTaskService.onLogin(player);
    }

    public static void onShopPurchase(ServerPlayer player) {
        DailyTaskService.onShopPurchase(player);
    }

    public static void onArenaCompletion(ServerPlayer player) {
        DailyTaskService.onArenaCompletion(player);
    }

    public static void onDungeonCompletion(ServerPlayer player) {
        DailyTaskService.onDungeonCompletion(player);
    }

    public static void onVendingMachinePurchase(ServerPlayer player) {
        DailyTaskService.onVendingMachinePurchase(player);
    }

    public static void onBuyFromPlayer(ServerPlayer buyer) {
        DailyTaskService.onBuyFromPlayer(buyer);
    }

    public static void onBuyFromPlayer(MinecraftServer server, UUID playerId) {
        DailyTaskService.onBuyFromPlayer(server, playerId);
    }

    public static void onSellToPlayer(ServerPlayer seller) {
        DailyTaskService.onSellToPlayer(seller);
    }

    public static void onSellToPlayer(MinecraftServer server, UUID playerId) {
        DailyTaskService.onSellToPlayer(server, playerId);
    }
}
