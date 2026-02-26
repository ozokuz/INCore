package io.github.ozokuz.incore.features.roguelike.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.List;

public final class RoguelikeNetworking {
    private RoguelikeNetworking() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(RoguelikeMinimapGraphPayload.TYPE, RoguelikeMinimapGraphPayload.STREAM_CODEC, RoguelikeMinimapGraphPayload::handle);
        registrar.playToClient(RoguelikeMinimapRevealPayload.TYPE, RoguelikeMinimapRevealPayload.STREAM_CODEC, RoguelikeMinimapRevealPayload::handle);
        registrar.playToClient(RoguelikeMinimapPartyPayload.TYPE, RoguelikeMinimapPartyPayload.STREAM_CODEC, RoguelikeMinimapPartyPayload::handle);
    }

    public static void syncGraph(ServerPlayer player, long instanceId, int originChunkX, int originChunkZ) {
        PacketDistributor.sendToPlayer(player, new RoguelikeMinimapGraphPayload(instanceId, originChunkX, originChunkZ));
    }

    public static void revealRoom(ServerPlayer player, long instanceId, int roomId) {
        PacketDistributor.sendToPlayer(player, new RoguelikeMinimapRevealPayload(instanceId, roomId));
    }

    public static void syncParty(ServerPlayer player, long instanceId, List<RoguelikeMinimapPartyPayload.Marker> markers) {
        PacketDistributor.sendToPlayer(player, new RoguelikeMinimapPartyPayload(instanceId, markers));
    }
}
