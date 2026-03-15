package ozokuz.incore.client.features.research;

import ozokuz.incore.client.features.machines.MachineInventoryScreen;
import ozokuz.incore.features.machines.multiblock.MultiblockOwnerKind;
import ozokuz.incore.features.research.station.WirelessLinkMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class WirelessLinkScreen extends MachineInventoryScreen<WirelessLinkMenu> {
    private Button clearButton;

    public WirelessLinkScreen(WirelessLinkMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageHeight = 190;
        this.inventoryLabelY = 96;
    }

    @Override
    protected void init() {
        super.init();
        clearButton = addRenderableWidget(Button.builder(
                Component.translatable("screen.incore.wireless_link.clear"),
                button -> {
                    if (minecraft != null && minecraft.gameMode != null) {
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
                    }
                }
        ).bounds(leftPos + 12, topPos + 22, imageWidth - 24, 20).build());
        updateButtonState();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateButtonState();
    }

    @Override
    protected int accentColor() {
        return 0xFFAD8450;
    }

    @Override
    protected int titleLabelY() {
        return 8;
    }

    @Override
    protected int machineSectionTopOffset() {
        return 58;
    }

    @Override
    protected int playerSectionTopOffset() {
        return 94;
    }

    @Override
    protected void renderSubtitle(GuiGraphics guiGraphics) {
        guiGraphics.drawString(font, statusText(), 12, 46, subtitleColor(), false);
    }

    private void updateButtonState() {
        if (clearButton == null) {
            return;
        }
        boolean visible = menu.ownerKind() == MultiblockOwnerKind.ORCHESTRATOR;
        clearButton.visible = visible;
        clearButton.active = visible && menu.hasStoredChannel();
    }

    private Component statusText() {
        String key = switch (menu.bindingStatus()) {
            case 1 -> "screen.incore.wireless_link.status.bound";
            case 2 -> "screen.incore.wireless_link.status.invalid";
            case 3 -> "screen.incore.wireless_link.status.stored";
            default -> "screen.incore.wireless_link.status.empty";
        };
        return Component.translatable(key);
    }
}
