package ozokuz.incore.integration.ldlib.ui.player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import ozokuz.incore.client.ui.UIScreenTheme;
import ozokuz.incore.client.ui.render.ThemedUi;
import ozokuz.incore.features.battlepass.network.BattlePassClientCache;
import ozokuz.incore.features.battlepass.network.BattlePassSyncPayload;

final class BattlePassUiSupport {
    static final int TARGET_WINDOW_WIDTH = 700;
    static final int TARGET_WINDOW_HEIGHT = 360;
    static final int MIN_WINDOW_WIDTH = 460;
    static final int MIN_WINDOW_HEIGHT = 280;

    static final int HEADER_HEIGHT = 74;
    static final int REWARD_CARD_WIDTH = 48;
    static final int REWARD_CARD_HEIGHT = 42;
    static final int REWARD_CARD_GAP = 5;
    static final int MISSION_CATEGORY_WIDTH = 112;
    static final int MISSION_CATEGORY_ROW_HEIGHT = 32;
    static final int MISSION_CATEGORY_GAP = 4;
    static final int MISSION_ROW_HEIGHT = 44;
    static final int XP_BAR_HEIGHT = 5;
    static final int XP_BAR_TEXTURE_WIDTH = 182;
    static final int XP_BAR_TEXTURE_HEIGHT = 5;
    static final int XP_BAR_CAP_WIDTH = 2;

    static final ResourceLocation XP_BAR_BACKGROUND = ResourceLocation.parse("incore:hud/experience_bar_background_white");
    static final ResourceLocation XP_BAR_PROGRESS = ResourceLocation.parse("incore:hud/experience_bar_progress_white");

    private BattlePassUiSupport() {
    }

    static Font font() {
        return Minecraft.getInstance().font;
    }

    static ThemedUi themed(GuiGraphics guiGraphics) {
        return new ThemedUi(guiGraphics, font(), UIScreenTheme.BATTLEPASS_TASKS.theme());
    }

    static List<BattlePassClientCache.RewardLevelEntry> orderedRewardLevels() {
        return BattlePassClientCache.getRewardLevels().stream()
                .sorted(Comparator.comparingInt(BattlePassClientCache.RewardLevelEntry::level))
                .toList();
    }

    static List<BattlePassClientCache.LaneEntry> rewardLanes() {
        List<BattlePassClientCache.LaneEntry> lanes = BattlePassClientCache.getLanes();
        if (!lanes.isEmpty()) {
            return lanes;
        }

        return List.of(
                new BattlePassClientCache.LaneEntry("basic", "Basic Core", true, -1),
                new BattlePassClientCache.LaneEntry("northium", "Northium Core", true, -1),
                new BattlePassClientCache.LaneEntry("integrated", "Integrated Core", true, -1)
        );
    }

