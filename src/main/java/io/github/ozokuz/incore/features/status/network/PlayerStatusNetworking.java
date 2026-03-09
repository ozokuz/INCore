package io.github.ozokuz.incore.features.status.network;

import io.github.ozokuz.incore.features.roguelike.state.RoguelikeSavedData;
import io.github.ozokuz.incore.features.vendingmachine.VendingMachineService;
import io.github.ozokuz.incore.features.vendingmachine.VendingMachineService.BalanceEntryView;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.List;

public final class PlayerStatusNetworking {
    private PlayerStatusNetworking() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("2");
        registrar.playToClient(
                PlayerStatusCurrencySyncPayload.TYPE,
                PlayerStatusCurrencySyncPayload.STREAM_CODEC,
                PlayerStatusCurrencySyncPayload::handle
        );
        registrar.playToClient(
                DungeonDifficultySyncPayload.TYPE,
                DungeonDifficultySyncPayload.STREAM_CODEC,
                DungeonDifficultySyncPayload::handle
        );
        registrar.playToServer(
                RequestPlayerStatusCurrencyPayload.TYPE,
                RequestPlayerStatusCurrencyPayload.STREAM_CODEC,
                RequestPlayerStatusCurrencyPayload::handle
        );
        registrar.playToServer(
                RequestDungeonDifficultyPayload.TYPE,
                RequestDungeonDifficultyPayload.STREAM_CODEC,
                RequestDungeonDifficultyPayload::handle
        );
        registrar.playToServer(
                SetDungeonDifficultyPayload.TYPE,
                SetDungeonDifficultyPayload.STREAM_CODEC,
                SetDungeonDifficultyPayload::handle
        );
    }

    public static void requestCurrencySync() {
        PacketDistributor.sendToServer(new RequestPlayerStatusCurrencyPayload(true));
    }

    public static void requestDungeonDifficultySync() {
        PacketDistributor.sendToServer(new RequestDungeonDifficultyPayload(true));
    }

    public static void setDungeonDifficulty(String difficulty) {
        PacketDistributor.sendToServer(new SetDungeonDifficultyPayload(difficulty));
    }

    static void syncCurrencyToPlayer(ServerPlayer player) {
        List<BalanceEntryView> balances = VendingMachineService.collectPlayerBalanceEntries(player);
        PacketDistributor.sendToPlayer(
                player,
                new PlayerStatusCurrencySyncPayload(
                        balances.stream()
                                .map(balance -> new PlayerStatusCurrencySyncPayload.BalanceEntry(
                                        balance.iconItemId(),
                                        Math.max(0, balance.amount())
                                ))
                                .toList()
                )
        );
    }

    static void syncDungeonDifficultyToPlayer(ServerPlayer player) {
        PacketDistributor.sendToPlayer(
                player,
                new DungeonDifficultySyncPayload(
                        RoguelikeSavedData.get(player.getServer()).dungeonDeathDifficulty(player.getUUID()).name()
                )
        );
    }
}
