package io.github.ozokuz.incore.features.researchv2.state;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

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
        List<String> assignedStationIds
) {
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
                List.copyOf(stations)
        );
    }
}
