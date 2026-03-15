package ozokuz.incore.features.tasks;

import net.minecraft.resources.ResourceLocation;

public record TaskDefinition(
        ResourceLocation id,
        Period period,
        TaskType type,
        ResourceLocation target,
        int goal,
        WeeklyDifficulty difficulty,
        String title,
        String description
) {
    public enum Period {
        DAILY,
        WEEKLY
    }

    public enum TaskType {
        ITEM_COLLECTION,
        MOB_KILL
    }

    public enum WeeklyDifficulty {
        EASY(1),
        MEDIUM(2),
        HARD(5),
        NONE(0);

        private final int points;

        WeeklyDifficulty(int points) {
            this.points = points;
        }

        public int points() {
            return this.points;
        }
    }
}
