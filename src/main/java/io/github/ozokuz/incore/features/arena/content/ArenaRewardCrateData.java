package io.github.ozokuz.incore.features.arena.content;

import io.github.ozokuz.incore.Registration;
import io.github.ozokuz.incore.features.arena.data.ArenaCatalogEntry;
import io.github.ozokuz.incore.features.arena.data.ArenaRewardStack;
import io.github.ozokuz.incore.features.sanity.SanityManager;
import io.github.ozokuz.incore.features.sanity.network.SanityNetworking;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class ArenaRewardCrateData {
    private static final String KEY_CATEGORY_ID = "incore:arena_category_id";
    private static final String KEY_CATEGORY_NAME = "incore:arena_category_name";
    private static final String KEY_DIFFICULTY_ID = "incore:arena_difficulty_id";
    private static final String KEY_DIFFICULTY_NAME = "incore:arena_difficulty_name";
    private static final String KEY_SANITY_COST = "incore:arena_sanity_cost";
    private static final String KEY_REWARDS = "incore:arena_rewards";

    private ArenaRewardCrateData() {
    }

    public static ItemStack createCrateStack(ArenaCatalogEntry entry) {
        CrateContents contents = CrateContents.fromEntry(entry);
        ItemStack stack = new ItemStack(Registration.ARENA_REWARD_RIFT_BLOCK_ITEM.get());
        write(stack, contents);
        return stack;
    }

    public static void write(ItemStack stack, CrateContents contents) {
        CompoundTag tag = new CompoundTag();
        tag.putString(KEY_CATEGORY_ID, contents.categoryId());
        tag.putString(KEY_CATEGORY_NAME, contents.categoryName());
        tag.putString(KEY_DIFFICULTY_ID, contents.difficultyId());
        tag.putString(KEY_DIFFICULTY_NAME, contents.difficultyName());
        tag.putInt(KEY_SANITY_COST, contents.sanityCost());

        ListTag rewardsTag = new ListTag();
        for (ArenaRewardStack reward : contents.rewards()) {
            CompoundTag rewardTag = new CompoundTag();
            rewardTag.putString("item", reward.itemId().toString());
            rewardTag.putInt("count", reward.count());
            rewardsTag.add(rewardTag);
        }
        tag.put(KEY_REWARDS, rewardsTag);

        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Nullable
    public static CrateContents read(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return null;
        }

        return read(data.copyTag());
    }

    @Nullable
    public static CrateContents read(CompoundTag tag) {
        if (!tag.contains(KEY_CATEGORY_ID, Tag.TAG_STRING)
                || !tag.contains(KEY_CATEGORY_NAME, Tag.TAG_STRING)
                || !tag.contains(KEY_DIFFICULTY_ID, Tag.TAG_STRING)
                || !tag.contains(KEY_DIFFICULTY_NAME, Tag.TAG_STRING)
                || !tag.contains(KEY_SANITY_COST, Tag.TAG_INT)
                || !tag.contains(KEY_REWARDS, Tag.TAG_LIST)) {
            return null;
        }

        List<ArenaRewardStack> rewards = new ArrayList<>();
        ListTag rewardsTag = tag.getList(KEY_REWARDS, Tag.TAG_COMPOUND);
        for (Tag rewardTagValue : rewardsTag) {
            CompoundTag rewardTag = (CompoundTag) rewardTagValue;
            if (!rewardTag.contains("item", Tag.TAG_STRING)) {
                continue;
            }

            ResourceLocation itemId = ResourceLocation.tryParse(rewardTag.getString("item"));
            int count = rewardTag.getInt("count");
            if (itemId == null || count <= 0 || !BuiltInRegistries.ITEM.containsKey(itemId)) {
                continue;
            }

            rewards.add(new ArenaRewardStack(itemId, count));
        }

        if (rewards.isEmpty()) {
            return null;
        }

        return new CrateContents(
                tag.getString(KEY_CATEGORY_ID),
                tag.getString(KEY_CATEGORY_NAME),
                tag.getString(KEY_DIFFICULTY_ID),
                tag.getString(KEY_DIFFICULTY_NAME),
                Math.max(0, tag.getInt(KEY_SANITY_COST)),
                List.copyOf(rewards)
        );
    }

    public static void appendTooltip(ItemStack stack, List<Component> tooltip) {
        CrateContents contents = read(stack);
        if (contents == null) {
            tooltip.add(Component.translatable("block.incore.arena_reward_rift.tooltip.invalid"));
            return;
        }

        tooltip.add(Component.translatable(
                "block.incore.arena_reward_rift.tooltip.source",
                contents.categoryName(),
                contents.difficultyName()
        ));
        tooltip.add(Component.translatable("block.incore.arena_reward_rift.tooltip.cost", contents.sanityCost()));
        tooltip.add(Component.translatable("block.incore.arena_reward_rift.tooltip.contains"));
        for (ArenaRewardStack reward : contents.rewards()) {
            Item item = BuiltInRegistries.ITEM.get(reward.itemId());
            Component itemName = item == null ? Component.literal(reward.itemId().toString()) : item.getDescription();
            tooltip.add(Component.translatable("block.incore.arena_reward_rift.tooltip.entry", reward.count(), itemName));
        }
    }

    public static Component nameForStack(ItemStack stack) {
        CrateContents contents = read(stack);
        return contents == null ? Component.translatable("block.incore.arena_reward_rift") : buildDisplayName(contents);
    }

    public static boolean tryOpen(ServerPlayer player, CrateContents contents) {
        int currentSanity = SanityManager.getCurrentSanity(player);
        int cap = SanityManager.getSanityCap(player);
        if (currentSanity < contents.sanityCost() || !SanityManager.tryConsume(player, contents.sanityCost())) {
            player.sendSystemMessage(Component.translatable("incore.arena.crate.not_enough", contents.sanityCost(), currentSanity, cap));
            return false;
        }

        for (ArenaRewardStack reward : contents.rewards()) {
            Item item = BuiltInRegistries.ITEM.get(reward.itemId());
            if (item == null || item == net.minecraft.world.item.Items.AIR) {
                continue;
            }

            ItemStack rewardStack = new ItemStack(item, reward.count());
            if (!player.addItem(rewardStack)) {
                player.drop(rewardStack, false);
            }
        }

        int remaining = SanityManager.getCurrentSanity(player);
        SanityNetworking.syncToPlayer(player);
        player.sendSystemMessage(Component.translatable("incore.arena.crate.opened", contents.sanityCost(), remaining, cap));
        return true;
    }

    private static Component buildDisplayName(CrateContents contents) {
        String source = contents.categoryName() + " " + contents.difficultyName();
        return Component.literal(source + " Sanity Reward Crate");
    }

    public record CrateContents(
            String categoryId,
            String categoryName,
            String difficultyId,
            String difficultyName,
            int sanityCost,
            List<ArenaRewardStack> rewards
    ) {
        static CrateContents fromEntry(ArenaCatalogEntry entry) {
            return new CrateContents(
                    entry.categoryId(),
                    entry.categoryName(),
                    entry.difficultyId(),
                    entry.difficultyName(),
                    entry.rewardSanityCost(),
                    entry.rewardItems()
            );
        }

        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putString(KEY_CATEGORY_ID, categoryId);
            tag.putString(KEY_CATEGORY_NAME, categoryName);
            tag.putString(KEY_DIFFICULTY_ID, difficultyId);
            tag.putString(KEY_DIFFICULTY_NAME, difficultyName);
            tag.putInt(KEY_SANITY_COST, sanityCost);

            ListTag rewardsTag = new ListTag();
            for (ArenaRewardStack reward : rewards) {
                CompoundTag rewardTag = new CompoundTag();
                rewardTag.putString("item", reward.itemId().toString());
                rewardTag.putInt("count", reward.count());
                rewardsTag.add(rewardTag);
            }
            tag.put(KEY_REWARDS, rewardsTag);
            return tag;
        }

        @Nullable
        public static CrateContents fromTag(CompoundTag tag) {
            return ArenaRewardCrateData.read(tag);
        }
    }
}
