package ozokuz.incore.integration.ldlib.ui.cards;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.texture.RectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import ozokuz.incore.client.ui.UIScreenTheme;
import ozokuz.incore.features.cards.CardPackService;
import ozokuz.incore.integration.ldlib.ui.INCoreLdLibStyles;

public final class CardPackOpeningLdLibScreen extends ModularUIScreen {
    private final State state;
    private final Label subtitleLabel;
    private final Button doneButton;
    private final Button skipButton;
    private final List<CardTile> cards;

    public CardPackOpeningLdLibScreen(CardPackService.PackRevealScreenData data) {
        this(new State(data));
    }

    private CardPackOpeningLdLibScreen(State state) {
        super(createUi(state), Component.translatable("screen.incore.cards.pack_open.title", state.data.boosterName()));
        this.state = state;
        this.subtitleLabel = state.subtitleLabel;
        this.doneButton = state.doneButton;
        this.skipButton = state.skipButton;
        this.cards = state.cards;
        refresh();
    }

    @Override
    public void tick() {
        super.tick();
        state.tick();
        refresh();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void refresh() {
        subtitleLabel.setText(Component.translatable("ldlib.cardpack.revealed", state.revealedCards, state.data.pulls().size()));
        doneButton.setActive(state.canDone());
        skipButton.setActive(state.canSkip());
        for (int i = 0; i < cards.size(); i++) {
            cards.get(i).refresh(i < state.revealedCards);
        }
    }

    private static ModularUI createUi(State state) {
        UIElement root = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
            layout.justifyContent(AlignContent.CENTER);
            layout.alignItems(AlignItems.CENTER);
        }).style(style -> style.backgroundTexture(RectTexture.of(0x66000000)));

        UIElement modal = new UIElement().layout(layout -> {
            layout.width(392);
            layout.height(236);
            layout.paddingAll(12);
            layout.gapAll(8);
            layout.flexDirection(FlexDirection.COLUMN);
        }).style(style -> style.backgroundTexture(RectTexture.of(UIScreenTheme.OtherContent.PACK_MODAL_FILL)));
        root.addChild(modal);

