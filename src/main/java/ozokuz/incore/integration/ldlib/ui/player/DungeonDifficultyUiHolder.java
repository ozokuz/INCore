package ozokuz.incore.integration.ldlib.ui.player;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import ozokuz.incore.features.roguelike.DungeonDeathDifficulty;
import ozokuz.incore.features.roguelike.state.RoguelikeSavedData;
import ozokuz.incore.integration.ldlib.ui.INCoreLdLibUiScaffold;
import ozokuz.incore.integration.ldlib.ui.INCorePlayerUiNavigator;

public final class DungeonDifficultyUiHolder implements com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType.PlayerUIHolder {
    @Override
    public ModularUI createUI(Player player) {
        return INCoreLdLibUiScaffold.build(player, createView(player));
    }

    static com.lowdragmc.lowdraglib2.gui.ui.UIElement createView(Player player) {
        var window = INCoreLdLibUiScaffold.createWindow(
                Component.translatable("screen.incore.dungeon_difficulty.title"),
                330,
                210
        );

        Label currentLabel = INCoreLdLibUiScaffold.wrappedLabel(
                Component.translatable("screen.incore.dungeon_difficulty.loading")
        );
        currentLabel.bind(DataBindingBuilder.componentS2C(() -> currentDifficultyLine(player)).build());

        Label descriptionLabel = INCoreLdLibUiScaffold.wrappedLabel(
                Component.translatable("screen.incore.dungeon_difficulty.description")
        );

        Button softcoreButton = choiceButton(player, DungeonDeathDifficulty.SOFTCORE);
        Button mediumcoreButton = choiceButton(player, DungeonDeathDifficulty.MEDIUMCORE);
        Button hardcoreButton = choiceButton(player, DungeonDeathDifficulty.HARDCORE);

        Button backButton = INCoreLdLibUiScaffold.actionButton(Component.translatable("gui.back"));
        backButton.setOnServerClick(event -> {
            if (player instanceof ServerPlayer serverPlayer) {
                INCorePlayerUiNavigator.goBack(serverPlayer);
            }
        });

        window.body().addChildren(currentLabel, descriptionLabel, softcoreButton, mediumcoreButton, hardcoreButton, backButton);
        return window.root();
    }

    private static Button choiceButton(Player player, DungeonDeathDifficulty difficulty) {
        Button button = INCoreLdLibUiScaffold.actionButton(
                Component.translatable("screen.incore.dungeon_difficulty.option." + difficulty.name().toLowerCase(Locale.ROOT))
        );
        button.setOnServerClick(event -> {
            if (player instanceof ServerPlayer serverPlayer && serverPlayer.getServer() != null) {
                RoguelikeSavedData.get(serverPlayer.getServer()).setDungeonDeathDifficulty(serverPlayer.getUUID(), difficulty);
            }
        });
        return button;
    }

    private static Component currentDifficultyLine(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || serverPlayer.getServer() == null) {
            return Component.translatable("screen.incore.dungeon_difficulty.loading");
        }
        DungeonDeathDifficulty difficulty = RoguelikeSavedData.get(serverPlayer.getServer()).dungeonDeathDifficulty(serverPlayer.getUUID());
        return Component.translatable(
                "screen.incore.dungeon_difficulty.current",
                Component.translatable("screen.incore.dungeon_difficulty.label." + difficulty.name().toLowerCase(Locale.ROOT))
        );
    }
}
