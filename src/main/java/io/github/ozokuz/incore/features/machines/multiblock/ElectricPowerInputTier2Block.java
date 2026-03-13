package io.github.ozokuz.incore.features.machines.multiblock;

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

public class ElectricPowerInputTier2Block extends AbstractMachinePowerInputBlock {
    public static final MapCodec<ElectricPowerInputTier2Block> CODEC = simpleCodec(ElectricPowerInputTier2Block::new);

    public ElectricPowerInputTier2Block() {
        this(Properties.of());
    }

    public ElectricPowerInputTier2Block(Properties properties) {
        super(MachinePowerFamily.ELECTRIC, 2, properties);
    }

    @Override
    protected @NotNull MapCodec<? extends AbstractMachinePowerInputBlock> codec() {
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
