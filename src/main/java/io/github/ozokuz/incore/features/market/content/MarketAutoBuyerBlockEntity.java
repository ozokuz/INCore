package io.github.ozokuz.incore.features.market.content;

import dev.ithundxr.createnumismatics.content.backend.BankAccount;
import io.github.ozokuz.incore.Config;
import io.github.ozokuz.incore.Registration;
import io.github.ozokuz.incore.features.market.MarketBanking;
import io.github.ozokuz.incore.features.market.MarketItemManager;
import io.github.ozokuz.incore.features.market.MarketPricingService;
import io.github.ozokuz.incore.features.market.MarketTeamAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class MarketAutoBuyerBlockEntity extends BlockEntity implements Container, MenuProvider {
    public static final int CARD_SLOT = 0;
    public static final int OUTPUT_START = 1;
    public static final int OUTPUT_COUNT = 27;
    public static final int SLOT_COUNT = OUTPUT_START + OUTPUT_COUNT;

    public static final int STATUS_READY = 0;
    public static final int STATUS_DISABLED = 1;
    public static final int STATUS_NO_CARD = 2;
    public static final int STATUS_NO_TARGET = 3;
    public static final int STATUS_PRICE_TOO_HIGH = 4;
    public static final int STATUS_NO_FUNDS = 5;
    public static final int STATUS_OUTPUT_FULL = 6;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private @Nullable UUID owner;

    private @Nullable ResourceLocation targetItemId;
    private int priceCapSpur = 64;
    private int batchSize = 1;
    private boolean enabled = true;
    private int progress;
    private int status = STATUS_READY;

    public final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> Math.max(1, Config.MARKET_AUTOBUYER_INTERVAL_TICKS.get());
                case 2 -> status;
                case 3 -> priceCapSpur;
                case 4 -> batchSize;
                case 5 -> enabled ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                progress = Math.max(0, value);
            }
            if (index == 2) {
                status = value;
            }
            if (index == 3) {
                priceCapSpur = Math.max(1, value);
            }
            if (index == 4) {
                batchSize = Math.clamp(value, 1, 64);
            }
            if (index == 5) {
                enabled = value != 0;
            }
        }

        @Override
        public int getCount() {
            return 6;
        }
    };

    public MarketAutoBuyerBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.MARKET_AUTOBUYER_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MarketAutoBuyerBlockEntity be) {
        if (level.isClientSide) {
            return;
        }

        if (!be.enabled) {
            be.progress = 0;
            be.status = STATUS_DISABLED;
            be.setChanged();
            return;
        }

        ItemStack card = be.items.get(CARD_SLOT);
        if (card.isEmpty()) {
            be.progress = 0;
            be.status = STATUS_NO_CARD;
            be.setChanged();
            return;
        }

        if (be.targetItemId == null || !MarketItemManager.isTradeable(be.targetItemId)) {
            be.progress = 0;
            be.status = STATUS_NO_TARGET;
            be.setChanged();
            return;
        }

        if (level.getServer() == null) {
            return;
        }

        int unitPrice = MarketPricingService.currentPrice(level.getServer(), be.targetItemId);
        if (unitPrice <= 0) {
            return;
        }

        if (unitPrice > be.priceCapSpur) {
            be.progress = 0;
            be.status = STATUS_PRICE_TOO_HIGH;
            be.setChanged();
            return;
        }

        if (!be.canInsertFully(be.targetItemId, be.batchSize)) {
            be.progress = 0;
            be.status = STATUS_OUTPUT_FULL;
            be.setChanged();
            return;
        }

        BankAccount account = MarketBanking.resolveCardAccount(null, card, false);
        if (account == null) {
            be.progress = 0;
            be.status = STATUS_NO_CARD;
            be.setChanged();
            return;
        }

        long totalCostLong = (long) unitPrice * be.batchSize;
        int totalCost = (int) Math.min(Integer.MAX_VALUE, totalCostLong);
        if (MarketBanking.balanceSpur(account) < totalCost) {
            be.progress = 0;
            be.status = STATUS_NO_FUNDS;
            be.setChanged();
            return;
        }

        be.status = STATUS_READY;
        be.progress++;
        int interval = Math.max(1, Config.MARKET_AUTOBUYER_INTERVAL_TICKS.get());
        if (be.progress < interval) {
            be.setChanged();
            return;
        }

        be.progress = 0;
        if (!MarketBanking.withdraw(account, totalCost)) {
            be.status = STATUS_NO_FUNDS;
            be.setChanged();
            return;
        }

        be.insertOutput(be.targetItemId, be.batchSize);
        MarketPricingService.applyBuy(level.getServer(), be.targetItemId, be.batchSize);
        be.setChanged();
    }

    public boolean canAccess(Player player) {
        return MarketTeamAccess.canAccess(owner, player);
    }

    public void setOwner(@Nullable UUID owner) {
        this.owner = owner;
        setChanged();
    }

    public @Nullable ResourceLocation targetItemId() {
        return targetItemId;
    }

    public void setTargetItemId(@Nullable ResourceLocation targetItemId) {
        this.targetItemId = targetItemId;
        setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public int priceCapSpur() {
        return priceCapSpur;
    }

    public void setPriceCapSpur(int priceCapSpur) {
        this.priceCapSpur = Math.max(1, priceCapSpur);
        setChanged();
    }

    public int batchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = Math.clamp(batchSize, 1, 64);
        setChanged();
    }

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        setChanged();
    }

    private boolean canInsertFully(ResourceLocation itemId, int count) {
        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item == null || item == net.minecraft.world.item.Items.AIR) {
            return false;
        }

        int remaining = count;
        for (int slot = OUTPUT_START; slot < SLOT_COUNT; slot++) {
            ItemStack stack = items.get(slot);
            if (stack.isEmpty()) {
                remaining -= Math.min(remaining, item.getDefaultMaxStackSize());
            } else if (stack.getItem() == item) {
                int space = Math.max(0, stack.getMaxStackSize() - stack.getCount());
                remaining -= Math.min(remaining, space);
            }

            if (remaining <= 0) {
                return true;
            }
        }

        return remaining <= 0;
    }

    private void insertOutput(ResourceLocation itemId, int count) {
        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item == null || item == net.minecraft.world.item.Items.AIR) {
            return;
        }

        int remaining = count;
        for (int slot = OUTPUT_START; slot < SLOT_COUNT && remaining > 0; slot++) {
            ItemStack stack = items.get(slot);
            if (stack.isEmpty()) {
                int move = Math.min(remaining, item.getDefaultMaxStackSize());
                items.set(slot, new ItemStack(item, move));
                remaining -= move;
                continue;
            }

            if (stack.getItem() != item) {
                continue;
            }

            int move = Math.min(remaining, stack.getMaxStackSize() - stack.getCount());
            if (move <= 0) {
                continue;
            }
            stack.grow(move);
            remaining -= move;
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);

        ListTag itemsTag = new ListTag();
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) {
                continue;
            }

            CompoundTag row = new CompoundTag();
            row.putInt("slot", i);
            row.put("stack", stack.save(registries));
            itemsTag.add(row);
        }
        tag.put("items", itemsTag);

        if (owner != null) {
            tag.putUUID("owner", owner);
        }

        if (targetItemId != null) {
            tag.putString("targetItemId", targetItemId.toString());
        }
        tag.putInt("priceCapSpur", priceCapSpur);
        tag.putInt("batchSize", batchSize);
        tag.putBoolean("enabled", enabled);
        tag.putInt("progress", progress);
        tag.putInt("status", status);
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);

        for (int i = 0; i < SLOT_COUNT; i++) {
            items.set(i, ItemStack.EMPTY);
        }

        ListTag itemsTag = tag.getList("items", Tag.TAG_COMPOUND);
        for (Tag rowTag : itemsTag) {
            CompoundTag row = (CompoundTag) rowTag;
            int slot = row.getInt("slot");
            if (slot < 0 || slot >= items.size()) {
                continue;
            }
            items.set(slot, ItemStack.parseOptional(registries, row.getCompound("stack")));
        }

        owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;

        targetItemId = tag.contains("targetItemId", Tag.TAG_STRING)
                ? ResourceLocation.tryParse(tag.getString("targetItemId"))
                : null;
        priceCapSpur = Math.max(1, tag.getInt("priceCapSpur"));
        batchSize = Math.clamp(tag.getInt("batchSize"), 1, 64);
        enabled = tag.getBoolean("enabled");
        progress = Math.max(0, tag.getInt("progress"));
        status = tag.getInt("status");
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("block.incore.market_autobuyer");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory, @NotNull Player player) {
        return new MarketAutoBuyerMenu(containerId, playerInventory, this);
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public @NotNull ItemStack getItem(int slot) {
        if (slot < 0 || slot >= items.size()) {
            return ItemStack.EMPTY;
        }
        return items.get(slot);
    }

    @Override
    public @NotNull ItemStack removeItem(int slot, int amount) {
        ItemStack stack = getItem(slot);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = stack.split(amount);
        if (stack.isEmpty()) {
            items.set(slot, ItemStack.EMPTY);
        }
        setChanged();
        return removed;
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = getItem(slot);
        items.set(slot, ItemStack.EMPTY);
        setChanged();
        return stack;
    }

    @Override
    public void setItem(int slot, @NotNull ItemStack stack) {
        if (slot < 0 || slot >= items.size()) {
            return;
        }
        items.set(slot, stack);
        setChanged();
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return this.level != null
                && this.level.getBlockEntity(this.worldPosition) == this
                && player.distanceToSqr(
                this.worldPosition.getX() + 0.5D,
                this.worldPosition.getY() + 0.5D,
                this.worldPosition.getZ() + 0.5D
        ) <= 64.0D;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < items.size(); i++) {
            items.set(i, ItemStack.EMPTY);
        }
        setChanged();
    }
}
