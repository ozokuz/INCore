package ozokuz.incore.integration.ldlib.ui.player;

import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import net.minecraft.world.entity.player.Player;
import ozokuz.incore.integration.ldlib.ui.INCoreLdLibUiScaffold;

public final class GachaInfoUiHolder implements PlayerUIMenuType.PlayerUIHolder {
    @Override
    public ModularUI createUI(Player player) {
        return INCoreLdLibUiScaffold.build(player, createView(player));
    }

    static UIElement createView(Player player) {
        return GachaViewSupport.overlay(GachaViewSupport.bindScreenData(player, new GachaInfoScreenElement()));
    }
}
