package ozokuz.incore.integration.ldlib.ui.player;

import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import ozokuz.incore.client.ui.UIScreenTheme;
import ozokuz.incore.features.status.network.PlayerStatusCurrencyClientCache;

public final class PlayerStatusCurrencyStripElement extends UIElement {
    private static final int GAP = 4;
    private static final int ICON_SIZE = 12;
    private String lastSignature = "";
    private int lastWidth = -1;

    public PlayerStatusCurrencyStripElement() {
        setAllowHitTest(false);
        layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
            layout.flexDirection(FlexDirection.ROW);
            layout.justifyContent(AlignContent.FLEX_END);
            layout.alignItems(AlignItems.CENTER);
            layout.gapAll(GAP);
        });
        internalSetup();
    }

    @Override
    public void screenTick() {
        rebuildIfNeeded();
        super.screenTick();
    }

    private void rebuildIfNeeded() {
        PlayerStatusCurrencyClientCache.Snapshot snapshot = PlayerStatusCurrencyClientCache.snapshot();
        int width = Math.max(0, Math.round(getSizeWidth()));
        String signature = snapshot.loaded() + "|" + snapshot.entries() + "|" + width;
        if (width == lastWidth && signature.equals(lastSignature)) {
            return;
        }
        lastWidth = width;
        lastSignature = signature;
        rebuild(snapshot, width);
    }

    private void rebuild(PlayerStatusCurrencyClientCache.Snapshot snapshot, int width) {
        clearAllChildren();

        if (snapshot.entries().isEmpty()) {
            addChild(infoLabel(snapshot.loaded()
                    ? Component.translatable("screen.incore.player_status.currencies_none")
                    : Component.translatable("screen.incore.player_status.currencies_loading"), UIScreenTheme.Info.STATUS_LINE_TEXT));
            return;
        }

        List<PlayerStatusCurrencyClientCache.CurrencyEntry> visible = new ArrayList<>(snapshot.entries());
        int hidden = 0;
        boolean showLabel = true;
        int availableWidth = Math.max(0, width - 2);

        while (totalWidth(visible, hidden, true) > availableWidth && !visible.isEmpty()) {
            visible.remove(0);
            hidden++;
        }
        if (totalWidth(visible, hidden, true) > availableWidth) {
            showLabel = false;
        }
        while (totalWidth(visible, hidden, showLabel) > availableWidth && !visible.isEmpty()) {
            visible.remove(0);
            hidden++;
        }
        if (totalWidth(visible, hidden, showLabel) > availableWidth) {
            showLabel = false;
        }

        if (hidden > 0) {
            addChild(infoLabel(
                    Component.translatable("screen.incore.player_status.currencies_overflow", hidden),
                    UIScreenTheme.Info.STATUS_OVERFLOW_TEXT
            ));
        }
        if (showLabel) {
            addChild(infoLabel(
                    Component.translatable("screen.incore.player_status.section_currencies"),
                    UIScreenTheme.Info.PRIMARY_TEXT
            ));
        }
        for (PlayerStatusCurrencyClientCache.CurrencyEntry entry : visible) {
            addChild(currencyEntry(entry));
        }
    }

    private static int totalWidth(List<PlayerStatusCurrencyClientCache.CurrencyEntry> visible, int hidden, boolean showLabel) {
        Font font = Minecraft.getInstance().font;
        int width = 0;
        int parts = 0;
        if (hidden > 0) {
            width += font.width(Component.translatable("screen.incore.player_status.currencies_overflow", hidden));
            parts++;
        }
        if (showLabel) {
            width += font.width(Component.translatable("screen.incore.player_status.section_currencies"));
            parts++;
        }
        for (PlayerStatusCurrencyClientCache.CurrencyEntry entry : visible) {
            width += entryWidth(font, entry);
            parts++;
        }
        if (parts > 1) {
            width += GAP * (parts - 1);
        }
        return width;
    }

    private static int entryWidth(Font font, PlayerStatusCurrencyClientCache.CurrencyEntry entry) {
        return ICON_SIZE + 2 + font.width("x" + Math.max(0, entry.amount()));
    }

    private static UIElement currencyEntry(PlayerStatusCurrencyClientCache.CurrencyEntry entry) {
        UIElement element = new UIElement().layout(layout -> {
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.gapAll(2);
        });

        ItemStack stack = iconFromId(entry.iconItemId());
        if (!stack.isEmpty()) {
            element.addChild(new UIElement()
                    .layout(layout -> {
                        layout.width(ICON_SIZE);
                        layout.height(ICON_SIZE);
                    })
                    .style(style -> style.backgroundTexture(new ItemStackTexture(stack)))
                    .setAllowHitTest(false));
        }

        element.addChild(infoLabel(Component.literal("x" + Math.max(0, entry.amount())), UIScreenTheme.Info.STATUS_BALANCE_TEXT));
        return element;
    }

    private static Label infoLabel(Component text, int color) {
        Label label = new Label();
        label.setText(text);
        label.setAllowHitTest(false);
        label.textStyle(style -> style
                .fontSize(8)
                .adaptiveWidth(true)
                .textWrap(TextWrap.HIDE)
                .textColor(color));
        return label;
    }

    private static ItemStack iconFromId(String itemIdString) {
        if (itemIdString == null || itemIdString.isBlank()) {
            return ItemStack.EMPTY;
        }

        ResourceLocation itemId = ResourceLocation.tryParse(itemIdString);
        if (itemId == null) {
            return ItemStack.EMPTY;
        }

        Item item = BuiltInRegistries.ITEM.get(itemId);
        return item == Items.AIR ? ItemStack.EMPTY : item.getDefaultInstance();
    }
}
