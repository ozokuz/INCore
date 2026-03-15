package ozokuz.incore.features.research.network;

import ozokuz.incore.features.research.ResearchManager;
import ozokuz.incore.features.research.discovery.network.ResearchSampleFabricatorCraftPayload;
import ozokuz.incore.features.research.team.ResearchTeamResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ResearchNetworking {
    private ResearchNetworking() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("2");
        registrar.playToClient(ResearchStateSyncPayload.TYPE, ResearchStateSyncPayload.STREAM_CODEC, ResearchStateSyncPayload::handle);
        registrar.playToServer(ResearchRequestSnapshotPayload.TYPE, ResearchRequestSnapshotPayload.STREAM_CODEC, ResearchRequestSnapshotPayload::handle);
        registrar.playToServer(ResearchQueueResearchPayload.TYPE, ResearchQueueResearchPayload.STREAM_CODEC, ResearchQueueResearchPayload::handle);
        registrar.playToServer(ResearchCancelQueueItemPayload.TYPE, ResearchCancelQueueItemPayload.STREAM_CODEC, ResearchCancelQueueItemPayload::handle);
        registrar.playToServer(ResearchRepairDiskSegmentPayload.TYPE, ResearchRepairDiskSegmentPayload.STREAM_CODEC, ResearchRepairDiskSegmentPayload::handle);
        registrar.playToServer(ResearchSampleFabricatorCraftPayload.TYPE, ResearchSampleFabricatorCraftPayload.STREAM_CODEC, ResearchSampleFabricatorCraftPayload::handle);
    }

    public static void requestSnapshot() {
        PacketDistributor.sendToServer(new ResearchRequestSnapshotPayload(true));
    }

    public static void queueResearch(ResourceLocation nodeId) {
        PacketDistributor.sendToServer(new ResearchQueueResearchPayload(nodeId.toString()));
    }

    public static void cancelQueueItem(ResourceLocation nodeId) {
        PacketDistributor.sendToServer(new ResearchCancelQueueItemPayload(nodeId.toString()));
    }

    public static void repairDiskSegment(net.minecraft.core.BlockPos pos, ResourceLocation nodeId, int segmentIndex) {
        PacketDistributor.sendToServer(new ResearchRepairDiskSegmentPayload(pos.asLong(), nodeId.toString(), segmentIndex));
    }

    public static void fabricateResearchSample(BlockPos pos, ResourceLocation nodeId) {
        PacketDistributor.sendToServer(new ResearchSampleFabricatorCraftPayload(pos.asLong(), nodeId.toString()));
    }

    public static void syncToPlayer(ServerPlayer player) {
        String teamId = ResearchTeamResolver.resolveTeamId(player);
        if (teamId == null || teamId.isBlank()) {
            return;
        }

        String json = ResearchManager.snapshotJson(player.serverLevel().getServer(), teamId);
        PacketDistributor.sendToPlayer(player, new ResearchStateSyncPayload(json));
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
