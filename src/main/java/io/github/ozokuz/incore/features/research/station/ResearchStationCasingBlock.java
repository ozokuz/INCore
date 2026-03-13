package io.github.ozokuz.incore.features.research.station;

import com.mojang.serialization.MapCodec;
import io.github.ozokuz.incore.features.research.station.network.StationNetworkService;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.NotNull;

public class ResearchStationCasingBlock extends Block {
    public static final MapCodec<ResearchStationCasingBlock> CODEC = simpleCodec(ResearchStationCasingBlock::new);

    public ResearchStationCasingBlock() {
        this(Properties.of().mapColor(MapColor.METAL).strength(3.0F).sound(SoundType.METAL));
    }

    public ResearchStationCasingBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected @NotNull MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void onPlace(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && !state.is(oldState.getBlock())) {
            ResearchStationMultiblockOrchestrator.onBlockChanged(level, pos);
            StationNetworkService.onTopologyChanged(level);
        }
    }

    @Override
    protected void onRemove(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            ResearchStationMultiblockOrchestrator.onBlockChanged(level, pos);
            StationNetworkService.onTopologyChanged(level);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
