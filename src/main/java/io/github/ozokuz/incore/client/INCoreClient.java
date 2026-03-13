package io.github.ozokuz.incore.client;

import io.github.ozokuz.incore.INCore;
import io.github.ozokuz.incore.Registration;
import io.github.ozokuz.incore.client.features.roguelike.DungeonAltarRenderer;
import io.github.ozokuz.incore.client.features.roguelike.DungeonCrystalModificationStationScreen;
import io.github.ozokuz.incore.client.features.roguelike.MeCrystalAutomationTerminalScreen;
import io.github.ozokuz.incore.client.features.roguelike.RoguelikePortalRenderer;
import io.github.ozokuz.incore.client.features.roguelike.RoguelikeMinimapHudFeature;
import io.github.ozokuz.incore.client.features.party.PartyHudFeature;
import io.github.ozokuz.incore.client.features.entropy.EntropyBarHudFeature;
import io.github.ozokuz.incore.client.features.stamina.StaminaBarHudFeature;
import io.github.ozokuz.incore.client.features.party.PartyManagementScreen;
import io.github.ozokuz.incore.client.features.battlepass.BattlePassScreen;
import io.github.ozokuz.incore.client.features.status.PlayerStatusScreen;
import io.github.ozokuz.incore.client.features.status.StatusScreenReturnTracker;
import io.github.ozokuz.incore.client.features.tasks.TaskOverviewScreen;
import io.github.ozokuz.incore.features.arena.network.ArenaNetworking;
import io.github.ozokuz.incore.client.features.cards.CardDeckStationScreen;
import io.github.ozokuz.incore.features.gacha.network.GachaNetworking;
import io.github.ozokuz.incore.client.features.market.MarketAutoTraderScreen;
import io.github.ozokuz.incore.client.features.market.MarketTerminalCardScreen;
import io.github.ozokuz.incore.client.features.market.MarketTerminalMeCardScreen;
import io.github.ozokuz.incore.client.features.market.ShipmentTerminalScreen;
import io.github.ozokuz.incore.features.research.client.CrudeResearchStationScreen;
import io.github.ozokuz.incore.features.research.client.DataloggerScreen;
import io.github.ozokuz.incore.features.research.client.PowerInputScreen;
import io.github.ozokuz.incore.features.research.client.AugmenterScreen;
import io.github.ozokuz.incore.features.research.client.LogicHousingScreen;
import io.github.ozokuz.incore.features.research.client.MaterialStorageScreen;
import io.github.ozokuz.incore.features.research.client.OrchestrationDriveScreen;
import io.github.ozokuz.incore.features.research.client.OutputPortScreen;
import io.github.ozokuz.incore.features.research.client.ResearchControllerScreen;
import io.github.ozokuz.incore.features.research.client.ResearchDriveScreen;
import io.github.ozokuz.incore.features.research.client.ResearchOrchestratorControllerScreen;
import io.github.ozokuz.incore.features.research.client.ResearchSampleFabricatorScreen;
import io.github.ozokuz.incore.features.market.network.MarketNetworking;
import io.github.ozokuz.incore.features.numismatics.network.NumismaticsNetworking;
import io.github.ozokuz.incore.features.playerlevel.PlayerFeatureUnlockIds;
import io.github.ozokuz.incore.features.playerlevel.network.PlayerLevelClientCache;
import io.github.ozokuz.incore.features.research.client.ResearchTreeScreen;
import io.github.ozokuz.incore.features.research.client.TranslatorScreen;
import io.github.ozokuz.incore.features.research.client.WirelessLinkScreen;
import io.github.ozokuz.incore.features.research.network.ResearchNetworking;
import io.github.ozokuz.incore.features.shop.network.ShopNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = INCore.MODID, dist = Dist.CLIENT)
public class INCoreClient {
    public INCoreClient(IEventBus modEventBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modEventBus.addListener(this::onRegisterKeyMappings);
        modEventBus.addListener(this::onRegisterScreens);
        modEventBus.addListener(this::onRegisterClientReloadListeners);
        modEventBus.addListener(this::onRegisterRenderers);
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
        StaminaBarHudFeature.register();
        EntropyBarHudFeature.register();
        PartyHudFeature.register();
        RoguelikeMinimapHudFeature.register();
    }

