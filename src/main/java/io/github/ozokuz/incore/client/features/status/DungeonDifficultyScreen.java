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

public class DungeonDifficultyScreen extends Screen {
    private static final UIScreenTheme THEME = UIScreenTheme.INFO;
    private final Screen parent;

    public DungeonDifficultyScreen(Screen parent) {
        super(Component.translatable("screen.incore.dungeon_difficulty.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int top = Math.max(40, this.height / 2 - 70);
        int buttonWidth = 180;
        int buttonHeight = 20;

        addRenderableWidget(Button.builder(Component.translatable("screen.incore.dungeon_difficulty.option.softcore"), button -> select(DungeonDeathDifficulty.SOFTCORE))
                .bounds(centerX - buttonWidth / 2, top + 26, buttonWidth, buttonHeight)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("screen.incore.dungeon_difficulty.option.mediumcore"), button -> select(DungeonDeathDifficulty.MEDIUMCORE))
                .bounds(centerX - buttonWidth / 2, top + 52, buttonWidth, buttonHeight)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("screen.incore.dungeon_difficulty.option.hardcore"), button -> select(DungeonDeathDifficulty.HARDCORE))
                .bounds(centerX - buttonWidth / 2, top + 78, buttonWidth, buttonHeight)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> this.minecraft.setScreen(parent))
                .bounds(centerX - buttonWidth / 2, top + 116, buttonWidth, buttonHeight)
                .build());

        PlayerStatusNetworking.requestDungeonDifficultySync();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        ThemedUi ui = new ThemedUi(guiGraphics, this.font, THEME.theme());
        ui.drawBackdrop(this.width, this.height);

        int windowWidth = 240;
        int windowHeight = 170;
        int left = (this.width - windowWidth) / 2;
        int top = (this.height - windowHeight) / 2;
        ui.drawWindow(left, top, windowWidth, windowHeight);

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawString(this.font, this.title, left + 12, top + 10, UIScreenTheme.Info.TITLE_TEXT, false);
        PlayerStatusDungeonDifficultyClientCache.Snapshot snapshot = PlayerStatusDungeonDifficultyClientCache.snapshot();
        Component current = snapshot.loaded()
                ? Component.translatable("screen.incore.dungeon_difficulty.current", labelFor(snapshot.difficulty()))
                : Component.translatable("screen.incore.dungeon_difficulty.loading");
        guiGraphics.drawWordWrap(this.font, current, left + 12, top + 26, windowWidth - 24, UIScreenTheme.Info.PRIMARY_TEXT);
        guiGraphics.drawWordWrap(
                this.font,
                Component.translatable("screen.incore.dungeon_difficulty.description"),
                left + 12,
                top + 44,
                windowWidth - 24,
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
        return Component.translatable("screen.incore.dungeon_difficulty.label." + difficulty.name().toLowerCase());
    }
}
