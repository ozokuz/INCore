package ozokuz.incore.features.shop;

public sealed interface ShopPurchaseableDefinition permits ShopSingleItemPurchaseableDefinition, ShopBundlePurchaseableDefinition {
    String serializedType();
}
