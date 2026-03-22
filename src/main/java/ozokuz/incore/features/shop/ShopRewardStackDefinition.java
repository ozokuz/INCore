package ozokuz.incore.features.shop;

public record ShopRewardStackDefinition(String stackSpec, int count) {
    public ShopRewardStackDefinition {
        count = Math.max(1, count);
    }
}
