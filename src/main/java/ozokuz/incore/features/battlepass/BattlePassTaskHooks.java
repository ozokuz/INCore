package ozokuz.incore.features.battlepass;

import ozokuz.incore.features.battlepass.network.BattlePassNetworking;
import net.minecraft.server.level.ServerPlayer;

import java.time.Instant;

public final class BattlePassTaskHooks {
    private BattlePassTaskHooks() {
    }

    public static void onPlayerLogin(ServerPlayer player) {
        Instant now = Instant.now();
        if (BattlePassProgressManager.checkAndProgressLogin(player, now)) {
            BattlePassNetworking.syncToPlayer(player);
        }
    }

    public static void onEntropyRecovered(ServerPlayer player, int amount) {
        if (amount <= 0) {
            return;
        }
        Instant now = Instant.now();
        BattlePassProgressManager.addProgressByTriggerType(player, BattlePassDefinition.TriggerType.ENTROPY_RECOVER, amount, now);
        BattlePassNetworking.syncToPlayer(player);
    }

    public static void onGachaCrateOpened(ServerPlayer player) {
        Instant now = Instant.now();
        BattlePassProgressManager.addProgressByTriggerType(player, BattlePassDefinition.TriggerType.GACHA_CRATE_OPEN, 1, now);
        BattlePassNetworking.syncToPlayer(player);
    }

    public static void onBannerPermitUsed(ServerPlayer player, int count) {
        if (count <= 0) {
            return;
        }
        Instant now = Instant.now();
        BattlePassProgressManager.addProgressByTriggerType(player, BattlePassDefinition.TriggerType.BANNER_PERMIT_USE, count, now);
        BattlePassNetworking.syncToPlayer(player);
    }

    public static void onArenaCompleted(ServerPlayer player) {
        Instant now = Instant.now();
        BattlePassProgressManager.addProgressByTriggerType(player, BattlePassDefinition.TriggerType.ARENA_COMPLETE, 1, now);
        BattlePassNetworking.syncToPlayer(player);
    }

    public static void onDungeonCompleted(ServerPlayer player) {
        Instant now = Instant.now();
        BattlePassProgressManager.addProgressByTriggerType(player, BattlePassDefinition.TriggerType.DUNGEON_COMPLETE, 1, now);
        BattlePassNetworking.syncToPlayer(player);
    }

    public static void onSurfaceOreMined(ServerPlayer player) {
        Instant now = Instant.now();
        BattlePassProgressManager.addProgressByTriggerType(player, BattlePassDefinition.TriggerType.SURFACE_ORE_MINE, 1, now);
        BattlePassNetworking.syncToPlayer(player);
    }

    public static void onVendingMachinePurchase(ServerPlayer player) {
        Instant now = Instant.now();
        BattlePassProgressManager.addProgressByTriggerType(player, BattlePassDefinition.TriggerType.VENDING_MACHINE_PURCHASE, 1, now);
        BattlePassNetworking.syncToPlayer(player);
    }

    public static void onMarketBuy(ServerPlayer player, int itemCount) {
        if (itemCount <= 0) {
            return;
        }
        Instant now = Instant.now();
        BattlePassProgressManager.addProgressByTriggerType(player, BattlePassDefinition.TriggerType.MARKET_BUY, itemCount, now);
        BattlePassNetworking.syncToPlayer(player);
    }

    public static void onMarketSell(ServerPlayer player, int itemCount) {
        if (itemCount <= 0) {
            return;
        }
        Instant now = Instant.now();
        BattlePassProgressManager.addProgressByTriggerType(player, BattlePassDefinition.TriggerType.MARKET_SELL, itemCount, now);
        BattlePassNetworking.syncToPlayer(player);
    }

    public static void onCardBoosterOpened(ServerPlayer player) {
        Instant now = Instant.now();
        BattlePassProgressManager.addProgressByTriggerType(player, BattlePassDefinition.TriggerType.CARD_BOOSTER_OPEN, 1, now);
        BattlePassNetworking.syncToPlayer(player);
    }

    public static void onResearchCompleted(ServerPlayer player) {
        Instant now = Instant.now();
        BattlePassProgressManager.addProgressByTriggerType(player, BattlePassDefinition.TriggerType.RESEARCH_COMPLETE, 1, now);
        BattlePassNetworking.syncToPlayer(player);
    }
}
