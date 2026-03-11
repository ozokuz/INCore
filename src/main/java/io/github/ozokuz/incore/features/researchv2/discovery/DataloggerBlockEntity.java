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
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class DataloggerBlockEntity extends BlockEntity {
    private static final int SCAN_INTERVAL_TICKS = 200;

    private ItemStack bufferedReport = ItemStack.EMPTY;
    private int scanTicks;

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
        return bufferedReport;
    }

    public ItemStack takeBufferedReport() {
        ItemStack stack = bufferedReport.copy();
        bufferedReport = ItemStack.EMPTY;
        setChanged();
        return stack;
    }

    public boolean hasBufferedReport() {
        return !bufferedReport.isEmpty();
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
        bufferedReport = report;
        setChanged();
    }

    public void dropContents() {
        if (level == null || bufferedReport.isEmpty()) {
            return;
        }
        Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), bufferedReport.copy());
        bufferedReport = ItemStack.EMPTY;
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        bufferedReport = ItemStack.parseOptional(registries, tag.getCompound("bufferedReport"));
        scanTicks = Math.max(0, tag.getInt("scanTicks"));
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        if (!bufferedReport.isEmpty()) {
            tag.put("bufferedReport", bufferedReport.save(registries));
        }
        tag.putInt("scanTicks", scanTicks);
    }

    public @NotNull Component getDisplayName() {
        return Component.translatable("block.incore.datalogger");
    }
}
