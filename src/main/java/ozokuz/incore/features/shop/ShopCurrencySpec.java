package ozokuz.incore.features.shop;

import net.minecraft.resources.ResourceLocation;

public sealed interface ShopCurrencySpec permits BankSpurShopCurrencyType.Spec, ItemShopCurrencyType.Spec {
    ResourceLocation typeId();
}
