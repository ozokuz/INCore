package io.github.ozokuz.incore.features.sanity.network;

import io.github.ozokuz.incore.Config;
import io.github.ozokuz.incore.features.sanity.SanityManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class SanityNetworking {
    private SanityNetworking() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(SanitySyncPayload.TYPE, SanitySyncPayload.STREAM_CODEC, SanitySyncPayload::handle);
        registrar.playToClient(SanityBoosterGainPayload.TYPE, SanityBoosterGainPayload.STREAM_CODEC, SanityBoosterGainPayload::handle);
    }

    public static void syncToPlayer(ServerPlayer player) {
        int current = SanityManager.getCurrentSanity(player);
        int cap = SanityManager.getSanityCap(player);
        int regenPerTick = Config.SANITY_REGEN_PER_MINUTE.get();
        long regenIntervalMillis = SanityManager.getRegenIntervalMillis();
        long millisUntilNextIncrease = SanityManager.getMillisUntilNextIncrease(player);
        long millisUntilFull = SanityManager.getMillisUntilFull(player);

        PacketDistributor.sendToPlayer(player, new SanitySyncPayload(
                current,
                cap,
                regenPerTick,
                regenIntervalMillis,
                millisUntilNextIncrease,
                millisUntilFull
        ));
    }

    public static void sendBoosterGainAnimation(ServerPlayer player, int from, int to, int cap, int gain) {
        if (gain <= 0) {
            return;
        }

        PacketDistributor.sendToPlayer(player, new SanityBoosterGainPayload(from, to, cap, gain));
    }
}
