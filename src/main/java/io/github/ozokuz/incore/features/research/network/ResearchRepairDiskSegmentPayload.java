package io.github.ozokuz.incore.features.research.network;

import io.github.ozokuz.incore.features.research.ResearchManager;
import io.github.ozokuz.incore.features.research.station.ResearchDriveBlockEntity;
import io.github.ozokuz.incore.features.research.station.ResearchStationRuntime;
import io.github.ozokuz.incore.features.research.team.ResearchTeamResolver;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ResearchRepairDiskSegmentPayload(long blockPos, String nodeId, int segmentIndex) implements CustomPacketPayload {
    public static final Type<ResearchRepairDiskSegmentPayload> TYPE = new Type<>(ResourceLocation.parse("incore:research_repair_disk_segment"));
    public static final StreamCodec<ByteBuf, ResearchRepairDiskSegmentPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG,
            ResearchRepairDiskSegmentPayload::blockPos,
            ByteBufCodecs.STRING_UTF8,
            ResearchRepairDiskSegmentPayload::nodeId,
            ByteBufCodecs.VAR_INT,
            ResearchRepairDiskSegmentPayload::segmentIndex,
            ResearchRepairDiskSegmentPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ResearchRepairDiskSegmentPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof net.minecraft.server.level.ServerPlayer player)) {
                return;
            }
            BlockPos pos = BlockPos.of(payload.blockPos());
            if (!(player.serverLevel().getBlockEntity(pos) instanceof ResearchDriveBlockEntity drive)) {
                return;
            }

            String teamId = ResearchTeamResolver.resolveTeamId(player);
            if (!teamId.equals(drive.teamId())) {
                return;
            }

            var state = ResearchManager.ensureTeamState(player.serverLevel().getServer(), teamId);
            boolean running = !state.researchQueue().isEmpty()
                    && state.researchQueue().get(0).assignedStationIds().contains(drive.stationId())
                    && state.researchQueue().get(0).status() == io.github.ozokuz.incore.features.research.state.ResearchQueueStatus.RUNNING;
            if (running) {
                return;
            }

            ResourceLocation node = ResourceLocation.tryParse(payload.nodeId());
            if (node == null) {
                return;
            }
            if (ResearchStationRuntime.clearDiskCorruptedSegment(drive, node, payload.segmentIndex())) {
                player.containerMenu.broadcastChanges();
            }
        });
    }
}
