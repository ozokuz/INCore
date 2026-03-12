package io.github.ozokuz.incore.features.assembly.content;

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

public class AutoAssemblerT2Block extends BaseAutoAssemblerBlock {
    public static final MapCodec<AutoAssemblerT2Block> CODEC = simpleCodec(AutoAssemblerT2Block::new);

    public AutoAssemblerT2Block() {
        this(defaultProperties());
    }

    public AutoAssemblerT2Block(Properties properties) {
        super(properties);
    }

    @Override
    protected @NotNull MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new AutoAssemblerT2BlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        return createTickerHelper(type, Registration.AUTO_ASSEMBLER_T2_BE.get(), AutoAssemblerT2BlockEntity::tick);
    }
}
