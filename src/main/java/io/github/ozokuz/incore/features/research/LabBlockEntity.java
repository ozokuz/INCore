package io.github.ozokuz.incore.features.research;

import io.github.ozokuz.incore.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class LabBlockEntity extends BlockEntity implements net.minecraft.world.MenuProvider {
    private ItemStack input = ItemStack.EMPTY;
    private @Nullable UUID owner;
    private int progress;
    private int maxProgress;
    private @Nullable LabProcessData activeProcess;

    public LabBlockEntity(BlockPos pos, BlockState blockState) {
        super(Registration.RESEARCH_LAB_BE.get(), pos, blockState);
    }

    public final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> maxProgress;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) progress = value;
            if (index == 1) maxProgress = value;
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public static void tick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, LabBlockEntity lab) {
        if (level.isClientSide || lab.owner == null) {
            return;
        }

        if (lab.activeProcess == null || !lab.matches(lab.activeProcess)) {
            lab.activeProcess = LabProcessManager.match(lab.input);
            lab.progress = 0;
            lab.maxProgress = lab.activeProcess == null ? 0 : lab.activeProcess.durationTicks();
            lab.setChanged();
        }

        if (lab.activeProcess == null || lab.maxProgress <= 0) {
            return;
        }

        var player = level.getServer().getPlayerList().getPlayer(lab.owner);
        if (player == null) {
            return;
        }

        lab.progress++;
        if (lab.progress >= lab.maxProgress) {
            lab.input.shrink(lab.activeProcess.itemCount());
            ResearchProgressService.addPoints(player, lab.activeProcess.rewardPoints());
            lab.progress = 0;
            lab.activeProcess = LabProcessManager.match(lab.input);
            lab.maxProgress = lab.activeProcess == null ? 0 : lab.activeProcess.durationTicks();
        }
        lab.setChanged();
    }

    public boolean matches(LabProcessData process) {
        return process != null && !input.isEmpty() && input.getCount() >= process.itemCount() && input.is(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(process.itemId()));
    }

    public ItemStack getInput() {
        return input;
    }

    public void setInput(ItemStack stack) {
        this.input = stack;
        setChanged();
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
        input = ItemStack.parseOptional(registries, tag.getCompound("input"));
        owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
        progress = tag.getInt("progress");
        maxProgress = tag.getInt("maxProgress");
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("input", input.saveOptional(registries));
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
}
