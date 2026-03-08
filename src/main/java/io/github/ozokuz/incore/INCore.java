package io.github.ozokuz.incore;

import appeng.api.AECapabilities;
import io.github.ozokuz.incore.data.ICBlockStateProvider;
import io.github.ozokuz.incore.data.ICItemModelProvider;
import io.github.ozokuz.incore.features.arena.data.ArenaCatalogManager;
import io.github.ozokuz.incore.features.arena.network.ArenaNetworking;
import io.github.ozokuz.incore.features.encounter_spawner.EncounterManager;
import io.github.ozokuz.incore.features.battlepass.BattlePassLaneManager;
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
import io.github.ozokuz.incore.features.cards.CardVendingMachineIntegration;
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
import io.github.ozokuz.incore.features.market.content.MarketTerminalMeBlockEntity;
import io.github.ozokuz.incore.features.market.network.MarketNetworking;
import io.github.ozokuz.incore.features.playerlevel.PlayerFeatureUnlockManager;
import io.github.ozokuz.incore.features.playerlevel.PlayerLevelRewardManager;
import io.github.ozokuz.incore.features.playerlevel.network.PlayerLevelNetworking;
import io.github.ozokuz.incore.features.numismatics.network.NumismaticsNetworking;
import io.github.ozokuz.incore.features.party.command.PartyCommands;
import io.github.ozokuz.incore.features.party.network.PartyNetworking;
import io.github.ozokuz.incore.features.research.LabTier;
import io.github.ozokuz.incore.features.researchv2.command.ResearchV2Commands;
import io.github.ozokuz.incore.features.researchv2.network.ResearchV2Networking;
import io.github.ozokuz.incore.features.researchv2.provider.ResearchProviderManager;
import io.github.ozokuz.incore.features.researchv2.registry.ResearchRegistry;
import io.github.ozokuz.incore.features.researchv2.station.ElectricPowerInputBlockEntity;
import io.github.ozokuz.incore.features.researchv2.station.HybridResearchStationResourceProvider;
import io.github.ozokuz.incore.features.roguelike.command.RoguelikeCommands;
import io.github.ozokuz.incore.features.roguelike.data.DungeonModifierManager;
import io.github.ozokuz.incore.features.roguelike.data.AltarOfferingManager;
import io.github.ozokuz.incore.features.roguelike.data.DungeonObjectiveManager;
import io.github.ozokuz.incore.features.roguelike.data.DungeonSocketManager;
import io.github.ozokuz.incore.features.roguelike.data.DungeonThemeManager;
import io.github.ozokuz.incore.features.roguelike.network.RoguelikeNetworking;
import io.github.ozokuz.incore.features.roguelike.content.DungeonAltarAutomatorBlockEntity;
import io.github.ozokuz.incore.features.entropy.command.EntropyCommands;
import io.github.ozokuz.incore.features.entropy.network.EntropyNetworking;
import io.github.ozokuz.incore.features.shop.ShopCategoryManager;
import io.github.ozokuz.incore.features.shop.ShopOfferManager;
import io.github.ozokuz.incore.features.shop.command.ShopCommands;
import io.github.ozokuz.incore.features.shop.network.ShopNetworking;
import io.github.ozokuz.incore.features.status.network.PlayerStatusNetworking;
import io.github.ozokuz.incore.features.surfaceore.network.SurfaceOreNetworking;
import io.github.ozokuz.incore.features.tasks.TaskDataManager;
import io.github.ozokuz.incore.features.tasks.command.TaskCommands;
import io.github.ozokuz.incore.features.tasks.network.TaskNetworking;
import com.simibubi.create.api.stress.BlockStressValues;
import io.github.ozokuz.incore.features.vendingmachine.VendingMachineBootstrap;
import io.github.ozokuz.incore.features.vendingmachine.VendingMachineOfferManager;
import io.github.ozokuz.incore.features.vendingmachine.network.VendingMachineNetworking;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
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
    private static final HybridResearchStationResourceProvider HYBRID_RESEARCH_PROVIDER = new HybridResearchStationResourceProvider();

    public INCore(IEventBus modEventBus, ModContainer modContainer) {
        Registration.register(modEventBus);
        modEventBus.register(this);
        modEventBus.addListener(EntropyNetworking::registerPayloads);
        modEventBus.addListener(GachaNetworking::registerPayloads);
        modEventBus.addListener(PlayerLevelNetworking::registerPayloads);
        modEventBus.addListener(TaskNetworking::registerPayloads);
        modEventBus.addListener(BattlePassNetworking::registerPayloads);
        modEventBus.addListener(ResearchV2Networking::registerPayloads);
        modEventBus.addListener(ArenaNetworking::registerPayloads);
        modEventBus.addListener(this::registerCapabilities);
        modEventBus.addListener(CardNetworking::registerPayloads);
        modEventBus.addListener(VendingMachineNetworking::registerPayloads);
        modEventBus.addListener(PlayerStatusNetworking::registerPayloads);
        modEventBus.addListener(SurfaceOreNetworking::registerPayloads);

        VendingMachineBootstrap.initialize();
        CardVendingMachineIntegration.initialize();
        modEventBus.addListener(NumismaticsNetworking::registerPayloads);
        modEventBus.addListener(MarketNetworking::registerPayloads);
        modEventBus.addListener(ShopNetworking::registerPayloads);
        modEventBus.addListener(PartyNetworking::registerPayloads);
        modEventBus.addListener(RoguelikeNetworking::registerPayloads);
        modEventBus.addListener(MarketMachineCapabilities::registerCapabilities);

        NeoForge.EVENT_BUS.addListener(this::onReloadListener);
        NeoForge.EVENT_BUS.addListener(EntropyCommands::register);
        NeoForge.EVENT_BUS.addListener(GachaCommands::register);
        NeoForge.EVENT_BUS.addListener(TaskCommands::register);
        NeoForge.EVENT_BUS.addListener(BattlePassCommands::register);
        NeoForge.EVENT_BUS.addListener(ResearchV2Commands::register);
        NeoForge.EVENT_BUS.addListener(RoguelikeCommands::register);
        NeoForge.EVENT_BUS.addListener(CardCommands::register);
        NeoForge.EVENT_BUS.addListener(MarketCommands::register);
        NeoForge.EVENT_BUS.addListener(ShopCommands::register);
        NeoForge.EVENT_BUS.addListener(PartyCommands::register);
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
            ResearchProviderManager.setResearchPowerProvider(HYBRID_RESEARCH_PROVIDER);
            ResearchProviderManager.setLogicModuleProvider(HYBRID_RESEARCH_PROVIDER);
            ResearchProviderManager.setResearchMaterialProvider(HYBRID_RESEARCH_PROVIDER);
        });
        event.enqueueWork(() -> {
            BlockStressValues.IMPACTS.register(Registration.SHIPMENT_TERMINAL_BLOCK.get(), () -> 1024.0D);
            BlockStressValues.IMPACTS.register(Registration.MARKET_AUTOTRADER_BLOCK.get(), () -> 1024.0D);
            BlockStressValues.IMPACTS.register(Registration.MECHANICAL_POWER_INPUT_BLOCK.get(), () -> Config.MECHANICAL_INPUT_STRESS_IMPACT.get().doubleValue());
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
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                Registration.ELECTRIC_POWER_INPUT_BE.get(),
                ElectricPowerInputBlockEntity::getEnergyStorage
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                Registration.BURNER_POWER_INPUT_BE.get(),
                (input, side) -> side != null && side == input.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING)
                        ? input.itemHandler()
                        : null
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                Registration.DUNGEON_ALTAR_AUTOMATOR_BE.get(),
                (be, side) -> be.itemHandler()
        );
        event.registerBlockEntity(
                AECapabilities.IN_WORLD_GRID_NODE_HOST,
                Registration.MARKET_TERMINAL_ME_BE.get(),
                (be, context) -> be
        );
        event.registerBlockEntity(
                AECapabilities.IN_WORLD_GRID_NODE_HOST,
                Registration.DUNGEON_ALTAR_AUTOMATOR_BE.get(),
                (be, context) -> be
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
        event.addListener(new PlayerFeatureUnlockManager());
        event.addListener(new PlayerLevelRewardManager());
        event.addListener(new TaskDataManager());
        event.addListener(new BattlePassLaneManager());
        event.addListener(new BattlePassManager());
        event.addListener(new ResearchRegistry());
        event.addListener(new AltarOfferingManager());
        event.addListener(new DungeonThemeManager());
        event.addListener(new DungeonSocketManager());
        event.addListener(new DungeonObjectiveManager());
        event.addListener(new DungeonModifierManager());
        event.addListener(new ArenaCatalogManager());
        event.addListener(new CardSetManager());
        event.addListener(new CardModuleManager());
        event.addListener(new CardBoosterManager());
        event.addListener(new CardBoosterBoxManager());
        event.addListener(new CardDeckCoreManager());
        event.addListener(new CardDeckBoxManager());
        event.addListener(new CardSynergyManager());
        event.addListener(new VendingMachineOfferManager());
        event.addListener(new MarketItemManager());
        event.addListener(new ShopCategoryManager());
        event.addListener(new ShopOfferManager());
    }
}
