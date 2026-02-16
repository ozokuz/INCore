package io.github.ozokuz.incore.features.research.network;

import io.github.ozokuz.incore.features.research.ResearchProgressService;
import io.github.ozokuz.incore.features.research.ResearchSyncData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ResearchNetworking {
    private ResearchNetworking() {}

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(OpenResearchScreenPayload.TYPE, OpenResearchScreenPayload.STREAM_CODEC, OpenResearchScreenPayload::handle);
        registrar.playToServer(RequestOpenResearchScreenPayload.TYPE, RequestOpenResearchScreenPayload.STREAM_CODEC, RequestOpenResearchScreenPayload::handle);
        registrar.playToServer(UnlockResearchEntryPayload.TYPE, UnlockResearchEntryPayload.STREAM_CODEC, UnlockResearchEntryPayload::handle);
        registrar.playToServer(SubmitResearchTaskPayload.TYPE, SubmitResearchTaskPayload.STREAM_CODEC, SubmitResearchTaskPayload::handle);
        registrar.playToServer(MoveResearchQueuePayload.TYPE, MoveResearchQueuePayload.STREAM_CODEC, MoveResearchQueuePayload::handle);
        registrar.playToServer(RemoveResearchQueueEntryPayload.TYPE, RemoveResearchQueueEntryPayload.STREAM_CODEC, RemoveResearchQueueEntryPayload::handle);
    }

    public static void requestOpen() {
        PacketDistributor.sendToServer(new RequestOpenResearchScreenPayload(true));
    }

    public static void requestUnlock(ResourceLocation id) {
        PacketDistributor.sendToServer(new UnlockResearchEntryPayload(id.toString()));
    }

    public static void requestTaskSubmit(ResourceLocation id) {
        PacketDistributor.sendToServer(new SubmitResearchTaskPayload(id.toString()));
    }

    public static void requestMoveQueue(int fromIndex, int toIndex) {
        PacketDistributor.sendToServer(new MoveResearchQueuePayload(fromIndex, toIndex));
    }

    public static void requestRemoveQueueEntry(ResourceLocation id) {
        PacketDistributor.sendToServer(new RemoveResearchQueueEntryPayload(id.toString()));
    }

    public static void openFor(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new OpenResearchScreenPayload(ResearchSyncData.build(player)));
    }

    public static void unlock(ServerPlayer player, ResourceLocation id) {
        if (ResearchProgressService.unlock(player, id)) {
            openFor(player);
        }
    }

    public static void submitTask(ServerPlayer player, ResourceLocation id) {
        if (ResearchProgressService.submitTask(player, id)) {
            openFor(player);
        }
    }

    public static void moveQueue(ServerPlayer player, int fromIndex, int toIndex) {
        if (ResearchProgressService.reorderQueue(player, fromIndex, toIndex)) {
            openFor(player);
        }
    }

    public static void removeQueueEntry(ServerPlayer player, ResourceLocation id) {
        if (ResearchProgressService.dequeueResearch(player, id)) {
            openFor(player);
        }
    }
}
