package io.github.ozokuz.incore.client.features.cards;

import io.github.ozokuz.incore.client.ui.UIScreenTheme;
import io.github.ozokuz.incore.client.ui.render.ThemedUi;
import io.github.ozokuz.incore.features.cards.CardPackService;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class CardPackOpeningScreen extends Screen {
    private static final UIScreenTheme THEME = UIScreenTheme.OTHER_CONTENT;
    private final CardPackService.PackRevealScreenData data;
    private Button doneButton;
    private Button skipButton;
    private int revealedCards;
    private int revealTickCounter;

    public CardPackOpeningScreen(CardPackService.PackRevealScreenData data) {
        super(Component.translatable("screen.incore.cards.pack_open.title", data.boosterName()));
        this.data = data;
    }

    @Override
    protected void init() {
        this.doneButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(this.width / 2 - 40, this.height - 28, 80, 20)
                .build());
        this.skipButton = this.addRenderableWidget(Button.builder(Component.literal("Skip"), button -> {
                    revealedCards = data.pulls().size();
                    revealTickCounter = 0;
                    doneButton.active = true;
                    skipButton.active = false;
                })
                .bounds(this.width / 2 - 122, this.height - 28, 74, 20)
                .build());
        this.doneButton.active = data.pulls().isEmpty();
        this.skipButton.active = !data.pulls().isEmpty();
    }

    @Override
    public void tick() {
        super.tick();
        if (revealedCards >= data.pulls().size()) {
            doneButton.active = true;
            skipButton.active = false;
            return;
        }

        revealTickCounter++;
        if (revealTickCounter >= 6) {
            revealTickCounter = 0;
            revealedCards = Math.min(data.pulls().size(), revealedCards + 1);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        themed(guiGraphics).drawBackdrop(this.width, this.height);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int left = this.width / 2 - 170;
        int right = this.width / 2 + 170;
        int top = 24;
        int bottom = this.height - 40;

        guiGraphics.fill(left, top, right, bottom, UIScreenTheme.OtherContent.PACK_MODAL_FILL);
        guiGraphics.fill(left, top, right, top + 1, UIScreenTheme.OtherContent.PACK_MODAL_BORDER);
        guiGraphics.fill(left, bottom - 1, right, bottom, UIScreenTheme.OtherContent.PACK_MODAL_BORDER);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, top + 8, UIScreenTheme.OtherContent.PACK_TITLE_TEXT);
        guiGraphics.drawString(this.font, "Revealed " + revealedCards + "/" + data.pulls().size(), left + 10, top + 8, UIScreenTheme.OtherContent.PACK_SUBTITLE_TEXT, false);

        int count = data.pulls().size();
        int columns = Math.max(1, Math.min(5, count));
        int cardWidth = 62;
        int cardHeight = 60;
        int gap = 8;
        int totalWidth = columns * cardWidth + Math.max(0, columns - 1) * gap;
        int startX = this.width / 2 - totalWidth / 2;
        int startY = top + 28;

        for (int i = 0; i < count; i++) {
            int col = i % columns;
            int row = i / columns;
            int x = startX + col * (cardWidth + gap);
            int y = startY + row * (cardHeight + gap);

            boolean revealed = i < revealedCards;
            int borderColor = revealed ? UIScreenTheme.OtherContent.PACK_CARD_BORDER_REVEALED : UIScreenTheme.OtherContent.PACK_CARD_BORDER_HIDDEN;
            int fillColor = revealed ? UIScreenTheme.OtherContent.PACK_CARD_FILL_REVEALED : UIScreenTheme.OtherContent.PACK_CARD_FILL_HIDDEN;
            guiGraphics.fill(x, y, x + cardWidth, y + cardHeight, fillColor);
            guiGraphics.fill(x, y, x + cardWidth, y + 1, borderColor);
            guiGraphics.fill(x, y + cardHeight - 1, x + cardWidth, y + cardHeight, borderColor);
            guiGraphics.fill(x, y, x + 1, y + cardHeight, borderColor);
            guiGraphics.fill(x + cardWidth - 1, y, x + cardWidth, y + cardHeight, borderColor);

            if (!revealed) {
                guiGraphics.drawCenteredString(this.font, Component.literal("?"), x + cardWidth / 2, y + 24, UIScreenTheme.OtherContent.PACK_UNKNOWN_TEXT);
                continue;
            }

            CardPackService.PackRevealEntry entry = data.pulls().get(i);
            int rarityColor = switch (Math.clamp(entry.rarity(), 1, 6)) {
                case 6 -> UIScreenTheme.OtherContent.PACK_RARITY_SIX_TEXT;
                case 5 -> UIScreenTheme.OtherContent.PACK_RARITY_FIVE_TEXT;
                case 4 -> UIScreenTheme.OtherContent.PACK_RARITY_FOUR_TEXT;
                case 3 -> UIScreenTheme.OtherContent.PACK_RARITY_THREE_TEXT;
                default -> UIScreenTheme.OtherContent.PACK_RARITY_DEFAULT_TEXT;
            };
            guiGraphics.drawCenteredString(this.font, Component.literal(entry.rarity() + "★"), x + cardWidth / 2, y + 6, rarityColor);
            guiGraphics.drawCenteredString(this.font, Component.literal(entry.moduleType()), x + cardWidth / 2, y + 16, UIScreenTheme.OtherContent.PACK_TYPE_TEXT);
            String name = entry.cardName();
            if (name.length() > 14) {
                name = name.substring(0, 14);
            }
            guiGraphics.drawCenteredString(this.font, Component.literal(name), x + cardWidth / 2, y + 29, entry.foil() ? UIScreenTheme.OtherContent.PACK_NAME_FOIL_TEXT : UIScreenTheme.OtherContent.PACK_NAME_TEXT);
            if (entry.foil()) {
                guiGraphics.drawCenteredString(this.font, Component.literal("FOIL"), x + cardWidth / 2, y + 42, UIScreenTheme.OtherContent.PACK_FOIL_LABEL_TEXT);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private ThemedUi themed(GuiGraphics guiGraphics) {
        return new ThemedUi(guiGraphics, this.font, THEME.theme());
    }
}
