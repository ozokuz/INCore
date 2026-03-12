package io.github.ozokuz.incore.features.assembly.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.Level;

import java.util.List;

public final class AssemblyShapedRecipe extends AbstractAssemblyRecipe {
    private final ShapedRecipePattern pattern;

    public AssemblyShapedRecipe(ShapedRecipePattern pattern, ItemStack result, int tier, int craftTimeTicks, AssemblyTierBehavior tierBehavior) {
        super(result, tier, craftTimeTicks, tierBehavior);
        this.pattern = pattern;
    }

    public int width() {
        return pattern.width();
    }

    public int height() {
        return pattern.height();
    }

    public ShapedRecipePattern pattern() {
        return pattern;
    }

    @Override
    public List<Ingredient> ingredients() {
        return pattern.ingredients();
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return pattern.matches(input);
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return super.getResultItem(registries);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return AssemblyRecipeTypes.ASSEMBLY_SHAPED_SERIALIZER.get();
    }
}
