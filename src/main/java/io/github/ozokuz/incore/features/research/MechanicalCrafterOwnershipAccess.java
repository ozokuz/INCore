package io.github.ozokuz.incore.features.research;

import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface MechanicalCrafterOwnershipAccess {
    void incore$setOwnerIfAbsent(ServerPlayer player);

    @Nullable UUID incore$getOwnerId();

    String incore$getOwnerScopeKey();
}
