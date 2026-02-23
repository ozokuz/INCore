package io.github.ozokuz.incore.features.roguelike.layout;

import io.github.ozokuz.incore.features.roguelike.data.DungeonThemeData;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class DungeonLayoutGenerator {
    public static final int GRID_SIZE = 9;
    public static final int CENTER_CELL = GRID_SIZE / 2;

    private DungeonLayoutGenerator() {
    }

    public static DungeonLayoutPlan generate(DungeonThemeData theme, RandomSource random) {
        List<Cell> nonStartCells = new ArrayList<>();
        for (int z = 0; z < GRID_SIZE; z++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                if (x == CENTER_CELL && z == CENTER_CELL) {
                    continue;
                }
                nonStartCells.add(new Cell(x, z));
            }
        }

        List<DungeonThemeData.RoomType> assignments = buildAssignments(theme, nonStartCells.size(), random);
        List<DungeonLayoutPlan.RoomPlacement> rooms = new ArrayList<>();
        rooms.add(new DungeonLayoutPlan.RoomPlacement(
                CENTER_CELL,
                CENTER_CELL,
                DungeonThemeData.RoomType.REGULAR,
                theme.startRoom(),
                true
        ));

        for (int i = 0; i < nonStartCells.size(); i++) {
            Cell cell = nonStartCells.get(i);
            DungeonThemeData.RoomType type = assignments.get(i);
            DungeonThemeData.TemplateRef template = pickRoomTemplate(theme, type, random)
                    .orElse(theme.startRoom());
            rooms.add(new DungeonLayoutPlan.RoomPlacement(cell.x(), cell.z(), type, template, false));
        }

        Map<Long, DungeonLayoutPlan.RoomPlacement> roomsByCell = new HashMap<>();
        for (DungeonLayoutPlan.RoomPlacement room : rooms) {
            roomsByCell.put(cellKey(room.cellX(), room.cellZ()), room);
        }

        List<DungeonLayoutPlan.HallwayPlacement> hallways = new ArrayList<>();
        for (DungeonLayoutPlan.RoomPlacement room : rooms) {
            addHallway(theme, random, roomsByCell, hallways, room, room.cellX() + 1, room.cellZ());
            addHallway(theme, random, roomsByCell, hallways, room, room.cellX(), room.cellZ() + 1);
        }

        return new DungeonLayoutPlan(GRID_SIZE, CENTER_CELL, List.copyOf(rooms), List.copyOf(hallways));
    }

    private static void addHallway(
            DungeonThemeData theme,
            RandomSource random,
            Map<Long, DungeonLayoutPlan.RoomPlacement> roomsByCell,
            List<DungeonLayoutPlan.HallwayPlacement> hallways,
            DungeonLayoutPlan.RoomPlacement from,
            int toX,
            int toZ
    ) {
        if (toX < 0 || toZ < 0 || toX >= GRID_SIZE || toZ >= GRID_SIZE) {
            return;
        }

        DungeonLayoutPlan.RoomPlacement to = roomsByCell.get(cellKey(toX, toZ));
        if (to == null || !shouldConnect(from, to)) {
            return;
        }

        DungeonLayoutPlan.Orientation orientation = from.cellX() != to.cellX()
                ? DungeonLayoutPlan.Orientation.EAST_WEST
                : DungeonLayoutPlan.Orientation.NORTH_SOUTH;

        DungeonThemeData.TemplatePool pool = orientation == DungeonLayoutPlan.Orientation.EAST_WEST
                ? theme.hallways().eastWest()
                : theme.hallways().northSouth();
        Optional<DungeonThemeData.TemplateRef> templateOptional = pool.pick(random);
        if (templateOptional.isEmpty()) {
            return;
        }

        hallways.add(new DungeonLayoutPlan.HallwayPlacement(
                from.cellX(),
                from.cellZ(),
                to.cellX(),
                to.cellZ(),
                orientation,
                templateOptional.get()
        ));
    }

    private static boolean shouldConnect(DungeonLayoutPlan.RoomPlacement a, DungeonLayoutPlan.RoomPlacement b) {
        if (!a.startRoom() && !b.startRoom()) {
            return true;
        }

        DungeonLayoutPlan.RoomPlacement other = a.startRoom() ? b : a;
        return other.cellX() == CENTER_CELL && other.cellZ() == CENTER_CELL + 1;
    }

    private static Optional<DungeonThemeData.TemplateRef> pickRoomTemplate(
            DungeonThemeData theme,
            DungeonThemeData.RoomType type,
            RandomSource random
    ) {
        DungeonThemeData.TemplatePool exact = theme.roomPool(type);
        if (!exact.isEmpty()) {
            return exact.pick(random);
        }

        DungeonThemeData.TemplatePool regular = theme.roomPool(DungeonThemeData.RoomType.REGULAR);
        if (!regular.isEmpty()) {
            return regular.pick(random);
        }

        for (DungeonThemeData.RoomType roomType : DungeonThemeData.RoomType.values()) {
            DungeonThemeData.TemplatePool fallbackPool = theme.roomPool(roomType);
            if (!fallbackPool.isEmpty()) {
                return fallbackPool.pick(random);
            }
        }

        return Optional.empty();
    }

    private static List<DungeonThemeData.RoomType> buildAssignments(
            DungeonThemeData theme,
            int count,
            RandomSource random
    ) {
        List<DungeonThemeData.RoomType> assignments = new ArrayList<>(count);
        if (count <= 0) {
            return assignments;
        }

        int specialCount = 0;
        specialCount += appendType(assignments, DungeonThemeData.RoomType.LIBRARY, theme.specialRoomQuotas().library(), theme, count);
        specialCount += appendType(assignments, DungeonThemeData.RoomType.FACTORY, theme.specialRoomQuotas().factory(), theme, count);
        specialCount += appendType(assignments, DungeonThemeData.RoomType.WORKSHOP, theme.specialRoomQuotas().workshop(), theme, count);

        int remaining = Math.max(0, count - specialCount);
        if (remaining > 0) {
            List<DungeonThemeData.RoomType> normalTypes = List.of(
                    DungeonThemeData.RoomType.REGULAR,
                    DungeonThemeData.RoomType.QUARRY,
                    DungeonThemeData.RoomType.MARKET,
                    DungeonThemeData.RoomType.LOOT,
                    DungeonThemeData.RoomType.NORTHIUM_CAVES
            );
            List<DungeonThemeData.RoomType> available = normalTypes.stream()
                    .filter(type -> !theme.roomPool(type).isEmpty())
                    .toList();

            if (available.isEmpty()) {
                available = List.of(DungeonThemeData.RoomType.REGULAR);
            }

            assignments.addAll(distributeWeighted(available, remaining, theme));
        }

        while (assignments.size() < count) {
            assignments.add(DungeonThemeData.RoomType.REGULAR);
        }
        if (assignments.size() > count) {
            assignments = new ArrayList<>(assignments.subList(0, count));
        }

        shuffle(assignments, random);
        return assignments;
    }

    private static List<DungeonThemeData.RoomType> distributeWeighted(
            List<DungeonThemeData.RoomType> types,
            int totalCount,
            DungeonThemeData theme
    ) {
        if (types.size() == 1) {
            List<DungeonThemeData.RoomType> repeated = new ArrayList<>(totalCount);
            for (int i = 0; i < totalCount; i++) {
                repeated.add(types.get(0));
            }
            return repeated;
        }

        EnumMap<DungeonThemeData.RoomType, Integer> assigned = new EnumMap<>(DungeonThemeData.RoomType.class);
        EnumMap<DungeonThemeData.RoomType, Double> fractional = new EnumMap<>(DungeonThemeData.RoomType.class);
        int totalWeight = 0;
        for (DungeonThemeData.RoomType type : types) {
            totalWeight += Math.max(1, theme.roomTypeWeight(type));
        }
        totalWeight = Math.max(1, totalWeight);

        int assignedCount = 0;
        for (DungeonThemeData.RoomType type : types) {
            double exact = (double) totalCount * Math.max(1, theme.roomTypeWeight(type)) / (double) totalWeight;
            int base = (int) Math.floor(exact);
            assigned.put(type, base);
            fractional.put(type, exact - base);
            assignedCount += base;
        }

        while (assignedCount < totalCount) {
            DungeonThemeData.RoomType best = types.get(0);
            double bestFraction = -1.0D;
            for (DungeonThemeData.RoomType type : types) {
                double fraction = fractional.getOrDefault(type, 0.0D);
                if (fraction > bestFraction) {
                    best = type;
                    bestFraction = fraction;
                }
            }
            assigned.put(best, assigned.getOrDefault(best, 0) + 1);
            fractional.put(best, 0.0D);
            assignedCount++;
        }

        List<DungeonThemeData.RoomType> result = new ArrayList<>(totalCount);
        for (DungeonThemeData.RoomType type : types) {
            int count = assigned.getOrDefault(type, 0);
            for (int i = 0; i < count; i++) {
                result.add(type);
            }
        }
        return result;
    }

    private static int appendType(
            List<DungeonThemeData.RoomType> assignments,
            DungeonThemeData.RoomType type,
            int requestedCount,
            DungeonThemeData theme,
            int maxCount
    ) {
        if (requestedCount <= 0 || assignments.size() >= maxCount || theme.roomPool(type).isEmpty()) {
            return 0;
        }

        int count = Math.min(requestedCount, maxCount - assignments.size());
        for (int i = 0; i < count; i++) {
            assignments.add(type);
        }
        return count;
    }

    private static <T> void shuffle(List<T> list, RandomSource random) {
        for (int i = list.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            if (i == j) {
                continue;
            }
            T value = list.get(i);
            list.set(i, list.get(j));
            list.set(j, value);
        }
    }

    private static long cellKey(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    private record Cell(int x, int z) {
    }
}
