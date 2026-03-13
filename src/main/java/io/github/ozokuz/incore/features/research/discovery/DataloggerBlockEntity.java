package io.github.ozokuz.incore.features.research.discovery;

import io.github.ozokuz.incore.Registration;
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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class DataloggerBlockEntity extends BlockEntity implements MenuProvider {
    private static final int SCAN_INTERVAL_TICKS = 200;

    private final ItemStackHandler items = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return false;
        }
    };
    private int scanTicks;
    public final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> hasBufferedReport() ? SCAN_INTERVAL_TICKS : scanTicks;
                case 1 -> SCAN_INTERVAL_TICKS;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                scanTicks = Math.clamp(value, 0, SCAN_INTERVAL_TICKS);
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public DataloggerBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.DATALOGGER_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DataloggerBlockEntity blockEntity) {
        if (level.isClientSide) {
            return;
        }
        blockEntity.serverTick();
    }

    public ItemStack bufferedReport() {
        return items.getStackInSlot(0);
    }

    public ItemStack takeBufferedReport() {
        return items.extractItem(0, 1, false);
    }

    public boolean hasBufferedReport() {
        return !items.getStackInSlot(0).isEmpty();
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

    private void serverTick() {
        if (level == null || hasBufferedReport()) {
            return;
        }
        scanTicks++;
        if (scanTicks < SCAN_INTERVAL_TICKS) {
            return;
        }
        scanTicks = 0;

        List<EnvironmentReportRegistry.EnvironmentReportDefinition> matches = EnvironmentReportRegistry.matching(level, worldPosition);
        if (matches.isEmpty()) {
            return;
        }

        Set<net.minecraft.resources.ResourceLocation> nodeIds = new LinkedHashSet<>();
        StringBuilder sourceId = new StringBuilder();
        String displayName = matches.size() == 1 ? matches.get(0).displayName() : "Research Data Report";
        for (EnvironmentReportRegistry.EnvironmentReportDefinition definition : matches) {
            nodeIds.addAll(definition.nodeIds());
            if (!sourceId.isEmpty()) {
                sourceId.append('+');
            }
            sourceId.append(definition.id());
        }
        if (nodeIds.isEmpty()) {
            return;
        }

        ItemStack report = new ItemStack(Registration.RESEARCH_DATA_REPORT_ITEM.get());
        DiscoveryPayloadData.write(report, new DiscoveryPayload(List.copyOf(nodeIds), "datalogger", sourceId.toString(), displayName, ""));
        items.setStackInSlot(0, report);
        setChanged();
    }

    public void dropContents() {
        if (level == null) {
            return;
        }
        ItemStack bufferedReport = items.getStackInSlot(0);
        if (!bufferedReport.isEmpty()) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), bufferedReport.copy());
            items.setStackInSlot(0, ItemStack.EMPTY);
        }
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("items"));
        scanTicks = Math.max(0, tag.getInt("scanTicks"));
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", items.serializeNBT(registries));
        tag.putInt("scanTicks", scanTicks);
    }

    public int progressTicks() {
        return hasBufferedReport() ? SCAN_INTERVAL_TICKS : scanTicks;
    }

    public int maxProgressTicks() {
        return SCAN_INTERVAL_TICKS;
    }

    public @NotNull Component getDisplayName() {
        return Component.translatable("block.incore.datalogger");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, @NotNull Inventory inventory, @NotNull Player player) {
        return new DataloggerMenu(containerId, inventory, this);
    }
}
