package io.github.ozokuz.incore.features.research.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.ozokuz.incore.features.research.network.ResearchNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.Set;

public class ResearchTechTreeScreen extends Screen {
    private final JsonObject payload;
    private int points;
    private final Set<String> unlocked = new HashSet<>();
    private final Set<String> completedTasks = new HashSet<>();

    public ResearchTechTreeScreen(String json) {
        super(Component.translatable("screen.incore.research.title"));
        this.payload = JsonParser.parseString(json).getAsJsonObject();
    }

    @Override
    protected void init() {
        super.init();
        points = payload.get("points").getAsInt();

        unlocked.clear();
        JsonArray unlockedArr = payload.getAsJsonArray("unlocked");
        unlockedArr.forEach(e -> unlocked.add(e.getAsString()));

        completedTasks.clear();
        JsonArray taskArr = payload.getAsJsonArray("completed_tasks");
        taskArr.forEach(e -> completedTasks.add(e.getAsString()));

        int y = 32;
        for (var element : payload.getAsJsonArray("entries")) {
            JsonObject entry = element.getAsJsonObject();
            String id = entry.get("id").getAsString();
            int cost = entry.get("cost").getAsInt();
            boolean isUnlocked = unlocked.contains(id);
            Button button = Button.builder(Component.literal((isUnlocked ? "✓ " : "") + entry.get("title").getAsString() + " [" + cost + "]"), b -> {
                ResourceLocation rl = ResourceLocation.tryParse(id);
                if (rl != null) {
                    ResearchNetworking.requestUnlock(rl);
                }
            }).bounds(width / 2 - 155, y, 150, 20).build();
            button.active = !isUnlocked;
            addRenderableWidget(button);
            y += 22;
        }

        y = 32;
        for (var element : payload.getAsJsonArray("tasks")) {
            JsonObject task = element.getAsJsonObject();
            String id = task.get("id").getAsString();
            boolean repeatable = task.get("repeatable").getAsBoolean();
            boolean done = completedTasks.contains(id);
            Button button = Button.builder(Component.literal((done && !repeatable ? "✓ " : "") + task.get("title").getAsString()), b -> {
                ResourceLocation rl = ResourceLocation.tryParse(id);
                if (rl != null) {
                    ResearchNetworking.requestTaskSubmit(rl);
                }
            }).bounds(width / 2 + 5, y, 150, 20).build();
            button.active = repeatable || !done;
            addRenderableWidget(button);
            y += 22;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
        guiGraphics.drawString(font, Component.translatable("screen.incore.research.points", points), width / 2 - 155, 18, 0xFFD55A, false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.research.entries"), width / 2 - 155, 24, 0xFFFFFF, false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.research.tasks"), width / 2 + 5, 24, 0xFFFFFF, false);
    }
}
