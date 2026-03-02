package io.github.ozokuz.incore.client.features.roguelike;

import io.github.ozokuz.incore.features.roguelike.RoguelikeConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class RoguelikeMinimapHudFeature {
    private static final int GRID_SIZE = 9;
    private static final int CENTER_CELL = GRID_SIZE / 2;
    private static final int CELL_PIXELS = 8;

    private RoguelikeMinimapHudFeature() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(RoguelikeMinimapHudFeature::onClientTick);
        NeoForge.EVENT_BUS.addListener(RoguelikeMinimapHudFeature::onRenderGui);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            RoguelikeMinimapClientCache.clear();
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

        RoguelikeMinimapClientCache.Snapshot snapshot = RoguelikeMinimapClientCache.snapshot();
        if (!snapshot.hasGraph()) {
            return;
        }

        renderMinimap(event.getGuiGraphics(), minecraft, snapshot);
    }

    private static void renderMinimap(GuiGraphics guiGraphics, Minecraft minecraft, RoguelikeMinimapClientCache.Snapshot snapshot) {
        int mapX = minecraft.getWindow().getGuiScaledWidth() - (GRID_SIZE * CELL_PIXELS) - 14;
        int mapY = 14;
        int mapW = GRID_SIZE * CELL_PIXELS;
        int mapH = GRID_SIZE * CELL_PIXELS;
        guiGraphics.fill(mapX - 3, mapY - 3, mapX + mapW + 3, mapY + mapH + 3, 0x88000000);

        Set<Integer> revealed = snapshot.revealedRooms();
        for (int roomId = 0; roomId < GRID_SIZE * GRID_SIZE; roomId++) {
            int cellX = roomId % GRID_SIZE;
            int cellZ = roomId / GRID_SIZE;
            int x1 = mapX + cellX * CELL_PIXELS;
            int z1 = mapY + cellZ * CELL_PIXELS;
            int fill = revealed.contains(roomId) ? 0xFF2A3A4F : 0x3318222E;
            if (cellX == CENTER_CELL && cellZ == CENTER_CELL) {
                fill = revealed.contains(roomId) ? 0xFF335D66 : 0x33335D66;
            }
            guiGraphics.fill(x1, z1, x1 + CELL_PIXELS - 1, z1 + CELL_PIXELS - 1, fill);
        }

        int playerRoom = roomIdForPlayer(snapshot.originChunkX(), snapshot.originChunkZ(), minecraft.player.getBlockX(), minecraft.player.getBlockZ());
        if (playerRoom >= 0) {
            drawMarker(guiGraphics, mapX, mapY, playerRoom, 0xFF6EF3FF);
        }

        for (Map.Entry<UUID, Integer> marker : snapshot.partyRoomMarkers().entrySet()) {
            if (minecraft.player.getUUID().equals(marker.getKey())) {
                continue;
            }
            drawMarker(guiGraphics, mapX, mapY, marker.getValue(), 0xFFFFD66E);
        }
    }

    private static void drawMarker(GuiGraphics guiGraphics, int mapX, int mapY, int roomId, int color) {
        if (roomId < 0 || roomId >= GRID_SIZE * GRID_SIZE) {
            return;
        }
        int cellX = roomId % GRID_SIZE;
        int cellZ = roomId / GRID_SIZE;
        int x = mapX + cellX * CELL_PIXELS + (CELL_PIXELS / 2) - 1;
        int y = mapY + cellZ * CELL_PIXELS + (CELL_PIXELS / 2) - 1;
        guiGraphics.fill(x, y, x + 2, y + 2, color);
    }

    public static int roomIdForPlayer(int originChunkX, int originChunkZ, int blockX, int blockZ) {
        int chunkX = blockX >> 4;
        int chunkZ = blockZ >> 4;
        int relativeX = chunkX - originChunkX;
        int relativeZ = chunkZ - originChunkZ;

        if (relativeX == 21 && relativeZ == 21) {
            return CENTER_CELL + (CENTER_CELL * GRID_SIZE);
        }

        for (int cellZ = 0; cellZ < GRID_SIZE; cellZ++) {
            for (int cellX = 0; cellX < GRID_SIZE; cellX++) {
                if (cellX == CENTER_CELL && cellZ == CENTER_CELL) {
                    continue;
                }
                int minChunkX = cellX * 5;
                int minChunkZ = cellZ * 5;
                int maxChunkX = minChunkX + 2;
                int maxChunkZ = minChunkZ + 2;
                if (relativeX >= minChunkX && relativeX <= maxChunkX && relativeZ >= minChunkZ && relativeZ <= maxChunkZ) {
                    return cellX + (cellZ * GRID_SIZE);
                }
            }
        }
        return -1;
    }
}
