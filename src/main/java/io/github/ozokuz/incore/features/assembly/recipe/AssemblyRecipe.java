package io.github.ozokuz.incore.features.assembly.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;

public interface AssemblyRecipe extends Recipe<CraftingInput> {
    int tier();

    int craftTimeTicks();

    AssemblyTierBehavior tierBehavior();

    ItemStack resultStack();

    @Override
    default boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    ItemStack getResultItem(HolderLookup.Provider registries);
}
