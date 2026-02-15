package io.github.ozokuz.incore.client.tasks;

import io.github.ozokuz.incore.client.status.AdvancementWindowRenderer;
import io.github.ozokuz.incore.features.tasks.client.TaskClientCache;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class TaskOverviewScreen extends Screen {
    public TaskOverviewScreen() {
        super(Component.translatable("screen.incore.tasks.title"));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int left = (this.width - 360) / 2;
        int top = (this.height - 220) / 2;
        AdvancementWindowRenderer.draw(guiGraphics, left, top, 360, 220);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        TaskClientCache.TaskSnapshot snapshot = TaskClientCache.snapshot();
        guiGraphics.drawString(this.font, this.title, left + 12, top + 10, 0xFFFFFF);

        int y = top + 28;
        guiGraphics.drawString(this.font, Component.translatable("screen.incore.tasks.daily_header", snapshot.dailyCompleted() ? "Complete" : "In Progress"), left + 12, y, 0xDADADA);
        y += 12;
        for (TaskClientCache.TaskEntry entry : snapshot.daily()) {
            guiGraphics.drawString(this.font, Component.literal("- " + entry.title() + " " + Math.min(entry.progress(), entry.goal()) + "/" + entry.goal()), left + 16, y, 0xFFFFFF);
            y += 10;
        }

        y += 8;
        guiGraphics.drawString(this.font, Component.translatable("screen.incore.tasks.weekly_header", snapshot.weeklyPoints()), left + 12, y, 0xDADADA);
        y += 12;
        for (TaskClientCache.TaskEntry entry : snapshot.weekly()) {
            String line = String.format("- [%s +%d] %s %d/%d", entry.difficulty(), entry.points(), entry.title(), Math.min(entry.progress(), entry.goal()), entry.goal());
            guiGraphics.drawString(this.font, Component.literal(line), left + 16, y, 0xFFFFFF);
            y += 10;
        }

        y += 8;
        guiGraphics.drawString(this.font, Component.translatable("screen.incore.tasks.tiers"), left + 12, y, 0xDADADA);
        y += 12;
        for (TaskClientCache.TierEntry tier : snapshot.tiers()) {
            String state = tier.claimed() ? "claimed" : (tier.unlocked() ? "unlocked" : "locked");
            guiGraphics.drawString(this.font, Component.literal("- Tier " + tier.tier() + " (" + tier.requiredPoints() + " pts): " + state), left + 16, y, 0xFFFFFF);
            y += 10;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
