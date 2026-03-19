package ozokuz.incore.integration.ldlib.ui.player;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.IBindable;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.texture.RectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import java.util.function.LongSupplier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import ozokuz.incore.client.ui.UIScreenTheme;
import ozokuz.incore.features.gacha.GachaService;

final class GachaViewSupport {
    private GachaViewSupport() {
    }

    static <T extends UIElement & IBindable<String>> T bindScreenData(Player player, T view) {
        view.bind(DataBindingBuilder.stringS2C(() -> player instanceof ServerPlayer serverPlayer ? GachaService.buildScreenJson(serverPlayer) : "").build());
        return view;
    }

    static UIElement overlay(UIElement child) {
        return new UIElement()
                .layout(layout -> {
                    layout.widthPercent(100);
                    layout.heightPercent(100);
                    layout.paddingVertical(12);
                    layout.paddingHorizontal(16);
                })
                .style(style -> style.backgroundTexture(RectTexture.of(0x66000000)))
                .addChild(child);
    }

    static UIElement panel(UIElement child) {
        return new UIElement()
                .layout(layout -> {
                    layout.widthPercent(100);
                    layout.heightPercent(100);
                })
                .style(style -> style.backgroundTexture(RectTexture.of(GachaAppUiSupport.BANNER_PANEL_FILL)))
                .addChild(child);
    }

    static Label lineLabel(Component text, int color) {
        Label label = TaskOverviewUiSupport.lineLabel(text, color);
        label.textStyle(style -> style
                .adaptiveWidth(false)
                .textWrap(TextWrap.HIDE)
        );
        return label;
    }

    static Label titleLabel(Component text) {
        Label label = lineLabel(text, UIScreenTheme.OtherContent.GACHA_TITLE_TEXT);
        label.layout(layout -> layout.widthPercent(100));
        label.textStyle(style -> style
                .adaptiveWidth(false)
                .textWrap(TextWrap.HIDE)
                .textAlignHorizontal(Horizontal.CENTER)
        );
        return label;
    }

    static Label centeredLabel(Component text, int color) {
        Label label = lineLabel(text, color);
        label.layout(layout -> layout.widthPercent(100));
        label.textStyle(style -> style
                .adaptiveWidth(false)
                .textWrap(TextWrap.HIDE)
                .textAlignHorizontal(Horizontal.CENTER)
        );
        return label;
    }

    static Label timerLabel(GachaService.BannerView banner, LongSupplier syncedAtMsSupplier, int color) {
        Label label = lineLabel(Component.empty(), color);
        Runnable update = () -> label.setText(Component.literal(GachaAppUiSupport.renderRemainingLabel(banner, syncedAtMsSupplier.getAsLong())));
        update.run();
        label.addEventListener(UIEvents.TICK, event -> update.run());
        return label;
    }

    static Button footerButton(Component text, int width, boolean active) {
        return TaskOverviewUiSupport.createButton(text, width, active);
    }

    static Button rowButton(Component text, int height, int fillColor) {
        Button button = new Button().setText(text);
        button.layout(layout -> {
            layout.widthPercent(100);
            layout.height(height);
            layout.alignItems(AlignItems.CENTER);
            layout.justifyContent(AlignContent.FLEX_START);
            layout.gapAll(6);
            layout.paddingHorizontal(6);
        });
        button.text.getLayout().flex(1);
        button.text.getLayout().heightPercent(100);
        button.textStyle(style -> style
                .adaptiveWidth(false)
                .textWrap(TextWrap.HIDE)
                .textAlignHorizontal(Horizontal.LEFT)
        );
        button.buttonStyle(style -> style
                .baseTexture(RectTexture.of(fillColor))
                .hoverTexture(RectTexture.of(fillColor))
                .pressedTexture(RectTexture.of(fillColor))
        );
        return button;
    }

    static UIElement icon(ItemStack stack, int size) {
        return new UIElement()
                .layout(layout -> {
                    layout.width(size);
                    layout.height(size);
                })
                .style(style -> style.backgroundTexture(new ItemStackTexture(stack)))
                .setAllowHitTest(false);
    }

    static ScrollerView scroller() {
        ScrollerView scroller = new ScrollerView()
                .scrollerStyle(style -> style
                        .mode(ScrollerMode.VERTICAL)
                        .horizontalScrollDisplay(ScrollDisplay.NEVER)
                        .verticalScrollDisplay(ScrollDisplay.AUTO)
                );
        scroller.layout(layout -> {
            layout.flex(1);
            layout.widthPercent(100);
            layout.minHeight(0);
        });
        scroller.viewPort.style(style -> style.backgroundTexture(RectTexture.of(0x00000000)));
        scroller.viewContainer.layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(4);
        });
        scroller.verticalScroller.layout(layout -> layout.width(6));
        scroller.verticalScroller.headButton.setDisplay(false);
        scroller.verticalScroller.tailButton.setDisplay(false);
        scroller.horizontalScroller.headButton.setDisplay(false);
        scroller.horizontalScroller.tailButton.setDisplay(false);
        scroller.verticalScroller.scrollContainer.style(style -> style.backgroundTexture(TaskOverviewUiSupport.SCROLL_TRACK_TEXTURE));
        scroller.verticalScroller.scrollBar.buttonStyle(style -> style
                .baseTexture(TaskOverviewUiSupport.SCROLL_THUMB_IDLE_TEXTURE)
                .hoverTexture(TaskOverviewUiSupport.SCROLL_THUMB_HOVER_TEXTURE)
                .pressedTexture(TaskOverviewUiSupport.SCROLL_THUMB_PRESSED_TEXTURE)
        );
        return scroller;
    }
}
