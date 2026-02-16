package io.github.ozokuz.incore.features.research;

import io.github.ozokuz.incore.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LabBlockEntity extends BlockEntity implements net.minecraft.world.MenuProvider {
    public static final int INPUT_SLOT_COUNT = 9;
    public static final int STATUS_NO_RESEARCH_SELECTED = 0;
    public static final int STATUS_NOT_ENOUGH_MATERIALS = 1;
    public static final int STATUS_WORKING = 2;

    private final NonNullList<ItemStack> inputs = NonNullList.withSize(INPUT_SLOT_COUNT, ItemStack.EMPTY);
    private @Nullable UUID owner;
    private int progress;
    private int maxProgress;
    private @Nullable ResourceLocation activeResearchId;
    private int activeResearchSyncIndex = -1;
    private int labStatus = STATUS_NO_RESEARCH_SELECTED;
    private int activeSlot = -1;
    private int overallProgressSync;
    private int overallMaxProgressSync;

    public LabBlockEntity(BlockPos pos, BlockState blockState) {
        super(Registration.RESEARCH_LAB_BE.get(), pos, blockState);
    }

    public final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> Math.max(0, progress);
                case 1 -> Math.max(0, maxProgress);
                case 2 -> {
                    if (level != null && !level.isClientSide) {
                        syncResearchIndex();
                    }
                    yield activeResearchSyncIndex;
                }
                case 3 -> labStatus;
                case 4 -> {
                    if (level != null && !level.isClientSide) {
                        yield overallProgressFromQueue();
                    }
                    yield Math.max(0, overallProgressSync);
                }
                case 5 -> {
                    if (level != null && !level.isClientSide) {
                        yield overallMaxFromQueue();
                    }
                    yield Math.max(0, overallMaxProgressSync);
                }
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) progress = Math.max(0, value);
            if (index == 1) maxProgress = Math.max(0, value);
            if (index == 2) {
                activeResearchSyncIndex = value;
                activeResearchId = decodeResearchIndex(value);
                if (activeResearchSyncIndex < 0) {
                    progress = 0;
                    maxProgress = 0;
                    labStatus = STATUS_NO_RESEARCH_SELECTED;
                }
            }
            if (index == 3) {
                labStatus = Math.clamp(value, STATUS_NO_RESEARCH_SELECTED, STATUS_WORKING);
            }
            if (index == 4) {
                overallProgressSync = Math.max(0, value);
            }
            if (index == 5) {
                overallMaxProgressSync = Math.max(0, value);
            }
        }

        @Override
        public int getCount() {
            return 6;
        }
    };

    public static void tick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, LabBlockEntity lab) {
        if (level.isClientSide || lab.owner == null) {
            return;
        }

        ServerPlayer player = level.getServer().getPlayerList().getPlayer(lab.owner);
        if (player == null) {
            return;
        }

        if (lab.activeResearchId == null || !lab.matchesActiveResearch(player)) {
            lab.refreshActiveProcess(player);
            lab.setChanged();
        }

        if (lab.activeResearchId == null || lab.maxProgress <= 0) {
            return;
        }

        if (!lab.canProcessActiveResearch(player)) {
            if (lab.progress != 0) {
                lab.progress = 0;
                lab.setChanged();
            }
            lab.updateLabStatus();
            return;
        }

        lab.progress++;
        lab.updateLabStatus();
        if (lab.progress >= lab.maxProgress) {
            ResearchEntryData entry = ResearchEntryManager.all().get(lab.activeResearchId);
            if (entry == null || !lab.consumeRequiredMaterials(entry)) {
                lab.refreshActiveProcess(player);
                lab.setChanged();
                return;
            }

            ResearchProgressService.addResearchProgress(player, lab.activeResearchId, 1);
            lab.refreshActiveProcess(player);
        }
        lab.setChanged();
    }

    private boolean matchesActiveResearch(ServerPlayer player) {
        if (activeResearchId == null) {
            return false;
        }

        ResourceLocation currentActive = ResearchProgressService.activeResearch(player);
        if (currentActive == null || !currentActive.equals(activeResearchId)) {
            return false;
        }

        ResearchEntryData entry = ResearchEntryManager.all().get(activeResearchId);
        if (entry == null || entry.researchMaterials().isEmpty()) {
            return false;
        }

        return true;
    }

    private boolean canProcessActiveResearch(ServerPlayer player) {
        if (!matchesActiveResearch(player)) {
            return false;
        }
        ResearchEntryData entry = ResearchEntryManager.all().get(activeResearchId);
        if (entry == null) {
            return false;
        }
        return hasRequiredMaterials(entry);
    }

    private void refreshActiveProcess(ServerPlayer player) {
        refreshActiveProcess(player, true);
    }

    private void refreshActiveProcess(ServerPlayer player, boolean resetProgress) {
        ResourceLocation previousResearch = activeResearchId;
        int previousSlot = activeSlot;
        maxProgress = 0;
        activeResearchId = null;
        activeSlot = -1;

        ResourceLocation activeResearch = ResearchProgressService.activeResearch(player);
        if (activeResearch != null) {
            ResearchEntryData entry = ResearchEntryManager.all().get(activeResearch);
            if (entry != null && !entry.researchMaterials().isEmpty()) {
                activeSlot = hasRequiredMaterials(entry) ? findRepresentativeSlot(entry) : -1;
                activeResearchId = activeResearch;
                maxProgress = entry.runDurationTicks();
                applyProgressState(resetProgress, previousResearch, previousSlot);
                syncResearchIndex();
                updateLabStatus();
                return;
            }
        }

        applyProgressState(resetProgress, previousResearch, previousSlot);
        syncResearchIndex();
        updateLabStatus();
    }

    private void applyProgressState(boolean resetProgress, @Nullable ResourceLocation previousResearch, int previousSlot) {
        if (activeResearchId == null) {
            progress = 0;
            return;
        }

        if (resetProgress
                || previousResearch == null
                || previousSlot < 0
                || !previousResearch.equals(activeResearchId)) {
            progress = 0;
            return;
        }

        progress = Math.clamp(progress, 0, maxProgress);
    }

    public ItemStack getInput(int slot) {
        if (slot < 0 || slot >= inputs.size()) {
            return ItemStack.EMPTY;
        }
        return inputs.get(slot);
    }

    public void setInput(int slot, ItemStack stack) {
        if (slot < 0 || slot >= inputs.size()) {
            return;
        }

        inputs.set(slot, stack);
        onInputsChanged();
    }

    public ItemStack removeInput(int slot, int amount) {
        if (slot < 0 || slot >= inputs.size()) {
            return ItemStack.EMPTY;
        }

        ItemStack input = inputs.get(slot);
        ItemStack removed = input.split(amount);
        if (input.isEmpty()) {
            inputs.set(slot, ItemStack.EMPTY);
        }
        onInputsChanged();
        return removed;
    }

    public ItemStack removeInputNoUpdate(int slot) {
        if (slot < 0 || slot >= inputs.size()) {
            return ItemStack.EMPTY;
        }

        ItemStack input = inputs.get(slot);
        inputs.set(slot, ItemStack.EMPTY);
        onInputsChanged();
        return input;
    }

    public boolean isInputEmpty() {
        return inputs.stream().allMatch(ItemStack::isEmpty);
    }

    public int inputSlotCount() {
        return inputs.size();
    }

    public NonNullList<ItemStack> getInputs() {
        return inputs;
    }

    public @Nullable UUID getOwner() {
        return owner;
    }

    public void setOwner(@Nullable UUID owner) {
        this.owner = owner;
        setChanged();
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);

        for (int i = 0; i < inputs.size(); i++) {
            inputs.set(i, ItemStack.EMPTY);
        }

        if (tag.contains("inputs", Tag.TAG_LIST)) {
            ListTag list = tag.getList("inputs", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag inputTag = list.getCompound(i);
                int slot = inputTag.getInt("slot");
                if (slot < 0 || slot >= inputs.size()) {
                    continue;
                }
                inputs.set(slot, ItemStack.parseOptional(registries, inputTag.getCompound("stack")));
            }
        } else {
            // Legacy data support from the old single-slot lab format.
            inputs.set(0, ItemStack.parseOptional(registries, tag.getCompound("input")));
        }

        owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
        progress = tag.getInt("progress");
        maxProgress = tag.getInt("maxProgress");
        activeSlot = -1;
        activeResearchId = null;
        activeResearchSyncIndex = -1;
        labStatus = STATUS_NO_RESEARCH_SELECTED;
    }

    private boolean hasRequiredMaterials(ResearchEntryData entry) {
        Map<Item, Integer> required = requiredByItem(entry);
        if (required.isEmpty()) {
            return false;
        }

        for (Map.Entry<Item, Integer> requirement : required.entrySet()) {
            int available = 0;
            for (ItemStack stack : inputs) {
                if (!stack.isEmpty() && stack.is(requirement.getKey())) {
                    available += stack.getCount();
                }
            }
            if (available < requirement.getValue()) {
                return false;
            }
        }

        return true;
    }

    private boolean consumeRequiredMaterials(ResearchEntryData entry) {
        Map<Item, Integer> required = requiredByItem(entry);
        if (required.isEmpty()) {
            return false;
        }
        if (!hasRequiredMaterials(entry)) {
            return false;
        }

        for (Map.Entry<Item, Integer> requirement : required.entrySet()) {
            int remaining = requirement.getValue();
            for (int slot = 0; slot < inputs.size() && remaining > 0; slot++) {
                ItemStack stack = inputs.get(slot);
                if (stack.isEmpty() || !stack.is(requirement.getKey())) {
                    continue;
                }

                int removed = Math.min(remaining, stack.getCount());
                stack.shrink(removed);
                if (stack.isEmpty()) {
                    inputs.set(slot, ItemStack.EMPTY);
                }
                remaining -= removed;
            }
        }
        return true;
    }

    private int findRepresentativeSlot(ResearchEntryData entry) {
        Map<Item, Integer> required = requiredByItem(entry);
        if (required.isEmpty()) {
            return -1;
        }

        for (int slot = 0; slot < inputs.size(); slot++) {
            ItemStack stack = inputs.get(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (required.containsKey(stack.getItem())) {
                return slot;
            }
        }
        return -1;
    }

    private Map<Item, Integer> requiredByItem(ResearchEntryData entry) {
        Map<Item, Integer> required = new HashMap<>();
        for (ResearchMaterialData material : entry.researchMaterials()) {
            if (material.itemId() == null || material.itemCount() <= 0) {
                continue;
            }
            Item item = BuiltInRegistries.ITEM.get(material.itemId());
            if (item == null) {
                continue;
            }
            required.merge(item, material.itemCount(), Integer::sum);
        }
        return required;
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);

        ListTag list = new ListTag();
        for (int slot = 0; slot < inputs.size(); slot++) {
            ItemStack input = inputs.get(slot);
            if (input.isEmpty()) {
                continue;
            }

            CompoundTag inputTag = new CompoundTag();
            inputTag.putInt("slot", slot);
            inputTag.put("stack", input.saveOptional(registries));
            list.add(inputTag);
        }
        tag.put("inputs", list);

        if (owner != null) {
            tag.putUUID("owner", owner);
        }
        tag.putInt("progress", progress);
        tag.putInt("maxProgress", maxProgress);
    }

    @Override
    public @NotNull net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("block.incore.research_lab");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, @NotNull Inventory inventory, @NotNull Player player) {
        return new LabMenu(containerId, inventory, this);
    }

    private static int encodeResearchIndex(@Nullable ResourceLocation researchId) {
        if (researchId == null) {
            return -1;
        }
        return sortedResearchIds().indexOf(researchId);
    }

    private static @Nullable ResourceLocation decodeResearchIndex(int index) {
        List<ResourceLocation> ids = sortedResearchIds();
        if (index < 0 || index >= ids.size()) {
            return null;
        }
        return ids.get(index);
    }

    private static List<ResourceLocation> sortedResearchIds() {
        return ResearchEntryManager.all().keySet().stream()
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .toList();
    }

    private void syncResearchIndex() {
        activeResearchSyncIndex = encodeResearchIndex(activeResearchId);
    }

    private void updateLabStatus() {
        if (activeResearchId == null || maxProgress <= 0) {
            labStatus = STATUS_NO_RESEARCH_SELECTED;
            return;
        }

        ResearchEntryData entry = ResearchEntryManager.all().get(activeResearchId);
        if (entry == null || entry.researchMaterials().isEmpty()) {
            labStatus = STATUS_NO_RESEARCH_SELECTED;
            return;
        }

        labStatus = hasRequiredMaterials(entry) ? STATUS_WORKING : STATUS_NOT_ENOUGH_MATERIALS;
    }

    private void onInputsChanged() {
        progress = 0;
        if (level == null || level.isClientSide) {
            setChanged();
            return;
        }

        if (owner == null || level.getServer() == null) {
            clearActiveProcessState();
            setChanged();
            return;
        }

        ServerPlayer player = level.getServer().getPlayerList().getPlayer(owner);
        if (player == null) {
            clearActiveProcessState();
            setChanged();
            return;
        }

        refreshActiveProcess(player);
        setChanged();
    }

    private void clearActiveProcessState() {
        maxProgress = 0;
        activeSlot = -1;
        activeResearchId = null;
        activeResearchSyncIndex = -1;
        labStatus = STATUS_NO_RESEARCH_SELECTED;
    }

    private int overallProgressFromQueue() {
        ServerPlayer player = ownerPlayer();
        if (player == null) {
            return 0;
        }
        ResourceLocation queueActive = ResearchProgressService.activeResearch(player);
        if (queueActive == null) {
            return 0;
        }
        return Math.max(0, ResearchProgressService.progressFor(player, queueActive));
    }

    private int overallMaxFromQueue() {
        ServerPlayer player = ownerPlayer();
        if (player == null) {
            return 0;
        }
        ResourceLocation queueActive = ResearchProgressService.activeResearch(player);
        if (queueActive == null) {
            return 0;
        }
        ResearchEntryData entry = ResearchEntryManager.all().get(queueActive);
        if (entry == null) {
            return 0;
        }
        return Math.max(0, entry.cost());
    }

    private @Nullable ServerPlayer ownerPlayer() {
        if (level == null || level.isClientSide || owner == null || level.getServer() == null) {
            return null;
        }
        return level.getServer().getPlayerList().getPlayer(owner);
    }
}
