package io.github.ozokuz.incore;

import io.github.ozokuz.incore.features.encounter_spawner.EncounterSpawnerBE;
import io.github.ozokuz.incore.features.encounter_spawner.EncounterSpawnerBlock;
import io.github.ozokuz.incore.features.encounter_spawner.EncounterWandItem;
import io.github.ozokuz.incore.features.battlepass.BattlePassLane;
import io.github.ozokuz.incore.features.battlepass.BattlePassLaneUnlockItem;
import io.github.ozokuz.incore.features.gacha.GachaCrateBlock;
import io.github.ozokuz.incore.features.gacha.GachaCrateBlockItem;
import io.github.ozokuz.incore.features.gacha.GachaCrateBlockEntity;
import io.github.ozokuz.incore.features.gacha.GachaPermitItem;
import io.github.ozokuz.incore.features.sanity.SanityBoosterItem;
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
    public static final DeferredBlock<Block> GACHA_CRATE_BLOCK = BLOCKS.register("gacha_crate", GachaCrateBlock::new);
    public static final Supplier<BlockEntityType<GachaCrateBlockEntity>> GACHA_CRATE_BE = BLOCK_ENTITY_TYPES.register("gacha_crate", () -> BlockEntityType.Builder.of(GachaCrateBlockEntity::new, GACHA_CRATE_BLOCK.get()).build(null));
    public static final DeferredItem<BlockItem> GACHA_CRATE_BLOCK_ITEM = ITEMS.registerItem("gacha_crate", properties -> new GachaCrateBlockItem(GACHA_CRATE_BLOCK.get(), properties));

    public static final DeferredItem<Item> ENCOUNTER_WAND_ITEM = ITEMS.registerItem("encounter_wand", EncounterWandItem::new);
    public static final DeferredItem<Item> SANITY_CRATE_ITEM = ITEMS.registerItem("sanity_crate", SanityCrateItem::new);
    public static final DeferredItem<Item> SANITY_BOOSTER_SMALL_ITEM = ITEMS.registerItem("sanity_booster_small", properties -> new SanityBoosterItem(properties, 50));
    public static final DeferredItem<Item> SANITY_BOOSTER_LARGE_ITEM = ITEMS.registerItem("sanity_booster_large", properties -> new SanityBoosterItem(properties, 100));
    public static final DeferredItem<Item> SANITY_VESSEL_ITEM = ITEMS.registerItem("sanity_vessel", SanityVesselItem::new);
    public static final DeferredItem<Item> ORIGINIUM_SUPPLY_UNLOCK_ITEM = ITEMS.registerItem(
            "originium_supply_unlock",
            properties -> new BattlePassLaneUnlockItem(properties, BattlePassLane.ORIGINIUM)
    );
    public static final DeferredItem<Item> PROTOCOL_CUSTOMIZED_UNLOCK_ITEM = ITEMS.registerItem(
            "protocol_customized_unlock",
            properties -> new BattlePassLaneUnlockItem(properties, BattlePassLane.PROTOCOL)
    );
    public static final DeferredItem<Item> BASIC_BANNER_PERMIT_ITEM = ITEMS.registerItem("basic_banner_permit", properties -> new GachaPermitItem(properties, GachaPermitItem.PermitMode.BASIC));
    public static final DeferredItem<Item> CHARTERED_BANNER_PERMIT_ITEM = ITEMS.registerItem("chartered_banner_permit", properties -> new GachaPermitItem(properties, GachaPermitItem.PermitMode.CHARTERED));
    public static final DeferredItem<Item> BANNER_PERMIT_ITEM = ITEMS.registerItem("banner_permit", properties -> new GachaPermitItem(properties, GachaPermitItem.PermitMode.SPECIFIC));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.incore"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> ENCOUNTER_WAND_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(ENCOUNTER_SPAWNER_BLOCK_ITEM.get());
                output.accept(GACHA_CRATE_BLOCK_ITEM.get());
                output.accept(ENCOUNTER_WAND_ITEM.get());
                output.accept(SANITY_CRATE_ITEM.get());
                output.accept(SANITY_BOOSTER_SMALL_ITEM.get());
                output.accept(SANITY_BOOSTER_LARGE_ITEM.get());
                output.accept(SANITY_VESSEL_ITEM.get());
                output.accept(ORIGINIUM_SUPPLY_UNLOCK_ITEM.get());
                output.accept(PROTOCOL_CUSTOMIZED_UNLOCK_ITEM.get());
                output.accept(BASIC_BANNER_PERMIT_ITEM.get());
                output.accept(CHARTERED_BANNER_PERMIT_ITEM.get());
                output.accept(BANNER_PERMIT_ITEM.get());
            }).build());
}
