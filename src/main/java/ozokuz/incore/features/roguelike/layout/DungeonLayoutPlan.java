package ozokuz.incore.features.roguelike.layout;

import ozokuz.incore.features.roguelike.data.DungeonThemeData;

import java.util.List;

public record DungeonLayoutPlan(
        int gridSize,
        int centerCell,
        List<RoomPlacement> rooms,
        List<HallwayPlacement> hallways
) {
    public record RoomPlacement(
            int cellX,
            int cellZ,
            DungeonThemeData.RoomType type,
            DungeonThemeData.TemplateRef template,
            boolean startRoom
    ) {
    }

    public record HallwayPlacement(
            int fromCellX,
            int fromCellZ,
            int toCellX,
            int toCellZ,
            Orientation orientation,
            DungeonThemeData.TemplateRef template
    ) {
    }

    public enum Orientation {
        NORTH_SOUTH,
        EAST_WEST
    }
}
