package io.github.ozokuz.incore.features.tasks;

import net.minecraft.world.item.Item;

public sealed interface TaskReward permits TaskReward.ItemReward, TaskReward.CommandReward, TaskReward.SanityReward {
    record ItemReward(Item item, int count) implements TaskReward {
    }

    record CommandReward(String command) implements TaskReward {
    }

    record SanityReward(int amount) implements TaskReward {
    }
}
