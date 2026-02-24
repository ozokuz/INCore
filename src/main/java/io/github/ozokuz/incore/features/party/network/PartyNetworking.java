package io.github.ozokuz.incore.features.party.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.List;

public final class PartyNetworking {
    private PartyNetworking() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(PartyHudSyncPayload.TYPE, PartyHudSyncPayload.STREAM_CODEC, PartyHudSyncPayload::handle);
    }

    public static void syncHudToPlayer(ServerPlayer player, List<PartyHudSyncPayload.MemberEntry> rows) {
        PacketDistributor.sendToPlayer(player, new PartyHudSyncPayload(rows));
    }
}
