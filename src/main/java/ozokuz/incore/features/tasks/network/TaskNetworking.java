package ozokuz.incore.features.tasks.network;

import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class TaskNetworking {
    private TaskNetworking() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(TaskClaimRewardsPayload.TYPE, TaskClaimRewardsPayload.STREAM_CODEC, TaskClaimRewardsPayload::handle);
    }

    public static void requestDailyRewardClaim() {
        PacketDistributor.sendToServer(new TaskClaimRewardsPayload(TaskClaimRewardsPayload.CLAIM_DAILY));
    }

    public static void requestWeeklyRewardsClaim() {
        PacketDistributor.sendToServer(new TaskClaimRewardsPayload(TaskClaimRewardsPayload.CLAIM_WEEKLY_UNLOCKED));
    }
}
