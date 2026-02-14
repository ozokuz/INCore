package io.github.ozokuz.incore.features.gacha;

import io.github.ozokuz.incore.Registration;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

public final class GachaCrateData {
    private static final String KEY_BANNER = "incore:banner";

    private GachaCrateData() {
    }

    public static ItemStack createBannerCrate(ResourceLocation bannerId, int count) {
        ItemStack stack = new ItemStack(Registration.GACHA_CRATE_BLOCK_ITEM.get(), count);
        writeBannerId(stack, bannerId);
        return stack;
    }

    public static void writeBannerId(ItemStack stack, ResourceLocation bannerId) {
        CompoundTag tag = new CompoundTag();
        tag.putString(KEY_BANNER, bannerId.toString());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static @Nullable ResourceLocation readBannerId(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return null;
        }

        CompoundTag tag = data.copyTag();
        if (!tag.contains(KEY_BANNER, Tag.TAG_STRING)) {
            return null;
        }

        return ResourceLocation.tryParse(tag.getString(KEY_BANNER));
    }
}
