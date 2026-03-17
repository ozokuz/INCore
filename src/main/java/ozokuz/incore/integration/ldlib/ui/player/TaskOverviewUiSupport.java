package ozokuz.incore.integration.ldlib.ui.player;

import com.google.gson.Gson;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.RectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar;
import java.util.Comparator;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;
import ozokuz.incore.client.ui.UIScreenTheme;
import ozokuz.incore.features.tasks.DailyTaskService;
import ozokuz.incore.features.tasks.TaskService;
import ozokuz.incore.integration.ldlib.ui.INCoreLdLibUiScaffold;
import ozokuz.incore.integration.ldlib.ui.elements.ClippedTextureProgressBar;
import ozokuz.incore.integration.ldlib.ui.texture.BeveledRectTexture;

final class TaskOverviewUiSupport {
    static final int TARGET_WINDOW_WIDTH = 560;
    static final int TARGET_WINDOW_HEIGHT = 320;
    static final int MIN_WINDOW_WIDTH = 252;
    static final int MIN_WINDOW_HEIGHT = 140;
    static final int SIDEBAR_TARGET_WIDTH = 166;
    static final int BUTTON_HEIGHT = 20;
    static final int WEEKLY_CARD_HEIGHT = 36;
    static final int TOTAL_DAILY_TASKS = 7;
    static final int REWARD_ICON_SIZE = 16;

    static final int WINDOW_FILL = UIScreenTheme.BattlepassTasks.WINDOW_FILL;
    static final int WINDOW_BORDER_LIGHT = UIScreenTheme.BattlepassTasks.WINDOW_BORDER_LIGHT;
    static final int WINDOW_BORDER_DARK = UIScreenTheme.BattlepassTasks.WINDOW_BORDER_DARK;
    static final int PANEL_FILL = UIScreenTheme.BattlepassTasks.PANEL_FILL;
    static final int PANEL_BORDER = UIScreenTheme.BattlepassTasks.BORDER_MUTED;
    static final int CARD_FILL = UIScreenTheme.BattlepassTasks.CARD_FILL_DEFAULT;
    static final int CARD_FILL_COMPLETE = UIScreenTheme.BattlepassTasks.CARD_FILL_COMPLETE;
    static final int ROW_BORDER_COMPLETE = UIScreenTheme.BattlepassTasks.ROW_BORDER_COMPLETE;
    static final int BADGE_FILL = UIScreenTheme.BattlepassTasks.BADGE_FILL_DEFAULT;
    static final int BADGE_FILL_COMPLETE = UIScreenTheme.BattlepassTasks.BADGE_FILL_COMPLETE;
    static final int BADGE_EDGE = UIScreenTheme.BattlepassTasks.BADGE_EDGE_INACTIVE;
    static final int CHIP_FILL = UIScreenTheme.BattlepassTasks.CHIP_FILL_DEFAULT;
    static final int CHIP_FILL_COMPLETE = UIScreenTheme.BattlepassTasks.CHIP_FILL_COMPLETE;
    static final int CHIP_BORDER = UIScreenTheme.BattlepassTasks.CHIP_BORDER_DEFAULT;
    static final int CHIP_BORDER_COMPLETE = UIScreenTheme.BattlepassTasks.CHIP_BORDER_COMPLETE;
    static final int TIER_FILL_UNLOCKED = UIScreenTheme.BattlepassTasks.TIER_ROW_UNLOCKED;
    static final int TIER_SLOT_FILL = UIScreenTheme.BattlepassTasks.TIER_SLOT_FILL;
    static final int TIER_SLOT_BORDER = UIScreenTheme.BattlepassTasks.TIER_SLOT_BORDER;
    static final int PROGRESS_BG = UIScreenTheme.BattlepassTasks.PROGRESS_BORDER;
    static final int PROGRESS_FILL = UIScreenTheme.BattlepassTasks.ACCENT_GOLD;
    static final int PROGRESS_FILL_COMPLETE = UIScreenTheme.BattlepassTasks.PROGRESS_FILL_COMPLETE;
    static final int TEXT_PRIMARY = UIScreenTheme.BattlepassTasks.TEXT_PRIMARY;
    static final int TEXT_SECONDARY = UIScreenTheme.BattlepassTasks.TEXT_SECONDARY;
    static final int TEXT_MUTED = UIScreenTheme.BattlepassTasks.TEXT_MUTED;
    static final int TEXT_SOFT = UIScreenTheme.BattlepassTasks.TEXT_SOFT;
    static final int TEXT_ACCENT = UIScreenTheme.BattlepassTasks.ACCENT_GOLD;
    static final int TEXT_COMPLETE = UIScreenTheme.BattlepassTasks.TASK_STATUS_COMPLETE;
    static final int TEXT_WARNING = UIScreenTheme.BattlepassTasks.TEXT_WARNING;
    static final int QUANTITY_CHIP_FILL = UIScreenTheme.BattlepassTasks.QUANTITY_CHIP_FILL;
    static final int QUANTITY_TEXT = UIScreenTheme.BattlepassTasks.QUANTITY_TEXT;

