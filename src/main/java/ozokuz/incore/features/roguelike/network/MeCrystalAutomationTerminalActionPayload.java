package ozokuz.incore.features.roguelike.network;

import ozokuz.incore.features.roguelike.content.MeCrystalAutomationTerminalMenu;
import ozokuz.incore.features.roguelike.content.MeCrystalAutomationTerminalPart;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record MeCrystalAutomationTerminalActionPayload(long hostPos, int side, int action) implements CustomPacketPayload {
    public static final int ACTION_REQUEST_ITEMS = 0;
    public static final int ACTION_REFRESH = 1;

    public static final Type<MeCrystalAutomationTerminalActionPayload> TYPE = new Type<>(ResourceLocation.parse("incore:me_crystal_automation_terminal_action"));
    public static final StreamCodec<ByteBuf, MeCrystalAutomationTerminalActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG,
            MeCrystalAutomationTerminalActionPayload::hostPos,
            ByteBufCodecs.VAR_INT,
            MeCrystalAutomationTerminalActionPayload::side,
            ByteBufCodecs.VAR_INT,
            MeCrystalAutomationTerminalActionPayload::action,
            MeCrystalAutomationTerminalActionPayload::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MeCrystalAutomationTerminalActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!(player.containerMenu instanceof MeCrystalAutomationTerminalMenu menu)) {
                return;
            }
            BlockPos payloadPos = BlockPos.of(payload.hostPos());
            Direction payloadSide = Direction.from3DDataValue(payload.side());
            if (!menu.hostPos().equals(payloadPos) || menu.side() != payloadSide) {
                return;
            }
            MeCrystalAutomationTerminalPart terminal = menu.part();
            if (terminal == null) {
                return;
            }
            if (payload.action() == ACTION_REFRESH) {
                terminal.refreshSnapshot();
            } else if (payload.action() == ACTION_REQUEST_ITEMS) {
                terminal.requestMissingItems();
            }
        });
    }
}
