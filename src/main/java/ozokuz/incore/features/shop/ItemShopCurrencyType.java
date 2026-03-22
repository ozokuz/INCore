package ozokuz.incore.features.shop;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;
import ozokuz.incore.features.vendingmachine.VendingMachineCurrencyUtil;

public final class ItemShopCurrencyType implements ShopCurrencyType {
    public static final ResourceLocation TYPE_ID = ResourceLocation.parse("incore:item");

    @Override
    public ResourceLocation id() {
        return TYPE_ID;
    }

    @Override
    public @Nullable ShopCurrencySpec parse(JsonObject json) {
        ResourceLocation itemId = ResourceLocation.tryParse(GsonHelper.getAsString(json, "item", ""));
        if (itemId == null || !BuiltInRegistries.ITEM.containsKey(itemId)) {
            return null;
        }
        return new Spec(itemId);
    }

    @Override
    public ShopCurrencyView buildView(ServerPlayer player, ShopCurrencySpec spec, int amountPerUnit) {
        if (!(spec instanceof Spec itemSpec)) {
            return new ShopCurrencyView(TYPE_ID.toString(), Items.BARRIER.builtInRegistryHolder().key().location().toString(), "", Math.max(1, amountPerUnit), 0);
        }
        Item item = BuiltInRegistries.ITEM.get(itemSpec.itemId());
        String label = item == Items.AIR ? itemSpec.itemId().toString() : item.getDescription().getString();
        return new ShopCurrencyView(
                TYPE_ID.toString(),
                itemSpec.itemId().toString(),
                label,
                Math.max(1, amountPerUnit),
                availableAmount(player, spec)
        );
    }

    @Override
    public int availableAmount(ServerPlayer player, ShopCurrencySpec spec) {
        if (!(spec instanceof Spec itemSpec)) {
            return 0;
        }
        return VendingMachineCurrencyUtil.countItem(player, itemSpec.itemId());
    }

    @Override
    public boolean consume(ServerPlayer player, ShopCurrencySpec spec, int totalAmount) {
        if (!(spec instanceof Spec itemSpec)) {
            return false;
        }
        if (totalAmount <= 0) {
            return true;
        }
        if (availableAmount(player, spec) < totalAmount) {
            return false;
        }
        VendingMachineCurrencyUtil.consumeItem(player, itemSpec.itemId(), totalAmount);
        return true;
    }

    public record Spec(ResourceLocation itemId) implements ShopCurrencySpec {
        @Override
        public ResourceLocation typeId() {
            return TYPE_ID;
        }
    }
}
