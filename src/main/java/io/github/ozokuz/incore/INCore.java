package io.github.ozokuz.incore;

import io.github.ozokuz.incore.data.ICBlockStateProvider;
import io.github.ozokuz.incore.data.ICItemModelProvider;
import io.github.ozokuz.incore.features.encounter_spawner.EncounterManager;
import io.github.ozokuz.incore.features.battlepass.BattlePassManager;
import io.github.ozokuz.incore.features.battlepass.command.BattlePassCommands;
import io.github.ozokuz.incore.features.battlepass.network.BattlePassNetworking;
import io.github.ozokuz.incore.features.gacha.GachaBannerManager;
import io.github.ozokuz.incore.features.gacha.GachaEventCategoryManager;
import io.github.ozokuz.incore.features.gacha.command.GachaCommands;
import io.github.ozokuz.incore.features.gacha.network.GachaNetworking;
import io.github.ozokuz.incore.features.playerlevel.PlayerLevelRewardManager;
import io.github.ozokuz.incore.features.playerlevel.network.PlayerLevelNetworking;
import io.github.ozokuz.incore.features.research.LabProcessManager;
import io.github.ozokuz.incore.features.research.ManualResearchTaskManager;
import io.github.ozokuz.incore.features.research.ResearchEntryManager;
import io.github.ozokuz.incore.features.research.network.ResearchNetworking;
import io.github.ozokuz.incore.features.sanity.command.SanityCommands;
import io.github.ozokuz.incore.features.sanity.network.SanityNetworking;
import io.github.ozokuz.incore.features.tasks.TaskDataManager;
import io.github.ozokuz.incore.features.tasks.command.TaskCommands;
import io.github.ozokuz.incore.features.tasks.network.TaskNetworking;
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
        modEventBus.addListener(GachaNetworking::registerPayloads);
        modEventBus.addListener(PlayerLevelNetworking::registerPayloads);
        modEventBus.addListener(TaskNetworking::registerPayloads);
        modEventBus.addListener(BattlePassNetworking::registerPayloads);
        modEventBus.addListener(ResearchNetworking::registerPayloads);

        NeoForge.EVENT_BUS.addListener(this::onReloadListener);
        NeoForge.EVENT_BUS.addListener(SanityCommands::register);
        NeoForge.EVENT_BUS.addListener(GachaCommands::register);
        NeoForge.EVENT_BUS.addListener(TaskCommands::register);
        NeoForge.EVENT_BUS.addListener(BattlePassCommands::register);

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
        event.addListener(new GachaBannerManager());
        event.addListener(new GachaEventCategoryManager());
        event.addListener(new PlayerLevelRewardManager());
        event.addListener(new TaskDataManager());
        event.addListener(new BattlePassManager());
        event.addListener(new ResearchEntryManager());
        event.addListener(new ManualResearchTaskManager());
        event.addListener(new LabProcessManager());
    }
}
