package io.github.ozokuz.incore.features.market.content;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class ShipmentTerminalBlockEntity extends KineticBlockEntity implements Container, MenuProvider {
    public static final int INPUT_SLOT_COUNT = 9;
    public static final int CARD_SLOT = 9;
    public static final int SLOT_COUNT = 10;

    public static final int STATUS_IDLE = 0;
    public static final int STATUS_NO_CARD = 1;
    public static final int STATUS_NO_ITEMS = 2;
    public static final int STATUS_INVALID_ITEM = 3;
    public static final int STATUS_NEED_FULL_STACK = 4;
    public static final int STATUS_NO_RPM = 5;
    public static final int STATUS_NO_STRESS = 6;
    public static final int STATUS_NO_POWER = 7;

    protected static final int MIN_REQUIRED_RPM = 128;
    protected static final float STATIC_STRESS = 1024.0F;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private @Nullable UUID owner;
    protected int progress;
    protected int status = STATUS_IDLE;

    public final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> Math.max(1, Config.MARKET_SHIPMENT_INTERVAL_TICKS.get());
                case 2 -> status;
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
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    public ShipmentTerminalBlockEntity(BlockPos pos, BlockState state) {
        this(Registration.SHIPMENT_TERMINAL_BE.get(), pos, state);
    }

    protected ShipmentTerminalBlockEntity(BlockEntityType<?> blockEntityType, BlockPos pos, BlockState state) {
        super(blockEntityType, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ShipmentTerminalBlockEntity be) {
        be.tick();
        if (level.isClientSide) {
            return;
        }
        be.serverTick(level);
    }

    protected void serverTick(Level level) {
        int interval = Math.max(1, Config.MARKET_SHIPMENT_INTERVAL_TICKS.get());
        refreshStressInNetwork();

        if (!hasOperationalPower()) {
            progress = 0;
            setChanged();
            return;
        }

        ItemStack card = items.get(CARD_SLOT);
        if (card.isEmpty()) {
            progress = 0;
            status = STATUS_NO_CARD;
            setChanged();
            return;
        }

        int slot = firstInputSlotWithItems();
        if (slot < 0) {
            progress = 0;
            status = STATUS_NO_ITEMS;
            setChanged();
            return;
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(items.get(slot).getItem());
        if (!MarketItemManager.isTradeable(itemId)) {
            progress = 0;
            status = STATUS_INVALID_ITEM;
            setChanged();
            return;
        }

        ItemStack inputStack = items.get(slot);
        int requiredStackSize = Math.max(1, inputStack.getMaxStackSize());
        if (inputStack.getCount() < requiredStackSize) {
            progress = 0;
            status = STATUS_NEED_FULL_STACK;
            setChanged();
            return;
        }

        if (!consumePowerForWorkTick()) {
            progress = 0;
            setChanged();
            return;
        }

        status = STATUS_IDLE;
        progress++;
        if (progress < interval) {
            setChanged();
            return;
        }

        progress = 0;
        if (level.getServer() == null) {
            return;
        }

        ItemStack stack = items.get(slot);
        int sellingCount = Math.max(1, stack.getMaxStackSize());
        int soldStacks = 1;
        int unitPrice = MarketPricingService.currentPrice(level.getServer(), itemId);
        if (unitPrice <= 0 || sellingCount <= 0) {
            return;
        }

        BankAccount account = MarketBanking.resolveCardAccount(null, card, false);
        if (account == null) {
            status = STATUS_NO_CARD;
            setChanged();
            return;
        }

        stack.shrink(sellingCount);
        long payoutLong = (long) unitPrice * soldStacks;
        int payout = (int) Math.min(Integer.MAX_VALUE, payoutLong);
        MarketBanking.deposit(account, payout);
        MarketPricingService.applySell(level.getServer(), itemId, soldStacks);
        setChanged();
    }

    protected boolean hasOperationalPower() {
        if (isOverStressed()) {
            status = STATUS_NO_STRESS;
            return false;
        }

        if (Math.abs(getSpeed()) < MIN_REQUIRED_RPM) {
            status = STATUS_NO_RPM;
            return false;
        }

        return true;
    }

    protected boolean consumePowerForWorkTick() {
        return true;
    }

    protected void refreshStressInNetwork() {
        if (hasNetwork()) {
            getOrCreateNetwork().updateStressFor(this, calculateStressApplied());
        }
    }

    public int progressForDisplay() {
        return progress;
    }

    public int maxProgressForDisplay() {
        return Math.max(1, Config.MARKET_SHIPMENT_INTERVAL_TICKS.get());
    }

    public int statusForDisplay() {
        return status;
    }

    public int rpmForDisplay() {
        return Math.round(Math.abs(getSpeed()));
    }

    @Override
    public float calculateStressApplied() {
        float speed = Math.abs(getTheoreticalSpeed());
        float applied = speed <= 0 ? 0 : STATIC_STRESS / speed;
        this.lastStressApplied = applied;
        return applied;
    }

    private int firstInputSlotWithItems() {
        for (int slot = 0; slot < INPUT_SLOT_COUNT; slot++) {
            if (!items.get(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    public void setOwner(@Nullable UUID owner) {
        this.owner = owner;
        setChanged();
    }

    public boolean canAccess(Player player) {
        return MarketTeamAccess.canAccess(owner, player);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);

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
        tag.putInt("progress", progress);
        tag.putInt("status", status);
        if (owner != null) {
            tag.putUUID("owner", owner);
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);

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

        progress = Math.max(0, tag.getInt("progress"));
        status = tag.getInt("status");
        owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("block.incore.shipment_terminal");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory, @NotNull Player player) {
        return new ShipmentTerminalMenu(containerId, playerInventory, this);
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
