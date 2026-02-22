package io.github.ozokuz.incore.features.vendor;

import io.github.ozokuz.incore.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class VendorBlockEntity extends BlockEntity {
    private boolean initialized;
    private boolean darkMarket;
    private @Nullable ResourceLocation categoryId;
    private final LinkedHashMap<ResourceLocation, Integer> offerStocks = new LinkedHashMap<>();
    private final LinkedHashMap<ResourceLocation, Integer> offerDiscounts = new LinkedHashMap<>();

    public VendorBlockEntity(BlockPos pos, BlockState blockState) {
        super(Registration.VENDOR_BE.get(), pos, blockState);
    }

    public boolean initialized() {
        return initialized;
    }

    public Map<ResourceLocation, Integer> offerStocks() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(offerStocks));
    }

    public boolean darkMarket() {
        return darkMarket;
    }

    public @Nullable ResourceLocation categoryId() {
        return categoryId;
    }

    public int discountPercentFor(ResourceLocation offerId) {
        return Math.max(0, offerDiscounts.getOrDefault(offerId, 0));
    }

    public boolean hasDiscountEntry(ResourceLocation offerId) {
        return offerDiscounts.containsKey(offerId);
    }

    public boolean hasOffer(ResourceLocation offerId) {
        return offerStocks.containsKey(offerId);
    }

    public int stockFor(ResourceLocation offerId) {
        return offerStocks.getOrDefault(offerId, 0);
    }

    public void initializeInventory(
            boolean darkMarket,
            @Nullable ResourceLocation categoryId,
            LinkedHashMap<ResourceLocation, Integer> stocks,
            LinkedHashMap<ResourceLocation, Integer> discounts
    ) {
        this.darkMarket = darkMarket;
        this.categoryId = categoryId;
        offerStocks.clear();
        for (var entry : stocks.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            offerStocks.put(entry.getKey(), Math.max(0, entry.getValue()));
        }
        offerDiscounts.clear();
        for (var entry : discounts.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            offerDiscounts.put(entry.getKey(), Math.max(0, entry.getValue()));
        }
        initialized = true;
        setChangedAndSync();
    }

    public boolean consumeStock(ResourceLocation offerId, int amount) {
        if (amount <= 0) {
            return true;
        }

        Integer current = offerStocks.get(offerId);
        if (current == null || current < amount) {
            return false;
        }

        offerStocks.put(offerId, current - amount);
        setChangedAndSync();
        return true;
    }

    public void addStock(ResourceLocation offerId, int amount) {
        if (amount <= 0) {
            return;
        }

        offerStocks.merge(offerId, amount, Integer::sum);
        setChangedAndSync();
    }

    public void setOfferDiscount(ResourceLocation offerId, int discountPercent) {
        if (offerId == null) {
            return;
        }

        offerDiscounts.put(offerId, Math.max(0, discountPercent));
        setChangedAndSync();
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        initialized = tag.getBoolean("initialized");
        darkMarket = tag.getBoolean("dark_market");
        categoryId = ResourceLocation.tryParse(tag.getString("category_id"));
        offerStocks.clear();
        ListTag list = tag.getList("offer_stocks", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            ResourceLocation offerId = ResourceLocation.tryParse(entry.getString("id"));
            if (offerId == null) {
                continue;
            }
            offerStocks.put(offerId, Math.max(0, entry.getInt("stock")));
        }
        offerDiscounts.clear();
        ListTag discountList = tag.getList("offer_discounts", Tag.TAG_COMPOUND);
        for (int i = 0; i < discountList.size(); i++) {
            CompoundTag entry = discountList.getCompound(i);
            ResourceLocation offerId = ResourceLocation.tryParse(entry.getString("id"));
            if (offerId == null) {
                continue;
            }
            offerDiscounts.put(offerId, Math.max(0, entry.getInt("discount_percent")));
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("initialized", initialized);
        tag.putBoolean("dark_market", darkMarket);
        if (categoryId != null) {
            tag.putString("category_id", categoryId.toString());
        } else {
            tag.remove("category_id");
        }
        ListTag list = new ListTag();
        for (var entry : offerStocks.entrySet()) {
            CompoundTag stockTag = new CompoundTag();
            stockTag.putString("id", entry.getKey().toString());
            stockTag.putInt("stock", Math.max(0, entry.getValue()));
            list.add(stockTag);
        }
        tag.put("offer_stocks", list);

        ListTag discountList = new ListTag();
        for (var entry : offerDiscounts.entrySet()) {
            CompoundTag discountTag = new CompoundTag();
            discountTag.putString("id", entry.getKey().toString());
            discountTag.putInt("discount_percent", Math.max(0, entry.getValue()));
            discountList.add(discountTag);
        }
        tag.put("offer_discounts", discountList);
    }

    private void setChangedAndSync() {
        setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
}
