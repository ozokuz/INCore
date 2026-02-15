package io.github.ozokuz.incore.features.tasks.network;

import io.github.ozokuz.incore.features.tasks.TaskService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class TaskNetworking {
    private TaskNetworking() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(TaskSyncPayload.TYPE, TaskSyncPayload.STREAM_CODEC, TaskSyncPayload::handle);
    }

    public static void syncToPlayer(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new TaskSyncPayload(TaskService.buildSyncJson(player)));
    }
}
