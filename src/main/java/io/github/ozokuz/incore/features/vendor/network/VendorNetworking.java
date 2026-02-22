package io.github.ozokuz.incore.features.vendor.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class VendorNetworking {
    private VendorNetworking() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(OpenVendorPayload.TYPE, OpenVendorPayload.STREAM_CODEC, OpenVendorPayload::handle);
        registrar.playToServer(BuyVendorOfferPayload.TYPE, BuyVendorOfferPayload.STREAM_CODEC, BuyVendorOfferPayload::handle);
    }

    public static void openVendorScreen(ServerPlayer player, String json) {
        PacketDistributor.sendToPlayer(player, new OpenVendorPayload(json));
    }

    public static void sendVendorPurchase(ResourceLocation offerId, long vendorPosLong, int quantity, boolean allowConversion) {
        PacketDistributor.sendToServer(new BuyVendorOfferPayload(offerId.toString(), vendorPosLong, quantity, allowConversion));
    }
}
