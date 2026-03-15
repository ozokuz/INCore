package ozokuz.incore.client.integration.ldlib;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import ozokuz.incore.integration.ldlib.ui.INCoreUiIds;
import ozokuz.incore.integration.ldlib.ui.RequestOpenIncoreUiPayload;

public final class ReturnToPlayerStatusScreen extends Screen {
    private boolean requestedOpen;

    public ReturnToPlayerStatusScreen() {
        super(Component.translatable("screen.incore.player_status.title"));
    }

    @Override
    protected void init() {
        if (!requestedOpen && Minecraft.getInstance().player != null) {
            requestedOpen = true;
            PacketDistributor.sendToServer(new RequestOpenIncoreUiPayload(INCoreUiIds.PLAYER_STATUS));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
    }
}
