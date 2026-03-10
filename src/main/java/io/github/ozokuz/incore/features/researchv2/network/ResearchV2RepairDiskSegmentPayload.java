package io.github.ozokuz.incore.features.researchv2.network;

import io.github.ozokuz.incore.features.researchv2.ResearchManager;
import io.github.ozokuz.incore.features.researchv2.station.ResearchDriveBlockEntity;
import io.github.ozokuz.incore.features.researchv2.station.ResearchStationRuntime;
import io.github.ozokuz.incore.features.researchv2.team.ResearchTeamResolver;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ResearchV2RepairDiskSegmentPayload(long blockPos, String nodeId, int segmentIndex) implements CustomPacketPayload {
    public static final Type<ResearchV2RepairDiskSegmentPayload> TYPE = new Type<>(ResourceLocation.parse("incore:research_v2_repair_disk_segment"));
    public static final StreamCodec<ByteBuf, ResearchV2RepairDiskSegmentPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG,
            ResearchV2RepairDiskSegmentPayload::blockPos,
            ByteBufCodecs.STRING_UTF8,
            ResearchV2RepairDiskSegmentPayload::nodeId,
            ByteBufCodecs.VAR_INT,
            ResearchV2RepairDiskSegmentPayload::segmentIndex,
            ResearchV2RepairDiskSegmentPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ResearchV2RepairDiskSegmentPayload payload, IPayloadContext context) {
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
                    && state.researchQueue().get(0).status() == io.github.ozokuz.incore.features.researchv2.state.ResearchQueueStatus.RUNNING;
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
