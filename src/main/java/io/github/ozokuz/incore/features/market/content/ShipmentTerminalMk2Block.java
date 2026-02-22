package io.github.ozokuz.incore.features.market.content;

import com.mojang.serialization.MapCodec;
import io.github.ozokuz.incore.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ShipmentTerminalMk2Block extends ShipmentTerminalBlock {
    public static final MapCodec<ShipmentTerminalMk2Block> CODEC = simpleCodec(ShipmentTerminalMk2Block::new);

    public ShipmentTerminalMk2Block() {
        this(BlockBehaviour.Properties.ofFullCopy(Registration.SHIPMENT_TERMINAL_BLOCK.get()));
    }

    public ShipmentTerminalMk2Block(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected @NotNull MapCodec<? extends ShipmentTerminalBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new ShipmentTerminalMk2BlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> blockEntityType) {
        if (blockEntityType != Registration.SHIPMENT_TERMINAL_MK2_BE.get()) {
            return null;
        }
        return (lvl, pos, blockState, blockEntity) -> ShipmentTerminalMk2BlockEntity.tick(lvl, pos, blockState, (ShipmentTerminalMk2BlockEntity) blockEntity);
    }
}
