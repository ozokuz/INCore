package io.github.ozokuz.incore.features.assembly.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

public final class AssemblyShapedRecipeSerializer implements RecipeSerializer<AssemblyShapedRecipe> {
    private static final MapCodec<AssemblyShapedRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ShapedRecipePattern.MAP_CODEC.forGetter(AssemblyShapedRecipe::pattern),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(AssemblyRecipe::resultStack),
            Codec.intRange(1, 3).fieldOf("tier").forGetter(AssemblyRecipe::tier),
            Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("craftTimeTicks", 100).forGetter(AssemblyRecipe::craftTimeTicks),
            AssemblyTierBehavior.CODEC.fieldOf("tierBehavior").forGetter(AssemblyRecipe::tierBehavior)
    ).apply(instance, AssemblyShapedRecipe::new));
    private final StreamCodec<RegistryFriendlyByteBuf, AssemblyShapedRecipe> streamCodec = StreamCodec.of(this::toNetwork, this::fromNetwork);

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, AssemblyShapedRecipe> streamCodec() {
        return streamCodec;
    }

    @Override
    public MapCodec<AssemblyShapedRecipe> codec() {
        return CODEC;
    }

    private AssemblyShapedRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
        ShapedRecipePattern pattern = ShapedRecipePattern.STREAM_CODEC.decode(buffer);
        ItemStack result = ItemStack.STREAM_CODEC.decode(buffer);
        int tier = buffer.readVarInt();
        int craftTimeTicks = buffer.readVarInt();
        AssemblyTierBehavior tierBehavior = AssemblyTierBehavior.STREAM_CODEC.decode(buffer);
        return new AssemblyShapedRecipe(pattern, result, tier, craftTimeTicks, tierBehavior);
    }

    private void toNetwork(RegistryFriendlyByteBuf buffer, AssemblyShapedRecipe recipe) {
        ShapedRecipePattern.STREAM_CODEC.encode(buffer, recipe.pattern());
        ItemStack.STREAM_CODEC.encode(buffer, recipe.resultStack());
        buffer.writeVarInt(recipe.tier());
        buffer.writeVarInt(recipe.craftTimeTicks());
        AssemblyTierBehavior.STREAM_CODEC.encode(buffer, recipe.tierBehavior());
    }
}