        UIElement header = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(2);
        });
        Label title = label(Component.translatable("screen.incore.cards.pack_open.title", state.data.boosterName()), UIScreenTheme.OtherContent.PACK_TITLE_TEXT, true);
        title.textStyle(style -> style.textAlignHorizontal(Horizontal.CENTER));
        state.subtitleLabel = label(Component.empty(), UIScreenTheme.OtherContent.PACK_SUBTITLE_TEXT, true);
        modal.addChildren(header.addChildren(title, state.subtitleLabel), divider());

        UIElement grid = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(8);
            layout.flex(1);
        });
        modal.addChild(grid);

        int count = state.data.pulls().size();
        int columns = Math.max(1, Math.min(5, count == 0 ? 1 : count));
        UIElement currentRow = row();
        grid.addChild(currentRow);
        for (int i = 0; i < count; i++) {
            if (i > 0 && i % columns == 0) {
                currentRow = row();
                grid.addChild(currentRow);
            }
            CardTile tile = new CardTile(state.data.pulls().get(i));
            state.cards.add(tile);
            currentRow.addChild(tile.root);
        }

        UIElement footer = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.ROW);
            layout.gapAll(8);
            layout.justifyContent(AlignContent.CENTER);
        });

        state.skipButton = actionButton(Component.translatable("ldlib.button.skip"), 74);
        state.skipButton.setOnClick(event -> state.revealAll());
        state.doneButton = actionButton(Component.translatable("gui.done"), 80);
        state.doneButton.setOnClick(event -> Minecraft.getInstance().setScreen(null));
        footer.addChildren(state.skipButton, state.doneButton);
        modal.addChild(footer);

        return ModularUI.of(UI.of(
                root,
                StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.MC),
                StylesheetManager.INSTANCE.getStylesheetSafe(INCoreLdLibStyles.BASE)
        ));
    }

    private static UIElement row() {
        return new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.ROW);
            layout.justifyContent(AlignContent.CENTER);
            layout.gapAll(8);
        });
    }

    private static UIElement divider() {
        return new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.height(1);
        }).style(style -> style.backgroundTexture(RectTexture.of(UIScreenTheme.OtherContent.PACK_MODAL_BORDER)));
    }

    private static Button actionButton(Component text, int width) {
        Button button = new Button().setText(text);
        button.layout(layout -> {
            layout.width(width);
            layout.height(20);
            layout.alignItems(AlignItems.CENTER);
            layout.justifyContent(AlignContent.CENTER);
        });
        button.text.getLayout().flex(1);
        button.text.getLayout().heightPercent(100);
        button.textStyle(style -> style
                .textColor(0xFFFFFFFF)
                .adaptiveWidth(false)
                .textWrap(TextWrap.HIDE)
                .textAlignHorizontal(Horizontal.CENTER));
        button.buttonStyle(style -> style
                .baseTexture(RectTexture.of(0xFF35516B))
                .hoverTexture(RectTexture.of(0xFF436782))
                .pressedTexture(RectTexture.of(0xFF263C50))
        );
        return button;
    }

    private static Label label(Component text, int color, boolean fullWidth) {
        Label label = new Label();
        label.setText(text);
        if (fullWidth) {
            label.layout(layout -> layout.widthPercent(100));
        }
        label.textStyle(style -> style
                .textColor(color)
                .adaptiveWidth(false)
                .textWrap(TextWrap.HIDE));
        return label;
    }

    private static final class State {
        private final CardPackService.PackRevealScreenData data;
        private int revealedCards;
        private int revealTickCounter;
        private Label subtitleLabel;
        private Button doneButton;
        private Button skipButton;
        private final List<CardTile> cards = new ArrayList<>();

        private State(CardPackService.PackRevealScreenData data) {
            this.data = data;
        }

        private void tick() {
            if (revealedCards >= data.pulls().size()) {
                return;
            }
            revealTickCounter++;
            if (revealTickCounter >= 6) {
                revealTickCounter = 0;
                revealedCards = Math.min(data.pulls().size(), revealedCards + 1);
            }
        }

        private void revealAll() {
            revealedCards = data.pulls().size();
            revealTickCounter = 0;
        }

        private boolean canSkip() {
            return revealedCards < data.pulls().size();
        }

        private boolean canDone() {
            return revealedCards >= data.pulls().size();
        }
    }

    private static final class CardTile {
        private final UIElement root;
        private final UIElement hiddenFace;
        private final UIElement revealedFace;
        private final Label rarityLabel;
        private final Label typeLabel;
        private final Label nameLabel;
        private final Label foilLabel;
        private final CardPackService.PackRevealEntry entry;

        private CardTile(CardPackService.PackRevealEntry entry) {
            this.entry = entry;
            this.root = new UIElement().layout(layout -> {
                layout.width(62);
                layout.height(60);
                layout.paddingAll(4);
                layout.flexDirection(FlexDirection.COLUMN);
                layout.alignItems(AlignItems.CENTER);
                layout.justifyContent(AlignContent.FLEX_START);
            });

            this.hiddenFace = new UIElement().layout(layout -> {
                layout.widthPercent(100);
                layout.heightPercent(100);
                layout.justifyContent(AlignContent.CENTER);
                layout.alignItems(AlignItems.CENTER);
            });
            hiddenFace.addChild(label(Component.literal("?"), UIScreenTheme.OtherContent.PACK_UNKNOWN_TEXT, true)
                    .textStyle(style -> style.textAlignHorizontal(Horizontal.CENTER)));

            this.revealedFace = new UIElement().layout(layout -> {
                layout.widthPercent(100);
                layout.heightPercent(100);
                layout.flexDirection(FlexDirection.COLUMN);
                layout.alignItems(AlignItems.CENTER);
                layout.gapAll(2);
            });
            this.rarityLabel = label(Component.empty(), UIScreenTheme.OtherContent.PACK_RARITY_DEFAULT_TEXT, true);
            this.rarityLabel.textStyle(style -> style.textAlignHorizontal(Horizontal.CENTER));
            this.typeLabel = label(Component.empty(), UIScreenTheme.OtherContent.PACK_TYPE_TEXT, true);
            this.typeLabel.textStyle(style -> style.textAlignHorizontal(Horizontal.CENTER));
            this.nameLabel = label(Component.empty(), UIScreenTheme.OtherContent.PACK_NAME_TEXT, true);
            this.nameLabel.textStyle(style -> style.textAlignHorizontal(Horizontal.CENTER));
            this.foilLabel = label(Component.translatable("ldlib.label.foil"), UIScreenTheme.OtherContent.PACK_FOIL_LABEL_TEXT, true);
            this.foilLabel.textStyle(style -> style.textAlignHorizontal(Horizontal.CENTER));
            revealedFace.addChildren(rarityLabel, typeLabel, nameLabel, foilLabel);

            root.addChildren(hiddenFace, revealedFace);
        }

        private void refresh(boolean revealed) {
            root.style(style -> style.backgroundTexture(RectTexture.of(
                    revealed ? UIScreenTheme.OtherContent.PACK_CARD_FILL_REVEALED : UIScreenTheme.OtherContent.PACK_CARD_FILL_HIDDEN
            )));
            hiddenFace.setDisplay(!revealed);
            revealedFace.setDisplay(revealed);
            if (!revealed) {
                return;
            }
            rarityLabel.setText(Component.literal(entry.rarity() + "★"));
            rarityLabel.textStyle(style -> style.textColor(rarityColor(entry)));
            typeLabel.setText(entry.moduleType() != null ? Component.literal(entry.moduleType()) : Component.empty());
            nameLabel.setText(entry.cardName() != null ? Component.literal(truncate(entry.cardName(), 14)) : Component.empty());
            nameLabel.textStyle(style -> style.textColor(entry.foil()
                    ? UIScreenTheme.OtherContent.PACK_NAME_FOIL_TEXT
                    : UIScreenTheme.OtherContent.PACK_NAME_TEXT));
            foilLabel.setDisplay(entry.foil());
        }

        private static int rarityColor(CardPackService.PackRevealEntry entry) {
            return switch (Math.clamp(entry.rarity(), 1, 6)) {
                case 6 -> UIScreenTheme.OtherContent.PACK_RARITY_SIX_TEXT;
                case 5 -> UIScreenTheme.OtherContent.PACK_RARITY_FIVE_TEXT;
                case 4 -> UIScreenTheme.OtherContent.PACK_RARITY_FOUR_TEXT;
                case 3 -> UIScreenTheme.OtherContent.PACK_RARITY_THREE_TEXT;
                default -> UIScreenTheme.OtherContent.PACK_RARITY_DEFAULT_TEXT;
            };
        }

        private static String truncate(String name, int maxLength) {
            return name.length() > maxLength ? name.substring(0, maxLength) : name;
        }
    }
}