    static final IGuiTexture BUTTON_IDLE_TEXTURE = new BeveledRectTexture(
            0xFF8C8C8C,
            0xFF5F5F5F,
            0xFFB3B3B3,
            0xFF696969,
            0,
            1
    );
    static final IGuiTexture BUTTON_HOVER_TEXTURE = new BeveledRectTexture(
            0xFFA0A0A0,
            0xFF707070,
            0xFFD1D1D1,
            0xFF787878,
            0,
            1
    );
    static final IGuiTexture BUTTON_PRESSED_TEXTURE = new BeveledRectTexture(
            0xFF7D7D7D,
            0xFF555555,
            0xFF707070,
            0xFFB1B1B1,
            0,
            1
    );
    static final IGuiTexture BUTTON_DISABLED_TEXTURE = new BeveledRectTexture(
            0xFF2C2C2C,
            0xFF3F3F3F,
            0xFF535353,
            0xFF090909,
            0,
            1
    );
    static final IGuiTexture SCROLL_TRACK_TEXTURE = new BeveledRectTexture(
            UIScreenTheme.BattlepassTasks.SCROLL_TRACK_FILL,
            UIScreenTheme.BattlepassTasks.BORDER_DARK,
            UIScreenTheme.BattlepassTasks.HEADER_BORDER_TOP,
            UIScreenTheme.BattlepassTasks.HEADER_BORDER_BOTTOM,
            1,
            0
    );
    static final IGuiTexture SCROLL_THUMB_IDLE_TEXTURE = new BeveledRectTexture(
            UIScreenTheme.BattlepassTasks.SCROLL_THUMB_FILL,
            UIScreenTheme.BattlepassTasks.SCROLL_THUMB_BORDER,
            0xFFF5F5F5,
            0xFF5E6570,
            1,
            0
    );
    static final IGuiTexture SCROLL_THUMB_HOVER_TEXTURE = new BeveledRectTexture(
            0xFFD8DEE7,
            UIScreenTheme.BattlepassTasks.SCROLL_THUMB_BORDER,
            0xFFFFFFFF,
            0xFF6A7380,
            1,
            0
    );
    static final IGuiTexture SCROLL_THUMB_PRESSED_TEXTURE = new BeveledRectTexture(
            0xFFB3BBC7,
            UIScreenTheme.BattlepassTasks.SCROLL_THUMB_BORDER,
            0xFFD0D6DE,
            0xFF4B525D,
            1,
            0
    );

    private static final Gson GSON = new Gson();
    private static final Comparator<TaskService.TaskView> WEEKLY_SORT = Comparator
            .comparingInt(TaskService.TaskView::points)
            .reversed()
            .thenComparing(entry -> entry.title() == null ? "" : entry.title(), String.CASE_INSENSITIVE_ORDER);

    private TaskOverviewUiSupport() {
    }

    static TaskOverviewSnapshot parseSnapshot(@Nullable String json) {
        if (json == null || json.isBlank()) {
            return TaskOverviewSnapshot.EMPTY;
        }
        try {
            TaskOverviewSnapshot parsed = GSON.fromJson(json, TaskOverviewSnapshot.class);
            return normalize(parsed);
        } catch (Exception ignored) {
            return TaskOverviewSnapshot.EMPTY;
        }
    }

    private static TaskOverviewSnapshot normalize(@Nullable TaskOverviewSnapshot parsed) {
        if (parsed == null) {
            return TaskOverviewSnapshot.EMPTY;
        }
        return new TaskOverviewSnapshot(
                parsed.weekly() == null ? List.of() : parsed.weekly(),
                Math.max(0, parsed.weeklyPoints()),
                parsed.dailyRewards() == null ? List.of() : parsed.dailyRewards(),
                parsed.tiers() == null ? List.of() : parsed.tiers(),
                parsed.fixedDailyTasks() == null ? List.of() : parsed.fixedDailyTasks(),
                Math.max(0, parsed.fixedDailyCompleted()),
                parsed.fixedDailyAllCompleted(),
                parsed.fixedDailyRewardClaimed()
        );
    }

