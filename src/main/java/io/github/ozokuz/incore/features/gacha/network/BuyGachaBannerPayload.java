package io.github.ozokuz.incore.features.gacha.network;

import io.github.ozokuz.incore.features.gacha.GachaBannerData;
import io.github.ozokuz.incore.features.gacha.GachaBannerManager;
import io.github.ozokuz.incore.features.gacha.GachaService;
import io.github.ozokuz.incore.features.playerlevel.PlayerFeatureUnlockService;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record BuyGachaBannerPayload(String bannerId) implements CustomPacketPayload {
    public static final Type<BuyGachaBannerPayload> TYPE = new Type<>(ResourceLocation.parse("incore:gacha_buy_banner"));
    public static final StreamCodec<ByteBuf, BuyGachaBannerPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            BuyGachaBannerPayload::bannerId,
            BuyGachaBannerPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BuyGachaBannerPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            ResourceLocation bannerId = ResourceLocation.tryParse(payload.bannerId());
            if (bannerId == null) {
                return;
            }
            GachaBannerData banner = GachaBannerManager.get(bannerId);
            if (banner == null) {
                return;
            }
            if (!PlayerFeatureUnlockService.hasUnlocked(player, GachaService.requiredUnlockForBanner(banner))) {
                player.sendSystemMessage(PlayerFeatureUnlockService.lockedMessage(GachaService.requiredUnlockForBanner(banner)));
                return;
            }

            GachaNetworking.applyBannerPurchase(player, bannerId);
        });
    }
}
