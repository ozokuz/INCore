package io.github.ozokuz.incore.features.cards.network;

import io.github.ozokuz.incore.features.cards.CardVendorService;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record BuyCardVendorOfferPayload(
        String offerId,
        long vendorPosLong,
        int quantity,
        boolean allowSpurConversion
) implements CustomPacketPayload {
    public static final Type<BuyCardVendorOfferPayload> TYPE = new Type<>(ResourceLocation.parse("incore:cards_vendor_buy"));
    public static final StreamCodec<ByteBuf, BuyCardVendorOfferPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            BuyCardVendorOfferPayload::offerId,
            ByteBufCodecs.VAR_LONG,
            BuyCardVendorOfferPayload::vendorPosLong,
            ByteBufCodecs.VAR_INT,
            BuyCardVendorOfferPayload::quantity,
            ByteBufCodecs.BOOL,
            BuyCardVendorOfferPayload::allowSpurConversion,
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
            if (offerId == null || payload.quantity() <= 0) {
                return;
            }

            BlockPos vendorPos = BlockPos.of(payload.vendorPosLong());
            if (CardVendorService.purchase(player, vendorPos, offerId, payload.quantity(), payload.allowSpurConversion())) {
                CardVendorService.openVendorScreen(player, vendorPos);
            }
        });
    }
}
