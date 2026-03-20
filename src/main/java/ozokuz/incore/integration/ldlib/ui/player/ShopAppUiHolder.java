package ozokuz.incore.integration.ldlib.ui.player;

import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import ozokuz.incore.features.shop.ShopService;
import ozokuz.incore.integration.ldlib.ui.INCoreLdLibUiScaffold;

public final class ShopAppUiHolder implements PlayerUIMenuType.PlayerUIHolder {
    @Override
    public ModularUI createUI(Player player) {
        return INCoreLdLibUiScaffold.build(player, createView(player));
    }

    static ShopAppUiElement createView(Player player) {
        ShopAppUiElement view = new ShopAppUiElement();
        view.bind(DataBindingBuilder.stringS2C(() -> player instanceof ServerPlayer serverPlayer ? ShopService.buildScreenJson(serverPlayer) : "").build());
        return view;
    }
}
