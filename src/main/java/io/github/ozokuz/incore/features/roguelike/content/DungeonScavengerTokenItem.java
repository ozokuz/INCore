package io.github.ozokuz.incore.features.roguelike.content;

import net.minecraft.world.item.Item;

public class DungeonScavengerTokenItem extends Item {
    public DungeonScavengerTokenItem(Properties properties) {
        super(properties.stacksTo(64));
    }
}
