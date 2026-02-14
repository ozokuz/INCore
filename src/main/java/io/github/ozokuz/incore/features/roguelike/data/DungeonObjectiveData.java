package io.github.ozokuz.incore.features.roguelike.data;

import com.google.gson.JsonObject;

public record DungeonObjectiveData(String type, int target, int rewardCrates, int weight) {
    public static DungeonObjectiveData fromJson(JsonObject json) {
        String type = json.has("type") ? json.get("type").getAsString() : "kill_mobs";
        int target = json.has("target") ? json.get("target").getAsInt() : 10;
        int rewardCrates = json.has("reward_crates") ? json.get("reward_crates").getAsInt() : 2;
        int weight = json.has("weight") ? json.get("weight").getAsInt() : 1;

        return new DungeonObjectiveData(type, Math.max(1, target), Math.max(1, rewardCrates), Math.max(1, weight));
    }

    public boolean isKillObjective() {
        return "kill_mobs".equals(type);
    }
}
