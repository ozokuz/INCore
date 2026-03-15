package ozokuz.incore.features.research.discovery;

import ozokuz.incore.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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

public class TranslatorBlockEntity extends BlockEntity implements MenuProvider {
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
    public final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> output().isEmpty() ? progressTicks : PROCESS_TIME;
                case 1 -> PROCESS_TIME;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                progressTicks = Math.clamp(value, 0, PROCESS_TIME);
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

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

    public ItemStackHandler itemHandler() {
        return items;
    }

    public boolean canInteractWith(Player player) {
        if (player == null || level == null || level.getBlockEntity(worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 64.0D;
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
        return output().isEmpty() ? progressTicks : PROCESS_TIME;
    }

    public int maxProgressTicks() {
        return PROCESS_TIME;
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

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, @NotNull Inventory inventory, @NotNull Player player) {
        return new TranslatorMenu(containerId, inventory, this);
    }
}
