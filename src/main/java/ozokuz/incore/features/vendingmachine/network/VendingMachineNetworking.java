package ozokuz.incore.features.vendingmachine.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class VendingMachineNetworking {
    private VendingMachineNetworking() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(OpenVendingMachinePayload.TYPE, OpenVendingMachinePayload.STREAM_CODEC, OpenVendingMachinePayload::handle);
        registrar.playToServer(BuyVendingMachineOfferPayload.TYPE, BuyVendingMachineOfferPayload.STREAM_CODEC, BuyVendingMachineOfferPayload::handle);
    }

    public static void openVendingMachineScreen(ServerPlayer player, String json) {
        PacketDistributor.sendToPlayer(player, new OpenVendingMachinePayload(json));
    }

    public static void sendVendingMachinePurchase(ResourceLocation offerId, long vending_machinePosLong, int quantity, boolean allowConversion) {
        PacketDistributor.sendToServer(new BuyVendingMachineOfferPayload(offerId.toString(), vending_machinePosLong, quantity, allowConversion));
    }
}
