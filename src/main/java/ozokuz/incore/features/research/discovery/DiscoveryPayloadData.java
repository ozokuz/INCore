package ozokuz.incore.features.research.discovery;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class DiscoveryPayloadData {
    private static final String KEY_NODE_IDS = "incore:discovery_node_ids";
    private static final String KEY_SOURCE_TYPE = "incore:discovery_source_type";
    private static final String KEY_SOURCE_ID = "incore:discovery_source_id";
    private static final String KEY_DISPLAY_NAME = "incore:discovery_display_name";
    private static final String KEY_ORIGIN_TEAM_ID = "incore:discovery_origin_team_id";

    private DiscoveryPayloadData() {
    }

    public static DiscoveryPayload read(ItemStack stack) {
        CompoundTag tag = readRoot(stack);
        List<ResourceLocation> nodeIds = new ArrayList<>();
        ListTag nodeList = tag.getList(KEY_NODE_IDS, Tag.TAG_STRING);
        for (Tag row : nodeList) {
            ResourceLocation nodeId = ResourceLocation.tryParse(row.getAsString());
            if (nodeId != null) {
                nodeIds.add(nodeId);
            }
        }
        nodeIds.sort(Comparator.naturalOrder());
        return new DiscoveryPayload(
                List.copyOf(nodeIds),
                tag.getString(KEY_SOURCE_TYPE),
                tag.getString(KEY_SOURCE_ID),
                tag.getString(KEY_DISPLAY_NAME),
                tag.getString(KEY_ORIGIN_TEAM_ID)
        );
    }

    public static void write(ItemStack stack, DiscoveryPayload payload) {
        if (stack.isEmpty() || payload == null) {
            return;
        }

        CompoundTag tag = readRoot(stack);
        ListTag nodeList = new ListTag();
        payload.nodeIds().stream()
                .sorted()
                .forEach(nodeId -> nodeList.add(StringTag.valueOf(nodeId.toString())));
        tag.put(KEY_NODE_IDS, nodeList);
        putString(tag, KEY_SOURCE_TYPE, payload.sourceType());
        putString(tag, KEY_SOURCE_ID, payload.sourceId());
        putString(tag, KEY_DISPLAY_NAME, payload.displayName());
        putString(tag, KEY_ORIGIN_TEAM_ID, payload.originTeamId());
        writeRoot(stack, tag);
    }

    public static boolean hasPayload(ItemStack stack) {
        return !read(stack).nodeIds().isEmpty();
    }

    private static void putString(CompoundTag tag, String key, String value) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isBlank()) {
            tag.remove(key);
            return;
        }
        tag.putString(key, normalized);
    }

    private static CompoundTag readRoot(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
    }

    private static void writeRoot(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}
