package ozokuz.incore.features.tasks;

import net.minecraft.world.item.Item;

public sealed interface TaskReward permits TaskReward.ItemReward, TaskReward.CommandReward, TaskReward.EntropyReward {
    record ItemReward(Item item, int count) implements TaskReward {
    }

    record CommandReward(String command) implements TaskReward {
    }

    record EntropyReward(int amount) implements TaskReward {
    }
}
