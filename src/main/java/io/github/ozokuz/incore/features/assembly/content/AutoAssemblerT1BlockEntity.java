package io.github.ozokuz.incore.features.assembly.content;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import io.github.ozokuz.incore.Config;
import io.github.ozokuz.incore.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AutoAssemblerT1BlockEntity extends KineticBlockEntity implements AutoAssemblerBlockEntity, MenuProvider, BaseAutoAssemblerBlock.AutoAssemblerSupport {
    private final AutoAssemblerSharedState state = new AutoAssemblerSharedState();

    public AutoAssemblerT1BlockEntity(BlockPos pos, BlockState state) {
        super(Registration.AUTO_ASSEMBLER_T1_BE.get(), pos, state);
        this.state.setOnDirty(this::setChanged);
    }

    public static void tick(Level level, BlockPos pos, BlockState blockState, AutoAssemblerT1BlockEntity blockEntity) {
        blockEntity.tick();
        if (!level.isClientSide) {
            blockEntity.refreshStressInNetwork();
            AutoAssemblerMachineLogic.tick(
                    level,
                    blockEntity,
                    blockEntity.state,
                    1,
                    !blockEntity.isOverStressed() && Math.abs(blockEntity.getSpeed()) >= Config.AUTO_ASSEMBLER_T1_MIN_RPM.get(),
                    () -> {}
            );
        }
    }

    protected void refreshStressInNetwork() {
        if (hasNetwork()) {
            getOrCreateNetwork().updateStressFor(this, calculateStressApplied());
        }
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public ItemStackHandler itemHandler() {
        return state.items();
    }

    @Override
    public @Nullable IItemHandler automationView(@Nullable Direction side) {
        Direction front = getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        return state.automationView(front, side);
    }

    @Override
    public boolean canAccess(Player player) {
        return level != null
                && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void setSelectedRecipeId(@Nullable net.minecraft.resources.ResourceLocation recipeId, @Nullable Player player) {
        state.setSelectedRecipeId(recipeId);
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            state.setTeamId(io.github.ozokuz.incore.features.researchv2.team.ResearchTeamResolver.resolveTeamId(serverPlayer));
        }
    }

    @Override
    public @Nullable net.minecraft.resources.ResourceLocation selectedRecipeId() {
        return state.selectedRecipeId();
    }

    @Override
    public int machineTier() {
        return 1;
    }

    @Override
    public net.minecraft.world.inventory.ContainerData data() {
        return state.data();
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("block.incore.auto_assembler_t1");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, @NotNull Inventory inventory, @NotNull Player player) {
        return new AutoAssemblerMenu(containerId, inventory, this);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        state.save(tag, registries);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        state.load(tag, registries);
        state.setOnDirty(this::setChanged);
    }

    @Override
    public void dropContents() {
        if (level != null) {
            state.dropContents(level, worldPosition);
        }
    }
}
