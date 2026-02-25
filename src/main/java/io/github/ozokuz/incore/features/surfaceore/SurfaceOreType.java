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

import java.util.Arrays;

public enum SurfaceOreType implements StringRepresentable {
    CRIMSITE("crimsite", "create:crimsite", "create:cut_crimsite_slab", DimensionCategory.OVERWORLD),
    VERIDIUM("veridium", "create:veridium", "create:cut_veridium_slab", DimensionCategory.OVERWORLD),
    ASURINE("asurine", "create:asurine", "create:cut_asurine_slab", DimensionCategory.OVERWORLD),
    OCHRUM("ochrum", "create:ochrum", "create:cut_ochrum_slab", DimensionCategory.OVERWORLD),
    CINNABAR("cinnabar", "incore:cinnabar_ore_stone", "incore:cinnabar_ore_stone_slab", DimensionCategory.OVERWORLD),
    MIXED_METALS("mixed_metals", "incore:mixed_metals_ore_stone", "incore:mixed_metals_ore_stone_slab", DimensionCategory.OVERWORLD),
    GEM_CLUSTERS("gem_clusters", "incore:gem_clusters_ore_stone", "incore:gem_clusters_ore_stone_slab", DimensionCategory.OVERWORLD);

    private static final SurfaceOreType[] VALUES = values();

    private final String serializedName;
    private final ResourceLocation oreStoneBlockId;
    private final ResourceLocation oreStoneSlabId;
    private final DimensionCategory dimension;

    SurfaceOreType(String serializedName, String oreStoneBlockId, String oreStoneSlabId, DimensionCategory dimension) {
        this.serializedName = serializedName;
        this.oreStoneBlockId = ResourceLocation.parse(oreStoneBlockId);
        this.oreStoneSlabId = ResourceLocation.parse(oreStoneSlabId);
        this.dimension = dimension;
    }

    public static SurfaceOreType random(RandomSource random) {
        return VALUES[random.nextInt(VALUES.length)];
    }

    public static SurfaceOreType random(RandomSource random, DimensionCategory category) {
        SurfaceOreType[] filtered = Arrays.stream(VALUES)
                .filter(type -> type.dimension == category)
                .toArray(SurfaceOreType[]::new);
        
        if (filtered.length == 0) {
            return null;
        }
        
        return filtered[random.nextInt(filtered.length)];
    }

    public DimensionCategory getDimension() {
        return dimension;
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
