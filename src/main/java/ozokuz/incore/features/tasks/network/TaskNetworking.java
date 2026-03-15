package ozokuz.incore.features.tasks.network;

import ozokuz.incore.features.tasks.TaskService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class TaskNetworking {
    private TaskNetworking() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(TaskSyncPayload.TYPE, TaskSyncPayload.STREAM_CODEC, TaskSyncPayload::handle);
        registrar.playToServer(TaskClaimRewardsPayload.TYPE, TaskClaimRewardsPayload.STREAM_CODEC, TaskClaimRewardsPayload::handle);
    }

    public static void syncToPlayer(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new TaskSyncPayload(TaskService.buildSyncJson(player)));
    }

    public static void requestDailyRewardClaim() {
        PacketDistributor.sendToServer(new TaskClaimRewardsPayload(TaskClaimRewardsPayload.CLAIM_DAILY));
    }

    public static void requestWeeklyRewardsClaim() {
        PacketDistributor.sendToServer(new TaskClaimRewardsPayload(TaskClaimRewardsPayload.CLAIM_WEEKLY_UNLOCKED));
    }

    public static void applyDailyRewardClaim(ServerPlayer player) {
        TaskService.claimDailyCompletionReward(player);
        syncToPlayer(player);
    }

    public static void applyWeeklyRewardsClaim(ServerPlayer player) {
        TaskService.claimUnlockedWeeklyTierRewards(player);
        syncToPlayer(player);
    }
}
