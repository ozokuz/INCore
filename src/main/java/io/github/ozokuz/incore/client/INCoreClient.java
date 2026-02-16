package io.github.ozokuz.incore.client;

import io.github.ozokuz.incore.INCore;
import io.github.ozokuz.incore.client.features.sanity.SanityBarHudFeature;
import io.github.ozokuz.incore.client.features.stamina.StaminaBarHudFeature;
import io.github.ozokuz.incore.client.status.BattlePassScreen;
import io.github.ozokuz.incore.client.status.PlayerStatusScreen;
import io.github.ozokuz.incore.client.tasks.TaskOverviewScreen;
import io.github.ozokuz.incore.features.gacha.network.GachaNetworking;
import io.github.ozokuz.incore.features.research.ManualResearchTaskManager;
import io.github.ozokuz.incore.features.research.ResearchEntryManager;
import io.github.ozokuz.incore.features.research.client.LabScreen;
import io.github.ozokuz.incore.features.research.network.ResearchNetworking;
import io.github.ozokuz.incore.Registration;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
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
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
        StaminaBarHudFeature.register();
        SanityBarHudFeature.register();
    }

    private void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(INCoreKeyMappings.OPEN_PLAYER_STATUS);
        event.register(INCoreKeyMappings.OPEN_GACHA_BANNERS);
        event.register(INCoreKeyMappings.OPEN_TASK_OVERVIEW);
        event.register(INCoreKeyMappings.OPEN_BATTLE_PASS);
        event.register(INCoreKeyMappings.OPEN_RESEARCH_TREE);
    }

    private void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(Registration.RESEARCH_LAB_MENU.get(), LabScreen::new);
    }

    private void onRegisterClientReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new ResearchEntryManager());
        event.registerReloadListener(new ManualResearchTaskManager());
    }

    private void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
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
    }
}