    static List<DailyTaskService.DailyTaskView> sortedDailyTasks(List<DailyTaskService.DailyTaskView> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return List.of();
        }
        return tasks.stream()
                .sorted(Comparator.comparing(task -> task.progress() >= task.goal()))
                .toList();
    }

    static List<TaskService.TaskView> sortedWeeklyTasks(List<TaskService.TaskView> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return List.of();
        }
        return tasks.stream().sorted(WEEKLY_SORT).toList();
    }

    static int countClaimableWeeklyTiers(TaskOverviewSnapshot snapshot) {
        int claimable = 0;
        for (TaskService.TierView tier : snapshot.tiers()) {
            if (tier.unlocked() && !tier.claimed()) {
                claimable++;
            }
        }
        return claimable;
    }

    static ItemStack iconStackFor(TaskService.RewardView reward) {
        if (reward == null) {
            return Items.BARRIER.getDefaultInstance();
        }

        String kind = reward.kind() == null ? "" : reward.kind();
        ResourceLocation itemId = ResourceLocation.tryParse(reward.itemId());
        Item item = switch (kind) {
            case "item" -> itemId != null ? BuiltInRegistries.ITEM.get(itemId) : Items.AIR;
            case "entropy" -> {
                ResourceLocation entropyItem = ResourceLocation.tryParse("incore:entropy_vessel");
                yield entropyItem != null ? BuiltInRegistries.ITEM.get(entropyItem) : Items.EXPERIENCE_BOTTLE;
            }
            case "command" -> Items.COMMAND_BLOCK;
            default -> itemId != null ? BuiltInRegistries.ITEM.get(itemId) : Items.AIR;
        };
        if (item == Items.AIR) {
            item = "command".equals(kind) ? Items.COMMAND_BLOCK : Items.BARRIER;
        }
        return new ItemStack(item, 1);
    }

    static int displayAmount(TaskService.RewardView reward) {
        if (reward == null) {
            return 1;
        }
        return switch (reward.kind() == null ? "" : reward.kind()) {
            case "item", "entropy" -> Math.max(1, reward.amount());
            default -> 1;
        };
    }

    static Component[] tooltipForReward(TaskService.RewardView reward, ItemStack stack) {
        if (reward == null) {
            return new Component[]{Component.translatable("screen.incore.tasks.tooltip_unknown_title")};
        }

        String kind = reward.kind() == null ? "" : reward.kind();
        if ("item".equals(kind)) {
            return displayAmount(reward) > 1
                    ? new Component[]{stack.getHoverName(), Component.literal("x" + displayAmount(reward)).withStyle(ChatFormatting.GRAY)}
                    : new Component[]{stack.getHoverName()};
        }
        if ("entropy".equals(kind)) {
            return new Component[]{
                    Component.translatable("screen.incore.tasks.tooltip_entropy_title"),
                    Component.translatable("screen.incore.tasks.tooltip_entropy_line", displayAmount(reward)).withStyle(ChatFormatting.GRAY)
            };
        }
        if ("command".equals(kind)) {
            return new Component[]{
                    Component.translatable("screen.incore.tasks.tooltip_command_title"),
                    (reward.text() == null || reward.text().isBlank()
                            ? Component.translatable("screen.incore.tasks.tooltip_command_empty")
                            : Component.literal(reward.text()))
                            .withStyle(ChatFormatting.GRAY)
            };
        }
        return reward.text() == null || reward.text().isBlank()
                ? new Component[]{Component.translatable("screen.incore.tasks.tooltip_unknown_title")}
                : new Component[]{
                        Component.translatable("screen.incore.tasks.tooltip_unknown_title"),
                        Component.literal(reward.text()).withStyle(ChatFormatting.GRAY)
                };
    }

    static ProgressBar progressBar(int backgroundColor, int fillColor, int height) {
        ProgressBar progressBar = new ClippedTextureProgressBar(
                RectTexture.of(backgroundColor),
                RectTexture.of(fillColor)
        ).setRange(0.0F, 1.0F);
        progressBar.layout(layout -> {
            layout.widthPercent(100);
            layout.height(height);
        });
        return progressBar;
    }

    static Button createButton(Component text, int width, boolean active) {
        Button button = new Button().setText(text);
        button.layout(layout -> {
            layout.width(width);
            layout.height(BUTTON_HEIGHT);
            layout.alignItems(dev.vfyjxf.taffy.style.AlignItems.CENTER);
            layout.justifyContent(dev.vfyjxf.taffy.style.AlignContent.CENTER);
        });
        button.text.getLayout().flex(1);
        button.text.getLayout().heightPercent(100);
        button.textStyle(style -> style
                .adaptiveWidth(false)
                .textWrap(TextWrap.HIDE)
                .textColor(active ? TEXT_PRIMARY : TEXT_MUTED)
        );
        button.buttonStyle(style -> style
                .baseTexture(active ? BUTTON_IDLE_TEXTURE : BUTTON_DISABLED_TEXTURE)
                .hoverTexture(active ? BUTTON_HOVER_TEXTURE : BUTTON_DISABLED_TEXTURE)
                .pressedTexture(active ? BUTTON_PRESSED_TEXTURE : BUTTON_DISABLED_TEXTURE)
        );
        button.setActive(active);
        return button;
    }

    static Label lineLabel(Component text, int color) {
        Label label = INCoreLdLibUiScaffold.wrappedLabel(text);
        label.textStyle(style -> style.textColor(color).textWrap(TextWrap.HIDE));
        return label;
    }

    record TaskOverviewSnapshot(
            List<TaskService.TaskView> weekly,
            int weeklyPoints,
            List<TaskService.RewardView> dailyRewards,
            List<TaskService.TierView> tiers,
            List<DailyTaskService.DailyTaskView> fixedDailyTasks,
            int fixedDailyCompleted,
            boolean fixedDailyAllCompleted,
            boolean fixedDailyRewardClaimed
    ) {
        static final TaskOverviewSnapshot EMPTY = new TaskOverviewSnapshot(
                List.of(),
                0,
                List.of(),
                List.of(),
                List.of(),
                0,
                false,
                false
        );
    }
}
