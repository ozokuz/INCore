package ozokuz.incore.features.roguelike.data;

import com.google.gson.JsonObject;

public record DungeonModifierData(
        int timerTicksDelta,
        int bonusCrates,
        int encounterReductionPercent,
        double mobHealthMultiplier,
        double mobDamageMultiplier,
        int weight
) {
    public static DungeonModifierData fromJson(JsonObject json) {
        int timerSecondsDelta = json.has("timer_seconds_delta") ? json.get("timer_seconds_delta").getAsInt() : 0;
        int bonusCrates = json.has("bonus_crates") ? json.get("bonus_crates").getAsInt() : 0;
        int encounterReductionPercent = json.has("encounter_reduction_percent") ? json.get("encounter_reduction_percent").getAsInt() : 0;
        double mobHealthMultiplier = json.has("mob_health_multiplier") ? json.get("mob_health_multiplier").getAsDouble() : 1.0D;
        double mobDamageMultiplier = json.has("mob_damage_multiplier") ? json.get("mob_damage_multiplier").getAsDouble() : 1.0D;
        int weight = json.has("weight") ? json.get("weight").getAsInt() : 1;

        return new DungeonModifierData(
                timerSecondsDelta * 20,
                Math.max(0, bonusCrates),
                Math.clamp(encounterReductionPercent, 0, 95),
                Math.max(0.1D, mobHealthMultiplier),
                Math.max(0.1D, mobDamageMultiplier),
                Math.max(1, weight)
        );
    }
}
