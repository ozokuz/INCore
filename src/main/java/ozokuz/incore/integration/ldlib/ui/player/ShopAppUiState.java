package ozokuz.incore.integration.ldlib.ui.player;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import ozokuz.incore.features.shop.ShopService;
import ozokuz.incore.features.shop.ShopTabId;

final class ShopAppUiState {
    private ShopTabId activeTab = ShopTabId.SUPPLIES;
    private @Nullable String selectedCategoryId;
    private @Nullable String selectedOfferId;
    private int offerScrollRow;
    private boolean purchaseWorkspaceOpen;
    private int quantity = 1;
    private int visibleOfferRows = ShopAppUiSupport.VISIBLE_OFFER_ROWS;

    void setVisibleOfferRows(int visibleOfferRows) {
        this.visibleOfferRows = Math.max(1, visibleOfferRows);
    }

    void reconcile(ShopService.ScreenData data) {
        List<ShopService.CategoryView> categories = ShopAppUiSupport.orderedCategories(data);
        if (categories.isEmpty()) {
            activeTab = ShopTabId.SUPPLIES;
            selectedCategoryId = null;
            selectedOfferId = null;
            offerScrollRow = 0;
            quantity = 1;
            purchaseWorkspaceOpen = false;
            return;
        }

        activeTab = resolveActiveTab(data, categories);
        List<ShopService.CategoryView> tabCategories = ShopAppUiSupport.categoriesForTab(data, activeTab);
        if (tabCategories.isEmpty()) {
            activeTab = ShopAppUiSupport.tabForCategory(categories.getFirst());
            tabCategories = ShopAppUiSupport.categoriesForTab(data, activeTab);
        }

        selectedCategoryId = resolveCategoryId(data, tabCategories);
        List<ShopService.OfferView> offers = ShopAppUiSupport.offersForCategory(data, selectedCategoryId);
        offerScrollRow = Math.clamp(offerScrollRow, 0, maxOfferScroll(offers.size()));
        String previousOfferId = selectedOfferId;
        selectedOfferId = resolveOfferId(data, offers);
        if (selectedOfferId == null) {
            purchaseWorkspaceOpen = false;
            quantity = 1;
            return;
        }

        if (!selectedOfferId.equals(previousOfferId)) {
            quantity = 1;
        }

        if (purchaseWorkspaceOpen && selectedOffer(data) == null) {
            purchaseWorkspaceOpen = false;
        }
        clampQuantity(data);
    }

    ShopTabId activeTab() {
        return activeTab;
    }

    @Nullable String selectedCategoryId() {
        return selectedCategoryId;
    }

    @Nullable String selectedOfferId() {
        return selectedOfferId;
    }

    boolean purchaseWorkspaceOpen() {
        return purchaseWorkspaceOpen;
    }

    int quantity() {
        return quantity;
    }

    int offerScrollRow() {
        return offerScrollRow;
    }

    void selectTab(ShopTabId tabId, ShopService.ScreenData data) {
        activeTab = tabId;
        offerScrollRow = 0;
        purchaseWorkspaceOpen = false;
        quantity = 1;
        List<ShopService.CategoryView> categories = ShopAppUiSupport.categoriesForTab(data, tabId);
        selectedCategoryId = categories.isEmpty() ? null : categories.getFirst().categoryId();
        List<ShopService.OfferView> offers = ShopAppUiSupport.offersForCategory(data, selectedCategoryId);
        selectedOfferId = offers.isEmpty() ? null : offers.getFirst().offerId();
    }

    void selectCategory(String categoryId, ShopService.ScreenData data) {
        ShopService.CategoryView category = ShopAppUiSupport.findCategory(data, categoryId);
        if (category == null) {
            return;
        }
        activeTab = ShopAppUiSupport.tabForCategory(category);
        selectedCategoryId = category.categoryId();
        offerScrollRow = 0;
        purchaseWorkspaceOpen = false;
        quantity = 1;
        List<ShopService.OfferView> offers = ShopAppUiSupport.offersForCategory(data, selectedCategoryId);
        selectedOfferId = offers.isEmpty() ? null : offers.getFirst().offerId();
    }

    void openPurchase(String offerId, ShopService.ScreenData data) {
        ShopService.OfferView offer = ShopAppUiSupport.findOffer(data, offerId);
        if (offer == null) {
            return;
        }
        ShopService.CategoryView category = ShopAppUiSupport.findCategory(data, offer.categoryId());
        if (category == null) {
            return;
        }
        activeTab = ShopAppUiSupport.tabForCategory(category);
        selectedCategoryId = category.categoryId();
        if (!offerId.equals(selectedOfferId)) {
            quantity = 1;
        }
        selectedOfferId = offerId;
        purchaseWorkspaceOpen = true;
        clampQuantity(data);
    }

    void closePurchase(ShopService.ScreenData data) {
        purchaseWorkspaceOpen = false;
        clampQuantity(data);
    }