    private void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(INCoreKeyMappings.OPEN_PLAYER_STATUS);
        event.register(INCoreKeyMappings.OPEN_GACHA_BANNERS);
        event.register(INCoreKeyMappings.OPEN_TASK_OVERVIEW);
        event.register(INCoreKeyMappings.OPEN_BATTLE_PASS);
        event.register(INCoreKeyMappings.OPEN_RESEARCH_TREE);
        event.register(INCoreKeyMappings.OPEN_COMBAT_CATALOG);
        event.register(INCoreKeyMappings.OPEN_NUMISMATICS_BANK);
        event.register(INCoreKeyMappings.OPEN_MARKET);
        event.register(INCoreKeyMappings.OPEN_SHOP);
        event.register(INCoreKeyMappings.OPEN_PARTY);
    }

    private void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(Registration.DECK_STATION_MENU.get(), CardDeckStationScreen::new);
        event.register(Registration.MARKET_TERMINAL_CARD_MENU.get(), MarketTerminalCardScreen::new);
        event.register(Registration.MARKET_TERMINAL_ME_CARD_MENU.get(), MarketTerminalMeCardScreen::new);
        event.register(Registration.SHIPMENT_TERMINAL_MENU.get(), ShipmentTerminalScreen::new);
        event.register(Registration.MARKET_AUTOTRADER_MENU.get(), MarketAutoTraderScreen::new);
        event.register(Registration.DUNGEON_CRYSTAL_MODIFICATION_STATION_MENU.get(), DungeonCrystalModificationStationScreen::new);
        event.register(Registration.ME_CRYSTAL_AUTOMATION_TERMINAL_MENU.get(), MeCrystalAutomationTerminalScreen::new);
        event.register(Registration.CRUDE_RESEARCH_STATION_MENU.get(), CrudeResearchStationScreen::new);
        event.register(Registration.LOGIC_HOUSING_MENU.get(), LogicHousingScreen::new);
        event.register(Registration.RESEARCH_DRIVE_MENU.get(), ResearchDriveScreen::new);
        event.register(Registration.ORCHESTRATION_DRIVE_MENU.get(), OrchestrationDriveScreen::new);
        event.register(Registration.MATERIAL_STORAGE_MENU.get(), MaterialStorageScreen::new);
        event.register(Registration.OUTPUT_PORT_MENU.get(), OutputPortScreen::new);
        event.register(Registration.AUGMENTER_MENU.get(), AugmenterScreen::new);
        event.register(Registration.WIRELESS_LINK_MENU.get(), WirelessLinkScreen::new);
        event.register(Registration.RESEARCH_CONTROLLER_MENU.get(), ResearchControllerScreen::new);
        event.register(Registration.RESEARCH_ORCHESTRATOR_CONTROLLER_MENU.get(), ResearchOrchestratorControllerScreen::new);
        event.register(Registration.POWER_INPUT_MENU.get(), PowerInputScreen::new);
        event.register(Registration.DATALOGGER_MENU.get(), DataloggerScreen::new);
        event.register(Registration.TRANSLATOR_MENU.get(), TranslatorScreen::new);
        event.register(Registration.RESEARCH_SAMPLE_FABRICATOR_MENU.get(), ResearchSampleFabricatorScreen::new);
    }

    private void onRegisterClientReloadListeners(RegisterClientReloadListenersEvent event) {
    }

    private void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Registration.DUNGEON_ALTAR_BE.get(), DungeonAltarRenderer::new);
        event.registerBlockEntityRenderer(Registration.ROGUELIKE_PORTAL_BE.get(), RoguelikePortalRenderer::new);
    }

    private void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean hasPlayer = minecraft.player != null;
        StatusScreenReturnTracker.onClientTick(minecraft);
        if (!hasPlayer || minecraft.screen != null) {
            return;
        }

        while (INCoreKeyMappings.OPEN_PLAYER_STATUS.consumeClick()) {
            minecraft.setScreen(new PlayerStatusScreen());
        }

        while (INCoreKeyMappings.OPEN_GACHA_BANNERS.consumeClick()) {
            if (ensureFeatureUnlocked(minecraft, PlayerFeatureUnlockIds.GACHA_BASIC)) {
                GachaNetworking.requestOpenBannerScreen();
            }
        }

        while (INCoreKeyMappings.OPEN_TASK_OVERVIEW.consumeClick()) {
            if (ensureFeatureUnlocked(minecraft, PlayerFeatureUnlockIds.TASKS_SCREEN)) {
                minecraft.setScreen(new TaskOverviewScreen());
            }
        }

        while (INCoreKeyMappings.OPEN_BATTLE_PASS.consumeClick()) {
            if (ensureFeatureUnlocked(minecraft, PlayerFeatureUnlockIds.BATTLEPASS_SCREEN)) {
                minecraft.setScreen(new BattlePassScreen(null));
            }
        }

        while (INCoreKeyMappings.OPEN_RESEARCH_TREE.consumeClick()) {
            minecraft.setScreen(new ResearchTreeScreen());
            ResearchNetworking.requestSnapshot();
        }

        while (INCoreKeyMappings.OPEN_COMBAT_CATALOG.consumeClick()) {
            if (ensureFeatureUnlocked(minecraft, PlayerFeatureUnlockIds.ARENA_TIER_1)) {
                ArenaNetworking.requestOpenCatalog();
            }
        }

        while (INCoreKeyMappings.OPEN_NUMISMATICS_BANK.consumeClick()) {
            NumismaticsNetworking.requestOpenBankScreen();
        }

        while (INCoreKeyMappings.OPEN_MARKET.consumeClick()) {
            if (ensureFeatureUnlocked(minecraft, PlayerFeatureUnlockIds.MARKET_BASIC)) {
                MarketNetworking.requestOpenMarketScreen();
            }
        }

        while (INCoreKeyMappings.OPEN_SHOP.consumeClick()) {
            if (ensureFeatureUnlocked(minecraft, PlayerFeatureUnlockIds.SHOP_SCREEN)) {
                ShopNetworking.requestOpenShopScreen();
            }
        }

        while (INCoreKeyMappings.OPEN_PARTY.consumeClick()) {
            minecraft.setScreen(new PartyManagementScreen());
        }
    }

    private static boolean ensureFeatureUnlocked(Minecraft minecraft, ResourceLocation featureId) {
        String rawId = featureId.toString();
        if (PlayerLevelClientCache.isFeatureUnlocked(rawId)) {
            return true;
        }

        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(
                    Component.translatable(
                            "incore.progression.locked_feature",
                            PlayerLevelClientCache.getFeatureDisplayName(rawId),
                            PlayerLevelClientCache.getFeatureRequiredLevel(rawId)
                    ),
                    true
            );
        }
        return false;
    }
}
