package io.github.ozokuz.incore.features.research;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.HashSet;
import java.util.Set;

public final class ResearchRecipeLockService {
    private ResearchRecipeLockService() {
    }

    public static boolean isRecipeLocked(ServerPlayer player, ResourceLocation recipeId) {
        if (recipeId == null) {
            return false;
        }
        Set<ResourceLocation> unlocked = unlockedLockSets(player);
        for (ResearchRecipeLockSetData lockSet : ResearchRecipeLockManager.all().values()) {
            if (unlocked.contains(lockSet.id())) {
                continue;
            }
            if (lockSet.recipes().contains(recipeId)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isOutputLocked(ServerPlayer player, ItemStack output) {
        if (output.isEmpty()) {
            return false;
        }
        RecipeManager recipeManager = player.serverLevel().getRecipeManager();
        for (ResourceLocation lockedId : lockedRecipeIds(player)) {
            RecipeHolder<?> holder = recipeManager.byKey(lockedId).orElse(null);
            if (holder == null) {
                continue;
            }
            ItemStack result = holder.value().getResultItem(player.registryAccess());
            if (!result.isEmpty() && ItemStack.isSameItemSameComponents(result, output)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isOutputLockedForOwnerScope(ServerLevel level, String ownerScopeKey, ItemStack output) {
        if (output.isEmpty() || ownerScopeKey == null || ownerScopeKey.isBlank()) {
            return false;
        }

        RecipeManager recipeManager = level.getRecipeManager();
        for (ResourceLocation lockedId : lockedRecipeIdsForOwnerScope(level, ownerScopeKey)) {
            RecipeHolder<?> holder = recipeManager.byKey(lockedId).orElse(null);
            if (holder == null) {
                continue;
            }
            ItemStack result = holder.value().getResultItem(level.registryAccess());
            if (!result.isEmpty() && ItemStack.isSameItemSameComponents(result, output)) {
                return true;
            }
        }
        return false;
    }

    public static Set<ResourceLocation> lockedRecipeIds(ServerPlayer player) {
        Set<ResourceLocation> unlocked = unlockedLockSets(player);
        Set<ResourceLocation> locked = new HashSet<>();
        for (ResearchRecipeLockSetData lockSet : ResearchRecipeLockManager.all().values()) {
            if (unlocked.contains(lockSet.id())) {
                continue;
            }
            locked.addAll(lockSet.recipes());
            locked.addAll(resolveTagMembers(lockSet.recipeTags()));
        }
        return locked;
    }

    private static Set<ResourceLocation> lockedRecipeIdsForOwnerScope(ServerLevel level, String ownerScopeKey) {
        Set<ResourceLocation> unlocked = unlockedLockSetsForOwnerScope(level, ownerScopeKey);
        Set<ResourceLocation> locked = new HashSet<>();
        for (ResearchRecipeLockSetData lockSet : ResearchRecipeLockManager.all().values()) {
            if (unlocked.contains(lockSet.id())) {
                continue;
            }
            locked.addAll(lockSet.recipes());
            locked.addAll(resolveTagMembers(lockSet.recipeTags()));
        }
        return locked;
    }

    private static Set<ResourceLocation> unlockedLockSets(ServerPlayer player) {
        Set<ResourceLocation> lockSets = new HashSet<>();
        for (ResourceLocation unlockedResearch : ResearchProgressService.unlocked(player)) {
            ResearchEntryData entry = ResearchEntryManager.all().get(unlockedResearch);
            if (entry == null) {
                continue;
            }
            lockSets.addAll(entry.recipeLockSets());
        }
        return lockSets;
    }

    private static Set<ResourceLocation> unlockedLockSetsForOwnerScope(ServerLevel level, String ownerScopeKey) {
        Set<ResourceLocation> lockSets = new HashSet<>();
        for (ResourceLocation unlockedResearch : ResearchProgressService.unlockedForOwnerKey(level.getServer(), ownerScopeKey)) {
            ResearchEntryData entry = ResearchEntryManager.all().get(unlockedResearch);
            if (entry == null) {
                continue;
            }
            lockSets.addAll(entry.recipeLockSets());
        }
        return lockSets;
    }

    private static Set<ResourceLocation> resolveTagMembers(Set<ResourceLocation> recipeTagIds) {
        // Recipe-tag expansion depends on internal recipe-holder tag APIs that vary by mappings/runtime.
        // Keep direct recipe id locks authoritative and treat tag entries as informational for now.
        return Set.of();
    }
}
