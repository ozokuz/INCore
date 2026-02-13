package io.github.ozokuz.incore;

import io.github.ozokuz.incore.data.ICBlockStateProvider;
import io.github.ozokuz.incore.data.ICItemModelProvider;
import io.github.ozokuz.incore.features.encounter_spawner.EncounterManager;
import io.github.ozokuz.incore.features.sanity.command.SanityCommands;
import io.github.ozokuz.incore.features.sanity.network.SanityNetworking;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;

@Mod(INCore.MODID)
public class INCore {
    public static final String MODID = "incore";
    public static final Logger LOGGER = LogUtils.getLogger();

    public INCore(IEventBus modEventBus, ModContainer modContainer) {
        Registration.register(modEventBus);
        modEventBus.register(this);
        modEventBus.addListener(SanityNetworking::registerPayloads);

        NeoForge.EVENT_BUS.addListener(this::onReloadListener);
        NeoForge.EVENT_BUS.addListener(SanityCommands::register);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    @SubscribeEvent
    private void commonSetup(FMLCommonSetupEvent event) {
    }

    @SubscribeEvent
    public void data(GatherDataEvent e) {
        DataGenerator generator = e.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = e.getExistingFileHelper();

        generator.addProvider(e.includeClient(), new ICBlockStateProvider(output, existingFileHelper));
        generator.addProvider(e.includeClient(), new ICItemModelProvider(output, existingFileHelper));
    }

    public void onReloadListener(AddReloadListenerEvent event) {
        event.addListener(new EncounterManager());
    }
}
