package io.github.ozokuz.incore.features.research.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RemoveResearchQueueEntryPayload(String entryId) implements CustomPacketPayload {
    public static final Type<RemoveResearchQueueEntryPayload> TYPE = new Type<>(ResourceLocation.parse("incore:remove_research_queue_entry"));
    public static final StreamCodec<ByteBuf, RemoveResearchQueueEntryPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            RemoveResearchQueueEntryPayload::entryId,
            RemoveResearchQueueEntryPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RemoveResearchQueueEntryPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            ResourceLocation id = ResourceLocation.tryParse(payload.entryId());
            if (id != null) {
                ResearchNetworking.removeQueueEntry(player, id);
            }
        });
    }
}
