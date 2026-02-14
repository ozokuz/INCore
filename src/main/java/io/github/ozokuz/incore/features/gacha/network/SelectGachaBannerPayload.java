package io.github.ozokuz.incore.features.gacha.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SelectGachaBannerPayload(String bannerId) implements CustomPacketPayload {
    public static final Type<SelectGachaBannerPayload> TYPE = new Type<>(ResourceLocation.parse("incore:gacha_select_banner"));
    public static final StreamCodec<ByteBuf, SelectGachaBannerPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            SelectGachaBannerPayload::bannerId,
            SelectGachaBannerPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SelectGachaBannerPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            ResourceLocation bannerId = ResourceLocation.tryParse(payload.bannerId());
            if (bannerId == null) {
                return;
            }

            GachaNetworking.applyBannerSelection(player, bannerId);
        });
    }
}
