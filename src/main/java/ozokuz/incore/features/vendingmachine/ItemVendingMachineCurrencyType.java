package ozokuz.incore.features.vendingmachine;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

public class ItemVendingMachineCurrencyType implements VendingMachineCurrencyType {
    public static final ResourceLocation TYPE_ID = ResourceLocation.parse("incore:item");
    private static final ResourceLocation SPUR_TYPE_ID = ResourceLocation.parse("incore:bank_spur");
    private static final ResourceLocation SPUR_ICON_ITEM = ResourceLocation.parse("numismatics:spur");

    @Override
    public ResourceLocation id() {
        return TYPE_ID;
    }

    @Override
    public @Nullable VendingMachineCurrencySpec parse(JsonObject json) {
        ResourceLocation itemId = ResourceLocation.tryParse(GsonHelper.getAsString(json, "item", ""));
        if (itemId == null || !BuiltInRegistries.ITEM.containsKey(itemId)) {
            return null;
        }

        int amount = Math.max(1, GsonHelper.getAsInt(json, "amount", 1));
        int spurConversionRate = GsonHelper.getAsInt(json, "spur_conversion_rate", 0);
        if (spurConversionRate <= 0) {
            return null;
        }
        return new Spec(itemId, amount, spurConversionRate);
    }

    @Override
    public VendingMachineCurrencyView buildView(ServerPlayer player, VendingMachineCurrencySpec spec) {
        if (!(spec instanceof Spec itemSpec)) {
            return new VendingMachineCurrencyView(TYPE_ID.toString(), "minecraft:barrier", "", 1, 0, SPUR_TYPE_ID.toString(), SPUR_ICON_ITEM.toString(), "SPUR", 1, 0);
        }

        Item item = BuiltInRegistries.ITEM.get(itemSpec.itemId());
        String label = item == Items.AIR ? itemSpec.itemId().toString() : item.getDescription().getString();
        return new VendingMachineCurrencyView(
                TYPE_ID.toString(),
                itemSpec.itemId().toString(),
                label,
                itemSpec.amount(),
                VendingMachineCurrencyUtil.countItem(player, itemSpec.itemId()),
                SPUR_TYPE_ID.toString(),
                SPUR_ICON_ITEM.toString(),
                "SPUR",
                itemSpec.spurConversionRate(),
                VendingMachineCurrencyUtil.getBankSpurBalance(player)
        );
    }

    @Override
    public boolean consume(ServerPlayer player, VendingMachineCurrencySpec spec, int quantity, boolean allowConversion) {
        if (!(spec instanceof Spec itemSpec) || quantity <= 0) {
            return false;
        }

        long required = (long) itemSpec.amount() * quantity;
        if (required <= 0L || required > Integer.MAX_VALUE) {
            return false;
        }

        int available = VendingMachineCurrencyUtil.countItem(player, itemSpec.itemId());
        int primaryToConsume = Math.min(available, (int) required);
        int missing = (int) required - primaryToConsume;

        if (missing > 0) {
            if (!allowConversion) {
                return false;
            }

            long requiredSpur = (long) missing * itemSpec.spurConversionRate();
            if (requiredSpur <= 0L || requiredSpur > Integer.MAX_VALUE) {
                return false;
            }

            if (!VendingMachineCurrencyUtil.deductBankSpur(player, (int) requiredSpur)) {
                return false;
            }
        }

        if (primaryToConsume > 0) {
            VendingMachineCurrencyUtil.consumeItem(player, itemSpec.itemId(), primaryToConsume);
        }
        return true;
    }

    @Override
    public VendingMachineCurrencySpec withUnitAmount(VendingMachineCurrencySpec spec, int unitAmount) {
        if (!(spec instanceof Spec itemSpec)) {
            return spec;
        }

        return new Spec(itemSpec.itemId(), Math.max(1, unitAmount), itemSpec.spurConversionRate());
    }

    private record Spec(ResourceLocation itemId, int amount, int spurConversionRate) implements VendingMachineCurrencySpec {
        @Override
        public ResourceLocation typeId() {
            return TYPE_ID;
        }

        @Override
        public int unitAmount() {
            return amount;
        }

        @Override
        public int spurConversionRate() {
            return spurConversionRate;
        }
    }
}
