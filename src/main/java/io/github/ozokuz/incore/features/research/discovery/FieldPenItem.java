package io.github.ozokuz.incore.features.research.discovery;

import net.minecraft.world.item.Item;

public class FieldPenItem extends Item {
    public FieldPenItem(Properties properties) {
        super(properties.durability(128).stacksTo(1));
    }
}
