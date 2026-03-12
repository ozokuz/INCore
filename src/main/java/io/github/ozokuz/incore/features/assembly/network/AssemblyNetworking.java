package io.github.ozokuz.incore.features.assembly.network;

import io.github.ozokuz.incore.features.assembly.content.AssemblyStationBlockEntity;
import io.github.ozokuz.incore.features.assembly.content.AutoAssemblerBlockEntity;
import io.github.ozokuz.incore.features.assembly.unlock.AssemblyRecipeUnlockManager;
import io.github.ozokuz.incore.features.researchv2.ResearchManager;
import io.github.ozokuz.incore.features.researchv2.team.ResearchTeamResolver;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;

public final class AssemblyNetworking {
    private AssemblyNetworking() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("assembly");
        registrar.playToClient(AssemblyStateSyncPayload.TYPE, AssemblyStateSyncPayload.STREAM_CODEC, AssemblyStateSyncPayload::handle);
        registrar.playToServer(AssemblyRequestSnapshotPayload.TYPE, AssemblyRequestSnapshotPayload.STREAM_CODEC, AssemblyRequestSnapshotPayload::handle);
        registrar.playToServer(AssemblyCraftPayload.TYPE, AssemblyCraftPayload.STREAM_CODEC, AssemblyCraftPayload::handle);
        registrar.playToServer(AssemblySelectRecipePayload.TYPE, AssemblySelectRecipePayload.STREAM_CODEC, AssemblySelectRecipePayload::handle);
    }

    public static void requestSnapshot() {
        PacketDistributor.sendToServer(new AssemblyRequestSnapshotPayload(true));
    }

    public static void craft(long blockPos, String recipeId) {
        PacketDistributor.sendToServer(new AssemblyCraftPayload(blockPos, recipeId));
    }

    public static void selectRecipe(long blockPos, String recipeId) {
        PacketDistributor.sendToServer(new AssemblySelectRecipePayload(blockPos, recipeId));
    }

    public static void syncToPlayer(ServerPlayer player) {
        String teamId = ResearchTeamResolver.resolveTeamId(player);
        if (teamId == null || teamId.isBlank()) {
            return;
        }
        List<String> unlocked = new ArrayList<>();
        AssemblyRecipeUnlockManager.all().forEach((recipeId, nodeId) -> {
            if (ResearchManager.isResearched(player.serverLevel().getServer(), teamId, nodeId)) {
                unlocked.add(recipeId.toString());
            }
        });
        unlocked.sort(String::compareTo);
        PacketDistributor.sendToPlayer(player, new AssemblyStateSyncPayload(unlocked));
    }

    public static void syncTeam(MinecraftServer server, String teamId) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            String playerTeamId = ResearchTeamResolver.resolveTeamId(player);
            if (teamId.equals(playerTeamId)) {
                syncToPlayer(player);
            }
        }
    }
}
