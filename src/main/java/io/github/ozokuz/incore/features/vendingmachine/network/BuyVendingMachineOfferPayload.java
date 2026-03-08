package io.github.ozokuz.incore.features.vendingmachine.network;

import io.github.ozokuz.incore.features.vendingmachine.VendingMachineService;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record BuyVendingMachineOfferPayload(
        String offerId,
        long vending_machinePosLong,
        int quantity,
        boolean allowConversion
) implements CustomPacketPayload {
    public static final Type<BuyVendingMachineOfferPayload> TYPE = new Type<>(ResourceLocation.parse("incore:vending_machine_buy"));
    public static final StreamCodec<ByteBuf, BuyVendingMachineOfferPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            BuyVendingMachineOfferPayload::offerId,
            ByteBufCodecs.VAR_LONG,
            BuyVendingMachineOfferPayload::vending_machinePosLong,
            ByteBufCodecs.VAR_INT,
            BuyVendingMachineOfferPayload::quantity,
            ByteBufCodecs.BOOL,
            BuyVendingMachineOfferPayload::allowConversion,
            BuyVendingMachineOfferPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BuyVendingMachineOfferPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            ResourceLocation offerId = ResourceLocation.tryParse(payload.offerId());
            if (offerId == null || payload.quantity() <= 0) {
                return;
            }

            BlockPos vending_machinePos = BlockPos.of(payload.vending_machinePosLong());
            if (VendingMachineService.purchase(player, vending_machinePos, offerId, payload.quantity(), payload.allowConversion())) {
                VendingMachineService.openVendingMachineScreen(player, vending_machinePos);
            }
        });
    }
}
