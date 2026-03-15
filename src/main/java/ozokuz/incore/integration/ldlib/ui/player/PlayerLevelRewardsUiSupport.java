package ozokuz.incore.integration.ldlib.ui.player;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import ozokuz.incore.client.ui.UIScreenTheme;
import ozokuz.incore.client.ui.render.ThemedUi;
import ozokuz.incore.features.playerlevel.network.PlayerLevelClientCache;
import ozokuz.incore.features.playerlevel.network.PlayerLevelSyncPayload;

final class PlayerLevelRewardsUiSupport {
    static final int TARGET_WINDOW_WIDTH = 660;
    static final int TARGET_WINDOW_HEIGHT = 368;
    static final int MIN_WINDOW_WIDTH = 440;
    static final int MIN_WINDOW_HEIGHT = 300;
    static final int HERO_HEIGHT = 82;
    static final int SIDEBAR_TARGET_WIDTH = 220;
    static final int LEVEL_CARD_HEIGHT = 30;
    static final int SCROLLBAR_WIDTH = 6;
    static final int SCROLLBAR_GAP = 3;
    static final int MIN_SCROLLBAR_THUMB_HEIGHT = 12;
    static final int REWARD_CARD_SIZE = 38;
    static final int REWARD_CARD_GAP = 8;
    static final int XP_BAR_HEIGHT = 5;
    static final SpriteTexture XP_BAR_BACKGROUND_TEXTURE = SpriteTexture.of(
            ResourceLocation.fromNamespaceAndPath("incore", "textures/gui/sprites/hud/experience_bar_background_white.png")
    );
    static final SpriteTexture XP_BAR_PROGRESS_TEXTURE = SpriteTexture.of(
            ResourceLocation.fromNamespaceAndPath("incore", "textures/gui/sprites/hud/experience_bar_progress_white.png")
    );

    static final int COLOR_TEXT_PRIMARY = UIScreenTheme.Info.TITLE_TEXT;
    static final int COLOR_TEXT_SECONDARY = UIScreenTheme.Info.PLR_TEXT_SECONDARY;
    static final int COLOR_TEXT_MUTED = UIScreenTheme.Info.PLR_TEXT_MUTED;

    private PlayerLevelRewardsUiSupport() {
    }

    static Font font() {
        return Minecraft.getInstance().font;
    }

    static ThemedUi themed(GuiGraphics guiGraphics) {
        return new ThemedUi(guiGraphics, font(), UIScreenTheme.INFO.theme());
    }

    static ItemStack iconStackFor(PlayerLevelClientCache.RewardEntry reward) {
        ResourceLocation itemId = ResourceLocation.tryParse(reward.iconItemId());
        Item item = itemId != null ? BuiltInRegistries.ITEM.get(itemId) : Items.AIR;
        if (item == Items.AIR) {
            item = Items.BARRIER;
        }

        int count = reward.kind() == PlayerLevelSyncPayload.REWARD_KIND_ITEM ? Math.max(1, reward.amount()) : 1;
        return new ItemStack(item, Math.min(99, count));
    }

    static List<Component> tooltipForReward(PlayerLevelClientCache.RewardEntry reward, ItemStack iconStack) {
        if (reward.kind() == PlayerLevelSyncPayload.REWARD_KIND_ITEM) {
            return Screen.getTooltipFromItem(Minecraft.getInstance(), iconStack);
        }
        return tooltipForNonItemReward(reward);
    }

    static List<Component> tooltipForNonItemReward(PlayerLevelClientCache.RewardEntry reward) {
        List<Component> lines = new ArrayList<>();
        if (reward.kind() == PlayerLevelSyncPayload.REWARD_KIND_ENTROPY_CAP) {
            lines.add(Component.translatable("screen.incore.player_level_rewards.tooltip_entropy_cap_title"));
            lines.add(Component.translatable("screen.incore.player_level_rewards.tooltip_entropy_cap", reward.amount()).withStyle(ChatFormatting.GRAY));
            if (!reward.text().isBlank()) {
                lines.add(Component.literal(reward.text()).withStyle(ChatFormatting.DARK_GRAY));
            }
            return lines;
        }
        if (reward.kind() == PlayerLevelSyncPayload.REWARD_KIND_COMMAND) {
            lines.add(Component.translatable("screen.incore.player_level_rewards.tooltip_command_title"));
            lines.add(Component.translatable("screen.incore.player_level_rewards.tooltip_command", reward.text()).withStyle(ChatFormatting.GRAY));
            return lines;
        }
        if (reward.kind() == PlayerLevelSyncPayload.REWARD_KIND_FEATURE_UNLOCK) {
            lines.add(Component.translatable("screen.incore.player_level_rewards.tooltip_feature_unlock_title"));
            lines.add(Component.translatable("screen.incore.player_level_rewards.tooltip_feature_unlock", reward.text(), reward.amount()).withStyle(ChatFormatting.GRAY));
            return lines;
        }

        lines.add(Component.translatable("screen.incore.player_level_rewards.tooltip_other_title"));
        lines.add(Component.literal(reward.text()).withStyle(ChatFormatting.GRAY));
        return lines;
    }

    static int rewardCardFill(int kind) {
        return switch (kind) {
            case PlayerLevelSyncPayload.REWARD_KIND_ENTROPY_CAP -> UIScreenTheme.Info.PLR_REWARD_ENTROPY_FILL;
            case PlayerLevelSyncPayload.REWARD_KIND_COMMAND -> UIScreenTheme.Info.PLR_REWARD_COMMAND_FILL;
            case PlayerLevelSyncPayload.REWARD_KIND_FEATURE_UNLOCK -> UIScreenTheme.Info.PLR_REWARD_FEATURE_FILL;
            default -> UIScreenTheme.Info.PLR_REWARD_DEFAULT_FILL;
        };
    }

    static int withAlpha(int rgb, int alpha) {
        int clamped = Math.clamp(alpha, 0, 255);
        return (clamped << 24) | (rgb & UIScreenTheme.Info.RGB_MASK);
    }

    static int brighten(int color, int amount) {
        int a = (color >>> 24) & 0xFF;
        int r = Math.clamp(((color >>> 16) & 0xFF) + amount, 0, 255);
        int g = Math.clamp(((color >>> 8) & 0xFF) + amount, 0, 255);
        int b = Math.clamp((color & 0xFF) + amount, 0, 255);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
