package ozokuz.incore.features.status.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RequestDungeonDifficultyPayload(boolean request) implements CustomPacketPayload {
    public static final Type<RequestDungeonDifficultyPayload> TYPE =
            new Type<>(ResourceLocation.parse("incore:request_dungeon_difficulty"));
    public static final StreamCodec<ByteBuf, RequestDungeonDifficultyPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            RequestDungeonDifficultyPayload::request,
            RequestDungeonDifficultyPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RequestDungeonDifficultyPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!payload.request() || !(context.player() instanceof ServerPlayer player)) {
                return;
            }
            PlayerStatusNetworking.syncDungeonDifficultyToPlayer(player);
        });
    }
}
