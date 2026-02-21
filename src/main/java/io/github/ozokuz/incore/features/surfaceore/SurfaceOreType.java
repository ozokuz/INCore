package io.github.ozokuz.incore.features.surfaceore;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public enum SurfaceOreType implements StringRepresentable {
    CRIMSITE("crimsite", "create:crimsite", "create:cut_crimsite_slab"),
    VERIDIUM("veridium", "create:veridium", "create:cut_veridium_slab"),
    ASURINE("asurine", "create:asurine", "create:cut_asurine_slab"),
    OCHRUM("ochrum", "create:ochrum", "create:cut_ochrum_slab"),
    CINNABAR("cinnabar", "incore:cinnabar_ore_stone", "incore:cinnabar_ore_stone_slab"),
    MIXED_METALS("mixed_metals", "incore:mixed_metals_ore_stone", "incore:mixed_metals_ore_stone_slab"),
    GEM_CLUSTERS("gem_clusters", "incore:gem_clusters_ore_stone", "incore:gem_clusters_ore_stone_slab");

    private static final SurfaceOreType[] VALUES = values();

    private final String serializedName;
    private final ResourceLocation oreStoneBlockId;
    private final ResourceLocation oreStoneSlabId;

    SurfaceOreType(String serializedName, String oreStoneBlockId, String oreStoneSlabId) {
        this.serializedName = serializedName;
        this.oreStoneBlockId = ResourceLocation.parse(oreStoneBlockId);
        this.oreStoneSlabId = ResourceLocation.parse(oreStoneSlabId);
    }

    public static SurfaceOreType random(RandomSource random) {
        return VALUES[random.nextInt(VALUES.length)];
    }

    public ItemStack oreDropStack() {
        Item dropItem = oreStoneBlock().asItem();
        return dropItem == Items.AIR ? new ItemStack(Blocks.STONE) : new ItemStack(dropItem);
    }

    public BlockState oreStoneState() {
        return oreStoneBlock().defaultBlockState();
    }

    public BlockState oreStoneSlabState() {
        Block slabBlock = BuiltInRegistries.BLOCK.get(oreStoneSlabId);
        if (slabBlock == Blocks.AIR || !(slabBlock instanceof SlabBlock)) {
            return Blocks.STONE_SLAB.defaultBlockState();
        }
        return slabBlock.defaultBlockState();
    }

    private Block oreStoneBlock() {
        Block oreStoneBlock = BuiltInRegistries.BLOCK.get(oreStoneBlockId);
        if (oreStoneBlock == Blocks.AIR) {
            return Blocks.STONE;
        }
        return oreStoneBlock;
    }

    @Override
    public @NotNull String getSerializedName() {
        return serializedName;
    }
}
