package io.github.ozokuz.incore.features.assembly.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public record AssemblyOutputDefinition(ResourceLocation itemId, int count) {
    public static final Codec<AssemblyOutputDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(AssemblyOutputDefinition::itemId),
            Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("count", 1).forGetter(AssemblyOutputDefinition::count)
    ).apply(instance, AssemblyOutputDefinition::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, AssemblyOutputDefinition> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> {
                ResourceLocation.STREAM_CODEC.encode(buffer, value.itemId());
                buffer.writeVarInt(value.count());
            },
            buffer -> new AssemblyOutputDefinition(ResourceLocation.STREAM_CODEC.decode(buffer), buffer.readVarInt())
    );

    public AssemblyOutputDefinition {
        count = Math.max(1, count);
        validateItem(itemId);
    }

    public ItemStack toStack() {
        Item item = BuiltInRegistries.ITEM.get(itemId);
        return item == null || item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item, count);
    }

    public ItemStack toStack(HolderLookup.Provider registries) {
        return toStack();
    }

    public static AssemblyOutputDefinition fromJson(JsonObject object) {
        ResourceLocation itemId = ResourceLocation.parse(object.get("id").getAsString());
        int count = object.has("count") ? Math.max(1, object.get("count").getAsInt()) : 1;
        return new AssemblyOutputDefinition(itemId, count);
    }

    public static AssemblyOutputDefinition fromJson(JsonElement element) {
        return fromJson(element.getAsJsonObject());
    }

    public JsonObject toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("id", itemId.toString());
        object.addProperty("count", count);
        return object;
    }

    private static void validateItem(ResourceLocation itemId) {
        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item == null || item == Items.AIR) {
            throw new IllegalArgumentException("Unknown assembly output item: " + itemId);
        }
    }
}
