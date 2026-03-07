package io.github.ozokuz.incore.features.researchv2.station;

import io.github.ozokuz.incore.Registration;
import io.github.ozokuz.incore.features.researchv2.team.ResearchTeamResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractResearchControllerBlock extends BaseEntityBlock {
    private final int tier;

    protected AbstractResearchControllerBlock(int tier) {
        this(tier, Properties.of().mapColor(MapColor.METAL).strength(3.5F).sound(SoundType.NETHERITE_BLOCK));
    }

    protected AbstractResearchControllerBlock(int tier, Properties properties) {
        super(properties);
        this.tier = Math.max(1, tier);
    }

    public int tier() {
        return tier;
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new ResearchControllerBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable LivingEntity placer, @NotNull ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide || !(placer instanceof ServerPlayer serverPlayer)) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof ResearchControllerBlockEntity controller) {
            controller.setTeamId(ResearchTeamResolver.resolveTeamId(serverPlayer));
            controller.revalidateStructure();
        }
    }

    @Override
    protected void onPlace(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && !state.is(oldState.getBlock())) {
            ResearchStationMultiblockOrchestrator.onBlockChanged(level, pos);
        }
    }

    @Override
    protected void onRemove(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            ResearchStationMultiblockOrchestrator.onBlockChanged(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        return createTickerHelper(type, Registration.RESEARCH_CONTROLLER_BE.get(), ResearchControllerBlockEntity::tick);
    }
}
