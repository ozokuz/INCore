package io.github.ozokuz.incore;

import io.github.ozokuz.incore.data.ICBlockStateProvider;
import io.github.ozokuz.incore.data.ICItemModelProvider;
import io.github.ozokuz.incore.features.arena.data.ArenaCatalogManager;
import io.github.ozokuz.incore.features.arena.network.ArenaNetworking;
import io.github.ozokuz.incore.features.encounter_spawner.EncounterManager;
import io.github.ozokuz.incore.features.battlepass.BattlePassManager;
import io.github.ozokuz.incore.features.battlepass.command.BattlePassCommands;
import io.github.ozokuz.incore.features.battlepass.network.BattlePassNetworking;
import io.github.ozokuz.incore.features.cards.CardBoosterBoxManager;
import io.github.ozokuz.incore.features.cards.CardBoosterManager;
import io.github.ozokuz.incore.features.cards.CardDeckBoxManager;
import io.github.ozokuz.incore.features.cards.CardDeckCoreManager;
import io.github.ozokuz.incore.features.cards.CardModuleManager;
import io.github.ozokuz.incore.features.cards.CardSetManager;
import io.github.ozokuz.incore.features.cards.CardSynergyManager;
import io.github.ozokuz.incore.features.cards.CardVendorIntegration;
import io.github.ozokuz.incore.features.cards.command.CardCommands;
import io.github.ozokuz.incore.features.cards.network.CardNetworking;
import io.github.ozokuz.incore.features.gacha.GachaBannerManager;
import io.github.ozokuz.incore.features.gacha.GachaEventCategoryManager;
import io.github.ozokuz.incore.features.gacha.command.GachaCommands;
import io.github.ozokuz.incore.features.gacha.network.GachaNetworking;
import io.github.ozokuz.incore.features.market.MarketItemManager;
import io.github.ozokuz.incore.features.market.MarketEvents;
import io.github.ozokuz.incore.features.market.command.MarketCommands;
import io.github.ozokuz.incore.features.market.content.MarketMachineCapabilities;
import io.github.ozokuz.incore.features.market.network.MarketNetworking;
import io.github.ozokuz.incore.features.playerlevel.PlayerLevelRewardManager;
import io.github.ozokuz.incore.features.playerlevel.network.PlayerLevelNetworking;
import io.github.ozokuz.incore.features.numismatics.network.NumismaticsNetworking;
import io.github.ozokuz.incore.features.research.ManualResearchTaskManager;
import io.github.ozokuz.incore.features.research.ResearchEntryManager;
import io.github.ozokuz.incore.features.research.LabTier;
import io.github.ozokuz.incore.features.research.ResearchMaterialManager;
import io.github.ozokuz.incore.features.research.ResearchRecipeLockManager;
import io.github.ozokuz.incore.features.research.command.ResearchCommands;
import io.github.ozokuz.incore.features.research.network.ResearchNetworking;
import io.github.ozokuz.incore.features.roguelike.command.RoguelikeCommands;
import io.github.ozokuz.incore.features.roguelike.data.AltarOfferingManager;
import io.github.ozokuz.incore.features.roguelike.data.DungeonObjectiveManager;
import io.github.ozokuz.incore.features.roguelike.data.DungeonSocketManager;
import io.github.ozokuz.incore.features.roguelike.data.DungeonThemeManager;
import io.github.ozokuz.incore.features.sanity.command.SanityCommands;
import io.github.ozokuz.incore.features.sanity.network.SanityNetworking;
import io.github.ozokuz.incore.features.shop.ShopCategoryManager;
import io.github.ozokuz.incore.features.shop.ShopOfferManager;
import io.github.ozokuz.incore.features.shop.command.ShopCommands;
import io.github.ozokuz.incore.features.shop.network.ShopNetworking;
import io.github.ozokuz.incore.features.status.network.PlayerStatusNetworking;
import io.github.ozokuz.incore.features.tasks.TaskDataManager;
import io.github.ozokuz.incore.features.tasks.command.TaskCommands;
import io.github.ozokuz.incore.features.tasks.network.TaskNetworking;
import com.simibubi.create.api.stress.BlockStressValues;
import io.github.ozokuz.incore.features.vendor.VendorBootstrap;
import io.github.ozokuz.incore.features.vendor.VendorOfferManager;
import io.github.ozokuz.incore.features.vendor.network.VendorNetworking;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import com.simibubi.create.api.stress.BlockStressValues;
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
        modEventBus.addListener(ArenaNetworking::registerPayloads);
        modEventBus.addListener(this::registerCapabilities);
        modEventBus.addListener(CardNetworking::registerPayloads);
        modEventBus.addListener(VendorNetworking::registerPayloads);
        modEventBus.addListener(PlayerStatusNetworking::registerPayloads);

        VendorBootstrap.initialize();
        CardVendorIntegration.initialize();
        modEventBus.addListener(NumismaticsNetworking::registerPayloads);
        modEventBus.addListener(MarketNetworking::registerPayloads);
        modEventBus.addListener(ShopNetworking::registerPayloads);
        modEventBus.addListener(MarketMachineCapabilities::registerCapabilities);

        NeoForge.EVENT_BUS.addListener(this::onReloadListener);
        NeoForge.EVENT_BUS.addListener(SanityCommands::register);
        NeoForge.EVENT_BUS.addListener(GachaCommands::register);
        NeoForge.EVENT_BUS.addListener(TaskCommands::register);
        NeoForge.EVENT_BUS.addListener(BattlePassCommands::register);
        NeoForge.EVENT_BUS.addListener(ResearchCommands::register);
        NeoForge.EVENT_BUS.addListener(RoguelikeCommands::register);
        NeoForge.EVENT_BUS.addListener(CardCommands::register);
        NeoForge.EVENT_BUS.addListener(MarketCommands::register);
        NeoForge.EVENT_BUS.addListener(ShopCommands::register);
        NeoForge.EVENT_BUS.addListener(MarketEvents::onServerTick);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    @SubscribeEvent
    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> BlockStressValues.IMPACTS.register(
                Registration.MECHANICAL_LAB_BLOCK.get(),
                () -> Config.MECHANICAL_LAB_STRESS_PER_RPM.get().doubleValue()
        ));
        event.enqueueWork(() -> {
            BlockStressValues.IMPACTS.register(Registration.SHIPMENT_TERMINAL_BLOCK.get(), () -> 1024.0D);
            BlockStressValues.IMPACTS.register(Registration.MARKET_AUTOBUYER_BLOCK.get(), () -> 1024.0D);
        });
    }

    @SubscribeEvent
    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                Registration.LAB_BLOCK_ENTITY.get(),
                (be, side) -> {
                    if (!(be instanceof io.github.ozokuz.incore.features.research.LabBlockEntity lab)) {
                        return null;
                    }
                    return lab.labTier() == LabTier.MODULAR ? lab.energyStorage() : null;
                }
        );
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
        event.addListener(new ResearchMaterialManager());
        event.addListener(new ResearchRecipeLockManager());
        event.addListener(new ResearchEntryManager());
        event.addListener(new ManualResearchTaskManager());
        event.addListener(new AltarOfferingManager());
        event.addListener(new DungeonThemeManager());
        event.addListener(new DungeonSocketManager());
        event.addListener(new DungeonObjectiveManager());
        event.addListener(new ArenaCatalogManager());
        event.addListener(new CardSetManager());
        event.addListener(new CardModuleManager());
        event.addListener(new CardBoosterManager());
        event.addListener(new CardBoosterBoxManager());
        event.addListener(new CardDeckCoreManager());
        event.addListener(new CardDeckBoxManager());
        event.addListener(new CardSynergyManager());
        event.addListener(new VendorOfferManager());
        event.addListener(new MarketItemManager());
        event.addListener(new ShopCategoryManager());
        event.addListener(new ShopOfferManager());
    }
}
