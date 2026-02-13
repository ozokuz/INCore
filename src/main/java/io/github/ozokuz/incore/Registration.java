package io.github.ozokuz.incore;

import io.github.ozokuz.incore.features.encounter_spawner.EncounterSpawnerBE;
import io.github.ozokuz.incore.features.encounter_spawner.EncounterSpawnerBlock;
import io.github.ozokuz.incore.features.encounter_spawner.EncounterWandItem;
import io.github.ozokuz.incore.features.sanity.SanityCrateItem;
import io.github.ozokuz.incore.features.sanity.SanityVesselItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class Registration {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(INCore.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(INCore.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, INCore.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,  INCore.MODID);

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        CREATIVE_MODE_TABS.register(bus);
        BLOCK_ENTITY_TYPES.register(bus);
    }

    public static final DeferredBlock<Block> ENCOUNTER_SPAWNER_BLOCK = BLOCKS.register("encounter_spawner", EncounterSpawnerBlock::new);
    public static final Supplier<BlockEntityType<EncounterSpawnerBE>> ENCOUNTER_SPAWNER_BE = BLOCK_ENTITY_TYPES.register("encounter_spawner", () -> BlockEntityType.Builder.of(EncounterSpawnerBE::new, ENCOUNTER_SPAWNER_BLOCK.get()).build(null));
    public static final DeferredItem<BlockItem> ENCOUNTER_SPAWNER_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("encounter_spawner", ENCOUNTER_SPAWNER_BLOCK);

    public static final DeferredItem<Item> ENCOUNTER_WAND_ITEM = ITEMS.registerItem("encounter_wand", EncounterWandItem::new);
    public static final DeferredItem<Item> SANITY_CRATE_ITEM = ITEMS.registerItem("sanity_crate", SanityCrateItem::new);
    public static final DeferredItem<Item> SANITY_VESSEL_ITEM = ITEMS.registerItem("sanity_vessel", SanityVesselItem::new);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.incore"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> ENCOUNTER_WAND_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(ENCOUNTER_SPAWNER_BLOCK_ITEM.get());
                output.accept(ENCOUNTER_WAND_ITEM.get());
                output.accept(SANITY_CRATE_ITEM.get());
                output.accept(SANITY_VESSEL_ITEM.get());
            }).build());
}
