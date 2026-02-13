package io.github.ozokuz.incore.client;

import io.github.ozokuz.incore.INCore;
import io.github.ozokuz.incore.Registration;
import io.github.ozokuz.incore.client.status.PlayerStatusScreen;
import io.github.ozokuz.incore.features.sanity.SanityClientCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = INCore.MODID, dist = Dist.CLIENT)
public class INCoreClient {
    public INCoreClient(IEventBus modEventBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modEventBus.addListener(this::onRegisterKeyMappings);
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
        NeoForge.EVENT_BUS.addListener(this::onRenderGui);
    }

    private void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(INCoreKeyMappings.OPEN_PLAYER_STATUS);
    }

    private void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }

        while (INCoreKeyMappings.OPEN_PLAYER_STATUS.consumeClick()) {
            minecraft.setScreen(new PlayerStatusScreen());
        }
    }

    private void onRenderGui(RenderGuiEvent.Post event) {
        int cap = SanityClientCache.getCap();
        if (cap <= 0 || SanityClientCache.getCurrent() < cap) {
            return;
        }

        GuiGraphics guiGraphics = event.getGuiGraphics();
        int centerX = guiGraphics.guiWidth() / 2;
        int x = centerX + 98;
        int y = guiGraphics.guiHeight() - 33;

        ItemStack icon = Registration.SANITY_VESSEL_ITEM.get().getDefaultInstance();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0);
        guiGraphics.pose().scale(0.75F, 0.75F, 1.0F);
        guiGraphics.renderItem(icon, 0, 0);
        guiGraphics.pose().popPose();
    }
}
