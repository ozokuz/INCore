package io.github.ozokuz.incore.features.tasks;

import net.minecraft.server.level.ServerPlayer;

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

    public static void onVendorPurchase(ServerPlayer player) {
        DailyTaskService.onVendorPurchase(player);
    }

    public static void onBuyFromPlayer(ServerPlayer buyer) {
        DailyTaskService.onBuyFromPlayer(buyer);
    }

    public static void onSellToPlayer(ServerPlayer seller) {
        DailyTaskService.onSellToPlayer(seller);
    }
}
