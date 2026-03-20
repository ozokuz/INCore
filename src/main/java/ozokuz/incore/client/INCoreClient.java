package ozokuz.incore.client;

import ozokuz.incore.INCore;
import ozokuz.incore.Registration;
import ozokuz.incore.client.features.roguelike.DungeonAltarRenderer;
import ozokuz.incore.client.features.roguelike.DungeonCrystalModificationStationScreen;
import ozokuz.incore.client.features.roguelike.MeCrystalAutomationTerminalScreen;
import ozokuz.incore.client.features.roguelike.RoguelikePortalRenderer;
import ozokuz.incore.client.features.roguelike.RoguelikeMinimapHudFeature;
import ozokuz.incore.client.features.party.PartyHudFeature;
import ozokuz.incore.client.features.entropy.EntropyBarHudFeature;
import ozokuz.incore.client.features.stamina.StaminaBarHudFeature;
import ozokuz.incore.client.features.status.StatusScreenReturnTracker;
import ozokuz.incore.client.features.cards.CardDeckStationScreen;
import ozokuz.incore.client.features.market.MarketAutoTraderScreen;
import ozokuz.incore.client.features.market.MarketTerminalCardScreen;
import ozokuz.incore.client.features.market.MarketTerminalMeCardScreen;
import ozokuz.incore.client.features.market.ShipmentTerminalScreen;
import ozokuz.incore.client.features.research.CrudeResearchStationScreen;
import ozokuz.incore.client.features.research.DataloggerScreen;
import ozokuz.incore.client.features.machines.PowerInputScreen;
import ozokuz.incore.client.features.machines.AugmenterScreen;
import ozokuz.incore.client.features.research.LogicHousingScreen;
import ozokuz.incore.client.features.research.MaterialStorageScreen;
import ozokuz.incore.client.features.research.OrchestrationDriveScreen;
import ozokuz.incore.client.features.machines.OutputPortScreen;
import ozokuz.incore.client.features.research.ResearchControllerScreen;
import ozokuz.incore.client.features.research.ResearchDriveScreen;
import ozokuz.incore.client.features.research.ResearchOrchestratorControllerScreen;
import ozokuz.incore.client.features.research.ResearchSampleFabricatorScreen;
import ozokuz.incore.features.market.network.MarketNetworking;
import ozokuz.incore.features.numismatics.network.NumismaticsNetworking;
import ozokuz.incore.features.playerlevel.PlayerFeatureUnlockIds;
import ozokuz.incore.features.playerlevel.network.PlayerLevelClientCache;
import ozokuz.incore.client.features.research.ResearchTreeScreen;
import ozokuz.incore.client.features.research.TranslatorScreen;
import ozokuz.incore.client.features.research.WirelessLinkScreen;
import ozokuz.incore.features.research.network.ResearchNetworking;
import ozokuz.incore.integration.ldlib.ui.INCoreUiIds;
import ozokuz.incore.integration.ldlib.ui.RequestOpenIncoreUiPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerMenu;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerScreen;
import ozokuz.incore.integration.ldlib.ui.player.PlayerStatusRouteUiHolder;
import ozokuz.incore.integration.ldlib.ui.RequestBackIncoreUiPayload;

