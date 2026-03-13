package io.github.ozokuz.incore.features.research.network;

import io.github.ozokuz.incore.features.research.ResearchManager;
import io.github.ozokuz.incore.features.research.team.ResearchTeamResolver;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ResearchQueueResearchPayload(String nodeId) implements CustomPacketPayload {
    public static final Type<ResearchQueueResearchPayload> TYPE = new Type<>(ResourceLocation.parse("incore:research_queue_research"));
    public static final StreamCodec<ByteBuf, ResearchQueueResearchPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            ResearchQueueResearchPayload::nodeId,
            ResearchQueueResearchPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ResearchQueueResearchPayload payload, IPayloadContext context) {
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

            if (!ResearchManager.queueResearch(player.serverLevel().getServer(), teamId, requestedNodeId)) {
                String reason = ResearchManager.explainQueueFailure(player.serverLevel().getServer(), teamId, requestedNodeId);
                player.sendSystemMessage(Component.translatable("incore.research.queue_rejected", reason));
            }
        });
    }
}
