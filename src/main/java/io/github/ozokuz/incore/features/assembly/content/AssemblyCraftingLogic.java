package io.github.ozokuz.incore.features.assembly.content;

import io.github.ozokuz.incore.features.assembly.recipe.AssemblyRecipe;
import io.github.ozokuz.incore.features.assembly.recipe.AssemblyTierBehavior;
import net.minecraft.core.HolderLookup;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class AssemblyCraftingLogic {
    private AssemblyCraftingLogic() {
    }

    public static CraftOutcome success(AssemblyRecipe recipe, int machineTier, HolderLookup.Provider registries) {
        List<ItemStack> outputs = new ArrayList<>();
        outputs.add(recipe.getResultItem(registries));
        if (machineTier >= 3) {
            recipe.tierBehavior().t3().leftoverOutputs().forEach(output -> outputs.add(output.toStack(registries)));
        }
        return new CraftOutcome(true, outputs);
    }

    public static CraftOutcome resolveAutoOutcome(AssemblyRecipe recipe, int machineTier, HolderLookup.Provider registries, RandomSource random) {
        AssemblyTierBehavior.TierOutcome behavior = recipe.tierBehavior().forTier(machineTier);
        if (machineTier == 3) {
            return success(recipe, machineTier, registries);
        }
        if (behavior.failureChance() > 0.0D && random.nextDouble() < behavior.failureChance()) {
            List<ItemStack> outputs = new ArrayList<>();
            behavior.failureOutputs().forEach(output -> outputs.add(output.toStack(registries)));
            if (machineTier == 2) {
                behavior.recycleOutputs().forEach(output -> outputs.add(output.toStack(registries)));
            }
            return new CraftOutcome(false, outputs);
        }
        return success(recipe, machineTier, registries);
    }

    public record CraftOutcome(boolean success, List<ItemStack> outputs) {
    }
}
