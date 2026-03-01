package io.github.ozokuz.incore.features.researchv2.network;

import io.github.ozokuz.incore.features.researchv2.ResearchManager;
import io.github.ozokuz.incore.features.researchv2.team.ResearchTeamResolver;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ResearchV2RequestSnapshotPayload(boolean request) implements CustomPacketPayload {
    public static final Type<ResearchV2RequestSnapshotPayload> TYPE = new Type<>(ResourceLocation.parse("incore:research_v2_request_snapshot"));
    public static final StreamCodec<ByteBuf, ResearchV2RequestSnapshotPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            ResearchV2RequestSnapshotPayload::request,
            ResearchV2RequestSnapshotPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ResearchV2RequestSnapshotPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!payload.request() || !(context.player() instanceof ServerPlayer player)) {
                return;
            }

            String teamId = ResearchTeamResolver.resolveTeamId(player);
            if (teamId == null || teamId.isBlank()) {
                return;
            }

            ResearchManager.ensureTeamState(player.serverLevel().getServer(), teamId);
            ResearchV2Networking.syncToPlayer(player);
        });
    }
}