    static RewardTrackLayout buildRewardTrackLayout(
            List<BattlePassClientCache.RewardLevelEntry> levels,
            int columns,
            int requestedScroll,
            boolean autoFocus
    ) {
        int visibleColumns = Math.max(1, columns);
        if (levels.isEmpty()) {
            return new RewardTrackLayout(List.of(), 0, 0);
        }

        int currentLevel = Math.max(0, BattlePassClientCache.getLevel());
        int currentIndex = resolveLevelIndex(levels, currentLevel);
        int highestIndex = levels.size() - 1;
        if (visibleColumns == 1) {
            return new RewardTrackLayout(List.of(highestIndex), 0, 0);
        }

        List<Integer> nonHighestIndices = new ArrayList<>();
        for (int i = 0; i < levels.size(); i++) {
            if (i != highestIndex) {
                nonHighestIndices.add(i);
            }
        }

        int baseSlots = Math.max(0, visibleColumns - 1);
        int baseMaxScroll = Math.max(0, nonHighestIndices.size() - baseSlots);
        int baseScroll = clamp(requestedScroll, 0, baseMaxScroll);

        if (autoFocus && baseSlots > 0 && !nonHighestIndices.isEmpty()) {
            int targetPos = baseMaxScroll;
            for (int i = 0; i < nonHighestIndices.size(); i++) {
                int level = levels.get(nonHighestIndices.get(i)).level();
                if (level > currentLevel) {
                    targetPos = i;
                    break;
                }
            }
            baseScroll = clamp(targetPos, 0, baseMaxScroll);
        }

        List<Integer> baseVisibleIndices = new ArrayList<>(baseSlots);
        for (int i = 0; i < baseSlots; i++) {
            int sourceIndex = baseScroll + i;
            if (sourceIndex >= nonHighestIndices.size()) {
                break;
            }
            baseVisibleIndices.add(nonHighestIndices.get(sourceIndex));
        }

        boolean hasSmallerVisible = baseVisibleIndices.stream().anyMatch(index -> index < currentIndex);
        boolean stickyCurrentLeft = currentIndex != highestIndex && !hasSmallerVisible && visibleColumns > 1;
        if (stickyCurrentLeft) {
            int dynamicSlots = Math.max(0, visibleColumns - 2);
            List<Integer> dynamicIndices = new ArrayList<>();
            for (int index : nonHighestIndices) {
                if (index != currentIndex) {
                    dynamicIndices.add(index);
                }
            }

            int maxScroll = Math.max(0, dynamicIndices.size() - dynamicSlots);
            int scroll = clamp(requestedScroll, 0, maxScroll);
            if (autoFocus && dynamicSlots > 0 && !dynamicIndices.isEmpty()) {
                int targetPos = maxScroll;
                for (int i = 0; i < dynamicIndices.size(); i++) {
                    int level = levels.get(dynamicIndices.get(i)).level();
                    if (level > currentLevel) {
                        targetPos = i;
                        break;
                    }
                }
                scroll = clamp(targetPos, 0, maxScroll);
            }

            List<Integer> visibleLevelIndices = new ArrayList<>(visibleColumns);
            visibleLevelIndices.add(currentIndex);
            for (int i = 0; i < dynamicSlots; i++) {
                int sourceIndex = scroll + i;
                if (sourceIndex >= dynamicIndices.size()) {
                    break;
                }
                visibleLevelIndices.add(dynamicIndices.get(sourceIndex));
            }
            visibleLevelIndices.add(highestIndex);
            return new RewardTrackLayout(visibleLevelIndices, scroll, maxScroll);
        }

        List<Integer> visibleLevelIndices = new ArrayList<>(visibleColumns);
        visibleLevelIndices.addAll(baseVisibleIndices);
        visibleLevelIndices.add(highestIndex);
        return new RewardTrackLayout(visibleLevelIndices, baseScroll, baseMaxScroll);
    }

    static int resolveLevelIndex(List<BattlePassClientCache.RewardLevelEntry> levels, int targetLevel) {
        if (levels.isEmpty()) {
            return 0;
        }

        int fallback = 0;
        for (int i = 0; i < levels.size(); i++) {
            int level = levels.get(i).level();
            if (level == targetLevel) {
                return i;
            }
            if (level < targetLevel) {
                fallback = i;
            } else if (level > targetLevel) {
                return i;
            }
        }
        return fallback;
    }

    static List<MissionCategory> buildMissionCategories(List<BattlePassClientCache.TaskEntry> tasks, int currentWeek) {
        List<MissionCategory> categories = new ArrayList<>();
        tasks.stream()
                .filter(BattlePassClientCache.TaskEntry::weekly)
                .map(BattlePassClientCache.TaskEntry::week)
                .distinct()
                .sorted()
                .forEach(week -> categories.add(MissionCategory.forWeek(week, categoryProgress(tasks, true, week, currentWeek))));

        if (tasks.stream().anyMatch(task -> !task.weekly())) {
            categories.add(MissionCategory.permanent(categoryProgress(tasks, false, 0, currentWeek)));
        }
        return categories;
    }

    static List<BattlePassClientCache.TaskEntry> tasksForCategory(
            List<BattlePassClientCache.TaskEntry> tasks,
            MissionCategory category
    ) {
        return tasks.stream()
                .filter(category::matches)
                .sorted(Comparator
                        .comparingInt(BattlePassClientCache.TaskEntry::week)
                        .thenComparing(BattlePassClientCache.TaskEntry::id))
                .toList();
    }

    static int tierColor(String tier) {
        if (tier == null) {
            return UIScreenTheme.BattlepassTasks.LEAGUE_DEFAULT;
        }

        return switch (tier.toLowerCase(Locale.ROOT)) {
            case "bronze" -> UIScreenTheme.BattlepassTasks.LEAGUE_BRONZE;
            case "silver" -> UIScreenTheme.BattlepassTasks.LEAGUE_SILVER;
            case "gold" -> UIScreenTheme.BattlepassTasks.LEAGUE_GOLD;
            case "platinum" -> UIScreenTheme.BattlepassTasks.LEAGUE_PLATINUM;
            case "diamond" -> UIScreenTheme.BattlepassTasks.LEAGUE_DIAMOND;
            default -> UIScreenTheme.BattlepassTasks.LEAGUE_DEFAULT;
        };
    }

