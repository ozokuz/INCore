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
        int timeProgress,
        int requiredTime,
        ResearchQueueStatus status,
        List<String> assignedStationIds
) {
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("nodeId", nodeId.toString());
        tag.putInt("timeProgress", Math.max(0, timeProgress));
        tag.putInt("requiredTime", Math.max(0, requiredTime));
        tag.putString("status", status.name());
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

        int timeProgress;
        if (tag.contains("timeProgress", Tag.TAG_INT)) {
            timeProgress = Math.max(0, tag.getInt("timeProgress"));
        } else {
            // Backward-compatible read for the old queue schema.
            timeProgress = Math.max(0, tag.getInt("progress"));
        }

        int requiredTime = Math.max(0, tag.getInt("requiredTime"));
        ResearchQueueStatus status = ResearchQueueStatus.fromSerialized(
                tag.contains("status", Tag.TAG_STRING) ? tag.getString("status") : null,
                timeProgress
        );

        List<String> stations = new ArrayList<>();
        ListTag listTag = tag.getList("assignedStationIds", Tag.TAG_STRING);
        for (Tag stationTag : listTag) {
            stations.add(stationTag.getAsString());
        }
        return new ResearchQueueEntry(nodeId, timeProgress, requiredTime, status, List.copyOf(stations));
    }
}
