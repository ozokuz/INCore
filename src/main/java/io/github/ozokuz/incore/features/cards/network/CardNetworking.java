package io.github.ozokuz.incore.features.cards.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class CardNetworking {
    private CardNetworking() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(OpenCardPackPayload.TYPE, OpenCardPackPayload.STREAM_CODEC, OpenCardPackPayload::handle);
    }

    public static void openPackResults(ServerPlayer player, String json) {
        PacketDistributor.sendToPlayer(player, new OpenCardPackPayload(json));
    }
}
