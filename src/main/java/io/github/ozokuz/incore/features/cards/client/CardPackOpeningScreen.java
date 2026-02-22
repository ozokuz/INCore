package io.github.ozokuz.incore.features.cards.client;

import io.github.ozokuz.incore.features.cards.CardPackService;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class CardPackOpeningScreen extends Screen {
    private final CardPackService.PackRevealScreenData data;

    public CardPackOpeningScreen(CardPackService.PackRevealScreenData data) {
        super(Component.translatable("screen.incore.cards.pack_open.title", data.boosterName()));
        this.data = data;
    }

    @Override
    protected void init() {
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(this.width / 2 - 40, this.height - 28, 80, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int left = this.width / 2 - 140;
        int right = this.width / 2 + 140;
        int top = 24;
        int bottom = this.height - 40;

        guiGraphics.fill(left, top, right, bottom, 0xCC0A131B);
        guiGraphics.fill(left, top, right, top + 1, 0xFF3FC6D5);
        guiGraphics.fill(left, bottom - 1, right, bottom, 0xFF3FC6D5);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, top + 8, 0xF2F9FF);

        int y = top + 26;
        for (CardPackService.PackRevealEntry entry : data.pulls()) {
            int color = entry.foil() ? 0x74DFFF : 0xE4E4E4;
            String row = entry.rarity() + "★ " + entry.cardName() + " [" + entry.moduleType() + "]" + (entry.foil() ? " ✨" : "");
            guiGraphics.drawString(this.font, row, left + 10, y, color, false);
            y += 11;
            if (y > bottom - 20) {
                break;
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
