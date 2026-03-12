package io.github.ozokuz.incore.features.assembly.recipe;

import io.github.ozokuz.incore.INCore;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class AssemblyRecipeTypes {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, INCore.MODID);
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, INCore.MODID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<AssemblyRecipe>> ASSEMBLY_RECIPE_TYPE = RECIPE_TYPES.register(
            "assembly",
            () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return "incore:assembly";
                }
            }
    );
    public static final DeferredHolder<RecipeSerializer<?>, AssemblyShapedRecipeSerializer> ASSEMBLY_SHAPED_SERIALIZER = RECIPE_SERIALIZERS.register("assembly_shaped", AssemblyShapedRecipeSerializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, AssemblyShapelessRecipeSerializer> ASSEMBLY_SHAPELESS_SERIALIZER = RECIPE_SERIALIZERS.register("assembly_shapeless", AssemblyShapelessRecipeSerializer::new);

    private AssemblyRecipeTypes() {
    }
}
