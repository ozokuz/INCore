package io.github.ozokuz.incore.features.cards.network;

import io.github.ozokuz.incore.features.cards.CardVendorService;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record BuyCardVendorOfferPayload(String offerId) implements CustomPacketPayload {
    public static final Type<BuyCardVendorOfferPayload> TYPE = new Type<>(ResourceLocation.parse("incore:cards_vendor_buy"));
    public static final StreamCodec<ByteBuf, BuyCardVendorOfferPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            BuyCardVendorOfferPayload::offerId,
            BuyCardVendorOfferPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BuyCardVendorOfferPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            ResourceLocation offerId = ResourceLocation.tryParse(payload.offerId());
            if (offerId == null) {
                return;
            }

            if (CardVendorService.purchase(player, offerId)) {
                CardVendorService.openVendorScreen(player);
            }
        });
    }
}
