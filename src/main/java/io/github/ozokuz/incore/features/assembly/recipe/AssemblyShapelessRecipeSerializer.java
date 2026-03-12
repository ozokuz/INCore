package io.github.ozokuz.incore.features.assembly.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

import java.util.ArrayList;
import java.util.List;

public final class AssemblyShapelessRecipeSerializer implements RecipeSerializer<AssemblyShapelessRecipe> {
    private static final MapCodec<AssemblyShapelessRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients").flatXmap(
                    AssemblyShapelessRecipeSerializer::validateIngredients,
                    DataResult::success
            ).forGetter(AssemblyShapelessRecipe::ingredients),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(AssemblyRecipe::resultStack),
            Codec.intRange(1, 3).fieldOf("tier").forGetter(AssemblyRecipe::tier),
            Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("craftTimeTicks", 100).forGetter(AssemblyRecipe::craftTimeTicks),
            AssemblyTierBehavior.CODEC.fieldOf("tierBehavior").forGetter(AssemblyRecipe::tierBehavior)
    ).apply(instance, AssemblyShapelessRecipe::new));
    private final StreamCodec<RegistryFriendlyByteBuf, AssemblyShapelessRecipe> streamCodec = StreamCodec.of(this::toNetwork, this::fromNetwork);

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, AssemblyShapelessRecipe> streamCodec() {
        return streamCodec;
    }

    @Override
    public MapCodec<AssemblyShapelessRecipe> codec() {
        return CODEC;
    }

    private AssemblyShapelessRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        List<Ingredient> ingredients = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ingredients.add(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer));
        }
        ItemStack result = ItemStack.STREAM_CODEC.decode(buffer);
        int tier = buffer.readVarInt();
        int craftTimeTicks = buffer.readVarInt();
        AssemblyTierBehavior tierBehavior = AssemblyTierBehavior.STREAM_CODEC.decode(buffer);
        return new AssemblyShapelessRecipe(ingredients, result, tier, craftTimeTicks, tierBehavior);
    }

    private void toNetwork(RegistryFriendlyByteBuf buffer, AssemblyShapelessRecipe recipe) {
        buffer.writeVarInt(recipe.ingredients().size());
        recipe.ingredients().forEach(ingredient -> Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, ingredient));
        ItemStack.STREAM_CODEC.encode(buffer, recipe.resultStack());
        buffer.writeVarInt(recipe.tier());
        buffer.writeVarInt(recipe.craftTimeTicks());
        AssemblyTierBehavior.STREAM_CODEC.encode(buffer, recipe.tierBehavior());
    }

    private static DataResult<List<Ingredient>> validateIngredients(List<Ingredient> ingredients) {
        if (ingredients.isEmpty()) {
            return DataResult.error(() -> "No ingredients for shapeless assembly recipe");
        }
        if (ingredients.size() > 9) {
            return DataResult.error(() -> "Too many ingredients for shapeless assembly recipe. Maximum is 9");
        }
        return DataResult.success(List.copyOf(ingredients));
    }
}
