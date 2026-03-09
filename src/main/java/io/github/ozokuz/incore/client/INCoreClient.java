package io.github.ozokuz.incore.client;

import io.github.ozokuz.incore.INCore;
import io.github.ozokuz.incore.Registration;
import io.github.ozokuz.incore.client.features.roguelike.DungeonAltarRenderer;
import io.github.ozokuz.incore.client.features.roguelike.DungeonCrystalModificationStationScreen;
import io.github.ozokuz.incore.client.features.roguelike.RoguelikePortalRenderer;
import io.github.ozokuz.incore.client.features.roguelike.RoguelikeMinimapHudFeature;
import io.github.ozokuz.incore.client.features.party.PartyHudFeature;
import io.github.ozokuz.incore.client.features.entropy.EntropyBarHudFeature;
import io.github.ozokuz.incore.client.features.stamina.StaminaBarHudFeature;
import io.github.ozokuz.incore.client.features.party.PartyManagementScreen;
import io.github.ozokuz.incore.client.features.battlepass.BattlePassScreen;
import io.github.ozokuz.incore.client.features.status.PlayerStatusScreen;
import io.github.ozokuz.incore.client.features.tasks.TaskOverviewScreen;
import io.github.ozokuz.incore.features.arena.network.ArenaNetworking;
import io.github.ozokuz.incore.client.features.cards.CardDeckStationScreen;
import io.github.ozokuz.incore.features.gacha.network.GachaNetworking;
import io.github.ozokuz.incore.client.features.market.MarketAutoTraderScreen;
import io.github.ozokuz.incore.client.features.market.MarketTerminalCardScreen;
import io.github.ozokuz.incore.client.features.market.ShipmentTerminalScreen;
import io.github.ozokuz.incore.features.market.network.MarketNetworking;
import io.github.ozokuz.incore.features.numismatics.network.NumismaticsNetworking;
import io.github.ozokuz.incore.features.research.ManualResearchTaskManager;
import io.github.ozokuz.incore.features.research.ResearchEntryManager;
import io.github.ozokuz.incore.features.research.ResearchMaterialManager;
import io.github.ozokuz.incore.features.research.ResearchRecipeLockManager;
import io.github.ozokuz.incore.features.research.client.LabScreen;
import io.github.ozokuz.incore.features.research.client.ResearchRecipeLockClientCache;
import io.github.ozokuz.incore.features.research.network.ResearchNetworking;
import io.github.ozokuz.incore.features.shop.network.ShopNetworking;
import net.minecraft.client.Minecraft;
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
    private boolean hadClientPlayer;

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
        event.register(Registration.BURNER_LAB_MENU.get(), LabScreen::new);
        event.register(Registration.DECK_STATION_MENU.get(), CardDeckStationScreen::new);
        event.register(Registration.RESEARCH_LAB_MENU.get(), LabScreen::new);
        event.register(Registration.MARKET_TERMINAL_CARD_MENU.get(), MarketTerminalCardScreen::new);
        event.register(Registration.SHIPMENT_TERMINAL_MENU.get(), ShipmentTerminalScreen::new);
        event.register(Registration.MARKET_AUTOTRADER_MENU.get(), MarketAutoTraderScreen::new);
        event.register(Registration.DUNGEON_CRYSTAL_MODIFICATION_STATION_MENU.get(), DungeonCrystalModificationStationScreen::new);
    }

    private void onRegisterClientReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new ResearchMaterialManager());
        event.registerReloadListener(new ResearchRecipeLockManager());
        event.registerReloadListener(new ResearchEntryManager());
        event.registerReloadListener(new ManualResearchTaskManager());
    }

    private void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Registration.DUNGEON_ALTAR_BE.get(), DungeonAltarRenderer::new);
        event.registerBlockEntityRenderer(Registration.ROGUELIKE_PORTAL_BE.get(), RoguelikePortalRenderer::new);
    }

    private void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean hasPlayer = minecraft.player != null;
        if (hasPlayer && !hadClientPlayer) {
            ResearchRecipeLockClientCache.onWorldJoined();
        }
        hadClientPlayer = hasPlayer;

        if (!hasPlayer || minecraft.screen != null) {
            return;
        }

        while (INCoreKeyMappings.OPEN_PLAYER_STATUS.consumeClick()) {
            minecraft.setScreen(new PlayerStatusScreen());
        }

        while (INCoreKeyMappings.OPEN_GACHA_BANNERS.consumeClick()) {
            GachaNetworking.requestOpenBannerScreen();
        }

        while (INCoreKeyMappings.OPEN_TASK_OVERVIEW.consumeClick()) {
            minecraft.setScreen(new TaskOverviewScreen());
        }

        while (INCoreKeyMappings.OPEN_BATTLE_PASS.consumeClick()) {
            minecraft.setScreen(new BattlePassScreen(null));
        }

        while (INCoreKeyMappings.OPEN_RESEARCH_TREE.consumeClick()) {
            ResearchNetworking.requestOpen();
        }

        while (INCoreKeyMappings.OPEN_COMBAT_CATALOG.consumeClick()) {
            ArenaNetworking.requestOpenCatalog();
        }

        while (INCoreKeyMappings.OPEN_NUMISMATICS_BANK.consumeClick()) {
            NumismaticsNetworking.requestOpenBankScreen();
        }

        while (INCoreKeyMappings.OPEN_MARKET.consumeClick()) {
            MarketNetworking.requestOpenMarketScreen();
        }

        while (INCoreKeyMappings.OPEN_SHOP.consumeClick()) {
            ShopNetworking.requestOpenShopScreen();
        }

        while (INCoreKeyMappings.OPEN_PARTY.consumeClick()) {
            minecraft.setScreen(new PartyManagementScreen());
        }
    }
}
