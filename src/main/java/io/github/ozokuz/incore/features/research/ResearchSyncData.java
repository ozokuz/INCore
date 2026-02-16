package io.github.ozokuz.incore.features.research;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Comparator;
import java.util.Map;

public final class ResearchSyncData {
    private ResearchSyncData() {}

    public static String build(ServerPlayer player) {
        JsonObject root = new JsonObject();
        ResourceLocation activeResearch = ResearchProgressService.activeResearch(player);
        root.addProperty("active_research", activeResearch == null ? "" : activeResearch.toString());
        root.addProperty("active_progress", ResearchProgressService.activeProgress(player));

        JsonArray unlocked = new JsonArray();
        ResearchProgressService.unlocked(player).stream().map(ResourceLocation::toString).sorted().forEach(unlocked::add);
        root.add("unlocked", unlocked);

        JsonArray completedTasks = new JsonArray();
        ResearchProgressService.completedTasks(player).stream().map(ResourceLocation::toString).sorted().forEach(completedTasks::add);
        root.add("completed_tasks", completedTasks);

        JsonArray queue = new JsonArray();
        ResearchProgressService.queuedResearch(player).stream().map(ResourceLocation::toString).forEach(queue::add);
        root.add("queue", queue);

        JsonObject progress = new JsonObject();
        ResearchProgressService.progressByEntry(player).entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> progress.addProperty(entry.getKey().toString(), entry.getValue()));
        root.add("progress", progress);

        JsonArray entries = new JsonArray();
        ResearchEntryManager.all().values().stream().sorted(Comparator.comparing(d -> d.id().toString())).forEach(entry -> {
            JsonObject json = new JsonObject();
            json.addProperty("id", entry.id().toString());
            json.addProperty("title", entry.title());
            json.addProperty("description", entry.description());
            json.addProperty("cost", entry.cost());
            json.addProperty("icon_item", entry.iconItem() == null ? "" : entry.iconItem().toString());
            json.addProperty("run_duration_ticks", entry.runDurationTicks());

            JsonArray unlocks = new JsonArray();
            entry.unlocks().forEach(unlocks::add);
            json.add("unlocks", unlocks);

            JsonArray materials = new JsonArray();
            entry.researchMaterials().forEach(material -> {
                JsonObject materialJson = new JsonObject();
                materialJson.addProperty("item", material.itemId() == null ? "" : material.itemId().toString());
                materialJson.addProperty("count", material.itemCount());
                materials.add(materialJson);
            });
            json.add("research_materials", materials);

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
            json.addProperty("repeatable", task.repeatable());
            tasks.add(json);
        });
        root.add("tasks", tasks);
        return root.toString();
    }
}
