package ozokuz.incore.features.vendingmachine.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.lang.reflect.InvocationTargetException;

public record OpenVendingMachinePayload(String json) implements CustomPacketPayload {
    public static final Type<OpenVendingMachinePayload> TYPE = new Type<>(ResourceLocation.parse("incore:vending_machine_open"));
    public static final StreamCodec<ByteBuf, OpenVendingMachinePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            OpenVendingMachinePayload::json,
            OpenVendingMachinePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenVendingMachinePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> openClient(payload.json()));
    }

    private static void openClient(String json) {
        try {
            Class<?> handler = Class.forName("ozokuz.incore.client.features.vendingmachine.VendingMachineClientPayloadHandlers");
            handler.getMethod("openVendingMachineScreen", String.class).invoke(null, json);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
        }
    }
}
