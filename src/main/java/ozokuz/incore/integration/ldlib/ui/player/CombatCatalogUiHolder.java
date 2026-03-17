package ozokuz.incore.integration.ldlib.ui.player;

import com.google.gson.Gson;
import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.texture.RectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import ozokuz.incore.client.ui.UIScreenTheme;
import ozokuz.incore.features.arena.ArenaService;
import ozokuz.incore.integration.ldlib.ui.INCoreLdLibUiScaffold;

public final class CombatCatalogUiHolder implements PlayerUIMenuType.PlayerUIHolder {
    private static final Gson GSON = new Gson();

    @Override
    public ModularUI createUI(Player player) {
        return INCoreLdLibUiScaffold.build(player, createView(player));
    }

    static UIElement createView(Player player) {
        CombatCatalogUiElement content = new CombatCatalogUiElement();
        content.layout(layout -> layout.flex(1));
        content.bind(DataBindingBuilder.stringS2C(() -> screenDataJson(player)).build());

        var title = INCoreLdLibUiScaffold.titleLabel(Component.translatable("screen.incore.arena_catalog.title"));
        title.layout(layout -> layout.widthPercent(100));
        title.textStyle(style -> style
                .adaptiveWidth(false)
                .textAlignHorizontal(Horizontal.CENTER)
                .textColor(UIScreenTheme.OtherContent.CATALOG_TEXT_HEADING)
        );

        return new UIElement()
                .layout(layout -> {
                    layout.widthPercent(100);
                    layout.heightPercent(100);
                    layout.flexDirection(FlexDirection.COLUMN);
                    layout.paddingTop(10);
                    layout.paddingRight(16);
                    layout.paddingBottom(16);
                    layout.paddingLeft(16);
                    layout.gapAll(10);
                })
                .style(style -> style.backgroundTexture(RectTexture.of(0x66000000)))
                .addChildren(title, content);
    }

    private static String screenDataJson(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return "";
        }
        return GSON.toJson(ArenaService.buildScreenData(serverPlayer));
    }
}
