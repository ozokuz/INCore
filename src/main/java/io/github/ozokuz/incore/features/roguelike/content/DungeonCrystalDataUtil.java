package io.github.ozokuz.incore.features.roguelike.content;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.List;

public final class DungeonCrystalDataUtil {
    private static final String KEY_CUSTOM = "incoreCustomDungeonCrystal";
    private static final String KEY_MODIFIERS = "incoreDungeonModifiers";

    private DungeonCrystalDataUtil() {
    }

    public static boolean isCustomCrystal(ItemStack stack) {
        CompoundTag tag = readCustomTag(stack);
        return tag != null && tag.getBoolean(KEY_CUSTOM);
    }

    public static void setCustomCrystal(ItemStack stack, boolean value) {
        CompoundTag tag = readOrCreateCustomTag(stack);
        tag.putBoolean(KEY_CUSTOM, value);
        writeCustomData(stack, tag);
    }

    public static List<ResourceLocation> readModifiers(ItemStack stack) {
        CompoundTag tag = readCustomTag(stack);
        if (tag == null) {
            return List.of();
        }

        ListTag modifiersTag = tag.getList(KEY_MODIFIERS, Tag.TAG_STRING);
        List<ResourceLocation> modifiers = new ArrayList<>(modifiersTag.size());
        for (Tag entry : modifiersTag) {
            ResourceLocation id = ResourceLocation.tryParse(entry.getAsString());
            if (id != null) {
                modifiers.add(id);
            }
        }
        return List.copyOf(modifiers);
    }

    public static void writeModifiers(ItemStack stack, List<ResourceLocation> modifiers) {
        CompoundTag tag = readOrCreateCustomTag(stack);
        ListTag modifiersTag = new ListTag();
        for (ResourceLocation id : modifiers) {
            if (id == null) {
                continue;
            }
            modifiersTag.add(StringTag.valueOf(id.toString()));
        }
        tag.put(KEY_MODIFIERS, modifiersTag);
        writeCustomData(stack, tag);
    }

    private static CompoundTag readOrCreateCustomTag(ItemStack stack) {
        CompoundTag tag = readCustomTag(stack);
        return tag == null ? new CompoundTag() : tag;
    }

    private static CompoundTag readCustomTag(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? null : data.copyTag();
    }

    private static void writeCustomData(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}
