package ozokuz.incore.integration.ldlib.ui.player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import ozokuz.incore.features.shop.ShopDetailsPresentationMode;
import ozokuz.incore.features.shop.ShopService;
import ozokuz.incore.features.shop.ShopTabId;

final class ShopAppUiState {
    private ShopTabId activeTab = ShopTabId.INDUSTRIAL_MARKET;
    private @Nullable String selectedCategoryId;
    private @Nullable String selectedOfferId;
    private int offerScrollRow;
    private boolean detailsModalOpen;
    private int quantity = 1;
    private int visibleOfferRows = ShopAppUiSupport.VISIBLE_OFFER_ROWS;
    private final Map<String, Float> scrollerPositions = new HashMap<>();

    void setVisibleOfferRows(int visibleOfferRows) {
        this.visibleOfferRows = Math.max(1, visibleOfferRows);
    }

    void reconcile(ShopService.ScreenData data) {
        if (data.tabs().isEmpty()) {
            activeTab = ShopTabId.INDUSTRIAL_MARKET;
            selectedCategoryId = null;
            selectedOfferId = null;
            offerScrollRow = 0;
            detailsModalOpen = false;
            quantity = 1;
            scrollerPositions.clear();
            return;
        }

        activeTab = resolveActiveTab(data);
        List<ShopService.CategoryView> tabCategories = ShopAppUiSupport.categoriesForTab(data, activeTab);
        if (tabCategories.isEmpty()) {
            activeTab = ShopTabId.fromString(data.tabs().getFirst().tabId());
            tabCategories = ShopAppUiSupport.categoriesForTab(data, activeTab);
        }

        selectedCategoryId = resolveCategoryId(data, tabCategories);
        ShopService.TabFeedView feed = ShopAppUiSupport.feedForTab(data, activeTab, selectedCategoryId);
        if (!feed.activeCategoryId().isBlank()) {
            selectedCategoryId = feed.activeCategoryId();
        }
        offerScrollRow = Math.clamp(offerScrollRow, 0, maxOfferScroll(feed.remainingOffers().size()));

        String previousOfferId = selectedOfferId;
        selectedOfferId = resolveOfferId(data, feed);
        if (selectedOfferId == null) {
            detailsModalOpen = false;
            quantity = 1;
            return;
        }
        ensureSelectedOfferVisible(feed);

        if (!selectedOfferId.equals(previousOfferId)) {
            quantity = 1;
        }

        if (ShopAppUiSupport.detailsModeFor(data, activeTab) != ShopDetailsPresentationMode.MODAL_OVERLAY) {
            detailsModalOpen = false;
        } else if (detailsModalOpen && selectedOffer(data) == null) {
            detailsModalOpen = false;
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

    boolean detailsModalOpen() {
        return detailsModalOpen;
    }

    int quantity() {
        return quantity;
    }

    int offerScrollRow() {
        return offerScrollRow;
    }

    float scrollerPosition(String key) {
        return scrollerPositions.getOrDefault(key, 0.0F);
    }

    void setScrollerPosition(String key, float position) {
        scrollerPositions.put(key, Math.clamp(position, 0.0F, 1.0F));
    }

    void selectTab(ShopTabId tabId, ShopService.ScreenData data) {
        activeTab = tabId;
        offerScrollRow = 0;
        detailsModalOpen = false;
        quantity = 1;
        clearScrollerPositions();
        List<ShopService.CategoryView> categories = ShopAppUiSupport.categoriesForTab(data, tabId);
        selectedCategoryId = categories.isEmpty() ? null : categories.getFirst().categoryId();
        ShopService.TabFeedView feed = ShopAppUiSupport.feedForTab(data, tabId, selectedCategoryId);
        selectedOfferId = firstDisplayOffer(feed);
    }

    void selectCategory(String categoryId, ShopService.ScreenData data) {
        ShopService.CategoryView category = ShopAppUiSupport.findCategory(data, categoryId);
        if (category == null) {
            return;
        }
        activeTab = ShopAppUiSupport.tabForCategory(data, category);
        selectedCategoryId = category.categoryId();
        offerScrollRow = 0;
        detailsModalOpen = false;
        quantity = 1;
        clearScrollerPositions();
        ShopService.TabFeedView feed = ShopAppUiSupport.feedForTab(data, activeTab, selectedCategoryId);
        selectedOfferId = firstDisplayOffer(feed);
    }

    void selectOffer(String offerId, ShopService.ScreenData data) {
        ShopService.OfferView offer = ShopAppUiSupport.findOffer(data, offerId);
        if (offer == null) {
            return;
        }
        ShopService.CategoryView category = ShopAppUiSupport.findCategory(data, offer.categoryId());
        if (category == null) {
            return;
        }
        activeTab = ShopAppUiSupport.tabForCategory(data, category);
        selectedCategoryId = category.categoryId();
        if (!offerId.equals(selectedOfferId)) {
            quantity = 1;
        }
        selectedOfferId = offerId;
        ensureSelectedOfferVisible(ShopAppUiSupport.activeFeed(data, this));
        clampQuantity(data);
    }

    void openDetails(String offerId, ShopService.ScreenData data) {
        selectOffer(offerId, data);
        if (ShopAppUiSupport.detailsModeFor(data, activeTab) == ShopDetailsPresentationMode.MODAL_OVERLAY) {
            detailsModalOpen = true;
        }
    }

    void closeDetails() {
        detailsModalOpen = false;
    }

    boolean consumeEscape() {
        if (!detailsModalOpen) {
            return false;
        }
        detailsModalOpen = false;
        return true;
    }

    void increaseQuantity(ShopService.ScreenData data) {
        quantity = Math.min(quantity + 1, quantityMax(data));
    }

    void decreaseQuantity() {
        quantity = Math.max(1, quantity - 1);
    }

    void scrollBy(int delta, ShopService.ScreenData data) {
        ShopService.TabFeedView feed = ShopAppUiSupport.activeFeed(data, this);
        offerScrollRow = Math.clamp(offerScrollRow + delta, 0, maxOfferScroll(feed.remainingOffers().size()));
    }

    boolean canScrollPrevious() {
        return offerScrollRow > 0;
    }

    boolean canScrollNext(ShopService.ScreenData data) {
        ShopService.TabFeedView feed = ShopAppUiSupport.activeFeed(data, this);
        return offerScrollRow < maxOfferScroll(feed.remainingOffers().size());
    }

    List<ShopService.OfferView> visibleOffers(ShopService.ScreenData data) {
        return ShopAppUiSupport.visibleOffers(ShopAppUiSupport.activeFeed(data, this), offerScrollRow, visibleOfferRows);
    }

    @Nullable ShopService.OfferView showcaseOffer(ShopService.ScreenData data) {
        ShopService.TabFeedView feed = ShopAppUiSupport.activeFeed(data, this);
        return feed.showcaseOffers().isEmpty() ? null : feed.showcaseOffers().getFirst();
    }

    @Nullable ShopService.OfferView selectedOffer(ShopService.ScreenData data) {
        return ShopAppUiSupport.findOffer(data, selectedOfferId);
    }

    @Nullable ShopService.OfferView effectiveSelectedOffer(ShopService.ScreenData data) {
        ShopService.OfferView selected = selectedOffer(data);
        return selected != null ? selected : showcaseOffer(data);
    }

    @Nullable ResourceLocation selectedOfferResource() {
        return selectedOfferId == null || selectedOfferId.isBlank() ? null : ResourceLocation.tryParse(selectedOfferId);
    }

    @Nullable ResourceLocation selectedCategoryResource() {
        return selectedCategoryId == null || selectedCategoryId.isBlank() ? null : ResourceLocation.tryParse(selectedCategoryId);
    }

    int quantityMax(ShopService.ScreenData data) {
        ShopService.OfferView offer = effectiveSelectedOffer(data);
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

    private void clearScrollerPositions() {
        scrollerPositions.clear();
    }

    private void ensureSelectedOfferVisible(ShopService.TabFeedView feed) {
        if (selectedOfferId == null || selectedOfferId.isBlank()) {
            return;
        }
        int selectedRow = -1;
        for (int i = 0; i < feed.remainingOffers().size(); i++) {
            if (selectedOfferId.equals(feed.remainingOffers().get(i).offerId())) {
                selectedRow = i;
                break;
            }
        }
        if (selectedRow < 0) {
            offerScrollRow = Math.clamp(offerScrollRow, 0, maxOfferScroll(feed.remainingOffers().size()));
            return;
        }
        if (selectedRow < offerScrollRow) {
            offerScrollRow = selectedRow;
        } else if (selectedRow >= offerScrollRow + visibleOfferRows) {
            offerScrollRow = selectedRow - visibleOfferRows + 1;
        }
        offerScrollRow = Math.clamp(offerScrollRow, 0, maxOfferScroll(feed.remainingOffers().size()));
    }

    private ShopTabId resolveActiveTab(ShopService.ScreenData data) {
        if (selectedCategoryId != null) {
            return ShopAppUiSupport.tabForCategoryId(data, selectedCategoryId);
        }
        if (!data.selectedCategoryId().isBlank()) {
            return ShopAppUiSupport.tabForCategoryId(data, data.selectedCategoryId());
        }
        if (ShopAppUiSupport.findTab(data, activeTab) != null) {
            return activeTab;
        }
        return ShopTabId.fromString(data.tabs().getFirst().tabId());
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

    private @Nullable String resolveOfferId(ShopService.ScreenData data, ShopService.TabFeedView feed) {
        List<ShopService.OfferView> displayOffers = ShopAppUiSupport.displayOffers(feed);
        if (selectedOfferId != null) {
            for (ShopService.OfferView offer : displayOffers) {
                if (selectedOfferId.equals(offer.offerId())) {
                    return selectedOfferId;
                }
            }
        }
        if (!data.selectedOfferId().isBlank()) {
            for (ShopService.OfferView offer : displayOffers) {
                if (data.selectedOfferId().equals(offer.offerId())) {
                    return data.selectedOfferId();
                }
            }
        }
        return firstDisplayOffer(feed);
    }

    private @Nullable String firstDisplayOffer(ShopService.TabFeedView feed) {
        if (!feed.showcaseOffers().isEmpty()) {
            return feed.showcaseOffers().getFirst().offerId();
        }
        return feed.remainingOffers().isEmpty() ? null : feed.remainingOffers().getFirst().offerId();
    }

    private int maxOfferScroll(int offerCount) {
        return Math.max(0, offerCount - visibleOfferRows);
    }
}
