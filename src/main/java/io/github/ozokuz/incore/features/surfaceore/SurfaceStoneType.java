package io.github.ozokuz.incore.features.surfaceore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public enum SurfaceStoneType implements StringRepresentable {
    STONE("stone", "minecraft:stone", "minecraft:stone_slab"),
    DEEPSLATE("deepslate", "minecraft:deepslate", "minecraft:cobbled_deepslate_slab"),
    LIMESTONE("limestone", "create:limestone", "create:cut_limestone_slab"),
    BASALT("basalt", "minecraft:basalt", "minecraft:stone_slab"),
    SCORIA("scoria", "create:scoria", "create:cut_scoria_slab");

    private static final SurfaceStoneType[] VALUES = values();

    private final String serializedName;
    private final ResourceLocation sourceStoneBlockId;
    private final ResourceLocation sourceStoneSlabId;

    SurfaceStoneType(String serializedName, String sourceStoneBlockId, String sourceStoneSlabId) {
        this.serializedName = serializedName;
        this.sourceStoneBlockId = ResourceLocation.parse(sourceStoneBlockId);
        this.sourceStoneSlabId = ResourceLocation.parse(sourceStoneSlabId);
    }

    public static SurfaceStoneType random(RandomSource random) {
        return VALUES[random.nextInt(VALUES.length)];
    }

    public BlockState stoneState() {
        return sourceStoneBlock().defaultBlockState();
    }

    public BlockState stoneSlabState() {
        Block slabBlock = BuiltInRegistries.BLOCK.get(sourceStoneSlabId);
        if (slabBlock == Blocks.AIR || !(slabBlock instanceof SlabBlock)) {
            return Blocks.STONE_SLAB.defaultBlockState();
        }
        return slabBlock.defaultBlockState();
    }

    public List<ItemStack> miningDrops(ServerLevel level, BlockPos pos, Player player, ItemStack tool) {
        if (this != DEEPSLATE) {
            Item sourceItem = sourceStoneBlock().asItem();
            if (sourceItem == Items.AIR) {
                sourceItem = Items.STONE;
            }
            return List.of(new ItemStack(sourceItem));
        }

        BlockState sourceState = sourceStoneBlock().defaultBlockState();
        List<ItemStack> drops = Block.getDrops(sourceState, level, pos, (BlockEntity) null, player, tool);
        if (!drops.isEmpty()) {
            return drops;
        }

        return List.of(new ItemStack(Items.COBBLED_DEEPSLATE));
    }

    private Block sourceStoneBlock() {
        Block sourceBlock = BuiltInRegistries.BLOCK.get(sourceStoneBlockId);
        if (sourceBlock == Blocks.AIR) {
            return Blocks.STONE;
        }
        return sourceBlock;
    }

    @Override
    public @NotNull String getSerializedName() {
        return serializedName;
    }
}
