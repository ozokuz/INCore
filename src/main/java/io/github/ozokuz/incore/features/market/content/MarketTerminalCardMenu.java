package io.github.ozokuz.incore.features.market.content;

import net.minecraft.world.entity.player.Inventory;

public class MarketTerminalCardMenu extends AbstractMarketTerminalCardMenu<MarketTerminalBlockEntity> {
    public MarketTerminalCardMenu(int containerId, Inventory playerInventory, MarketTerminalBlockEntity blockEntity) {
        super(io.github.ozokuz.incore.Registration.MARKET_TERMINAL_CARD_MENU.get(), containerId, playerInventory, blockEntity);
    }
}
