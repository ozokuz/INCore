package io.github.ozokuz.incore.features.roguelike.content;

import net.minecraft.world.item.Item;

public class EmptyDungeonCrystalItem extends Item {
    public EmptyDungeonCrystalItem(Properties properties) {
        super(properties.stacksTo(16));
    }
}
