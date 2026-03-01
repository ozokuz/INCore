package io.github.ozokuz.incore.features.researchv2.provider;

import net.minecraft.server.MinecraftServer;

public interface IResearchPowerProvider {
    boolean hasPower(MinecraftServer server, String teamId, int amount);

    boolean consumePower(MinecraftServer server, String teamId, int amount);

    int availablePower(MinecraftServer server, String teamId);
}
