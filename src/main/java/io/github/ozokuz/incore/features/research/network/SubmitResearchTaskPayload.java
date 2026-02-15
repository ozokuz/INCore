package io.github.ozokuz.incore.features.research.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SubmitResearchTaskPayload(String taskId) implements CustomPacketPayload {
    public static final Type<SubmitResearchTaskPayload> TYPE = new Type<>(ResourceLocation.parse("incore:submit_research_task"));
    public static final StreamCodec<ByteBuf, SubmitResearchTaskPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            SubmitResearchTaskPayload::taskId,
            SubmitResearchTaskPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SubmitResearchTaskPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            ResourceLocation id = ResourceLocation.tryParse(payload.taskId());
            if (id != null) {
                ResearchNetworking.submitTask(player, id);
            }
        });
    }
}
