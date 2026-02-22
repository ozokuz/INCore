package io.github.ozokuz.incore.features.research.client;

import io.github.ozokuz.incore.INCore;
import io.github.ozokuz.incore.features.research.ResearchEntryData;
import io.github.ozokuz.incore.features.research.ResearchEntryManager;
import io.github.ozokuz.incore.features.research.ResearchRecipeLockManager;
import io.github.ozokuz.incore.features.research.ResearchRecipeLockSetData;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ResearchRecipeLockClientCache {
    private static volatile Set<ResourceLocation> lockedRecipeIds = Set.of();
    private static volatile Map<ResourceLocation, List<String>> recipeResearchTitles = Map.of();
    private static volatile boolean mappingDirty = true;

    private static final Set<ResourceLocation> duplicateLogReported = new HashSet<>();
    private static boolean duplicateChatReported;

    private ResearchRecipeLockClientCache() {
    }

    public static synchronized void setLockedRecipeIds(Set<ResourceLocation> ids) {
        lockedRecipeIds = Set.copyOf(ids);
    }

    public static boolean isLocked(ResourceLocation recipeId) {
        return recipeId != null && lockedRecipeIds.contains(recipeId);
    }

    public static Set<ResourceLocation> allLocked() {
        return lockedRecipeIds;
    }

    public static synchronized void markDataReloaded() {
        mappingDirty = true;
        resetDuplicateSessionWarnings();
    }

    public static synchronized void onWorldJoined() {
        mappingDirty = true;
        resetDuplicateSessionWarnings();
    }

    public static synchronized boolean consumeDuplicateChatWarning() {
        if (duplicateChatReported) {
            return false;
        }
        duplicateChatReported = true;
        return true;
    }

    public static synchronized LockDisplay lockDisplay(ResourceLocation recipeId) {
        if (recipeId == null) {
            return new LockDisplay(null, 0);
        }
        ensureMappings();
        List<String> titles = recipeResearchTitles.getOrDefault(recipeId, List.of());
        if (titles.isEmpty()) {
            return new LockDisplay(null, 0);
        }

        if (titles.size() > 1 && duplicateLogReported.add(recipeId)) {
            INCore.LOGGER.error(
                    "Recipe {} is locked by multiple research entries {}. Using first match '{}'.",
                    recipeId,
                    titles,
                    titles.getFirst()
            );
        }
        return new LockDisplay(titles.getFirst(), titles.size());
    }

    private static void ensureMappings() {
        if (!mappingDirty) {
            return;
        }

        Map<ResourceLocation, List<String>> next = new LinkedHashMap<>();
        ResearchEntryManager.all().entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> {
                    ResearchEntryData research = entry.getValue();
                    String title = displayTitle(entry.getKey(), research);
                    for (ResourceLocation lockSetId : research.recipeLockSets()) {
                        ResearchRecipeLockSetData lockSet = ResearchRecipeLockManager.get(lockSetId);
                        if (lockSet == null) {
                            continue;
                        }
                        for (ResourceLocation recipeId : lockSet.recipes()) {
                            List<String> titles = next.computeIfAbsent(recipeId, id -> new ArrayList<>());
                            if (!titles.contains(title)) {
                                titles.add(title);
                            }
                        }
                    }
                });

        next.replaceAll((id, titles) -> List.copyOf(titles));
        recipeResearchTitles = Map.copyOf(next);
        mappingDirty = false;
    }

    private static String displayTitle(ResourceLocation id, ResearchEntryData data) {
        if (data == null || data.title() == null || data.title().isBlank()) {
            return id.toString();
        }
        return data.title();
    }

    private static void resetDuplicateSessionWarnings() {
        duplicateLogReported.clear();
        duplicateChatReported = false;
    }

    public record LockDisplay(@Nullable String primaryResearchTitle, int candidateCount) {
        public boolean hasDuplicateCandidates() {
            return candidateCount > 1;
        }
    }
}
