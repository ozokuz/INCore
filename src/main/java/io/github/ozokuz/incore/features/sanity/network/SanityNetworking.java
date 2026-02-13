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
    }

    public static void syncToPlayer(ServerPlayer player) {
        int current = SanityManager.getCurrentSanity(player);
        int cap = SanityManager.getSanityCap(player);
        int regenPerMinute = Config.SANITY_REGEN_PER_MINUTE.get();
        long millisUntilNextIncrease = SanityManager.getMillisUntilNextIncrease(player);
        long millisUntilFull = SanityManager.getMillisUntilFull(player);

        PacketDistributor.sendToPlayer(player, new SanitySyncPayload(
                current,
                cap,
                regenPerMinute,
                millisUntilNextIncrease,
                millisUntilFull
        ));
    }
}
