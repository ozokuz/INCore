package ozokuz.incore.features.research.network;

import ozokuz.incore.features.research.ResearchManager;
import ozokuz.incore.features.research.team.ResearchTeamResolver;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ResearchCancelQueueItemPayload(String nodeId) implements CustomPacketPayload {
    public static final Type<ResearchCancelQueueItemPayload> TYPE = new Type<>(ResourceLocation.parse("incore:research_cancel_queue_item"));
    public static final StreamCodec<ByteBuf, ResearchCancelQueueItemPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            ResearchCancelQueueItemPayload::nodeId,
            ResearchCancelQueueItemPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ResearchCancelQueueItemPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            ResourceLocation requestedNodeId = ResourceLocation.tryParse(payload.nodeId());
            if (requestedNodeId == null) {
                return;
            }

            String teamId = ResearchTeamResolver.resolveTeamId(player);
            if (teamId == null || teamId.isBlank()) {
                return;
            }

            ResearchManager.cancelQueuedResearchCascade(player.serverLevel().getServer(), teamId, requestedNodeId);
        });
    }
}
