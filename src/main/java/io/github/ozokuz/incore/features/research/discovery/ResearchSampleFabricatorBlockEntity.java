package io.github.ozokuz.incore.features.research.discovery;

import io.github.ozokuz.incore.Registration;
import io.github.ozokuz.incore.features.research.ResearchManager;
import io.github.ozokuz.incore.features.research.registry.ResearchRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ResearchSampleFabricatorBlockEntity extends BlockEntity implements MenuProvider {
    private static final int PROCESS_TIME = 100;

    private final ItemStackHandler items = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot == 0 && stack.is(Registration.BLANK_RESEARCH_SAMPLE_ITEM.get());
        }
    };
    private @Nullable ResourceLocation pendingNodeId;
    private String pendingTeamId = "";
    private int progressTicks;
    public final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> items.getStackInSlot(1).isEmpty() ? progressTicks : PROCESS_TIME;
                case 1 -> PROCESS_TIME;
                case 2 -> isProcessing() ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> progressTicks = Math.clamp(value, 0, PROCESS_TIME);
                case 2 -> {
                    if (value <= 0) {
                        pendingNodeId = null;
                        pendingTeamId = "";
                    }
                }
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    public ResearchSampleFabricatorBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.RESEARCH_SAMPLE_FABRICATOR_BE.get(), pos, state);
    }

    public ItemStackHandler itemHandler() {
        return items;
    }

    public boolean canAccess(Player player) {
        if (player == null || level == null || level.getBlockEntity(worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    public boolean fabricate(String teamId, ResourceLocation nodeId) {
        if (level == null || level.isClientSide || teamId == null || teamId.isBlank() || nodeId == null) {
            return false;
        }
        if (!ResearchRegistry.nodes().containsKey(nodeId)) {
            return false;
        }
        if (!ResearchManager.isResearched(level.getServer(), teamId, nodeId)) {
            return false;
        }
        if (isProcessing() || !items.getStackInSlot(0).is(Registration.BLANK_RESEARCH_SAMPLE_ITEM.get()) || !items.getStackInSlot(1).isEmpty()) {
            return false;
        }

        items.extractItem(0, 1, false);
        pendingNodeId = nodeId;
        pendingTeamId = teamId;
        progressTicks = 0;
        setChanged();
        return true;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ResearchSampleFabricatorBlockEntity blockEntity) {
        if (!level.isClientSide) {
            blockEntity.serverTick();
        }
    }

    private void serverTick() {
        if (!isProcessing()) {
            progressTicks = 0;
            return;
        }
        if (!items.getStackInSlot(1).isEmpty()) {
            clearPending();
            return;
        }

        progressTicks++;
        if (progressTicks < PROCESS_TIME) {
            return;
        }

        ResourceLocation nodeId = pendingNodeId;
        String teamId = pendingTeamId;
        clearPending();
        if (level == null || nodeId == null || teamId.isBlank() || !ResearchRegistry.nodes().containsKey(nodeId)) {
            return;
        }

        ItemStack sample = new ItemStack(Registration.RESEARCH_SAMPLE_ITEM.get());
        String displayName = ResearchRegistry.nodes().get(nodeId).name();
        DiscoveryPayloadData.write(sample, new DiscoveryPayload(java.util.List.of(nodeId), "research_sample", nodeId.toString(), displayName, teamId));
        items.setStackInSlot(1, sample);
        setChanged();
    }

    private void clearPending() {
        pendingNodeId = null;
        pendingTeamId = "";
        progressTicks = 0;
        setChanged();
    }

    public boolean isProcessing() {
        return pendingNodeId != null && !pendingTeamId.isBlank();
    }

    public int progressTicks() {
        return items.getStackInSlot(1).isEmpty() ? progressTicks : PROCESS_TIME;
    }

    public int maxProgressTicks() {
        return PROCESS_TIME;
    }

    public void dropContents() {
        if (level == null) {
            return;
        }
        for (int slot = 0; slot < items.getSlots(); slot++) {
            ItemStack stack = items.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack.copy());
            }
        }
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("items"));
        pendingNodeId = ResourceLocation.tryParse(tag.getString("pendingNodeId"));
        pendingTeamId = tag.getString("pendingTeamId");
        progressTicks = Math.max(0, tag.getInt("progressTicks"));
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", items.serializeNBT(registries));
        if (pendingNodeId != null) {
            tag.putString("pendingNodeId", pendingNodeId.toString());
        }
        if (!pendingTeamId.isBlank()) {
            tag.putString("pendingTeamId", pendingTeamId);
        }
        tag.putInt("progressTicks", progressTicks);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("block.incore.research_sample_fabricator");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, @NotNull Inventory inventory, @NotNull Player player) {
        return new ResearchSampleFabricatorMenu(containerId, inventory, this);
    }
}
