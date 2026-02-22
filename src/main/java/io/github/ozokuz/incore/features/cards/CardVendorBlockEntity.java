package io.github.ozokuz.incore.features.cards;

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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collections;

public class CardVendorBlockEntity extends BlockEntity {
    private boolean initialized;
    private final LinkedHashMap<ResourceLocation, Integer> offerStocks = new LinkedHashMap<>();

    public CardVendorBlockEntity(BlockPos pos, BlockState blockState) {
        super(Registration.CARD_VENDOR_BE.get(), pos, blockState);
    }

    public boolean initialized() {
        return initialized;
    }

    public Map<ResourceLocation, Integer> offerStocks() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(offerStocks));
    }

    public boolean hasOffer(ResourceLocation offerId) {
        return offerStocks.containsKey(offerId);
    }

    public int stockFor(ResourceLocation offerId) {
        return offerStocks.getOrDefault(offerId, 0);
    }

    public void setInventory(LinkedHashMap<ResourceLocation, Integer> stocks) {
        offerStocks.clear();
        for (var entry : stocks.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            offerStocks.put(entry.getKey(), Math.max(0, entry.getValue()));
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

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        initialized = tag.getBoolean("initialized");
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
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("initialized", initialized);
        ListTag list = new ListTag();
        for (var entry : offerStocks.entrySet()) {
            CompoundTag stockTag = new CompoundTag();
            stockTag.putString("id", entry.getKey().toString());
            stockTag.putInt("stock", Math.max(0, entry.getValue()));
            list.add(stockTag);
        }
        tag.put("offer_stocks", list);
    }

    private void setChangedAndSync() {
        setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
}
