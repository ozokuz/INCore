package ozokuz.incore.features.shop;

public record ShopSingleItemPurchaseableDefinition(String stackSpec, int count) implements ShopPurchaseableDefinition {
    public ShopSingleItemPurchaseableDefinition {
        count = Math.max(1, count);
    }

    @Override
    public String serializedType() {
        return "single_item";
    }
}
