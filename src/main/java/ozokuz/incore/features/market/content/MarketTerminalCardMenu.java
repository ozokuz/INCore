package ozokuz.incore.features.market.content;

import net.minecraft.world.entity.player.Inventory;
import ozokuz.incore.Registration;

public class MarketTerminalCardMenu extends AbstractMarketTerminalCardMenu<MarketTerminalBlockEntity> {
    public MarketTerminalCardMenu(int containerId, Inventory playerInventory, MarketTerminalBlockEntity blockEntity) {
        super(Registration.MARKET_TERMINAL_CARD_MENU.get(), containerId, playerInventory, blockEntity);
    }
}
