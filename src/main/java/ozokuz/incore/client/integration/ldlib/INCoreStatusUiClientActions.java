package ozokuz.incore.client.integration.ldlib;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerMenu;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import ozokuz.incore.client.INCoreKeyMappings;
import ozokuz.incore.client.features.battlepass.BattlePassScreen;
import ozokuz.incore.client.features.research.ResearchTreeScreen;
import ozokuz.incore.client.features.status.StatusScreenReturnTracker;
import ozokuz.incore.features.market.network.MarketNetworking;
import ozokuz.incore.features.numismatics.network.NumismaticsNetworking;
import ozokuz.incore.features.playerlevel.PlayerFeatureUnlockIds;
import ozokuz.incore.features.playerlevel.network.PlayerLevelClientCache;
import ozokuz.incore.features.research.network.ResearchNetworking;
import ozokuz.incore.features.shop.network.ShopNetworking;
import ozokuz.incore.integration.ldlib.ui.player.PlayerStatusRouteUiHolder;
import ozokuz.incore.integration.ldlib.ui.player.PlayerStatusAction;

@OnlyIn(Dist.CLIENT)
public final class INCoreStatusUiClientActions {
    private INCoreStatusUiClientActions() {
    }

    public static void bindAction(Button button, PlayerStatusAction action, Component label) {
        button.setOnClick(event -> runAction(action));
        button.style(style -> style.tooltips(tooltipLines(label, keyMappingFor(action))));
    }

    private static Component[] tooltipLines(Component label, KeyMapping keyMapping) {
        if (keyMapping == null) {
            return new Component[]{label};
        }
        return new Component[]{
                label,
                Component.translatable("screen.incore.player_status.quick_nav_key", keyMapping.getTranslatedKeyMessage())
                        .withStyle(ChatFormatting.GRAY)
        };
    }

    private static void prepareParent(Screen parent) {
        if (parent != null) {
            StatusScreenReturnTracker.prepare(parent);
        }
    }

    private static Screen legacyReturnTarget() {
        return isPlayerStatusRouteOpen() ? new ReturnToPlayerStatusScreen() : currentScreen();
    }

    private static void prepareExternalReturnIfNeeded() {
        if (!isPlayerStatusRouteOpen()) {
            return;
        }
        StatusScreenReturnTracker.prepareExternal(new ReturnToPlayerStatusScreen());
        closePlayerStatusRouteIfOpen();
    }

    private static void runAction(PlayerStatusAction action) {
        switch (action) {
            case RESEARCH -> {
                Screen parent = legacyReturnTarget();
                closePlayerStatusRouteIfOpen();
                openScreen(new ResearchTreeScreen(parent));
                ResearchNetworking.requestSnapshot();
            }
            case FTB_QUESTS -> {
                prepareExternalReturnIfNeeded();
                invokeStaticNoArgs("dev.ftb.mods.ftbquests.client.FTBQuestsClient", "openGui");
            }
            case BATTLE_PASS -> {
                if (ensureFeatureUnlocked(PlayerFeatureUnlockIds.BATTLEPASS_SCREEN)) {
                    Screen parent = legacyReturnTarget();
                    closePlayerStatusRouteIfOpen();
                    openScreen(new BattlePassScreen(parent));
                }
            }
            case MARKET -> {
                if (ensureFeatureUnlocked(PlayerFeatureUnlockIds.MARKET_BASIC)) {
                    Screen parent = legacyReturnTarget();
                    closePlayerStatusRouteIfOpen();
                    prepareParent(parent);
                    MarketNetworking.requestOpenMarketScreen();
                }
            }
            case SHOP -> {
                if (ensureFeatureUnlocked(PlayerFeatureUnlockIds.SHOP_SCREEN)) {
                    Screen parent = legacyReturnTarget();
                    closePlayerStatusRouteIfOpen();
                    prepareParent(parent);
                    ShopNetworking.requestOpenShopScreen();
                }
            }
            case FTB_TEAMS -> {
                prepareExternalReturnIfNeeded();
                invokeStaticNoArgs("dev.ftb.mods.ftbteams.net.OpenGUIMessage", "sendToServer");
            }
            case NUMISMATICS -> {
                Screen parent = legacyReturnTarget();
                closePlayerStatusRouteIfOpen();
                if (parent != null) {
                    StatusScreenReturnTracker.prepareExternal(parent);
                }
                NumismaticsNetworking.requestOpenBankScreen();
            }
            default -> {
            }
        }
    }

    private static KeyMapping keyMappingFor(PlayerStatusAction action) {
        return switch (action) {
            case GACHA -> INCoreKeyMappings.OPEN_GACHA_BANNERS;
            case TASKS -> INCoreKeyMappings.OPEN_TASK_OVERVIEW;
            case RESEARCH -> INCoreKeyMappings.OPEN_RESEARCH_TREE;
            case BATTLE_PASS -> INCoreKeyMappings.OPEN_BATTLE_PASS;
            case MARKET -> INCoreKeyMappings.OPEN_MARKET;
            case SHOP -> INCoreKeyMappings.OPEN_SHOP;
            case NUMISMATICS -> INCoreKeyMappings.OPEN_NUMISMATICS_BANK;
            case PARTY -> INCoreKeyMappings.OPEN_PARTY;
            default -> null;
        };
    }

    private static void openScreen(Screen screen) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null) {
            minecraft.setScreen(screen);
        }
    }

    private static Screen currentScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft == null ? null : minecraft.screen;
    }

    private static boolean ensureFeatureUnlocked(ResourceLocation featureId) {
        Minecraft minecraft = Minecraft.getInstance();
        String rawId = featureId.toString();
        if (PlayerLevelClientCache.isFeatureUnlocked(rawId)) {
            return true;
        }
        if (minecraft != null && minecraft.player != null) {
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

    private static boolean isPlayerStatusRouteOpen() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft != null
                && minecraft.player != null
                && minecraft.player.containerMenu instanceof ModularUIContainerMenu menu
                && menu.uiHolder instanceof PlayerStatusRouteUiHolder;
    }

    private static void closePlayerStatusRouteIfOpen() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        if (minecraft.player.containerMenu instanceof ModularUIContainerMenu menu
                && menu.uiHolder instanceof PlayerStatusRouteUiHolder) {
            minecraft.player.closeContainer();
        }
    }

    private static void invokeStaticNoArgs(String className, String methodName) {
        try {
            Class.forName(className).getMethod(methodName).invoke(null);
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
