package io.github.ozokuz.incore.features.surfaceore;

import io.github.ozokuz.incore.INCore;
import io.github.ozokuz.incore.Registration;
import net.minecraft.client.renderer.item.CompassItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CompassItem;
import net.minecraft.world.item.component.LodestoneTracker;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = INCore.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SurfaceOreDebugCompassClientEvents {
    private SurfaceOreDebugCompassClientEvents() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            registerCompassAngleProperty(Registration.SURFACE_ORE_DEBUG_COMPASS_ITEM.get());
            registerCompassAngleProperty(Registration.SURFACE_STONE_DEBUG_COMPASS_ITEM.get());
        });
    }

    private static void registerCompassAngleProperty(net.minecraft.world.item.Item item) {
        ItemProperties.register(
                item,
                ResourceLocation.withDefaultNamespace("angle"),
                new CompassItemPropertyFunction((level, stack, entity) -> {
                    LodestoneTracker tracker = stack.get(DataComponents.LODESTONE_TRACKER);
                    return tracker != null ? tracker.target().orElse(null) : CompassItem.getSpawnPosition(level);
                })
        );
    }
}
