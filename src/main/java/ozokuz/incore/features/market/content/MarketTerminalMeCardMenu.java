package ozokuz.incore.features.market.content;

import ozokuz.incore.Registration;
import net.minecraft.world.entity.player.Inventory;

public class MarketTerminalMeCardMenu extends AbstractMarketTerminalCardMenu<MarketTerminalMeBlockEntity> {
    public MarketTerminalMeCardMenu(int containerId, Inventory playerInventory, MarketTerminalMeBlockEntity blockEntity) {
        super(Registration.MARKET_TERMINAL_ME_CARD_MENU.get(), containerId, playerInventory, blockEntity);
    }
}
