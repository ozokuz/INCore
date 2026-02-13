package io.github.ozokuz.incore.features.encounter_spawner;

import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

public class EncounterWandItem extends Item {
    public EncounterWandItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        if (context.getLevel().isClientSide) return InteractionResult.PASS;

        var pos = context.getClickedPos();
        if (context.getLevel().getBlockState(pos).getBlock() instanceof EncounterSpawnerBlock) {
            var be = (EncounterSpawnerBE) context.getLevel().getBlockEntity(pos);

            var customName = stack.get(DataComponents.CUSTOM_NAME);
            if (customName == null) {
                context.getPlayer().sendSystemMessage(Component.translatable("incore.encounter_wand.messages.not_named"));
                return InteractionResult.FAIL;
            }

            var data = stack.get(DataComponents.CUSTOM_DATA);
            if (data == null) {
                context.getPlayer().sendSystemMessage(Component.translatable("incore.encounter_wand.messages.no_location"));
                return InteractionResult.FAIL;
            }

            be.setEncounterId(customName.getString());
            var posArr = data.copyTag().getIntArray("pos");
            be.setSpawnOffset(pos.subtract(new Vec3i(posArr[0], posArr[1], posArr[2])));

            context.getPlayer().sendSystemMessage(Component.translatable("incore.encounter_wand.messages.stored_to_spawner"));

            return InteractionResult.SUCCESS_NO_ITEM_USED;
        }

        var tag = new CompoundTag();
        tag.putIntArray("pos", new int[]{pos.getX(), pos.getY(), pos.getZ()});
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        var position = String.format("[%d, %d, %d]",  pos.getX(), pos.getY(), pos.getZ());
        stack.set(DataComponents.LORE, new ItemLore(List.of(Component.translatable("incore.encounter_wand.tooltip.position", position))));

        context.getPlayer().sendSystemMessage(Component.translatable("incore.encounter_wand.messages.store_location", position));

        return InteractionResult.SUCCESS_NO_ITEM_USED;
    }
}
