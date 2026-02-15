package io.github.ozokuz.incore.features.research;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Comparator;

public final class ResearchSyncData {
    private ResearchSyncData() {}

    public static String build(ServerPlayer player) {
        JsonObject root = new JsonObject();
        root.addProperty("points", ResearchProgressService.getPoints(player));

        JsonArray unlocked = new JsonArray();
        ResearchProgressService.unlocked(player).stream().map(ResourceLocation::toString).sorted().forEach(unlocked::add);
        root.add("unlocked", unlocked);

        JsonArray completedTasks = new JsonArray();
        ResearchProgressService.completedTasks(player).stream().map(ResourceLocation::toString).sorted().forEach(completedTasks::add);
        root.add("completed_tasks", completedTasks);

        JsonArray entries = new JsonArray();
        ResearchEntryManager.all().values().stream().sorted(Comparator.comparing(d -> d.id().toString())).forEach(entry -> {
            JsonObject json = new JsonObject();
            json.addProperty("id", entry.id().toString());
            json.addProperty("title", entry.title());
            json.addProperty("description", entry.description());
            json.addProperty("cost", entry.cost());

            JsonArray prereq = new JsonArray();
            entry.prerequisites().stream().map(ResourceLocation::toString).sorted().forEach(prereq::add);
            json.add("prerequisites", prereq);

            JsonArray requiredTasks = new JsonArray();
            entry.requiredTasks().stream().map(ResourceLocation::toString).sorted().forEach(requiredTasks::add);
            json.add("required_tasks", requiredTasks);
            entries.add(json);
        });
        root.add("entries", entries);

        JsonArray tasks = new JsonArray();
        ManualResearchTaskManager.all().values().stream().sorted(Comparator.comparing(d -> d.id().toString())).forEach(task -> {
            JsonObject json = new JsonObject();
            json.addProperty("id", task.id().toString());
            json.addProperty("title", task.title());
            json.addProperty("description", task.description());
            json.addProperty("item", task.itemId() == null ? "" : task.itemId().toString());
            json.addProperty("count", task.itemCount());
            json.addProperty("reward_points", task.rewardPoints());
            json.addProperty("repeatable", task.repeatable());
            tasks.add(json);
        });
        root.add("tasks", tasks);
        return root.toString();
    }
}
