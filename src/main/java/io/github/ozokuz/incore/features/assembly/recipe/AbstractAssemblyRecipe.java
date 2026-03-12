package io.github.ozokuz.incore.features.assembly.recipe;

import io.github.ozokuz.incore.INCore;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractAssemblyRecipe implements AssemblyRecipe {
    private final ItemStack result;
    private final int tier;
    private final int craftTimeTicks;
    private final AssemblyTierBehavior tierBehavior;

    protected AbstractAssemblyRecipe(ItemStack result, int tier, int craftTimeTicks, AssemblyTierBehavior tierBehavior) {
        this.result = result.copy();
        this.tier = Math.clamp(tier, 1, 3);
        this.craftTimeTicks = Math.max(1, craftTimeTicks);
        this.tierBehavior = tierBehavior;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public int tier() {
        return tier;
    }

    @Override
    public int craftTimeTicks() {
        return craftTimeTicks;
    }

    @Override
    public AssemblyTierBehavior tierBehavior() {
        return tierBehavior;
    }

    @Override
    public ItemStack resultStack() {
        return result.copy();
    }

    @Override
    public String getGroup() {
        return "";
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeType<?> getType() {
        return AssemblyRecipeTypes.ASSEMBLY_RECIPE_TYPE.get();
    }

    public abstract List<Ingredient> ingredients();

    protected static List<ItemStack> nonEmptyStacks(CraftingInput input) {
        List<ItemStack> stacks = new ArrayList<>();
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (!stack.isEmpty()) {
                stacks.add(stack);
            }
        }
        return stacks;
    }

    protected static void warnInvalid(String message) {
        INCore.LOGGER.warn("Invalid assembly recipe: {}", message);
    }

    @Override
    public abstract RecipeSerializer<?> getSerializer();
}
