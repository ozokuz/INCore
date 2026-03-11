package io.github.ozokuz.incore.features.researchv2.discovery;

import io.github.ozokuz.incore.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

public class TranslatorBlockEntity extends BlockEntity {
    private static final int PROCESS_TIME = 100;

    private final ItemStackHandler items = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot == 0 && stack.is(Registration.CONTINUUM_DATA_REPORT_ITEM.get());
        }
    };
    private int progressTicks;

    public TranslatorBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.TRANSLATOR_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TranslatorBlockEntity blockEntity) {
        if (level.isClientSide) {
            return;
        }
        blockEntity.serverTick();
    }

    public ItemStack input() {
        return items.getStackInSlot(0);
    }

    public ItemStack output() {
        return items.getStackInSlot(1);
    }

    public boolean tryInsertInput(ItemStack stack) {
        if (stack.isEmpty() || !output().isEmpty()) {
            return false;
        }
        ItemStack remainder = items.insertItem(0, stack.copyWithCount(1), false);
        return remainder.isEmpty();
    }

    public ItemStack takeOutput() {
        return items.extractItem(1, 1, false);
    }

    public int progressTicks() {
        return progressTicks;
    }

    private void serverTick() {
        if (!output().isEmpty()) {
            progressTicks = 0;
            return;
        }

        ItemStack input = input();
        if (input.isEmpty() || !input.is(Registration.CONTINUUM_DATA_REPORT_ITEM.get())) {
            progressTicks = 0;
            return;
        }

        net.minecraft.resources.ResourceLocation reportId = ContinuumDataReportData.readReportId(input);
        ContinuumReportRegistry.ContinuumReportDefinition definition = ContinuumReportRegistry.get(reportId);
        if (definition == null) {
            progressTicks = 0;
            return;
        }

        progressTicks++;
        if (progressTicks < PROCESS_TIME) {
            return;
        }
        progressTicks = 0;

        items.extractItem(0, 1, false);
        ItemStack decoded = new ItemStack(Registration.DECODED_CONTINUUM_REPORT_ITEM.get());
        DiscoveryPayloadData.write(decoded, new DiscoveryPayload(definition.nodeIds(), "continuum_decode", definition.id().toString(), definition.displayName(), ""));
        items.setStackInSlot(1, decoded);
        setChanged();
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
        progressTicks = Math.max(0, tag.getInt("progressTicks"));
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", items.serializeNBT(registries));
        tag.putInt("progressTicks", progressTicks);
    }

    public @NotNull Component getDisplayName() {
        return Component.translatable("block.incore.translator");
    }
}
