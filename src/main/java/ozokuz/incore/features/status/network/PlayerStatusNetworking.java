package ozokuz.incore.features.status.network;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import ozokuz.incore.features.vendingmachine.VendingMachineService;
import ozokuz.incore.features.vendingmachine.VendingMachineService.BalanceEntryView;

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
        registrar.playToServer(
                RequestPlayerStatusCurrencyPayload.TYPE,
                RequestPlayerStatusCurrencyPayload.STREAM_CODEC,
                RequestPlayerStatusCurrencyPayload::handle
        );
    }

    public static void requestCurrencySync() {
        PacketDistributor.sendToServer(new RequestPlayerStatusCurrencyPayload(true));
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
}
