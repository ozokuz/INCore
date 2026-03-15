package ozokuz.incore.features.entropy.network;

import ozokuz.incore.Config;
import ozokuz.incore.features.entropy.EntropyManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class EntropyNetworking {
    private EntropyNetworking() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(EntropySyncPayload.TYPE, EntropySyncPayload.STREAM_CODEC, EntropySyncPayload::handle);
        registrar.playToClient(EntropyBoosterGainPayload.TYPE, EntropyBoosterGainPayload.STREAM_CODEC, EntropyBoosterGainPayload::handle);
    }

    public static void syncToPlayer(ServerPlayer player) {
        int current = EntropyManager.getCurrentEntropy(player);
        int cap = EntropyManager.getEntropyCap(player);
        int regenPerTick = Config.ENTROPY_REGEN_PER_MINUTE.get();
        long regenIntervalMillis = EntropyManager.getRegenIntervalMillis();
        long millisUntilNextIncrease = EntropyManager.getMillisUntilNextIncrease(player);
        long millisUntilFull = EntropyManager.getMillisUntilFull(player);

        PacketDistributor.sendToPlayer(player, new EntropySyncPayload(
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

        PacketDistributor.sendToPlayer(player, new EntropyBoosterGainPayload(from, to, cap, gain));
    }
}