    boolean consumeEscape() {
        if (!purchaseWorkspaceOpen) {
            return false;
        }
        purchaseWorkspaceOpen = false;
        return true;
    }

    void increaseQuantity(ShopService.ScreenData data) {
        quantity = Math.min(quantity + 1, quantityMax(data));
    }

    void decreaseQuantity() {
        quantity = Math.max(1, quantity - 1);
    }

    void scrollBy(int delta, ShopService.ScreenData data) {
        List<ShopService.OfferView> offers = ShopAppUiSupport.offersForCategory(data, selectedCategoryId);
        offerScrollRow = Math.clamp(offerScrollRow + delta, 0, maxOfferScroll(offers.size()));
    }

    boolean canScrollPrevious() {
        return offerScrollRow > 0;
    }

    boolean canScrollNext(ShopService.ScreenData data) {
        List<ShopService.OfferView> offers = ShopAppUiSupport.offersForCategory(data, selectedCategoryId);
        return offerScrollRow < maxOfferScroll(offers.size());
    }

    List<ShopService.OfferView> visibleOffers(ShopService.ScreenData data) {
        List<ShopService.OfferView> offers = ShopAppUiSupport.offersForCategory(data, selectedCategoryId);
        if (offers.isEmpty()) {
            return List.of();
        }
        int start = Math.min(offerScrollRow, Math.max(0, offers.size() - 1));
        int end = Math.min(offers.size(), start + visibleOfferRows);
        return offers.subList(start, end);
    }

    @Nullable ShopService.OfferView selectedOffer(ShopService.ScreenData data) {
        return ShopAppUiSupport.findOffer(data, selectedOfferId);
    }

    @Nullable ResourceLocation selectedOfferResource() {
        return selectedOfferId == null || selectedOfferId.isBlank() ? null : ResourceLocation.tryParse(selectedOfferId);
    }

    @Nullable ResourceLocation selectedCategoryResource() {
        return selectedCategoryId == null || selectedCategoryId.isBlank() ? null : ResourceLocation.tryParse(selectedCategoryId);
    }

    int quantityMax(ShopService.ScreenData data) {
        ShopService.OfferView offer = selectedOffer(data);
        if (offer == null) {
            return 1;
        }
        if (offer.availableStock() < 0) {
            return 64;
        }
        return Math.max(1, Math.min(64, offer.availableStock()));
    }

    private void clampQuantity(ShopService.ScreenData data) {
        quantity = Math.clamp(quantity, 1, quantityMax(data));
    }

    private ShopTabId resolveActiveTab(ShopService.ScreenData data, List<ShopService.CategoryView> categories) {
        if (selectedCategoryId != null) {
            ShopService.CategoryView selectedCategory = ShopAppUiSupport.findCategory(data, selectedCategoryId);
            if (selectedCategory != null) {
                return ShopAppUiSupport.tabForCategory(selectedCategory);
            }
        }

        if (!data.selectedCategoryId().isBlank()) {
            ShopService.CategoryView selectedCategory = ShopAppUiSupport.findCategory(data, data.selectedCategoryId());
            if (selectedCategory != null) {
                return ShopAppUiSupport.tabForCategory(selectedCategory);
            }
        }

        List<ShopService.CategoryView> currentTabCategories = ShopAppUiSupport.categoriesForTab(data, activeTab);
        if (!currentTabCategories.isEmpty()) {
            return activeTab;
        }
        return ShopAppUiSupport.tabForCategory(categories.getFirst());
    }

    private @Nullable String resolveCategoryId(
            ShopService.ScreenData data,
            List<ShopService.CategoryView> tabCategories
    ) {
        if (selectedCategoryId != null) {
            for (ShopService.CategoryView category : tabCategories) {
                if (selectedCategoryId.equals(category.categoryId())) {
                    return selectedCategoryId;
                }
            }
        }

        if (!data.selectedCategoryId().isBlank()) {
            for (ShopService.CategoryView category : tabCategories) {
                if (data.selectedCategoryId().equals(category.categoryId())) {
                    return data.selectedCategoryId();
                }
            }
        }

        return tabCategories.isEmpty() ? null : tabCategories.getFirst().categoryId();
    }

    private @Nullable String resolveOfferId(
            ShopService.ScreenData data,
            List<ShopService.OfferView> offers
    ) {
        if (selectedOfferId != null) {
            for (ShopService.OfferView offer : offers) {
                if (selectedOfferId.equals(offer.offerId())) {
                    return selectedOfferId;
                }
            }
        }

        if (!data.selectedOfferId().isBlank()) {
            for (ShopService.OfferView offer : offers) {
                if (data.selectedOfferId().equals(offer.offerId())) {
                    return data.selectedOfferId();
                }
            }
        }

        return offers.isEmpty() ? null : offers.getFirst().offerId();
    }

    private int maxOfferScroll(int offerCount) {
        return Math.max(0, offerCount - visibleOfferRows);
    }
}
