package io.github.ozokuz.incore.features.research.station;

import com.google.gson.JsonObject;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class OrchestrationDiskData {
    private static final String KEY_STATE = "incore:orchestration_state";

    private OrchestrationDiskData() {
    }

    public static String readJson(ItemStack stack) {
        CompoundTag tag = readRoot(stack);
        return tag.getString(KEY_STATE);
    }

    public static void writeJson(ItemStack stack, String json) {
        if (stack.isEmpty()) {
            return;
        }
        CompoundTag tag = readRoot(stack);
        tag.putString(KEY_STATE, json == null ? "" : json);
        writeRoot(stack, tag);
    }

    public static void clear(ItemStack stack) {
        writeJson(stack, "");
    }

    public static void writeSummary(
            ItemStack stack,
            String orchestratorId,
            String teamId,
            String channelId,
            int wirelessMembers,
            int wiredComponents,
            int assignedStations
    ) {
        JsonObject root = new JsonObject();
        root.addProperty("orchestratorId", orchestratorId == null ? "" : orchestratorId);
        root.addProperty("teamId", teamId == null ? "" : teamId);
        root.addProperty("channelId", channelId == null ? "" : channelId);
        root.addProperty("wirelessMembers", Math.max(0, wirelessMembers));
        root.addProperty("wiredComponents", Math.max(0, wiredComponents));
        root.addProperty("assignedStations", Math.max(0, assignedStations));
        writeJson(stack, root.toString());
    }

    private static CompoundTag readRoot(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
    }

    private static void writeRoot(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}