    static String capitalize(String text) {
        if (text == null || text.isBlank()) {
            return "Unknown";
        }

        String lower = text.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    static String truncate(String text, int maxWidth) {
        if (font().width(text) <= maxWidth) {
            return text;
        }

        String ellipsis = "...";
        int available = Math.max(0, maxWidth - font().width(ellipsis));
        return font().plainSubstrByWidth(text, available) + ellipsis;
    }

    static Component localizeSetId(String setId) {
        ResourceLocation id = ResourceLocation.tryParse(setId);
        if (id == null) {
            return Component.literal(simplifySetId(setId));
        }

        String translationKey = "battlepass_set." + id.getNamespace() + "." + id.getPath().replace('/', '.');
        if (I18n.exists(translationKey)) {
            return Component.translatable(translationKey);
        }

        return Component.literal(simplifySetId(setId));
    }

    static String formatTimeLeft(long endsAtMillis) {
        long remainingMillis = endsAtMillis - BattlePassClientCache.getServerNowMillis();
        if (remainingMillis <= 0L) {
            return Component.translatable("screen.incore.battle_pass.ended").getString();
        }
        return formatDuration(remainingMillis, true);
    }

    static String formatDuration(long millis, boolean roundUp) {
        long totalSeconds = roundUp
                ? Math.max(0L, (millis + 999L) / 1000L)
                : Math.max(0L, millis / 1000L);
        long weeks = totalSeconds / 604800L;
        long days = (totalSeconds % 604800L) / 86400L;
        long hours = (totalSeconds % 86400L) / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;

        if (weeks > 0L) {
            return days > 0L
                    ? String.format(Locale.ROOT, "%dw %dd", weeks, days)
                    : String.format(Locale.ROOT, "%dw", weeks);
        }
        if (days > 0L) {
            return hours > 0L
                    ? String.format(Locale.ROOT, "%dd %02dh", days, hours)
                    : String.format(Locale.ROOT, "%dd", days);
        }
        if (hours > 0L) {
            return minutes > 0L
                    ? String.format(Locale.ROOT, "%02dh %02dm", hours, minutes)
                    : String.format(Locale.ROOT, "%02dh", hours);
        }

        long displayMinutes = roundUp
                ? Math.max(1L, (millis + 59999L) / 60000L)
                : millis / 60000L;
        return displayMinutes + "m";
    }

    static ItemStack iconForReward(BattlePassClientCache.RewardEntry reward) {
        ResourceLocation id = ResourceLocation.tryParse(reward.iconItemId());
        Item item = id == null ? Items.BARRIER : BuiltInRegistries.ITEM.get(id);
        if (item == Items.AIR) {
            item = Items.BARRIER;
        }

        int count = reward.kind() == BattlePassSyncPayload.REWARD_KIND_ITEM ? Math.max(1, reward.amount()) : 1;
        return new ItemStack(item, Math.min(99, count));
    }

    static List<Component> tooltipForReward(BattlePassClientCache.RewardEntry reward, ItemStack iconStack) {
        if (reward.kind() == BattlePassSyncPayload.REWARD_KIND_ITEM) {
            return Screen.getTooltipFromItem(Minecraft.getInstance(), iconStack);
        }

        List<Component> lines = new ArrayList<>();
        if (reward.kind() == BattlePassSyncPayload.REWARD_KIND_ENTROPY_CAP) {
            lines.add(Component.translatable("screen.incore.player_level_rewards.tooltip_entropy_cap_title"));
            lines.add(Component.translatable("screen.incore.player_level_rewards.tooltip_entropy_cap", reward.amount()).withStyle(ChatFormatting.GRAY));
        } else if (reward.kind() == BattlePassSyncPayload.REWARD_KIND_COMMAND) {
            lines.add(Component.translatable("screen.incore.player_level_rewards.tooltip_command_title"));
            lines.add(Component.translatable("screen.incore.player_level_rewards.tooltip_command", reward.text()).withStyle(ChatFormatting.GRAY));
        } else {
            lines.add(Component.translatable("screen.incore.player_level_rewards.tooltip_other_title"));
            lines.add(Component.literal(reward.text()).withStyle(ChatFormatting.GRAY));
        }

        if (!reward.text().isBlank() && reward.kind() == BattlePassSyncPayload.REWARD_KIND_ENTROPY_CAP) {
            lines.add(Component.literal(reward.text()).withStyle(ChatFormatting.DARK_GRAY));
        }
        return lines;
    }

    static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static CategoryProgress categoryProgress(
            List<BattlePassClientCache.TaskEntry> tasks,
            boolean weekly,
            int week,
            int currentWeek
    ) {
        int available = 0;
        int completed = 0;
        for (BattlePassClientCache.TaskEntry task : tasks) {
            if (weekly != task.weekly()) {
                continue;
            }
            if (weekly && task.week() != week) {
                continue;
            }
            if (!isTaskAvailable(task, currentWeek)) {
                continue;
            }

            available++;
            if (task.completed()) {
                completed++;
            }
        }

        return new CategoryProgress(completed, completionCapFromAvailable(available));
    }

    private static boolean isTaskAvailable(BattlePassClientCache.TaskEntry task, int currentWeek) {
        return !task.weekly() || task.week() <= currentWeek;
    }

    private static int completionCapFromAvailable(int availableTaskCount) {
        if (availableTaskCount <= 0) {
            return 0;
        }
        return Math.max(1, availableTaskCount / 2);
    }

    private static String simplifySetId(String setId) {
        int separator = setId.indexOf(':');
        return separator >= 0 && separator + 1 < setId.length() ? setId.substring(separator + 1) : setId;
    }

    enum BattlePassTab {
        REWARDS,
        MISSIONS
    }

    static final class BattlePassUiState {
        private BattlePassTab activeTab = BattlePassTab.REWARDS;
        private int missionScroll;
        private int rewardLevelScroll;
        private int selectedMissionCategoryIndex;
        private String selectedMissionTaskId;
        private int selectedRewardLevel = -1;
        private int selectedRewardTrack = -1;
        private boolean rewardAutoFocus = true;

        BattlePassTab activeTab() {
            return this.activeTab;
        }

        void setActiveTab(BattlePassTab tab) {
            if (this.activeTab == tab) {
                return;
            }
            this.activeTab = tab;
            if (tab == BattlePassTab.REWARDS) {
                this.rewardAutoFocus = true;
            }
        }

        int missionScroll() {
            return this.missionScroll;
        }

        void setMissionScroll(int missionScroll) {
            this.missionScroll = missionScroll;
        }

        int rewardLevelScroll() {
            return this.rewardLevelScroll;
        }

        void setRewardLevelScroll(int rewardLevelScroll) {
            this.rewardLevelScroll = rewardLevelScroll;
        }

        int selectedMissionCategoryIndex() {
            return this.selectedMissionCategoryIndex;
        }

        void setSelectedMissionCategoryIndex(int selectedMissionCategoryIndex) {
            this.selectedMissionCategoryIndex = selectedMissionCategoryIndex;
        }

        String selectedMissionTaskId() {
            return this.selectedMissionTaskId;
        }

        void setSelectedMissionTaskId(String selectedMissionTaskId) {
            this.selectedMissionTaskId = selectedMissionTaskId;
        }

        int selectedRewardLevel() {
            return this.selectedRewardLevel;
        }

        void setSelectedRewardLevel(int selectedRewardLevel) {
            this.selectedRewardLevel = selectedRewardLevel;
        }

        int selectedRewardTrack() {
            return this.selectedRewardTrack;
        }

        void setSelectedRewardTrack(int selectedRewardTrack) {
            this.selectedRewardTrack = selectedRewardTrack;
        }

        boolean rewardAutoFocus() {
            return this.rewardAutoFocus;
        }

        void clearRewardAutoFocus() {
            this.rewardAutoFocus = false;
        }
    }

    record RewardTrackLayout(List<Integer> visibleLevelIndices, int scroll, int maxScroll) {
    }

    record MissionCategory(boolean weekly, int week, Component label, Component progressLabel) {
        static MissionCategory forWeek(int week, CategoryProgress progress) {
            return new MissionCategory(
                    true,
                    week,
                    Component.literal("Week " + week),
                    Component.literal(progress.completed() + "/" + progress.cap() + " completions")
            );
        }

        static MissionCategory permanent(CategoryProgress progress) {
            return new MissionCategory(
                    false,
                    0,
                    Component.translatable("screen.incore.battle_pass.task_permanent"),
                    Component.literal(progress.completed() + "/" + progress.cap() + " completions")
            );
        }

        boolean matches(BattlePassClientCache.TaskEntry task) {
            return this.weekly ? task.weekly() && task.week() == this.week : !task.weekly();
        }
    }

    record CategoryProgress(int completed, int cap) {
    }
}
