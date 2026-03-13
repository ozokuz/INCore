package io.github.ozokuz.incore.features.research.station;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ResearchDiskData {
    private static final String KEY_TIER = "incore:disk_tier";
    private static final String KEY_SNAPSHOTS = "incore:disk_snapshots";

    private ResearchDiskData() {
    }

    public static ResearchDiskTier readTier(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return ResearchDiskTier.T1;
        }
        CompoundTag tag = data.copyTag();
        return switch (tag.getInt(KEY_TIER)) {
            case 2 -> ResearchDiskTier.T2;
            case 3 -> ResearchDiskTier.T3;
            case 4 -> ResearchDiskTier.T4;
            default -> ResearchDiskTier.T1;
        };
    }

    public static void initialize(ItemStack stack, ResearchDiskTier tier) {
        CompoundTag tag = readRoot(stack);
        tag.putInt(KEY_TIER, switch (tier) {
            case T1 -> 1;
            case T2 -> 2;
            case T3 -> 3;
            case T4 -> 4;
        });
        writeRoot(stack, tag);
    }

    public static boolean isLocked(ItemStack stack) {
        return false;
    }

    public static void setLocked(ItemStack stack, boolean locked) {
        // Research disks no longer persist an in-use lock bit.
    }

    public static List<Snapshot> readSnapshots(ItemStack stack) {
        CompoundTag tag = readRoot(stack);
        ListTag list = tag.getList(KEY_SNAPSHOTS, Tag.TAG_COMPOUND);
        List<Snapshot> snapshots = new ArrayList<>();
        for (Tag rowTag : list) {
            if (!(rowTag instanceof CompoundTag row)) {
                continue;
            }
            ResourceLocation nodeId = ResourceLocation.tryParse(row.getString("nodeId"));
            if (nodeId == null) {
                continue;
            }
            Set<Integer> corrupted = new LinkedHashSet<>();
            ListTag corruptedList = row.getList("corruptedSegments", Tag.TAG_INT);
            for (Tag segmentTag : corruptedList) {
                corrupted.add(((IntTag) segmentTag).getAsInt());
            }
            snapshots.add(new Snapshot(
                    nodeId,
                    Math.max(0, row.getInt("completedRuns")),
                    Math.max(1, row.getInt("requiredRuns")),
                    Math.max(0, row.getInt("lastWriteOrdinal")),
                    row.getBoolean("completed"),
                    Set.copyOf(corrupted)
            ));
        }
        snapshots.sort(Comparator.comparing(snapshot -> snapshot.nodeId().toString()));
        return List.copyOf(snapshots);
    }

    public static void writeSnapshots(ItemStack stack, List<Snapshot> snapshots) {
        ResearchDiskTier tier = readTier(stack);
        List<Snapshot> trimmed = new ArrayList<>(snapshots);
        if (trimmed.size() > tier.snapshotCapacity()) {
            trimmed = trimmed.subList(Math.max(0, trimmed.size() - tier.snapshotCapacity()), trimmed.size());
        }

        CompoundTag tag = readRoot(stack);
        ListTag rows = new ListTag();
        for (Snapshot snapshot : trimmed) {
            CompoundTag row = new CompoundTag();
            row.putString("nodeId", snapshot.nodeId().toString());
            row.putInt("completedRuns", snapshot.completedRuns());
            row.putInt("requiredRuns", snapshot.requiredRuns());
            row.putInt("lastWriteOrdinal", snapshot.lastWriteOrdinal());
            row.putBoolean("completed", snapshot.completed());
            ListTag corrupted = new ListTag();
            snapshot.corruptedSegments().stream()
                    .sorted()
                    .forEach(segment -> corrupted.add(IntTag.valueOf(segment)));
            row.put("corruptedSegments", corrupted);
            rows.add(row);
        }
        tag.put(KEY_SNAPSHOTS, rows);
        writeRoot(stack, tag);
    }

    public static void clearCorruptedSegment(ItemStack stack, ResourceLocation nodeId, int segmentIndex) {
        List<Snapshot> snapshots = new ArrayList<>(readSnapshots(stack));
        for (int i = 0; i < snapshots.size(); i++) {
            Snapshot snapshot = snapshots.get(i);
            if (!snapshot.nodeId().equals(nodeId)) {
                continue;
            }
            Set<Integer> nextSegments = new LinkedHashSet<>(snapshot.corruptedSegments());
            nextSegments.remove(segmentIndex);
            snapshots.set(i, snapshot.withCorruptedSegments(Set.copyOf(nextSegments)));
            break;
        }
        writeSnapshots(stack, snapshots);
    }

    public static void clearSnapshots(ItemStack stack, ResourceLocation nodeId) {
        List<Snapshot> snapshots = new ArrayList<>(readSnapshots(stack));
        snapshots.removeIf(snapshot -> snapshot.nodeId().equals(nodeId));
        writeSnapshots(stack, snapshots);
    }

    public static boolean hasCorruption(ItemStack stack) {
        return readSnapshots(stack).stream().anyMatch(snapshot -> !snapshot.corruptedSegments().isEmpty());
    }

    private static CompoundTag readRoot(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
    }

    private static void writeRoot(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public record Snapshot(
            ResourceLocation nodeId,
            int completedRuns,
            int requiredRuns,
            int lastWriteOrdinal,
            boolean completed,
            Set<Integer> corruptedSegments
    ) {
        public Snapshot withCorruptedSegments(Set<Integer> segments) {
            return new Snapshot(nodeId, completedRuns, requiredRuns, lastWriteOrdinal, completed, segments);
        }
    }
}
