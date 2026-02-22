package io.github.ozokuz.incore.mixin.research;

import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity;
import com.simibubi.create.content.kinetics.crafter.RecipeGridHandler;
import io.github.ozokuz.incore.features.research.MechanicalCrafterOwnershipAccess;
import io.github.ozokuz.incore.features.research.ResearchEvents;
import io.github.ozokuz.incore.features.research.ResearchRecipeLockService;
import io.github.ozokuz.incore.features.research.ResearchScopeResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(value = MechanicalCrafterBlockEntity.class, remap = false)
public abstract class MechanicalCrafterBlockEntityMixin implements MechanicalCrafterOwnershipAccess {
    @Unique private static final String INCORE_OWNER_UUID_NBT = "incoreOwner";
    @Unique private static final String INCORE_OWNER_SCOPE_NBT = "incoreOwnerScope";
    @Unique private @Nullable UUID incore$ownerId;
    @Unique private String incore$ownerScopeKey = "";

    @Inject(method = "write", at = @At("TAIL"), remap = false)
    private void incore$saveOwnership(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        if (incore$ownerId != null) {
            tag.putUUID(INCORE_OWNER_UUID_NBT, incore$ownerId);
        }
        if (incore$ownerScopeKey != null && !incore$ownerScopeKey.isBlank()) {
            tag.putString(INCORE_OWNER_SCOPE_NBT, incore$ownerScopeKey);
        }
    }

    @Inject(method = "read", at = @At("TAIL"), remap = false)
    private void incore$loadOwnership(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        incore$ownerId = tag.hasUUID(INCORE_OWNER_UUID_NBT) ? tag.getUUID(INCORE_OWNER_UUID_NBT) : null;
        incore$ownerScopeKey = tag.getString(INCORE_OWNER_SCOPE_NBT);
        if ((incore$ownerScopeKey == null || incore$ownerScopeKey.isBlank()) && incore$ownerId != null) {
            incore$ownerScopeKey = "player:" + incore$ownerId;
        }
    }

    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/content/kinetics/crafter/RecipeGridHandler;tryToApplyRecipe(Lnet/minecraft/world/level/Level;Lcom/simibubi/create/content/kinetics/crafter/RecipeGridHandler$GroupedItems;)Lnet/minecraft/world/item/ItemStack;",
                    remap = false
            ),
            remap = false
    )
    private ItemStack incore$preventLockedMechanicalCraft(Level level, RecipeGridHandler.GroupedItems groupedItems) {
        ItemStack result = RecipeGridHandler.tryToApplyRecipe(level, groupedItems);
        if (result == null || result.isEmpty() || level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
            return result;
        }

        MechanicalCrafterBlockEntity self = (MechanicalCrafterBlockEntity) (Object) this;
        ServerPlayer nearest = findNearestServerPlayer(serverLevel, self.getBlockPos(), 24.0D);
        if (nearest != null) {
            if (ResearchRecipeLockService.isOutputLocked(nearest, result)) {
                ResearchEvents.notifyLockedCraft(nearest);
                return null;
            }
            return result;
        }

        String ownerScope = incore$getOwnerScopeKey();
        if (ownerScope.isBlank()) {
            return result;
        }

        if (!ResearchRecipeLockService.isOutputLockedForOwnerScope(serverLevel, ownerScope, result)) {
            return result;
        }

        ServerPlayer notifyTarget = ownerNotificationTarget(serverLevel, incore$ownerId, ownerScope, 24.0D);
        if (notifyTarget != null) {
            ResearchEvents.notifyLockedCraft(notifyTarget);
        }
        return null;
    }

    @Override
    public void incore$setOwnerIfAbsent(ServerPlayer player) {
        if (incore$ownerId != null && incore$ownerScopeKey != null && !incore$ownerScopeKey.isBlank()) {
            return;
        }
        incore$ownerId = player.getUUID();
        incore$ownerScopeKey = ResearchScopeResolver.ownerKey(player);
    }

    @Override
    public @Nullable UUID incore$getOwnerId() {
        return incore$ownerId;
    }

    @Override
    public String incore$getOwnerScopeKey() {
        return incore$ownerScopeKey == null ? "" : incore$ownerScopeKey;
    }

    @Unique
    private @Nullable ServerPlayer ownerNotificationTarget(ServerLevel level, @Nullable UUID ownerId, String ownerScope, double maxDistance) {
        if (ownerId != null) {
            ServerPlayer ownerPlayer = level.getServer().getPlayerList().getPlayer(ownerId);
            if (ownerPlayer != null && ownerPlayer.distanceToSqr(((MechanicalCrafterBlockEntity) (Object) this).getBlockPos().getX() + 0.5D, ((MechanicalCrafterBlockEntity) (Object) this).getBlockPos().getY() + 0.5D, ((MechanicalCrafterBlockEntity) (Object) this).getBlockPos().getZ() + 0.5D) <= maxDistance * maxDistance) {
                return ownerPlayer;
            }
        }

        MechanicalCrafterBlockEntity self = (MechanicalCrafterBlockEntity) (Object) this;
        ServerPlayer nearestTeamMate = null;
        double bestDistanceSqr = maxDistance * maxDistance;
        for (ServerPlayer candidate : level.players()) {
            if (candidate.isSpectator() || !ownerScope.equals(ResearchScopeResolver.ownerKey(candidate))) {
                continue;
            }
            double distanceSqr = candidate.distanceToSqr(self.getBlockPos().getX() + 0.5D, self.getBlockPos().getY() + 0.5D, self.getBlockPos().getZ() + 0.5D);
            if (distanceSqr <= bestDistanceSqr) {
                bestDistanceSqr = distanceSqr;
                nearestTeamMate = candidate;
            }
        }
        return nearestTeamMate;
    }

    @Unique
    private static @Nullable ServerPlayer findNearestServerPlayer(ServerLevel level, BlockPos pos, double maxDistance) {
        double bestDistanceSqr = maxDistance * maxDistance;
        ServerPlayer nearest = null;

        for (ServerPlayer candidate : level.players()) {
            if (candidate.isSpectator()) {
                continue;
            }
            double distanceSqr = candidate.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
            if (distanceSqr > bestDistanceSqr) {
                continue;
            }
            bestDistanceSqr = distanceSqr;
            nearest = candidate;
        }

        return nearest;
    }
}
