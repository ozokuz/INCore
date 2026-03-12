package io.github.ozokuz.incore.features.assembly.content;

import io.github.ozokuz.incore.Config;
import io.github.ozokuz.incore.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class AutoAssemblerT3BlockEntity extends AutoAssemblerFeBlockEntity {
    public AutoAssemblerT3BlockEntity(BlockPos pos, BlockState state) {
        super(Registration.AUTO_ASSEMBLER_T3_BE.get(), pos, state, Config.AUTO_ASSEMBLER_T3_ENERGY_CAPACITY.get(), Config.AUTO_ASSEMBLER_T3_MAX_RECEIVE.get());
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AutoAssemblerT3BlockEntity blockEntity) {
        if (!level.isClientSide) {
            blockEntity.serverTick(level);
        }
    }

    @Override
    protected int machineTierInternal() {
        return 3;
    }

    @Override
    protected int fePerTick() {
        return Config.AUTO_ASSEMBLER_T3_FE_PER_TICK.get();
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("block.incore.auto_assembler_t3");
    }
}
