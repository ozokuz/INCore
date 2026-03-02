package io.github.ozokuz.incore.features.researchv2.station;

import io.github.ozokuz.incore.Registration;
import io.github.ozokuz.incore.features.researchv2.ResearchManager;
import io.github.ozokuz.incore.features.researchv2.state.TeamResearchState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CrudeResearchStationBlockEntity extends BlockEntity implements MenuProvider, Container {
    public static final int FUEL_SLOT = 0;
    public static final int LOGIC_SLOT = 1;
    public static final int DRIVE_SLOT = 2;
    public static final int SLOT_COUNT = 3;

    private static final int DATA_RP_BUFFER = 0;
    private static final int DATA_BURN_TIME = 1;
    private static final int DATA_BURN_TOTAL = 2;
    private static final int DATA_HAS_TEAM = 3;
    private static final int DATA_RUN_TICK_PROGRESS = 4;
    private static final int DATA_RUN_TICK_REQUIRED = 5;
    private static final int DATA_COMPLETED_RUNS = 6;
    private static final int DATA_REQUIRED_RUNS = 7;
    private static final int DATA_QUEUE_STATUS = 8;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private String teamId = "";
    private int burnTimeRemaining;
    private int burnTimeTotal;
    private int runTickProgressDisplay;
    private int runTickRequiredDisplay;
    private int completedRunsDisplay;
    private int requiredRunsDisplay;
    private int queueStatusDisplay = -1;

    public final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_RP_BUFFER -> availableResearchPower();
                case DATA_BURN_TIME -> Math.max(0, burnTimeRemaining);
                case DATA_BURN_TOTAL -> Math.max(0, burnTimeTotal);
                case DATA_HAS_TEAM -> teamId.isBlank() ? 0 : 1;
                case DATA_RUN_TICK_PROGRESS -> Math.max(0, runTickProgressDisplay);
                case DATA_RUN_TICK_REQUIRED -> Math.max(1, runTickRequiredDisplay);
                case DATA_COMPLETED_RUNS -> Math.max(0, completedRunsDisplay);
                case DATA_REQUIRED_RUNS -> Math.max(1, requiredRunsDisplay);
                case DATA_QUEUE_STATUS -> queueStatusDisplay;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case DATA_BURN_TIME -> burnTimeRemaining = Math.max(0, value);
                case DATA_BURN_TOTAL -> burnTimeTotal = Math.max(0, value);
                case DATA_RUN_TICK_PROGRESS -> runTickProgressDisplay = Math.max(0, value);
                case DATA_RUN_TICK_REQUIRED -> runTickRequiredDisplay = Math.max(1, value);
                case DATA_COMPLETED_RUNS -> completedRunsDisplay = Math.max(0, value);
                case DATA_REQUIRED_RUNS -> requiredRunsDisplay = Math.max(1, value);
                case DATA_QUEUE_STATUS -> queueStatusDisplay = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return 9;
        }
    };

    public CrudeResearchStationBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.CRUDE_RESEARCH_STATION_BE.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            CrudeResearchStationRegistry.register(this);
        }
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide) {
            CrudeResearchStationRegistry.unregister(this);
        }
        super.setRemoved();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CrudeResearchStationBlockEntity station) {
        if (level.isClientSide) {
            return;
        }
        station.serverTick();
    }

    private void serverTick() {
        boolean changed = false;

        if (level != null && level.getServer() != null && !teamId.isBlank()) {
            ResearchManager.ensureTeamState(level.getServer(), teamId);
        }

        if (refreshResearchDisplayData()) {
            changed = true;
        }

        if (changed) {
            setChanged();
        }
    }

    private boolean tryStartBurningFuel() {
        ItemStack fuel = items.get(FUEL_SLOT);
        if (fuel.isEmpty()) {
            return false;
        }

        int burnTime = fuel.getBurnTime(null);
        if (burnTime <= 0) {
            return false;
        }

        fuel.shrink(1);
        if (fuel.isEmpty()) {
            items.set(FUEL_SLOT, ItemStack.EMPTY);
        }
        burnTimeRemaining = burnTime;
        burnTimeTotal = burnTime;
        return true;
    }

    public String teamId() {
        return teamId;
    }

    public void setTeamId(@Nullable String teamId) {
        String next = teamId == null ? "" : teamId.strip();
        if (this.teamId.equals(next)) {
            return;
        }
        this.teamId = next;
        setChanged();
    }

    public int researchPowerBuffer() {
        return availableResearchPower();
    }

    public int consumeResearchPower(int amount) {
        int requested = Math.max(0, amount);
        if (requested <= 0) {
            return 0;
        }

        int remaining = requested;
        int consumed = 0;
        while (remaining > 0) {
            if (burnTimeRemaining <= 0) {
                if (!tryStartBurningFuel()) {
                    break;
                }
            }

            int step = Math.min(remaining, Math.max(0, burnTimeRemaining));
            if (step <= 0) {
                break;
            }
            burnTimeRemaining -= step;
            remaining -= step;
            consumed += step;
            if (burnTimeRemaining <= 0) {
                burnTimeTotal = 0;
            }
        }

        if (consumed <= 0) {
            return 0;
        }
        setChanged();
        return consumed;
    }

    public int availableResearchPower() {
        long total = Math.max(0, burnTimeRemaining);

        ItemStack fuel = items.get(FUEL_SLOT);
        if (!fuel.isEmpty()) {
            int burnPerItem = fuel.getBurnTime(null);
            if (burnPerItem > 0) {
                total += (long) burnPerItem * (long) fuel.getCount();
            }
        }
        if (total >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) Math.max(0L, total);
    }

    public int burnTimeRemainingForDisplay() {
        return Math.max(0, burnTimeRemaining);
    }

    public int burnTimeTotalForDisplay() {
        return Math.max(0, burnTimeTotal);
    }

    public int runTickProgressForDisplay() {
        return Math.max(0, runTickProgressDisplay);
    }

    public int runTickRequiredForDisplay() {
        return Math.max(1, runTickRequiredDisplay);
    }

    public int completedRunsForDisplay() {
        return Math.max(0, completedRunsDisplay);
    }

    public int requiredRunsForDisplay() {
        return Math.max(1, requiredRunsDisplay);
    }

    public int queueStatusForDisplay() {
        return queueStatusDisplay;
    }

    public int availableBasicLogicDurability() {
        ItemStack stack = items.get(LOGIC_SLOT);
        if (!stack.is(Registration.BASIC_LOGIC_MODULE_ITEM.get())) {
            return 0;
        }
        int maxDamage = stack.getMaxDamage();
        if (maxDamage <= 0) {
            return 0;
        }
        return Math.max(0, maxDamage - stack.getDamageValue());
    }

    public int consumeBasicLogicDurability(int amount) {
        int requested = Math.max(0, amount);
        if (requested <= 0) {
            return 0;
        }

        ItemStack stack = items.get(LOGIC_SLOT);
        if (!stack.is(Registration.BASIC_LOGIC_MODULE_ITEM.get())) {
            return 0;
        }

        int maxDamage = stack.getMaxDamage();
        if (maxDamage <= 0) {
            return 0;
        }

        int available = Math.max(0, maxDamage - stack.getDamageValue());
        if (available <= 0) {
            items.set(LOGIC_SLOT, ItemStack.EMPTY);
            setChanged();
            return 0;
        }

        int consumed = Math.min(requested, available);
        int nextDamage = stack.getDamageValue() + consumed;
        if (nextDamage >= maxDamage) {
            items.set(LOGIC_SLOT, ItemStack.EMPTY);
        } else {
            stack.setDamageValue(nextDamage);
        }
        if (consumed > 0) {
            setChanged();
        }
        return consumed;
    }

    public int countStarterData() {
        ItemStack stack = items.get(DRIVE_SLOT);
        if (!stack.is(Registration.STARTER_DATA_ITEM.get())) {
            return 0;
        }
        return stack.getCount();
    }

    public int consumeStarterData(int amount) {
        int requested = Math.max(0, amount);
        if (requested <= 0) {
            return 0;
        }

        ItemStack stack = items.get(DRIVE_SLOT);
        if (!stack.is(Registration.STARTER_DATA_ITEM.get())) {
            return 0;
        }

        int consumed = Math.min(requested, stack.getCount());
        stack.shrink(consumed);
        if (stack.isEmpty()) {
            items.set(DRIVE_SLOT, ItemStack.EMPTY);
        }
        if (consumed > 0) {
            setChanged();
        }
        return consumed;
    }

    public NonNullList<ItemStack> itemsForDrop() {
        return NonNullList.of(ItemStack.EMPTY, items.toArray(ItemStack[]::new));
    }

    private boolean refreshResearchDisplayData() {
        if (level == null || level.getServer() == null || teamId.isBlank()) {
            return setDisplayData(0, 1, 0, 1, -1);
        }

        TeamResearchState state = ResearchManager.ensureTeamState(level.getServer(), teamId);
        if (state.researchQueue().isEmpty()) {
            return setDisplayData(0, 1, 0, 1, -1);
        }

        var head = state.researchQueue().get(0);
        return setDisplayData(
                Math.max(0, head.runTickProgress()),
                Math.max(1, head.runTickRequired()),
                Math.max(0, head.completedRuns()),
                Math.max(1, head.requiredRuns()),
                head.status().ordinal()
        );
    }

    private boolean setDisplayData(int runTickProgress, int runTickRequired, int completedRuns, int requiredRuns, int queueStatus) {
        int normalizedProgress = Math.max(0, runTickProgress);
        int normalizedTickRequired = Math.max(1, runTickRequired);
        int normalizedRequiredRuns = Math.max(1, requiredRuns);
        int normalizedCompletedRuns = Math.max(0, Math.min(completedRuns, normalizedRequiredRuns));

        if (runTickProgressDisplay == normalizedProgress
                && runTickRequiredDisplay == normalizedTickRequired
                && completedRunsDisplay == normalizedCompletedRuns
                && requiredRunsDisplay == normalizedRequiredRuns
                && queueStatusDisplay == queueStatus) {
            return false;
        }

        runTickProgressDisplay = normalizedProgress;
        runTickRequiredDisplay = normalizedTickRequired;
        completedRunsDisplay = normalizedCompletedRuns;
        requiredRunsDisplay = normalizedRequiredRuns;
        queueStatusDisplay = queueStatus;
        return true;
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        clearContent();

        ListTag itemsTag = tag.getList("items", Tag.TAG_COMPOUND);
        for (int i = 0; i < itemsTag.size(); i++) {
            CompoundTag row = itemsTag.getCompound(i);
            int slot = row.getInt("slot");
            if (slot < 0 || slot >= SLOT_COUNT) {
                continue;
            }
            items.set(slot, ItemStack.parseOptional(registries, row.getCompound("stack")));
        }

        teamId = tag.getString("teamId");
        burnTimeRemaining = Math.max(0, tag.getInt("burnTimeRemaining"));
        burnTimeTotal = Math.max(0, tag.getInt("burnTimeTotal"));
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);

        ListTag itemsTag = new ListTag();
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack stack = items.get(slot);
            if (stack.isEmpty()) {
                continue;
            }

            CompoundTag row = new CompoundTag();
            row.putInt("slot", slot);
            row.put("stack", stack.save(registries));
            itemsTag.add(row);
        }
        tag.put("items", itemsTag);

        if (!teamId.isBlank()) {
            tag.putString("teamId", teamId);
        }
        tag.putInt("burnTimeRemaining", Math.max(0, burnTimeRemaining));
        tag.putInt("burnTimeTotal", Math.max(0, burnTimeTotal));
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("block.incore.crude_research_station");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, @NotNull Inventory inventory, @NotNull Player player) {
        return new CrudeResearchStationMenu(containerId, inventory, this);
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
        if (slot < 0 || slot >= SLOT_COUNT) {
            return ItemStack.EMPTY;
        }
        return items.get(slot);
    }

    @Override
    public @NotNull ItemStack removeItem(int slot, int amount) {
        if (slot < 0 || slot >= SLOT_COUNT || amount <= 0) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = items.get(slot);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack removed = stack.split(amount);
        if (stack.isEmpty()) {
            items.set(slot, ItemStack.EMPTY);
        }
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int slot) {
        if (slot < 0 || slot >= SLOT_COUNT) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = items.get(slot);
        items.set(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setItem(int slot, @NotNull ItemStack stack) {
        if (slot < 0 || slot >= SLOT_COUNT) {
            return;
        }

        ItemStack next = stack.copy();
        int max = getMaxStackSize(next);
        if (!next.isEmpty() && next.getCount() > max) {
            next.setCount(max);
        }
        items.set(slot, next);
        setChanged();
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        if (level == null || level.getBlockEntity(worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
        if (slot == FUEL_SLOT) {
            return stack.getBurnTime(null) > 0;
        }
        if (slot == LOGIC_SLOT) {
            return stack.is(Registration.BASIC_LOGIC_MODULE_ITEM.get());
        }
        if (slot == DRIVE_SLOT) {
            return stack.is(Registration.STARTER_DATA_ITEM.get());
        }
        return false;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < SLOT_COUNT; i++) {
            items.set(i, ItemStack.EMPTY);
        }
    }
}
