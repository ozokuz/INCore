package ozokuz.incore.features.shop;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public interface ShopCurrencyType {
    ResourceLocation id();

    @Nullable ShopCurrencySpec parse(JsonObject json);

    ShopCurrencyView buildView(ServerPlayer player, ShopCurrencySpec spec, int amountPerUnit);

    int availableAmount(ServerPlayer player, ShopCurrencySpec spec);

    boolean consume(ServerPlayer player, ShopCurrencySpec spec, int totalAmount);
}
