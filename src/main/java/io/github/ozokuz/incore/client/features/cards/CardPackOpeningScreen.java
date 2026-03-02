package io.github.ozokuz.incore.client.features.cards;

import io.github.ozokuz.incore.features.cards.CardPackService;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class CardPackOpeningScreen extends Screen {
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
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int left = this.width / 2 - 170;
        int right = this.width / 2 + 170;
        int top = 24;
        int bottom = this.height - 40;

        guiGraphics.fill(left, top, right, bottom, 0xCC0A131B);
        guiGraphics.fill(left, top, right, top + 1, 0xFF3FC6D5);
        guiGraphics.fill(left, bottom - 1, right, bottom, 0xFF3FC6D5);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, top + 8, 0xF2F9FF);
        guiGraphics.drawString(this.font, "Revealed " + revealedCards + "/" + data.pulls().size(), left + 10, top + 8, 0xA9DBEC, false);

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
            int borderColor = revealed ? 0xFF4FCDE3 : 0xFF444E63;
            int fillColor = revealed ? 0xAA111D2C : 0xAA101018;
            guiGraphics.fill(x, y, x + cardWidth, y + cardHeight, fillColor);
            guiGraphics.fill(x, y, x + cardWidth, y + 1, borderColor);
            guiGraphics.fill(x, y + cardHeight - 1, x + cardWidth, y + cardHeight, borderColor);
            guiGraphics.fill(x, y, x + 1, y + cardHeight, borderColor);
            guiGraphics.fill(x + cardWidth - 1, y, x + cardWidth, y + cardHeight, borderColor);

            if (!revealed) {
                guiGraphics.drawCenteredString(this.font, Component.literal("?"), x + cardWidth / 2, y + 24, 0x8B93A6);
                continue;
            }

            CardPackService.PackRevealEntry entry = data.pulls().get(i);
            int rarityColor = switch (Math.clamp(entry.rarity(), 1, 6)) {
                case 6 -> 0xFFCE5E;
                case 5 -> 0xFF8E4B;
                case 4 -> 0xBC8EFF;
                case 3 -> 0x64D8FF;
                default -> 0xD0D9E8;
            };
            guiGraphics.drawCenteredString(this.font, Component.literal(entry.rarity() + "★"), x + cardWidth / 2, y + 6, rarityColor);
            guiGraphics.drawCenteredString(this.font, Component.literal(entry.moduleType()), x + cardWidth / 2, y + 16, 0x8FC3E0);
            String name = entry.cardName();
            if (name.length() > 14) {
                name = name.substring(0, 14);
            }
            guiGraphics.drawCenteredString(this.font, Component.literal(name), x + cardWidth / 2, y + 29, entry.foil() ? 0x8AEEFF : 0xE0E7F2);
            if (entry.foil()) {
                guiGraphics.drawCenteredString(this.font, Component.literal("FOIL"), x + cardWidth / 2, y + 42, 0x79F7FF);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
