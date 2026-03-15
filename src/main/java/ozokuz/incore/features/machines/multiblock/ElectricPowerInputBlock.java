package ozokuz.incore.features.machines.multiblock;

import com.mojang.serialization.MapCodec;
import ozokuz.incore.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ElectricPowerInputBlock extends AbstractMachinePowerInputBlock {
    public static final MapCodec<ElectricPowerInputBlock> CODEC = simpleCodec(ElectricPowerInputBlock::new);

    public ElectricPowerInputBlock() {
        this(Properties.of());
    }

    public ElectricPowerInputBlock(Properties properties) {
        super(MachinePowerFamily.ELECTRIC, 1, properties);
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
