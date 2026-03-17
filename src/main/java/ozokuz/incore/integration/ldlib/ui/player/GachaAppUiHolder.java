package ozokuz.incore.integration.ldlib.ui.player;

import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.texture.RectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import ozokuz.incore.features.gacha.GachaService;
import ozokuz.incore.integration.ldlib.ui.INCoreLdLibUiScaffold;

public final class GachaAppUiHolder implements PlayerUIMenuType.PlayerUIHolder {
    @Override
    public ModularUI createUI(Player player) {
        return INCoreLdLibUiScaffold.build(player, createView(player));
    }

    static UIElement createView(Player player) {
        GachaAppUiElement view = new GachaAppUiElement();
        view.bind(DataBindingBuilder.stringS2C(() -> player instanceof ServerPlayer serverPlayer ? GachaService.buildScreenJson(serverPlayer) : "").build());
        return new UIElement()
                .layout(layout -> {
                    layout.widthPercent(100);
                    layout.heightPercent(100);
                    layout.paddingVertical(12);
                    layout.paddingHorizontal(16);
                })
                .style(style -> style.backgroundTexture(RectTexture.of(0x66000000)))
                .addChildren(view);
    }
}
