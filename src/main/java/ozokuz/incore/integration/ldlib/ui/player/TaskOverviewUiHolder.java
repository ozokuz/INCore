package ozokuz.incore.integration.ldlib.ui.player;

import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import ozokuz.incore.features.tasks.TaskService;
import ozokuz.incore.integration.ldlib.ui.INCoreLdLibUiScaffold;

public final class TaskOverviewUiHolder implements PlayerUIMenuType.PlayerUIHolder {
    @Override
    public ModularUI createUI(Player player) {
        return INCoreLdLibUiScaffold.build(player, createView(player));
    }

    static TaskOverviewUiElement createView(Player player) {
        TaskOverviewUiElement view = new TaskOverviewUiElement();
        view.bind(DataBindingBuilder.stringS2C(() -> screenDataJson(player)).build());
        return view;
    }

    private static String screenDataJson(Player player) {
        return player instanceof ServerPlayer serverPlayer ? TaskService.buildSyncJson(serverPlayer) : "";
    }
}
