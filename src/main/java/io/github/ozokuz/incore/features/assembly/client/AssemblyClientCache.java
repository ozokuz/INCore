package io.github.ozokuz.incore.features.assembly.client;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class AssemblyClientCache {
    private static Set<String> unlockedRecipeIds = Set.of();

    private AssemblyClientCache() {
    }

    public static synchronized void update(List<String> recipeIds) {
        unlockedRecipeIds = Set.copyOf(new LinkedHashSet<>(recipeIds));
    }

    public static synchronized Set<String> unlockedRecipeIds() {
        return unlockedRecipeIds;
    }

    public static synchronized boolean isUnlocked(String recipeId) {
        return recipeId != null && unlockedRecipeIds.contains(recipeId);
    }
}
