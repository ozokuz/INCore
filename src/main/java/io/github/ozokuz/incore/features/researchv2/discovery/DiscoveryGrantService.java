package io.github.ozokuz.incore.features.researchv2.discovery;

import io.github.ozokuz.incore.features.researchv2.ResearchManager;
import io.github.ozokuz.incore.features.researchv2.registry.ResearchRegistry;
import io.github.ozokuz.incore.features.researchv2.team.ResearchTeamResolver;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public final class DiscoveryGrantService {
    private DiscoveryGrantService() {
    }

    public static boolean grantFromHeldItem(ServerPlayer player, InteractionHand hand) {
        if (player == null || hand == null) {
            return false;
        }
        return grantFromStack(player, player.getItemInHand(hand), hand);
    }

    public static boolean grantFromStack(ServerPlayer player, ItemStack stack, InteractionHand hand) {
        if (player == null || stack.isEmpty()) {
            return false;
        }

        DiscoveryPayload payload = DiscoveryPayloadData.read(stack);
        if (payload.nodeIds().isEmpty()) {
            player.sendSystemMessage(Component.translatable("incore.discovery.invalid_payload"));
            return false;
        }

        String teamId = ResearchTeamResolver.resolveTeamId(player);
        if (teamId == null || teamId.isBlank()) {
            return false;
        }

        boolean changed = grantFromPayload(player.serverLevel().getServer(), teamId, payload);
        if (!changed) {
            player.sendSystemMessage(Component.translatable("incore.discovery.no_new"));
            return false;
        }

        stack.shrink(1);
        if (hand != null) {
            player.swing(hand, true);
        }
        player.sendSystemMessage(Component.translatable("incore.discovery.granted", payload.displayName().isBlank() ? payload.sourceId() : payload.displayName()));
        return true;
    }

    public static boolean grantFromPayload(MinecraftServer server, String teamId, DiscoveryPayload payload) {
        if (server == null || teamId == null || teamId.isBlank() || payload == null) {
            return false;
        }
        boolean changed = false;
        for (ResourceLocation nodeId : payload.nodeIds()) {
            if (nodeId == null || !ResearchRegistry.nodes().containsKey(nodeId)) {
                continue;
            }
            changed = ResearchManager.grantDiscovery(server, teamId, nodeId, reasonFor(payload, nodeId)) || changed;
        }
        return changed;
    }

    private static String reasonFor(DiscoveryPayload payload, ResourceLocation nodeId) {
        String sourceType = payload.sourceType();
        String sourceId = payload.sourceId();
        return switch (sourceType) {
            case "field_research" -> "field_research:" + sourceId;
            case "datalogger" -> "datalogger:" + sourceId;
            case "continuum_decode" -> "continuum_decode:" + sourceId;
            case "research_sample" -> "research_sample:" + payload.originTeamId() + ":" + nodeId;
            default -> sourceType + ":" + sourceId;
        };
    }
}
