package io.github.ozokuz.incore.features.vendor;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;

public class BankSpurVendorCurrencyType implements VendorCurrencyType {
    public static final ResourceLocation TYPE_ID = ResourceLocation.parse("incore:bank_spur");
    private static final ResourceLocation SPUR_ICON_ITEM = ResourceLocation.parse("numismatics:spur");

    @Override
    public ResourceLocation id() {
        return TYPE_ID;
    }

    @Override
    public VendorCurrencySpec parse(JsonObject json) {
        int amount = Math.max(1, GsonHelper.getAsInt(json, "amount", 1));
        int spurConversionRate = GsonHelper.getAsInt(json, "spur_conversion_rate", 0);
        if (spurConversionRate != 1) {
            return null;
        }
        return new Spec(amount, spurConversionRate);
    }

    @Override
    public VendorCurrencyView buildView(ServerPlayer player, VendorCurrencySpec spec) {
        if (!(spec instanceof Spec spurSpec)) {
            return new VendorCurrencyView(TYPE_ID.toString(), SPUR_ICON_ITEM.toString(), "SPUR", 1, 0, TYPE_ID.toString(), SPUR_ICON_ITEM.toString(), "SPUR", 1, 0);
        }

        int availableSpur = VendorCurrencyUtil.getBankSpurBalance(player);
        return new VendorCurrencyView(
                TYPE_ID.toString(),
                SPUR_ICON_ITEM.toString(),
                "SPUR",
                spurSpec.amount(),
                availableSpur,
                TYPE_ID.toString(),
                SPUR_ICON_ITEM.toString(),
                "SPUR",
                spurSpec.spurConversionRate(),
                availableSpur
        );
    }

    @Override
    public boolean consume(ServerPlayer player, VendorCurrencySpec spec, int quantity, boolean allowConversion) {
        if (!(spec instanceof Spec spurSpec) || quantity <= 0) {
            return false;
        }

        long required = (long) spurSpec.amount() * quantity;
        if (required <= 0L || required > Integer.MAX_VALUE) {
            return false;
        }

        return VendorCurrencyUtil.deductBankSpur(player, (int) required);
    }

    @Override
    public VendorCurrencySpec withUnitAmount(VendorCurrencySpec spec, int unitAmount) {
        if (!(spec instanceof Spec spurSpec)) {
            return spec;
        }

        return new Spec(Math.max(1, unitAmount), spurSpec.spurConversionRate());
    }

    private record Spec(int amount, int spurConversionRate) implements VendorCurrencySpec {
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
