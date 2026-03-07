package io.github.ozokuz.incore.features.researchv2.station;

import com.mojang.serialization.MapCodec;
import io.github.ozokuz.incore.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ElectricPowerInputTier4Block extends AbstractResearchPowerInputBlock {
    public static final MapCodec<ElectricPowerInputTier4Block> CODEC = simpleCodec(ElectricPowerInputTier4Block::new);

    public ElectricPowerInputTier4Block() {
        this(Properties.of());
    }

    public ElectricPowerInputTier4Block(Properties properties) {
        super(ResearchPowerFamily.ELECTRIC, 4, properties);
    }

    @Override
    protected @NotNull MapCodec<? extends AbstractResearchPowerInputBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new ElectricPowerInputBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        return createTickerHelper(type, Registration.ELECTRIC_POWER_INPUT_BE.get(), ElectricPowerInputBlockEntity::tick);
    }
}
