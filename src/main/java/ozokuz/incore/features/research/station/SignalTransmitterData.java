package ozokuz.incore.features.research.station;

import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class SignalTransmitterData {
    private static final String KEY_CHANNEL_ID = "incore:signal_channel_id";
    private static final String KEY_OWNER_TEAM_ID = "incore:signal_owner_team_id";

    private SignalTransmitterData() {
    }

    public static String readChannelId(ItemStack stack) {
        CompoundTag tag = readRoot(stack);
        return tag.getString(KEY_CHANNEL_ID);
    }

    public static String readOwnerTeamId(ItemStack stack) {
        CompoundTag tag = readRoot(stack);
        return tag.getString(KEY_OWNER_TEAM_ID);
    }

    public static boolean hasBinding(ItemStack stack) {
        return !readChannelId(stack).isBlank() && !readOwnerTeamId(stack).isBlank();
    }

    public static boolean matches(ItemStack stack, String channelId, String ownerTeamId) {
        String normalizedChannel = channelId == null ? "" : channelId.strip();
        String normalizedOwnerTeam = ownerTeamId == null ? "" : ownerTeamId.strip();
        return !normalizedChannel.isBlank()
                && !normalizedOwnerTeam.isBlank()
                && normalizedChannel.equals(readChannelId(stack))
                && normalizedOwnerTeam.equals(readOwnerTeamId(stack));
    }

    public static void initialize(ItemStack stack, String ownerTeamId) {
        String normalizedOwnerTeam = ownerTeamId == null ? "" : ownerTeamId.strip();
        if (stack.isEmpty() || normalizedOwnerTeam.isBlank()) {
            return;
        }
        write(stack, UUID.randomUUID().toString(), normalizedOwnerTeam);
    }

    public static void write(ItemStack stack, String channelId, String ownerTeamId) {
        String normalizedChannel = channelId == null ? "" : channelId.strip();
        String normalizedOwnerTeam = ownerTeamId == null ? "" : ownerTeamId.strip();
        if (stack.isEmpty() || normalizedChannel.isBlank() || normalizedOwnerTeam.isBlank()) {
            return;
        }
        CompoundTag tag = readRoot(stack);
        tag.putString(KEY_CHANNEL_ID, normalizedChannel);
        tag.putString(KEY_OWNER_TEAM_ID, normalizedOwnerTeam);
        writeRoot(stack, tag);
    }

    private static CompoundTag readRoot(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
    }

    private static void writeRoot(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}
