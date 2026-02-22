package io.github.ozokuz.incore.features.gacha.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ClaimBasicGuaranteedSixPayload(String bannerId, String itemId) implements CustomPacketPayload {
    public static final Type<ClaimBasicGuaranteedSixPayload> TYPE = new Type<>(ResourceLocation.parse("incore:gacha_claim_basic_six"));
    public static final StreamCodec<ByteBuf, ClaimBasicGuaranteedSixPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            ClaimBasicGuaranteedSixPayload::bannerId,
            ByteBufCodecs.STRING_UTF8,
            ClaimBasicGuaranteedSixPayload::itemId,
            ClaimBasicGuaranteedSixPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ClaimBasicGuaranteedSixPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            ResourceLocation bannerId = ResourceLocation.tryParse(payload.bannerId());
            ResourceLocation itemId = ResourceLocation.tryParse(payload.itemId());
            if (bannerId == null || itemId == null) {
                return;
            }

            GachaNetworking.claimBasicGuaranteedSix(player, bannerId, itemId);
        });
    }
}
