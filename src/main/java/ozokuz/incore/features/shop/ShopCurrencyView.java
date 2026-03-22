package ozokuz.incore.features.shop;

public record ShopCurrencyView(
        String typeId,
        String iconItemId,
        String label,
        int amountPerUnit,
        int availableAmount
) {
}
