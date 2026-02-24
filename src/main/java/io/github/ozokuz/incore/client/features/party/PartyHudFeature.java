package io.github.ozokuz.incore.client.features.party;

import io.github.ozokuz.incore.features.party.client.PartyHudClientCache;
import io.github.ozokuz.incore.features.roguelike.RoguelikeConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.List;

public final class PartyHudFeature {
    private PartyHudFeature() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(PartyHudFeature::onClientTick);
        NeoForge.EVENT_BUS.addListener(PartyHudFeature::onRenderGui);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            PartyHudClientCache.clear();
        }
    }

    private static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || minecraft.player.isSpectator()) {
            return;
        }

        if (!minecraft.player.level().dimension().equals(RoguelikeConstants.DUNGEON_DIMENSION)) {
            return;
        }

        List<PartyHudClientCache.MemberView> rows = PartyHudClientCache.members();
        if (rows.isEmpty()) {
            return;
        }

        renderPanel(event.getGuiGraphics(), minecraft, rows);
    }

    private static void renderPanel(GuiGraphics guiGraphics, Minecraft minecraft, List<PartyHudClientCache.MemberView> rows) {
        List<String> lines = new ArrayList<>(rows.size() + 1);
        lines.add("Party");
        for (PartyHudClientCache.MemberView row : rows) {
            float health = Math.max(0.0F, row.health());
            float maxHealth = Math.max(1.0F, row.maxHealth());
            lines.add(row.name() + "  " + formatHealth(health) + "/" + formatHealth(maxHealth));
        }

        int x = 10;
        int y = 10;
        int lineHeight = minecraft.font.lineHeight + 2;
        int width = 0;
        for (String line : lines) {
            width = Math.max(width, minecraft.font.width(line));
        }
        width += 8;
        int height = 8 + (lines.size() * lineHeight);

        guiGraphics.fill(x, y, x + width, y + height, 0x66000000);
        guiGraphics.drawString(minecraft.font, lines.getFirst(), x + 4, y + 4, 0xFFE1C06A, false);

        for (int i = 1; i < lines.size(); i++) {
            PartyHudClientCache.MemberView row = rows.get(i - 1);
            float ratio = row.maxHealth() <= 0.0F ? 0.0F : (row.health() / row.maxHealth());
            int color = healthColor(ratio);
            guiGraphics.drawString(minecraft.font, lines.get(i), x + 4, y + 4 + (i * lineHeight), color, false);
        }
    }

    private static String formatHealth(float value) {
        return String.format(java.util.Locale.ROOT, "%.1f", Mth.clamp(value, 0.0F, 999.0F));
    }

    private static int healthColor(float ratio) {
        if (ratio <= 0.25F) {
            return 0xFFFF5555;
        }
        if (ratio <= 0.50F) {
            return 0xFFFFAA00;
        }
        return 0xFFFFFFFF;
    }
}
