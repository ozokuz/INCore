package io.github.ozokuz.incore.features.gacha;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

public class GachaPermitItem extends Item {
    private static final String KEY_BANNER = "incore:banner";
    private static final String KEY_BANNER_NAME = "incore:banner_name";

    private final PermitMode permitMode;

    public GachaPermitItem(Properties properties, PermitMode permitMode) {
        super(properties);
        this.permitMode = permitMode;
    }

    public PermitMode getPermitMode() {
        return permitMode;
    }

    public static ItemStack createBannerPermit(Item item, ResourceLocation bannerId, int count) {
        return createBannerPermit(item, bannerId, null, count);
    }

    public static ItemStack createBannerPermit(Item item, ResourceLocation bannerId, String bannerName, int count) {
        ItemStack stack = new ItemStack(item, count);
        CompoundTag tag = new CompoundTag();
        tag.putString(KEY_BANNER, bannerId.toString());
        if (bannerName != null && !bannerName.isBlank()) {
            tag.putString(KEY_BANNER_NAME, bannerName);
            stack.set(DataComponents.CUSTOM_NAME, Component.translatable("item.incore.time_piece.named", bannerName));
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    public static ResourceLocation readBannerPermit(ItemStack stack) {
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

    public static boolean matchesBanner(ItemStack stack, ResourceLocation bannerId) {
        ResourceLocation fromPermit = readBannerPermit(stack);
        return fromPermit != null && fromPermit.equals(bannerId);
    }

    public static String readBannerName(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return null;
        }

        CompoundTag tag = data.copyTag();
        if (!tag.contains(KEY_BANNER_NAME, Tag.TAG_STRING)) {
            return null;
        }
        return tag.getString(KEY_BANNER_NAME);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        if (permitMode != PermitMode.SPECIFIC) {
            return;
        }

        String bannerName = readBannerName(stack);
        if (bannerName != null && !bannerName.isBlank()) {
            tooltipComponents.add(Component.translatable("item.incore.time_piece.tooltip.banner_name", bannerName));
            return;
        }

        ResourceLocation bannerId = readBannerPermit(stack);
        if (bannerId != null) {
            tooltipComponents.add(Component.translatable("item.incore.time_piece.tooltip.banner", bannerId.toString()));
        }
    }

    public enum PermitMode {
        BASIC,
        CHARTERED,
        SPECIFIC
    }
}
