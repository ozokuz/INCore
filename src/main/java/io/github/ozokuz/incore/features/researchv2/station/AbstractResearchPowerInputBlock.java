package io.github.ozokuz.incore.features.researchv2.station;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractResearchPowerInputBlock extends BaseEntityBlock implements ResearchPowerInputBlockProvider {
    private final ResearchPowerFamily family;
    private final int powerTier;

    protected AbstractResearchPowerInputBlock(ResearchPowerFamily family) {
        this(family, 1, Properties.of().mapColor(MapColor.METAL).strength(3.0F).sound(SoundType.METAL));
    }

    protected AbstractResearchPowerInputBlock(ResearchPowerFamily family, int powerTier, Properties properties) {
        super(properties);
        this.family = family;
        this.powerTier = Math.max(1, powerTier);
    }

    @Override
    public final ResearchPowerFamily family() {
        return family;
    }

    @Override
    public final int powerTier() {
        return powerTier;
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
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
}
