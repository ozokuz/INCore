package ozokuz.incore.features.research.station;

import ozokuz.incore.Registration;
import ozokuz.incore.features.research.material.ResearchMaterialDefinition;
import ozokuz.incore.features.research.material.ResearchMaterialManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class StationInventoryRules {
    private StationInventoryRules() {
    }

    public static boolean isLogicModule(ItemStack stack) {
        return stack.getItem() instanceof LogicModuleItem || stack.is(Registration.BASIC_LOGIC_MODULE_ITEM.get());
    }

    public static boolean isResearchDisk(ItemStack stack) {
        return stack.getItem() instanceof ResearchDiskItem;
    }

    public static boolean isOrchestrationDisk(ItemStack stack) {
        return stack.getItem() instanceof OrchestrationDiskItem;
    }

    public static boolean isSignalTransmitter(ItemStack stack) {
        return stack.getItem() instanceof SignalTransmitterItem;
    }

    public static boolean isResearchMaterial(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        for (ResearchMaterialDefinition definition : ResearchMaterialManager.all().values()) {
            if (definition.itemId().equals(itemId)) {
                return true;
            }
        }
        return false;
    }
}
