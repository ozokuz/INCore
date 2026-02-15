package io.github.ozokuz.incore.features.research.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record UnlockResearchEntryPayload(String entryId) implements CustomPacketPayload {
    public static final Type<UnlockResearchEntryPayload> TYPE = new Type<>(ResourceLocation.parse("incore:unlock_research_entry"));
    public static final StreamCodec<ByteBuf, UnlockResearchEntryPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            UnlockResearchEntryPayload::entryId,
            UnlockResearchEntryPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UnlockResearchEntryPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            ResourceLocation id = ResourceLocation.tryParse(payload.entryId());
            if (id != null) {
                ResearchNetworking.unlock(player, id);
            }
        });
    }
}
