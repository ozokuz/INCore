package io.github.ozokuz.incore.features.vendor;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

public class ItemVendorProductType implements VendorProductType {
    public static final ResourceLocation TYPE_ID = ResourceLocation.parse("incore:item");

    @Override
    public ResourceLocation id() {
        return TYPE_ID;
    }

    @Override
    public @Nullable VendorProductSpec parse(JsonObject json) {
        ResourceLocation itemId = ResourceLocation.tryParse(GsonHelper.getAsString(json, "item", ""));
        if (itemId == null || !BuiltInRegistries.ITEM.containsKey(itemId)) {
            return null;
        }

        int count = Math.max(1, GsonHelper.getAsInt(json, "count", 1));
        return new Spec(itemId, count);
    }

    @Override
    public boolean isAvailable(VendorProductSpec spec) {
        if (!(spec instanceof Spec itemSpec)) {
            return false;
        }
        return BuiltInRegistries.ITEM.containsKey(itemSpec.itemId());
    }

    @Override
    public ItemStack previewStack(VendorProductSpec spec) {
        if (!(spec instanceof Spec itemSpec)) {
            return ItemStack.EMPTY;
        }

        Item item = BuiltInRegistries.ITEM.get(itemSpec.itemId());
        return item == Items.AIR ? ItemStack.EMPTY : item.getDefaultInstance();
    }

    @Override
    public String productId(VendorProductSpec spec) {
        return spec instanceof Spec itemSpec ? itemSpec.itemId().toString() : "";
    }

    @Override
    public boolean grant(ServerPlayer player, VendorProductSpec spec, int quantity) {
        if (!(spec instanceof Spec itemSpec) || quantity <= 0) {
            return false;
        }

        Item item = BuiltInRegistries.ITEM.get(itemSpec.itemId());
        if (item == Items.AIR) {
            return false;
        }

        long total = (long) itemSpec.count() * quantity;
        if (total <= 0L || total > Integer.MAX_VALUE) {
            return false;
        }

        VendorItemUtil.giveOrDropStacked(player, new ItemStack(item, (int) total));
        return true;
    }

    private record Spec(ResourceLocation itemId, int count) implements VendorProductSpec {
        @Override
        public ResourceLocation typeId() {
            return TYPE_ID;
        }

        @Override
        public int unitCount() {
            return count;
        }
    }
}
