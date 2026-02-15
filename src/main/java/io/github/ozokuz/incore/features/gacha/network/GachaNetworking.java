package io.github.ozokuz.incore.features.gacha.network;

import io.github.ozokuz.incore.features.gacha.GachaService;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class GachaNetworking {
    private GachaNetworking() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(OpenGachaBannersPayload.TYPE, OpenGachaBannersPayload.STREAM_CODEC, OpenGachaBannersPayload::handle);
        registrar.playToServer(RequestOpenGachaBannersPayload.TYPE, RequestOpenGachaBannersPayload.STREAM_CODEC, RequestOpenGachaBannersPayload::handle);
        registrar.playToServer(BuyGachaBannerPayload.TYPE, BuyGachaBannerPayload.STREAM_CODEC, BuyGachaBannerPayload::handle);
    }

    public static void openBannerScreen(ServerPlayer player, String json) {
        PacketDistributor.sendToPlayer(player, new OpenGachaBannersPayload(json));
    }

    public static void sendBannerPurchase(ResourceLocation bannerId) {
        PacketDistributor.sendToServer(new BuyGachaBannerPayload(bannerId.toString()));
    }

    public static void requestOpenBannerScreen() {
        PacketDistributor.sendToServer(new RequestOpenGachaBannersPayload(true));
    }

    public static void openBannerScreenFor(ServerPlayer player) {
        GachaService.openBannerScreen(player);
    }

    public static void applyBannerPurchase(ServerPlayer player, ResourceLocation bannerId) {
        GachaService.acquireCrateForBanner(player, bannerId);
    }
}
