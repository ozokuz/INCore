package ozokuz.incore.features.shop;

import com.google.gson.JsonObject;
import dev.ithundxr.createnumismatics.Numismatics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public final class BankSpurShopCurrencyType implements ShopCurrencyType {
    public static final ResourceLocation TYPE_ID = ResourceLocation.parse("incore:bank_spur");
    private static final ResourceLocation SPUR_ICON_ITEM = ResourceLocation.parse("numismatics:spur");

    @Override
    public ResourceLocation id() {
        return TYPE_ID;
    }

    @Override
    public @Nullable ShopCurrencySpec parse(JsonObject json) {
        return new Spec();
    }

    @Override
    public ShopCurrencyView buildView(ServerPlayer player, ShopCurrencySpec spec, int amountPerUnit) {
        return new ShopCurrencyView(
                TYPE_ID.toString(),
                SPUR_ICON_ITEM.toString(),
                "SPUR",
                Math.max(1, amountPerUnit),
                availableAmount(player, spec)
        );
    }

    @Override
    public int availableAmount(ServerPlayer player, ShopCurrencySpec spec) {
        return Math.max(0, Numismatics.BANK.getAccount(player).getBalance());
    }

    @Override
    public boolean consume(ServerPlayer player, ShopCurrencySpec spec, int totalAmount) {
        if (totalAmount <= 0) {
            return true;
        }
        return Numismatics.BANK.getAccount(player).deduct(totalAmount);
    }

    public record Spec() implements ShopCurrencySpec {
        @Override
        public ResourceLocation typeId() {
            return TYPE_ID;
        }
    }
}
