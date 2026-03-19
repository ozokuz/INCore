package ozokuz.incore.features.gacha.network;

import ozokuz.incore.features.gacha.GachaService;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.network.PacketDistributor;

public final class GachaNetworking {
    private GachaNetworking() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(SelectGachaBannerPayload.TYPE, SelectGachaBannerPayload.STREAM_CODEC, SelectGachaBannerPayload::handle);
        registrar.playToServer(BuyGachaBannerPayload.TYPE, BuyGachaBannerPayload.STREAM_CODEC, BuyGachaBannerPayload::handle);
        registrar.playToServer(ClaimBasicGuaranteedSixPayload.TYPE, ClaimBasicGuaranteedSixPayload.STREAM_CODEC, ClaimBasicGuaranteedSixPayload::handle);
    }

    public static void sendBannerSelection(ResourceLocation bannerId) {
        PacketDistributor.sendToServer(new SelectGachaBannerPayload(bannerId.toString()));
    }

    public static void sendBannerPurchase(ResourceLocation bannerId) {
        PacketDistributor.sendToServer(new BuyGachaBannerPayload(bannerId.toString()));
    }

    public static void sendBasicGuaranteedSixClaim(ResourceLocation bannerId, ResourceLocation itemId) {
        PacketDistributor.sendToServer(new ClaimBasicGuaranteedSixPayload(bannerId.toString(), itemId.toString()));
    }

    public static void applyBannerPurchase(ServerPlayer player, ResourceLocation bannerId) {
        GachaService.acquireCrateForBanner(player, bannerId);
    }

    public static void claimBasicGuaranteedSix(ServerPlayer player, ResourceLocation bannerId, ResourceLocation itemId) {
        GachaService.claimBasicGuaranteedSix(player, bannerId, itemId);
    }
}
