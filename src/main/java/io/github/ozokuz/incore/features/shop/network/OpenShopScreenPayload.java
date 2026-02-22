package io.github.ozokuz.incore.features.shop.network;

import io.github.ozokuz.incore.features.shop.client.ShopPayloadUpdatable;
import io.github.ozokuz.incore.features.shop.client.ShopSelectionScreen;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenShopScreenPayload(String json) implements CustomPacketPayload {
    private static final int MAX_JSON_BYTES = 4 * 1024 * 1024;
    public static final Type<OpenShopScreenPayload> TYPE = new Type<>(ResourceLocation.parse("incore:open_shop_screen"));
    public static final StreamCodec<ByteBuf, OpenShopScreenPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(MAX_JSON_BYTES),
            OpenShopScreenPayload::json,
            OpenShopScreenPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenShopScreenPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen instanceof ShopPayloadUpdatable updatable) {
                updatable.updatePayload(payload.json());
                return;
            }
            minecraft.setScreen(new ShopSelectionScreen(payload.json()));
        });
    }
}
