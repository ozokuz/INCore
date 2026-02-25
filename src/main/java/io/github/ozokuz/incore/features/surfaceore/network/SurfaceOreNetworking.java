package io.github.ozokuz.incore.features.surfaceore.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class SurfaceOreNetworking {
    private SurfaceOreNetworking() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(WaypointPayload.TYPE, WaypointPayload.STREAM_CODEC, WaypointPayload::handle);
    }

    public static void sendWaypointToPlayer(ServerPlayer player, String name, String marker, long posPacked) {
        PacketDistributor.sendToPlayer(player, new WaypointPayload(name, marker, posPacked));
    }
}
