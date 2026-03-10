package io.github.ozokuz.incore.features.researchv2.state;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public record ResearchQueueEntry(
        ResourceLocation nodeId,
        int runTickProgress,
        int runTickRequired,
        int completedRuns,
        int requiredRuns,
        boolean runInputsCommitted,
        ResearchQueueStatus status,
        int runPowerMultiplierBps,
        int runBonusRunChanceBps,
        int runCorruptionMultiplierBps,
        List<String> assignedStationIds,
        List<ActiveResearchRun> activeRuns
) {
    public ResearchQueueEntry {
        assignedStationIds = assignedStationIds == null ? List.of() : List.copyOf(assignedStationIds);
        activeRuns = activeRuns == null ? List.of() : List.copyOf(activeRuns);
        if (!activeRuns.isEmpty()) {
            Set<String> activeStationIds = new LinkedHashSet<>();
            activeRuns.stream()
                    .map(ActiveResearchRun::stationId)
                    .filter(id -> id != null && !id.isBlank())
                    .forEach(activeStationIds::add);
            assignedStationIds = List.copyOf(activeStationIds);
        }
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("nodeId", nodeId.toString());
        tag.putInt("runTickProgress", Math.max(0, runTickProgress));
        tag.putInt("runTickRequired", Math.max(1, runTickRequired));
        tag.putInt("completedRuns", Math.max(0, completedRuns));
        tag.putInt("requiredRuns", Math.max(1, requiredRuns));
        tag.putBoolean("runInputsCommitted", runInputsCommitted);
        tag.putString("status", status.name());
        tag.putInt("runPowerMultiplierBps", Math.max(0, runPowerMultiplierBps));
        tag.putInt("runBonusRunChanceBps", Math.max(0, runBonusRunChanceBps));
        tag.putInt("runCorruptionMultiplierBps", Math.max(0, runCorruptionMultiplierBps));
        ListTag stations = new ListTag();
        assignedStationIds.forEach(id -> stations.add(StringTag.valueOf(id)));
        tag.put("assignedStationIds", stations);
        ListTag activeRunTags = new ListTag();
        activeRuns.forEach(run -> activeRunTags.add(run.toTag()));
        tag.put("activeRuns", activeRunTags);
        return tag;
    }

    public static @Nullable ResearchQueueEntry fromTag(CompoundTag tag) {
        ResourceLocation nodeId = ResourceLocation.tryParse(tag.getString("nodeId"));
        if (nodeId == null) {
            return null;
        }

        int runTickProgress = Math.max(0, tag.getInt("runTickProgress"));
        int runTickRequired = Math.max(1, tag.getInt("runTickRequired"));
        int completedRuns = Math.max(0, tag.getInt("completedRuns"));
        int requiredRuns = Math.max(1, tag.getInt("requiredRuns"));
        boolean runInputsCommitted = tag.getBoolean("runInputsCommitted");
        ResearchQueueStatus status = ResearchQueueStatus.fromSerialized(
                tag.contains("status", Tag.TAG_STRING) ? tag.getString("status") : null,
                runTickProgress
        );
        int runPowerMultiplierBps = Math.max(0, tag.getInt("runPowerMultiplierBps"));
        int runBonusRunChanceBps = Math.max(0, tag.getInt("runBonusRunChanceBps"));
        int runCorruptionMultiplierBps = Math.max(0, tag.getInt("runCorruptionMultiplierBps"));

        List<String> stations = new ArrayList<>();
        ListTag listTag = tag.getList("assignedStationIds", Tag.TAG_STRING);
        for (Tag stationTag : listTag) {
            stations.add(stationTag.getAsString());
        }

        List<ActiveResearchRun> activeRuns = new ArrayList<>();
        ListTag activeRunTags = tag.getList("activeRuns", Tag.TAG_COMPOUND);
        for (Tag activeRunTag : activeRunTags) {
            ActiveResearchRun activeRun = ActiveResearchRun.fromTag((CompoundTag) activeRunTag);
            if (activeRun != null) {
                activeRuns.add(activeRun);
            }
        }

        if (activeRuns.isEmpty() && runInputsCommitted && !stations.isEmpty()) {
            String stationId = stations.get(0);
            if (stationId != null && !stationId.isBlank()) {
                activeRuns.add(new ActiveResearchRun(
                        stationId,
                        runTickProgress,
                        runTickRequired,
                        runPowerMultiplierBps <= 0 ? 10_000 : runPowerMultiplierBps,
                        Math.max(0, runBonusRunChanceBps),
                        runCorruptionMultiplierBps <= 0 ? 10_000 : runCorruptionMultiplierBps
                ));
            }
        }

        return new ResearchQueueEntry(
                nodeId,
                runTickProgress,
                runTickRequired,
                completedRuns,
                requiredRuns,
                runInputsCommitted,
                status,
                runPowerMultiplierBps,
                runBonusRunChanceBps,
                runCorruptionMultiplierBps,
                List.copyOf(stations),
                List.copyOf(activeRuns)
        );
    }

    public @Nullable ActiveResearchRun primaryActiveRun() {
        return activeRuns.isEmpty() ? null : activeRuns.get(0);
    }

    public @Nullable ActiveResearchRun activeRun(String stationId) {
        if (stationId == null || stationId.isBlank()) {
            return primaryActiveRun();
        }
        for (ActiveResearchRun activeRun : activeRuns) {
            if (stationId.equals(activeRun.stationId())) {
                return activeRun;
            }
        }
        return primaryActiveRun();
    }
}
