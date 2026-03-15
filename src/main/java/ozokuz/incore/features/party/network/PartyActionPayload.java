package ozokuz.incore.features.party.network;

import ozokuz.incore.INCore;
import ozokuz.incore.features.party.PartyService;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public record PartyActionPayload(ActionType actionType, UUID targetPlayerId) implements CustomPacketPayload {
    public static final Type<PartyActionPayload> TYPE = new Type<>(ResourceLocation.parse("incore:party_action"));
    public static final StreamCodec<ByteBuf, PartyActionPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public PartyActionPayload decode(ByteBuf buffer) {
            FriendlyByteBuf buf = new FriendlyByteBuf(buffer);
            ActionType actionType = ActionType.values()[buf.readVarInt()];
            boolean hasTarget = buf.readBoolean();
            UUID targetPlayerId = hasTarget ? buf.readUUID() : null;
            return new PartyActionPayload(actionType, targetPlayerId);
        }

        @Override
        public void encode(ByteBuf buffer, PartyActionPayload payload) {
            FriendlyByteBuf buf = new FriendlyByteBuf(buffer);
            buf.writeVarInt(payload.actionType().ordinal());
            buf.writeBoolean(payload.targetPlayerId() != null);
            if (payload.targetPlayerId() != null) {
                buf.writeUUID(payload.targetPlayerId());
            }
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PartyActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            MinecraftServer server = player.getServer();
            if (server == null) {
                return;
            }

            switch (payload.actionType()) {
                case REQUEST_SYNC -> PartyNetworking.syncStateToPlayer(server, player);
                case CREATE -> PartyService.createParty(player);
                case INVITE -> {
                    if (payload.targetPlayerId() != null) {
                        ServerPlayer target = server.getPlayerList().getPlayer(payload.targetPlayerId());
                        if (target != null) {
                            PartyService.invite(player, target);
                        }
                    }
                }
                case ACCEPT -> PartyService.acceptInvite(player);
                case DECLINE -> PartyService.declineInvite(player);
                case LEAVE -> PartyService.leaveParty(player);
                case KICK -> {
                    if (payload.targetPlayerId() != null) {
                        ServerPlayer target = server.getPlayerList().getPlayer(payload.targetPlayerId());
                        if (target != null) {
                            PartyService.kickMember(player, target);
                        }
                    }
                }
                case PROMOTE -> {
                    if (payload.targetPlayerId() != null) {
                        ServerPlayer target = server.getPlayerList().getPlayer(payload.targetPlayerId());
                        if (target != null) {
                            PartyService.promoteLeader(player, target);
                        }
                    }
                }
                default -> {
                    INCore.LOGGER.warn(
                            "Unhandled party action {} from player {} targeting {}",
                            payload.actionType(),
                            player.getGameProfile().getName(),
                            payload.targetPlayerId()
                    );
                }
            }
        });
    }

    public enum ActionType {
        REQUEST_SYNC,
        CREATE,
        INVITE,
        ACCEPT,
        DECLINE,
        LEAVE,
        KICK,
        PROMOTE
    }
}
