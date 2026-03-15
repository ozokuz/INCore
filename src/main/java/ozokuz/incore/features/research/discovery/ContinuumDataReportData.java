package ozokuz.incore.features.research.discovery;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class ContinuumDataReportData {
    private static final String KEY_REPORT_ID = "incore:continuum_report_id";

    private ContinuumDataReportData() {
    }

    public static ResourceLocation readReportId(ItemStack stack) {
        CompoundTag tag = readRoot(stack);
        return ResourceLocation.tryParse(tag.getString(KEY_REPORT_ID));
    }

    public static void writeReportId(ItemStack stack, ResourceLocation reportId) {
        if (stack.isEmpty() || reportId == null) {
            return;
        }
        CompoundTag tag = readRoot(stack);
        tag.putString(KEY_REPORT_ID, reportId.toString());
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
