package ozokuz.incore.features.roguelike.content;

import appeng.api.ids.AEComponents;
import appeng.api.implementations.items.MemoryCardColors;
import appeng.api.util.AEColor;
import appeng.items.tools.MemoryCardItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record AutomatorMemoryCardLink(ResourceLocation dimensionId, BlockPos automatorPos, @Nullable UUID ownerId) {
    private static final String ROOT_KEY = "incore_automator_link";
    private static final String TYPE = "dungeon_altar_automator";
    private static final int VERSION = 1;

    public static void write(ItemStack stack, ResourceKey<Level> dimension, BlockPos automatorPos, @Nullable UUID ownerId) {
        MemoryCardItem.clearCard(stack);
        CompoundTag root = currentTag(stack);
        CompoundTag linkTag = new CompoundTag();
        linkTag.putInt("version", VERSION);
        linkTag.putString("type", TYPE);
        linkTag.putString("dimension", dimension.location().toString());
        linkTag.putLong("automatorPos", automatorPos.asLong());
        if (ownerId != null) {
            linkTag.putUUID("ownerId", ownerId);
        }
        root.put(ROOT_KEY, linkTag);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        stack.set(
                AEComponents.EXPORTED_SETTINGS_SOURCE,
                Component.translatable(
                        "incore.roguelike.automator.memory_card.source",
                        automatorPos.getX(),
                        automatorPos.getY(),
                        automatorPos.getZ()
                )
        );
        stack.set(
                AEComponents.MEMORY_CARD_COLORS,
                new MemoryCardColors(
                        AEColor.BLUE, AEColor.BLUE, AEColor.CYAN, AEColor.CYAN,
                        AEColor.TRANSPARENT, AEColor.TRANSPARENT, AEColor.LIGHT_BLUE, AEColor.LIGHT_BLUE
                )
        );
    }

    public static @Nullable AutomatorMemoryCardLink read(ItemStack stack) {
        CompoundTag root = currentTag(stack);
        if (!root.contains(ROOT_KEY, CompoundTag.TAG_COMPOUND)) {
            return null;
        }
        CompoundTag linkTag = root.getCompound(ROOT_KEY);
        if (linkTag.getInt("version") != VERSION || !TYPE.equals(linkTag.getString("type"))) {
            return null;
        }
        ResourceLocation dimensionId = ResourceLocation.tryParse(linkTag.getString("dimension"));
        if (dimensionId == null || !linkTag.contains("automatorPos")) {
            return null;
        }
        UUID ownerId = linkTag.hasUUID("ownerId") ? linkTag.getUUID("ownerId") : null;
        return new AutomatorMemoryCardLink(dimensionId, BlockPos.of(linkTag.getLong("automatorPos")), ownerId);
    }

    public static boolean isMemoryCard(ItemStack stack) {
        return stack.getItem() instanceof appeng.api.implementations.items.IMemoryCard;
    }

    private static CompoundTag currentTag(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
    }
}
