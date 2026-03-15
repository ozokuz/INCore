package ozokuz.incore.client.features.research;

import ozokuz.incore.features.research.network.ResearchNetworking;
import ozokuz.incore.features.research.station.ResearchDiskData;
import ozokuz.incore.features.research.station.ResearchDriveMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class CorruptedDiskScreen extends Screen {
    private final Screen parent;
    private final ResearchDriveMenu menu;

    public CorruptedDiskScreen(Screen parent, ResearchDriveMenu menu) {
        super(Component.translatable("screen.incore.corrupted_disk"));
        this.parent = parent;
        this.menu = menu;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> onClose())
                .bounds(width / 2 - 40, height - 28, 80, 20)
                .build());

        int y = 30;
        var drive = menu.drive();
        var disk = drive.mountedDisk();
        for (ResearchDiskData.Snapshot snapshot : ResearchDiskData.readSnapshots(disk)) {
            if (snapshot.corruptedSegments().isEmpty()) {
                y += 20;
                continue;
            }
            for (int segment : snapshot.corruptedSegments()) {
                ResourceLocation nodeId = snapshot.nodeId();
                addRenderableWidget(Button.builder(
                        Component.literal(nodeId.getPath() + " [" + segment + "]"),
                        button -> ResearchNetworking.repairDiskSegment(drive.getBlockPos(), nodeId, segment)
                ).bounds(width / 2 - 90, y, 180, 20).build());
                y += 22;
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
        guiGraphics.drawCenteredString(font, Component.translatable("screen.incore.corrupted_disk.subtitle"), width / 2, 18, 0xA0A0A0);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }
}
