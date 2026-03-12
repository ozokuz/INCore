package io.github.ozokuz.incore.features.assembly.content;

import io.github.ozokuz.incore.features.assembly.recipe.AssemblyRecipe;
import io.github.ozokuz.incore.features.assembly.recipe.AssemblyRecipeTypes;
import io.github.ozokuz.incore.features.assembly.recipe.AssemblyShapedRecipe;
import io.github.ozokuz.incore.features.assembly.recipe.AssemblyShapelessRecipe;
import io.github.ozokuz.incore.features.assembly.unlock.AssemblyRecipeUnlockManager;
import io.github.ozokuz.incore.features.researchv2.ResearchManager;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class AssemblyRecipeUtil {
    private AssemblyRecipeUtil() {
    }

    public static List<RecipeHolder<AssemblyRecipe>> allRecipes(RecipeManager recipeManager) {
        List<RecipeHolder<AssemblyRecipe>> recipes = new ArrayList<>();
        for (RecipeHolder<AssemblyRecipe> holder : recipeManager.getAllRecipesFor(AssemblyRecipeTypes.ASSEMBLY_RECIPE_TYPE.get())) {
            recipes.add(holder);
        }
        recipes.sort(Comparator.comparing(holder -> holder.id().toString()));
        return List.copyOf(recipes);
    }

    public static RecipeHolder<AssemblyRecipe> findRecipeHolder(RecipeManager recipeManager, ResourceLocation recipeId) {
        if (recipeId == null) {
            return null;
        }
        RecipeHolder<?> holder = recipeManager.byKey(recipeId).orElse(null);
        if (holder == null || !(holder.value() instanceof AssemblyRecipe recipe)) {
            return null;
        }
        return new RecipeHolder<>(holder.id(), recipe);
    }

    public static AssemblyRecipe findRecipe(RecipeManager recipeManager, ResourceLocation recipeId) {
        RecipeHolder<AssemblyRecipe> holder = findRecipeHolder(recipeManager, recipeId);
        return holder != null ? holder.value() : null;
    }

    public static boolean isUnlocked(MinecraftServer server, String teamId, ResourceLocation recipeId) {
        ResourceLocation nodeId = AssemblyRecipeUnlockManager.requiredResearchNode(recipeId);
        return nodeId != null && ResearchManager.isResearched(server, teamId, nodeId);
    }

    public static List<String> unlockedRecipeIds(MinecraftServer server, String teamId, RecipeManager recipeManager) {
        List<String> ids = new ArrayList<>();
        for (RecipeHolder<AssemblyRecipe> holder : allRecipes(recipeManager)) {
            if (isUnlocked(server, teamId, holder.id())) {
                ids.add(holder.id().toString());
            }
        }
        return List.copyOf(ids);
    }

    public static CraftingInput craftingInput(ItemStackHandler handler, int startSlot) {
        List<ItemStack> stacks = new ArrayList<>();
        for (int slot = startSlot; slot < startSlot + 9; slot++) {
            stacks.add(handler.getStackInSlot(slot));
        }
        return CraftingInput.of(3, 3, stacks);
    }

    public static List<Integer> consumedSlots(AssemblyRecipe recipe, ItemStackHandler handler, int startSlot) {
        if (recipe instanceof AssemblyShapedRecipe shaped) {
            return consumedSlotsShaped(shaped, handler, startSlot);
        }
        if (recipe instanceof AssemblyShapelessRecipe shapeless) {
            return consumedSlotsShapeless(shapeless, handler, startSlot);
        }
        return List.of();
    }

    private static List<Integer> consumedSlotsShaped(AssemblyShapedRecipe recipe, ItemStackHandler handler, int startSlot) {
        for (int xOffset = 0; xOffset <= 3 - recipe.width(); xOffset++) {
            for (int yOffset = 0; yOffset <= 3 - recipe.height(); yOffset++) {
                List<Integer> normal = matchShaped(recipe, handler, startSlot, xOffset, yOffset, false);
                if (!normal.isEmpty()) {
                    return normal;
                }
                List<Integer> mirrored = matchShaped(recipe, handler, startSlot, xOffset, yOffset, true);
                if (!mirrored.isEmpty()) {
                    return mirrored;
                }
            }
        }
        return List.of();
    }

    private static List<Integer> matchShaped(AssemblyShapedRecipe recipe, ItemStackHandler handler, int startSlot, int xOffset, int yOffset, boolean mirrored) {
        List<Integer> consumed = new ArrayList<>();
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                var expected = net.minecraft.world.item.crafting.Ingredient.EMPTY;
                int localX = x - xOffset;
                int localY = y - yOffset;
                if (localX >= 0 && localY >= 0 && localX < recipe.width() && localY < recipe.height()) {
                    int patternX = mirrored ? (recipe.width() - 1 - localX) : localX;
                    expected = recipe.ingredients().get(patternX + localY * recipe.width());
                }
                int slot = startSlot + x + y * 3;
                ItemStack stack = handler.getStackInSlot(slot);
                if (expected.isEmpty()) {
                    if (!stack.isEmpty()) {
                        return List.of();
                    }
                } else if (!expected.test(stack)) {
                    return List.of();
                } else {
                    consumed.add(slot);
                }
            }
        }
        return consumed;
    }

    private static List<Integer> consumedSlotsShapeless(AssemblyShapelessRecipe recipe, ItemStackHandler handler, int startSlot) {
        List<Integer> slots = new ArrayList<>();
        List<ItemStack> stacks = new ArrayList<>();
        for (int slot = startSlot; slot < startSlot + 9; slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                slots.add(slot);
                stacks.add(stack);
            }
        }
        if (stacks.size() != recipe.ingredients().size()) {
            return List.of();
        }
        List<Integer> mapped = new ArrayList<>();
        if (matchShapeless(recipe, stacks, slots, new boolean[recipe.ingredients().size()], 0, mapped)) {
            return List.copyOf(mapped);
        }
        return List.of();
    }

    private static boolean matchShapeless(AssemblyShapelessRecipe recipe, List<ItemStack> stacks, List<Integer> slots, boolean[] used, int index, List<Integer> mapped) {
        if (index >= stacks.size()) {
            return true;
        }
        ItemStack stack = stacks.get(index);
        for (int ingredientIndex = 0; ingredientIndex < recipe.ingredients().size(); ingredientIndex++) {
            if (used[ingredientIndex] || !recipe.ingredients().get(ingredientIndex).test(stack)) {
                continue;
            }
            used[ingredientIndex] = true;
            mapped.add(slots.get(index));
            if (matchShapeless(recipe, stacks, slots, used, index + 1, mapped)) {
                return true;
            }
            mapped.remove(mapped.size() - 1);
            used[ingredientIndex] = false;
        }
        return false;
    }

    public static boolean canFitOutputs(IItemHandler handler, int outputStart, int outputCount, List<ItemStack> outputs) {
        List<ItemStack> simulated = new ArrayList<>();
        for (int slot = outputStart; slot < outputStart + outputCount; slot++) {
            simulated.add(handler.getStackInSlot(slot).copy());
        }
        for (ItemStack output : outputs) {
            if (output.isEmpty()) {
                continue;
            }
            if (!insertIntoCopies(simulated, output.copy())) {
                return false;
            }
        }
        return true;
    }

    private static boolean insertIntoCopies(List<ItemStack> simulated, ItemStack stack) {
        for (int i = 0; i < simulated.size(); i++) {
            ItemStack existing = simulated.get(i);
            if (existing.isEmpty()) {
                simulated.set(i, stack.copy());
                return true;
            }
            if (ItemStack.isSameItemSameComponents(existing, stack) && existing.getCount() + stack.getCount() <= existing.getMaxStackSize()) {
                existing.grow(stack.getCount());
                return true;
            }
        }
        return false;
    }

    public static void insertOutputs(ItemStackHandler handler, int outputStart, int outputCount, List<ItemStack> outputs) {
        for (ItemStack output : outputs) {
            if (output.isEmpty()) {
                continue;
            }
            ItemStack remaining = output.copy();
            for (int slot = outputStart; slot < outputStart + outputCount && !remaining.isEmpty(); slot++) {
                remaining = handler.insertItem(slot, remaining, false);
            }
        }
    }

    public static void consumeSlots(ItemStackHandler handler, List<Integer> slots) {
        for (int slot : slots) {
            handler.extractItem(slot, 1, false);
        }
    }
}
