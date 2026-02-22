package io.github.ozokuz.incore.features.vendor.network;

import io.github.ozokuz.incore.features.vendor.VendorService;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record BuyVendorOfferPayload(
        String offerId,
        long vendorPosLong,
        int quantity,
        boolean allowConversion
) implements CustomPacketPayload {
    public static final Type<BuyVendorOfferPayload> TYPE = new Type<>(ResourceLocation.parse("incore:vendor_buy"));
    public static final StreamCodec<ByteBuf, BuyVendorOfferPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            BuyVendorOfferPayload::offerId,
            ByteBufCodecs.VAR_LONG,
            BuyVendorOfferPayload::vendorPosLong,
            ByteBufCodecs.VAR_INT,
            BuyVendorOfferPayload::quantity,
            ByteBufCodecs.BOOL,
            BuyVendorOfferPayload::allowConversion,
            BuyVendorOfferPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BuyVendorOfferPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            ResourceLocation offerId = ResourceLocation.tryParse(payload.offerId());
            if (offerId == null || payload.quantity() <= 0) {
                return;
            }

            BlockPos vendorPos = BlockPos.of(payload.vendorPosLong());
            if (VendorService.purchase(player, vendorPos, offerId, payload.quantity(), payload.allowConversion())) {
                VendorService.openVendorScreen(player, vendorPos);
            }
        });
    }
}
