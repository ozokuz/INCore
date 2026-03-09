package io.github.ozokuz.incore.features.market.content;

import io.github.ozokuz.incore.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MarketTerminalBlockEntity extends AbstractMarketTerminalBlockEntity {

    public MarketTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.MARKET_TERMINAL_BE.get(), pos, state);
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory, @NotNull Player player) {
        return new MarketTerminalCardMenu(containerId, playerInventory, this);
    }
}
