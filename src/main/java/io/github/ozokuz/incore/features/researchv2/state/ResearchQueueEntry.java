package io.github.ozokuz.incore.features.researchv2.state;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public record ResearchQueueEntry(ResourceLocation nodeId, int progress, List<String> assignedStationIds) {
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("nodeId", nodeId.toString());
        tag.putInt("progress", Math.max(0, progress));
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

        int progress = Math.max(0, tag.getInt("progress"));
        List<String> stations = new ArrayList<>();
        ListTag listTag = tag.getList("assignedStationIds", Tag.TAG_STRING);
        for (Tag stationTag : listTag) {
            stations.add(stationTag.getAsString());
        }
        return new ResearchQueueEntry(nodeId, progress, List.copyOf(stations));
    }
}
