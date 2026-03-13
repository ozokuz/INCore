package io.github.ozokuz.incore.features.research.network;

import io.github.ozokuz.incore.features.research.ResearchManager;
import io.github.ozokuz.incore.features.research.team.ResearchTeamResolver;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ResearchRequestSnapshotPayload(boolean request) implements CustomPacketPayload {
    public static final Type<ResearchRequestSnapshotPayload> TYPE = new Type<>(ResourceLocation.parse("incore:research_request_snapshot"));
    public static final StreamCodec<ByteBuf, ResearchRequestSnapshotPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            ResearchRequestSnapshotPayload::request,
            ResearchRequestSnapshotPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ResearchRequestSnapshotPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!payload.request() || !(context.player() instanceof ServerPlayer player)) {
                return;
            }

            String teamId = ResearchTeamResolver.resolveTeamId(player);
            if (teamId == null || teamId.isBlank()) {
                return;
            }

            ResearchManager.ensureTeamState(player.serverLevel().getServer(), teamId);
            ResearchNetworking.syncToPlayer(player);
        });
    }
}
