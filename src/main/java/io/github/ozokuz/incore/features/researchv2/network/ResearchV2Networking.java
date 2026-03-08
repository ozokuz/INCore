package io.github.ozokuz.incore.features.researchv2.network;

import io.github.ozokuz.incore.features.researchv2.ResearchManager;
import io.github.ozokuz.incore.features.researchv2.team.ResearchTeamResolver;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ResearchV2Networking {
    private ResearchV2Networking() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("2");
        registrar.playToClient(ResearchV2StateSyncPayload.TYPE, ResearchV2StateSyncPayload.STREAM_CODEC, ResearchV2StateSyncPayload::handle);
        registrar.playToServer(ResearchV2RequestSnapshotPayload.TYPE, ResearchV2RequestSnapshotPayload.STREAM_CODEC, ResearchV2RequestSnapshotPayload::handle);
        registrar.playToServer(ResearchV2QueueResearchPayload.TYPE, ResearchV2QueueResearchPayload.STREAM_CODEC, ResearchV2QueueResearchPayload::handle);
        registrar.playToServer(ResearchV2CancelQueueItemPayload.TYPE, ResearchV2CancelQueueItemPayload.STREAM_CODEC, ResearchV2CancelQueueItemPayload::handle);
        registrar.playToServer(ResearchV2RepairDiskSegmentPayload.TYPE, ResearchV2RepairDiskSegmentPayload.STREAM_CODEC, ResearchV2RepairDiskSegmentPayload::handle);
    }

    public static void requestSnapshot() {
        PacketDistributor.sendToServer(new ResearchV2RequestSnapshotPayload(true));
    }

    public static void queueResearch(ResourceLocation nodeId) {
        PacketDistributor.sendToServer(new ResearchV2QueueResearchPayload(nodeId.toString()));
    }

    public static void cancelQueueItem(ResourceLocation nodeId) {
        PacketDistributor.sendToServer(new ResearchV2CancelQueueItemPayload(nodeId.toString()));
    }

    public static void repairDiskSegment(net.minecraft.core.BlockPos pos, ResourceLocation nodeId, int segmentIndex) {
        PacketDistributor.sendToServer(new ResearchV2RepairDiskSegmentPayload(pos.asLong(), nodeId.toString(), segmentIndex));
    }

    public static void syncToPlayer(ServerPlayer player) {
        String teamId = ResearchTeamResolver.resolveTeamId(player);
        if (teamId == null || teamId.isBlank()) {
            return;
        }

        String json = ResearchManager.snapshotJson(player.serverLevel().getServer(), teamId);
        PacketDistributor.sendToPlayer(player, new ResearchV2StateSyncPayload(json));
    }

    public static void syncTeam(MinecraftServer server, String teamId) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            String playerTeamId = ResearchTeamResolver.resolveTeamId(player);
            if (teamId.equals(playerTeamId)) {
                syncToPlayer(player);
            }
        }
    }
}
