package ozokuz.incore.features.numismatics.network;

import dev.ithundxr.createnumismatics.Numismatics;
import dev.ithundxr.createnumismatics.content.backend.BankAccount;
import dev.ithundxr.createnumismatics.util.Utils;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class NumismaticsNetworking {
    private NumismaticsNetworking() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                RequestOpenNumismaticsBankPayload.TYPE,
                RequestOpenNumismaticsBankPayload.STREAM_CODEC,
                RequestOpenNumismaticsBankPayload::handle
        );
    }

    public static void requestOpenBankScreen() {
        PacketDistributor.sendToServer(new RequestOpenNumismaticsBankPayload(true));
    }

    public static void openBankScreenFor(ServerPlayer player) {
        BankAccount account = Numismatics.BANK.getAccount(player);
        if (account == null) {
            return;
        }

        Utils.openScreen(player, account, account::sendToMenu);
    }
}