@Mod(value = INCore.MODID, dist = Dist.CLIENT)
public class INCoreClient {
    public INCoreClient(IEventBus modEventBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modEventBus.addListener(this::onRegisterKeyMappings);
        modEventBus.addListener(this::onRegisterScreens);
        modEventBus.addListener(this::onRegisterClientReloadListeners);
        modEventBus.addListener(this::onRegisterRenderers);
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
        NeoForge.EVENT_BUS.addListener(this::onScreenKeyPressed);
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
        if (!hasPlayer) {
            return;
        }

        while (INCoreKeyMappings.OPEN_PLAYER_STATUS.consumeClick()) {
            if (minecraft.screen == null || isPlayerStatusRouteUiOpen(minecraft)) {
                PacketDistributor.sendToServer(new RequestOpenIncoreUiPayload(INCoreUiIds.PLAYER_STATUS));
            }
        }

        while (INCoreKeyMappings.OPEN_PARTY.consumeClick()) {
            if (minecraft.screen == null || isPlayerStatusRouteUiOpen(minecraft)) {
                PacketDistributor.sendToServer(new RequestOpenIncoreUiPayload(INCoreUiIds.PARTY_MANAGEMENT));
            }
        }

        while (INCoreKeyMappings.OPEN_TASK_OVERVIEW.consumeClick()) {
            if ((minecraft.screen == null || isPlayerStatusRouteUiOpen(minecraft))
                    && ensureFeatureUnlocked(minecraft, PlayerFeatureUnlockIds.TASKS_SCREEN)) {
                PacketDistributor.sendToServer(new RequestOpenIncoreUiPayload(INCoreUiIds.TASK_OVERVIEW));
            }
        }

        while (INCoreKeyMappings.OPEN_COMBAT_CATALOG.consumeClick()) {
            if ((minecraft.screen == null || isPlayerStatusRouteUiOpen(minecraft))
                    && ensureFeatureUnlocked(minecraft, PlayerFeatureUnlockIds.ARENA_TIER_1)) {
                PacketDistributor.sendToServer(new RequestOpenIncoreUiPayload(INCoreUiIds.COMBAT_CATALOG));
            }
        }

        while (INCoreKeyMappings.OPEN_GACHA_BANNERS.consumeClick()) {
            if ((minecraft.screen == null || isPlayerStatusRouteUiOpen(minecraft))
                    && ensureFeatureUnlocked(minecraft, PlayerFeatureUnlockIds.GACHA_BASIC)) {
                PacketDistributor.sendToServer(new RequestOpenIncoreUiPayload(INCoreUiIds.GACHA_APP));
            }
        }

        while (INCoreKeyMappings.OPEN_BATTLE_PASS.consumeClick()) {
            if ((minecraft.screen == null || isPlayerStatusRouteUiOpen(minecraft))
                    && ensureFeatureUnlocked(minecraft, PlayerFeatureUnlockIds.BATTLEPASS_SCREEN)) {
                PacketDistributor.sendToServer(new RequestOpenIncoreUiPayload(INCoreUiIds.BATTLE_PASS));
            }
        }

        while (INCoreKeyMappings.OPEN_SHOP.consumeClick()) {
            if ((minecraft.screen == null || isPlayerStatusRouteUiOpen(minecraft))
                    && ensureFeatureUnlocked(minecraft, PlayerFeatureUnlockIds.SHOP_SCREEN)) {
                PacketDistributor.sendToServer(new RequestOpenIncoreUiPayload(INCoreUiIds.SHOP_APP));
            }
        }

        if (minecraft.screen != null) {
            return;
        }

        while (INCoreKeyMappings.OPEN_RESEARCH_TREE.consumeClick()) {
            minecraft.setScreen(new ResearchTreeScreen());
            ResearchNetworking.requestSnapshot();
        }

        while (INCoreKeyMappings.OPEN_NUMISMATICS_BANK.consumeClick()) {
            NumismaticsNetworking.requestOpenBankScreen();
        }

        while (INCoreKeyMappings.OPEN_MARKET.consumeClick()) {
            if (ensureFeatureUnlocked(minecraft, PlayerFeatureUnlockIds.MARKET_BASIC)) {
                MarketNetworking.requestOpenMarketScreen();
            }
        }
    }

    private void onScreenKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getKeyCode() != GLFW.GLFW_KEY_ESCAPE
                && !minecraft.options.keyInventory.matches(event.getKeyCode(), event.getScanCode())) {
            return;
        }
        if (!(event.getScreen() instanceof ModularUIContainerScreen screen)) {
            return;
        }
        if (!(screen.getMenu().uiHolder instanceof PlayerStatusRouteUiHolder routeHolder)) {
            return;
        }
        if (routeHolder.consumeCurrentRouteEscape()) {
            event.setCanceled(true);
            return;
        }
        event.setCanceled(true);
        PacketDistributor.sendToServer(RequestBackIncoreUiPayload.INSTANCE);
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

    private static boolean isPlayerStatusRouteUiOpen(Minecraft minecraft) {
        return minecraft.player != null
                && minecraft.player.containerMenu instanceof ModularUIContainerMenu menu
                && menu.uiHolder instanceof PlayerStatusRouteUiHolder;
    }
}
