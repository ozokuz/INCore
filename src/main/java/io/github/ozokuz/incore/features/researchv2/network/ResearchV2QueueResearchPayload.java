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

public record ResearchV2QueueResearchPayload(String nodeId) implements CustomPacketPayload {
    public static final Type<ResearchV2QueueResearchPayload> TYPE = new Type<>(ResourceLocation.parse("incore:research_v2_queue_research"));
    public static final StreamCodec<ByteBuf, ResearchV2QueueResearchPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            ResearchV2QueueResearchPayload::nodeId,
            ResearchV2QueueResearchPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ResearchV2QueueResearchPayload payload, IPayloadContext context) {
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

            ResearchManager.queueResearch(player.serverLevel().getServer(), teamId, requestedNodeId);
        });
    }
}
