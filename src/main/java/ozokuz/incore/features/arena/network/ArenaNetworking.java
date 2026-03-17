package ozokuz.incore.features.arena.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import ozokuz.incore.features.arena.ArenaService;

public final class ArenaNetworking {
    private ArenaNetworking() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(StartArenaRunPayload.TYPE, StartArenaRunPayload.STREAM_CODEC, StartArenaRunPayload::handle);
    }

    public static void requestStartRun(ResourceLocation entryId) {
        PacketDistributor.sendToServer(new StartArenaRunPayload(entryId.toString()));
    }

    public static void startRunFor(ServerPlayer player, ResourceLocation entryId) {
        ArenaService.startRun(player, entryId);
    }
}
