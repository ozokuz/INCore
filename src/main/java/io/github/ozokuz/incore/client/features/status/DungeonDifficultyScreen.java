package io.github.ozokuz.incore.client.features.status;

import io.github.ozokuz.incore.client.ui.UIScreenTheme;
import io.github.ozokuz.incore.client.ui.render.ThemedUi;
import io.github.ozokuz.incore.features.roguelike.DungeonDeathDifficulty;
import io.github.ozokuz.incore.features.status.network.PlayerStatusDungeonDifficultyClientCache;
import io.github.ozokuz.incore.features.status.network.PlayerStatusNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public class DungeonDifficultyScreen extends Screen {
    private static final UIScreenTheme THEME = UIScreenTheme.INFO;
    private static final int WINDOW_WIDTH = 240;
    private static final int WINDOW_HEIGHT = 190;
    private static final int WINDOW_PADDING = 12;
    private static final int BUTTON_WIDTH = 180;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 6;
    private static final int TEXT_GAP = 8;
    private final Screen parent;

    public DungeonDifficultyScreen(Screen parent) {
        super(Component.translatable("screen.incore.dungeon_difficulty.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        Layout layout = layout();

        addRenderableWidget(Button.builder(Component.translatable("screen.incore.dungeon_difficulty.option.softcore"), button -> select(DungeonDeathDifficulty.SOFTCORE))
                .bounds(layout.buttonX(), layout.firstButtonY(), BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("screen.incore.dungeon_difficulty.option.mediumcore"), button -> select(DungeonDeathDifficulty.MEDIUMCORE))
                .bounds(layout.buttonX(), layout.firstButtonY() + BUTTON_HEIGHT + BUTTON_GAP, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("screen.incore.dungeon_difficulty.option.hardcore"), button -> select(DungeonDeathDifficulty.HARDCORE))
                .bounds(layout.buttonX(), layout.firstButtonY() + ((BUTTON_HEIGHT + BUTTON_GAP) * 2), BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> this.minecraft.setScreen(parent))
                .bounds(layout.buttonX(), layout.firstButtonY() + ((BUTTON_HEIGHT + BUTTON_GAP) * 3) + TEXT_GAP, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());

        PlayerStatusNetworking.requestDungeonDifficultySync();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        ThemedUi ui = new ThemedUi(guiGraphics, this.font, THEME.theme());
        ui.drawBackdrop(this.width, this.height);

        Layout layout = layout();
        ui.drawWindow(layout.left(), layout.top(), WINDOW_WIDTH, WINDOW_HEIGHT);

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawString(this.font, this.title, layout.left() + WINDOW_PADDING, layout.top() + 10, UIScreenTheme.Info.TITLE_TEXT, false);
        PlayerStatusDungeonDifficultyClientCache.Snapshot snapshot = PlayerStatusDungeonDifficultyClientCache.snapshot();
        Component current = snapshot.loaded()
                ? Component.translatable("screen.incore.dungeon_difficulty.current", labelFor(snapshot.difficulty()))
                : Component.translatable("screen.incore.dungeon_difficulty.loading");
        guiGraphics.drawWordWrap(this.font, current, layout.left() + WINDOW_PADDING, layout.currentY(), layout.textWidth(), UIScreenTheme.Info.PRIMARY_TEXT);
        guiGraphics.drawWordWrap(
                this.font,
                Component.translatable("screen.incore.dungeon_difficulty.description"),
                layout.left() + WINDOW_PADDING,
                layout.descriptionY(),
                layout.textWidth(),
                UIScreenTheme.Info.SECONDARY_TEXT
        );
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void select(DungeonDeathDifficulty difficulty) {
        PlayerStatusDungeonDifficultyClientCache.update(difficulty);
        PlayerStatusNetworking.setDungeonDifficulty(difficulty.name());
    }

    private static Component labelFor(DungeonDeathDifficulty difficulty) {
        return Component.translatable("screen.incore.dungeon_difficulty.label." + difficulty.name().toLowerCase(Locale.ROOT));
    }

    private Layout layout() {
        int left = (this.width - WINDOW_WIDTH) / 2;
        int top = Math.max(40, this.height / 2 - (WINDOW_HEIGHT / 2));
        int textWidth = WINDOW_WIDTH - (WINDOW_PADDING * 2);
        int currentY = top + 26;
        int currentHeight = this.font.wordWrapHeight(
                Component.translatable("screen.incore.dungeon_difficulty.current", labelFor(DungeonDeathDifficulty.HARDCORE)),
                textWidth
        );
        int descriptionY = currentY + currentHeight + 4;
        int descriptionHeight = this.font.wordWrapHeight(Component.translatable("screen.incore.dungeon_difficulty.description"), textWidth);
        int firstButtonY = descriptionY + descriptionHeight + TEXT_GAP;
        int buttonX = left + ((WINDOW_WIDTH - BUTTON_WIDTH) / 2);
        return new Layout(left, top, textWidth, currentY, descriptionY, firstButtonY, buttonX);
    }

    private record Layout(int left, int top, int textWidth, int currentY, int descriptionY, int firstButtonY, int buttonX) {
    }
}
