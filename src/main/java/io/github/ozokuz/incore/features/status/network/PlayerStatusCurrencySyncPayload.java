package io.github.ozokuz.incore.features.status.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record PlayerStatusCurrencySyncPayload(List<BalanceEntry> balances) implements CustomPacketPayload {
    public static final Type<PlayerStatusCurrencySyncPayload> TYPE =
            new Type<>(ResourceLocation.parse("incore:player_status_currency_sync"));
    public static final StreamCodec<ByteBuf, PlayerStatusCurrencySyncPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public PlayerStatusCurrencySyncPayload decode(ByteBuf buffer) {
            FriendlyByteBuf buf = new FriendlyByteBuf(buffer);
            int count = buf.readVarInt();
            List<BalanceEntry> decoded = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                decoded.add(new BalanceEntry(buf.readUtf(256), buf.readVarInt()));
            }
            return new PlayerStatusCurrencySyncPayload(decoded);
        }

        @Override
        public void encode(ByteBuf buffer, PlayerStatusCurrencySyncPayload payload) {
            FriendlyByteBuf buf = new FriendlyByteBuf(buffer);
            buf.writeVarInt(payload.balances().size());
            for (BalanceEntry entry : payload.balances()) {
                buf.writeUtf(entry.iconItemId(), 256);
                buf.writeVarInt(Math.max(0, entry.amount()));
            }
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PlayerStatusCurrencySyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> PlayerStatusCurrencyClientCache.update(
                payload.balances().stream()
                        .map(balance -> new PlayerStatusCurrencyClientCache.CurrencyEntry(
                                balance.iconItemId(),
                                Math.max(0, balance.amount())
                        ))
                        .toList()
        ));
    }

    public record BalanceEntry(String iconItemId, int amount) {
    }
}
