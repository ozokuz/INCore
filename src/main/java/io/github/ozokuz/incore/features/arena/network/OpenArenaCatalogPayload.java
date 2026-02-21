package io.github.ozokuz.incore.features.arena.network;

import io.github.ozokuz.incore.client.arena.CombatCatalogScreen;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenArenaCatalogPayload(String json) implements CustomPacketPayload {
    public static final Type<OpenArenaCatalogPayload> TYPE = new Type<>(ResourceLocation.parse("incore:open_arena_catalog"));
    public static final StreamCodec<ByteBuf, OpenArenaCatalogPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            OpenArenaCatalogPayload::json,
            OpenArenaCatalogPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenArenaCatalogPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen instanceof CombatCatalogScreen screen) {
                screen.updatePayload(payload.json());
                return;
            }

            minecraft.setScreen(new CombatCatalogScreen(payload.json()));
        });
    }
}
