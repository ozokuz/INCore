package io.github.ozokuz.incore.features.assembly.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public final class AssemblyShapelessRecipe extends AbstractAssemblyRecipe {
    private final List<Ingredient> ingredients;

    public AssemblyShapelessRecipe(List<Ingredient> ingredients, ItemStack result, int tier, int craftTimeTicks, AssemblyTierBehavior tierBehavior) {
        super(result, tier, craftTimeTicks, tierBehavior);
        this.ingredients = List.copyOf(ingredients);
    }

    @Override
    public List<Ingredient> ingredients() {
        return ingredients;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        List<ItemStack> stacks = nonEmptyStacks(input);
        if (stacks.size() != ingredients.size()) {
            return false;
        }
        return matchesRemaining(stacks, new boolean[ingredients.size()], 0);
    }

    private boolean matchesRemaining(List<ItemStack> stacks, boolean[] used, int index) {
        if (index >= stacks.size()) {
            return true;
        }
        ItemStack stack = stacks.get(index);
        for (int ingredientIndex = 0; ingredientIndex < ingredients.size(); ingredientIndex++) {
            if (used[ingredientIndex]) {
                continue;
            }
            if (!ingredients.get(ingredientIndex).test(stack)) {
                continue;
            }
            used[ingredientIndex] = true;
            if (matchesRemaining(stacks, used, index + 1)) {
                return true;
            }
            used[ingredientIndex] = false;
        }
        return false;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return AssemblyRecipeTypes.ASSEMBLY_SHAPELESS_SERIALIZER.get();
    }
}
