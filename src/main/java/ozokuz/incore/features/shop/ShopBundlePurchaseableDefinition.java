package ozokuz.incore.features.shop;

import java.util.List;

public record ShopBundlePurchaseableDefinition(List<ShopRewardStackDefinition> items) implements ShopPurchaseableDefinition {
    public ShopBundlePurchaseableDefinition {
        items = List.copyOf(items);
    }

    @Override
    public String serializedType() {
        return "bundle";
    }
}
