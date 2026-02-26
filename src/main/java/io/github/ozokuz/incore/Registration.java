package io.github.ozokuz.incore;

import io.github.ozokuz.incore.features.arena.content.ArenaOrbBlock;
import io.github.ozokuz.incore.features.arena.content.ArenaRewardCrateBlock;
import io.github.ozokuz.incore.features.arena.content.ArenaRewardCrateBlockEntity;
import io.github.ozokuz.incore.features.arena.content.ArenaRewardCrateBlockItem;
import io.github.ozokuz.incore.features.encounter_spawner.EncounterSpawnerBE;
import io.github.ozokuz.incore.features.encounter_spawner.EncounterSpawnerBlock;
import io.github.ozokuz.incore.features.encounter_spawner.EncounterWandItem;
import io.github.ozokuz.incore.features.battlepass.BattlePassLane;
import io.github.ozokuz.incore.features.battlepass.BattlePassLaneUnlockItem;
import io.github.ozokuz.incore.features.gacha.GachaCrateBlock;
import io.github.ozokuz.incore.features.gacha.GachaCrateBlockItem;
import io.github.ozokuz.incore.features.gacha.GachaCrateBlockEntity;
import io.github.ozokuz.incore.features.gacha.GachaPermitItem;
import io.github.ozokuz.incore.features.research.BurnerLabBlock;
import io.github.ozokuz.incore.features.cards.CardBoosterBoxItem;
import io.github.ozokuz.incore.features.cards.CardBoosterItem;
import io.github.ozokuz.incore.features.cards.CardModuleItem;
import io.github.ozokuz.incore.features.cards.CardSleeveItem;
import io.github.ozokuz.incore.features.cards.CardTokenItem;
import io.github.ozokuz.incore.features.cards.DeckBoxItem;
import io.github.ozokuz.incore.features.cards.DeckCoreItem;
import io.github.ozokuz.incore.features.cards.DeckItem;
import io.github.ozokuz.incore.features.cards.DeckStationBlock;
import io.github.ozokuz.incore.features.cards.DeckStationBlockEntity;
import io.github.ozokuz.incore.features.cards.DeckStationMenu;
import io.github.ozokuz.incore.features.cards.DecryptorBlock;
import io.github.ozokuz.incore.features.vendor.VendorBlock;
import io.github.ozokuz.incore.features.vendor.VendorBlockEntity;
import io.github.ozokuz.incore.features.vendor.VendorDiscountCharmItem;
import io.github.ozokuz.incore.features.market.content.MarketAutoBuyerBlock;
import io.github.ozokuz.incore.features.market.content.MarketAutoBuyerBlockEntity;
import io.github.ozokuz.incore.features.market.content.MarketAutoBuyerMk2Block;
import io.github.ozokuz.incore.features.market.content.MarketAutoBuyerMk2BlockEntity;
import io.github.ozokuz.incore.features.market.content.MarketAutoBuyerMenu;
import io.github.ozokuz.incore.features.market.content.MarketTerminalCardMenu;
import io.github.ozokuz.incore.features.market.content.MarketTerminalBlock;
import io.github.ozokuz.incore.features.market.content.MarketTerminalBlockEntity;
import io.github.ozokuz.incore.features.market.content.ShipmentTerminalBlock;
import io.github.ozokuz.incore.features.market.content.ShipmentTerminalBlockEntity;
import io.github.ozokuz.incore.features.market.content.ShipmentTerminalMk2Block;
import io.github.ozokuz.incore.features.market.content.ShipmentTerminalMk2BlockEntity;
import io.github.ozokuz.incore.features.market.content.ShipmentTerminalMenu;
import io.github.ozokuz.incore.features.research.LabBlockEntity;
import io.github.ozokuz.incore.features.research.LabMenu;
import io.github.ozokuz.incore.features.surfaceore.SurfaceOrePatchFeature;
import io.github.ozokuz.incore.features.surfaceore.SurfaceOreDebugCompassItem;
import io.github.ozokuz.incore.features.surfaceore.SurfaceOreSpotBlock;
import io.github.ozokuz.incore.features.surfaceore.SurfaceOreSpotBlockEntity;
import io.github.ozokuz.incore.features.surfaceore.SurfaceOreType;
import io.github.ozokuz.incore.features.surfaceore.SurfaceStoneDebugCompassItem;
import io.github.ozokuz.incore.features.surfaceore.SurfaceStonePatchFeature;
import io.github.ozokuz.incore.features.surfaceore.SurfaceStoneSpotBlock;
import io.github.ozokuz.incore.features.surfaceore.SurfaceStoneType;
import io.github.ozokuz.incore.features.research.MechanicalLabBlock;
import io.github.ozokuz.incore.features.research.ModularLabBlock;
import io.github.ozokuz.incore.features.research.ProductivityModuleCardItem;
import io.github.ozokuz.incore.features.research.SpeedModuleCardItem;
import io.github.ozokuz.incore.features.roguelike.content.DungeonCompletionCrateItem;
import io.github.ozokuz.incore.features.roguelike.content.DungeonCrystalItem;
import io.github.ozokuz.incore.features.roguelike.content.DungeonObjectiveAltarBlock;
import io.github.ozokuz.incore.features.roguelike.content.CustomDungeonCrystalForgeBlock;
import io.github.ozokuz.incore.features.roguelike.content.CustomDungeonCrystalForgeBlockEntity;
import io.github.ozokuz.incore.features.roguelike.content.CustomDungeonCrystalForgeMenu;
import io.github.ozokuz.incore.features.roguelike.content.DungeonReturnPortalBlock;
import io.github.ozokuz.incore.features.roguelike.content.DungeonScavengerTokenItem;
import io.github.ozokuz.incore.features.roguelike.content.EmptyDungeonCrystalItem;
import io.github.ozokuz.incore.features.roguelike.content.RoguelikeAltarBlock;
import io.github.ozokuz.incore.features.roguelike.content.RoguelikeAltarBlockEntity;
import io.github.ozokuz.incore.features.roguelike.content.RoguelikePortalBlock;
import io.github.ozokuz.incore.features.roguelike.content.RoguelikePortalBlockEntity;
import io.github.ozokuz.incore.features.sanity.SanityBoosterItem;
import io.github.ozokuz.incore.features.sanity.SanityCrateItem;
import io.github.ozokuz.incore.features.sanity.SanityVesselItem;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.function.Supplier;

public class Registration {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(INCore.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(INCore.MODID);
    public static final DeferredRegister.DataComponents DATA_COMPONENT_TYPES = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, INCore.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, INCore.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,  INCore.MODID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, INCore.MODID);
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, INCore.MODID);

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        DATA_COMPONENT_TYPES.register(bus);
        CREATIVE_MODE_TABS.register(bus);
        BLOCK_ENTITY_TYPES.register(bus);
        MENU_TYPES.register(bus);
        FEATURES.register(bus);
    }

    public static final DeferredBlock<Block> ENCOUNTER_SPAWNER_BLOCK = BLOCKS.register("encounter_spawner", EncounterSpawnerBlock::new);
    public static final Supplier<BlockEntityType<EncounterSpawnerBE>> ENCOUNTER_SPAWNER_BE = BLOCK_ENTITY_TYPES.register("encounter_spawner", () -> BlockEntityType.Builder.of(EncounterSpawnerBE::new, ENCOUNTER_SPAWNER_BLOCK.get()).build(null));
    public static final DeferredItem<BlockItem> ENCOUNTER_SPAWNER_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("encounter_spawner", ENCOUNTER_SPAWNER_BLOCK);
    public static final DeferredBlock<Block> GACHA_CRATE_BLOCK = BLOCKS.register("gacha_crate", GachaCrateBlock::new);
    public static final Supplier<BlockEntityType<GachaCrateBlockEntity>> GACHA_CRATE_BE = BLOCK_ENTITY_TYPES.register("gacha_crate", () -> BlockEntityType.Builder.of(GachaCrateBlockEntity::new, GACHA_CRATE_BLOCK.get()).build(null));
    public static final DeferredItem<BlockItem> GACHA_CRATE_BLOCK_ITEM = ITEMS.registerItem("gacha_crate", properties -> new GachaCrateBlockItem(GACHA_CRATE_BLOCK.get(), properties));
    public static final DeferredBlock<Block> ARENA_ORB_BLOCK = BLOCKS.register("arena_orb", ArenaOrbBlock::new);
    public static final DeferredItem<BlockItem> ARENA_ORB_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("arena_orb", ARENA_ORB_BLOCK);
    public static final DeferredBlock<Block> ARENA_REWARD_CRATE_BLOCK = BLOCKS.register("arena_reward_crate", ArenaRewardCrateBlock::new);
    public static final Supplier<BlockEntityType<ArenaRewardCrateBlockEntity>> ARENA_REWARD_CRATE_BE =
            BLOCK_ENTITY_TYPES.register("arena_reward_crate", () -> BlockEntityType.Builder.of(ArenaRewardCrateBlockEntity::new, ARENA_REWARD_CRATE_BLOCK.get()).build(null));
    public static final DeferredItem<BlockItem> ARENA_REWARD_CRATE_BLOCK_ITEM =
            ITEMS.registerItem("arena_reward_crate", properties -> new ArenaRewardCrateBlockItem(ARENA_REWARD_CRATE_BLOCK.get(), properties));
    public static final DeferredBlock<Block> DECK_STATION_BLOCK = BLOCKS.register("deck_station", () -> new DeckStationBlock());
    public static final Supplier<BlockEntityType<DeckStationBlockEntity>> DECK_STATION_BE = BLOCK_ENTITY_TYPES.register("deck_station", () -> BlockEntityType.Builder.of(DeckStationBlockEntity::new, DECK_STATION_BLOCK.get()).build(null));
    public static final Supplier<MenuType<DeckStationMenu>> DECK_STATION_MENU = MENU_TYPES.register("deck_station", () -> IMenuTypeExtension.create((id, inv, data) -> new DeckStationMenu(id, inv, (DeckStationBlockEntity) inv.player.level().getBlockEntity(data.readBlockPos()))));
    public static final DeferredItem<BlockItem> DECK_STATION_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("deck_station", DECK_STATION_BLOCK);
    public static final DeferredBlock<Block> CARD_DECRYPTOR_BLOCK = BLOCKS.register("card_decryptor", DecryptorBlock::new);
    public static final DeferredItem<BlockItem> CARD_DECRYPTOR_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("card_decryptor", CARD_DECRYPTOR_BLOCK);
    public static final DeferredBlock<Block> VENDOR_BLOCK = BLOCKS.register("vendor", () -> new VendorBlock());
    public static final Supplier<BlockEntityType<VendorBlockEntity>> VENDOR_BE = BLOCK_ENTITY_TYPES.register("vendor", () -> BlockEntityType.Builder.of(VendorBlockEntity::new, VENDOR_BLOCK.get()).build(null));
    public static final DeferredItem<BlockItem> VENDOR_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("vendor", VENDOR_BLOCK);
    public static final Supplier<MenuType<LabMenu>> RESEARCH_LAB_MENU = MENU_TYPES.register("research_lab", () -> IMenuTypeExtension.create((id, inv, data) -> new LabMenu(id, inv, (LabBlockEntity) inv.player.level().getBlockEntity(data.readBlockPos()))));
    public static final DeferredBlock<Block> MARKET_TERMINAL_BLOCK = BLOCKS.register("market_terminal", () -> new MarketTerminalBlock());
    public static final Supplier<BlockEntityType<MarketTerminalBlockEntity>> MARKET_TERMINAL_BE = BLOCK_ENTITY_TYPES.register(
            "market_terminal",
            () -> BlockEntityType.Builder.of(MarketTerminalBlockEntity::new, MARKET_TERMINAL_BLOCK.get()).build(null)
    );
    public static final Supplier<MenuType<MarketTerminalCardMenu>> MARKET_TERMINAL_CARD_MENU = MENU_TYPES.register(
            "market_terminal_card",
            () -> IMenuTypeExtension.create((id, inv, data) -> new MarketTerminalCardMenu(
                    id,
                    inv,
                    (MarketTerminalBlockEntity) inv.player.level().getBlockEntity(data.readBlockPos())
            ))
    );
    public static final DeferredItem<BlockItem> MARKET_TERMINAL_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("market_terminal", MARKET_TERMINAL_BLOCK);
    public static final DeferredBlock<Block> SHIPMENT_TERMINAL_BLOCK = BLOCKS.register("shipment_terminal", () -> new ShipmentTerminalBlock());
    public static final Supplier<BlockEntityType<ShipmentTerminalBlockEntity>> SHIPMENT_TERMINAL_BE = BLOCK_ENTITY_TYPES.register(
            "shipment_terminal",
            () -> BlockEntityType.Builder.of(ShipmentTerminalBlockEntity::new, SHIPMENT_TERMINAL_BLOCK.get()).build(null)
    );
    public static final Supplier<MenuType<ShipmentTerminalMenu>> SHIPMENT_TERMINAL_MENU = MENU_TYPES.register(
            "shipment_terminal",
            () -> IMenuTypeExtension.create((id, inv, data) -> new ShipmentTerminalMenu(
                    id,
                    inv,
                    (ShipmentTerminalBlockEntity) inv.player.level().getBlockEntity(data.readBlockPos())
            ))
    );
    public static final DeferredItem<BlockItem> SHIPMENT_TERMINAL_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("shipment_terminal", SHIPMENT_TERMINAL_BLOCK);
    public static final DeferredBlock<Block> SHIPMENT_TERMINAL_MK2_BLOCK = BLOCKS.register("shipment_terminal_mk2", () -> new ShipmentTerminalMk2Block());
    public static final Supplier<BlockEntityType<ShipmentTerminalMk2BlockEntity>> SHIPMENT_TERMINAL_MK2_BE = BLOCK_ENTITY_TYPES.register(
            "shipment_terminal_mk2",
            () -> BlockEntityType.Builder.of(ShipmentTerminalMk2BlockEntity::new, SHIPMENT_TERMINAL_MK2_BLOCK.get()).build(null)
    );
    public static final DeferredItem<BlockItem> SHIPMENT_TERMINAL_MK2_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("shipment_terminal_mk2", SHIPMENT_TERMINAL_MK2_BLOCK);
    public static final DeferredBlock<Block> MARKET_AUTOBUYER_BLOCK = BLOCKS.register("market_autobuyer", () -> new MarketAutoBuyerBlock());
    public static final Supplier<BlockEntityType<MarketAutoBuyerBlockEntity>> MARKET_AUTOBUYER_BE = BLOCK_ENTITY_TYPES.register(
            "market_autobuyer",
            () -> BlockEntityType.Builder.of(MarketAutoBuyerBlockEntity::new, MARKET_AUTOBUYER_BLOCK.get()).build(null)
    );
    public static final Supplier<MenuType<MarketAutoBuyerMenu>> MARKET_AUTOBUYER_MENU = MENU_TYPES.register(
            "market_autobuyer",
            () -> IMenuTypeExtension.create((id, inv, data) -> new MarketAutoBuyerMenu(
                    id,
                    inv,
                    (MarketAutoBuyerBlockEntity) inv.player.level().getBlockEntity(data.readBlockPos())
            ))
    );
    public static final DeferredItem<BlockItem> MARKET_AUTOBUYER_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("market_autobuyer", MARKET_AUTOBUYER_BLOCK);
    public static final DeferredBlock<Block> MARKET_AUTOBUYER_MK2_BLOCK = BLOCKS.register("market_autobuyer_mk2", () -> new MarketAutoBuyerMk2Block());
    public static final Supplier<BlockEntityType<MarketAutoBuyerMk2BlockEntity>> MARKET_AUTOBUYER_MK2_BE = BLOCK_ENTITY_TYPES.register(
            "market_autobuyer_mk2",
            () -> BlockEntityType.Builder.of(MarketAutoBuyerMk2BlockEntity::new, MARKET_AUTOBUYER_MK2_BLOCK.get()).build(null)
    );
    public static final DeferredItem<BlockItem> MARKET_AUTOBUYER_MK2_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("market_autobuyer_mk2", MARKET_AUTOBUYER_MK2_BLOCK);
    public static final DeferredBlock<Block> CINNABAR_ORE_STONE_BLOCK = BLOCKS.register("cinnabar_ore_stone", () -> new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.STONE)
            .requiresCorrectToolForDrops()
            .strength(1.8F, 6.0F)));
    public static final DeferredBlock<Block> MIXED_METALS_ORE_STONE_BLOCK = BLOCKS.register("mixed_metals_ore_stone", () -> new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.STONE)
            .requiresCorrectToolForDrops()
            .strength(1.8F, 6.0F)));
    public static final DeferredBlock<Block> GEM_CLUSTERS_ORE_STONE_BLOCK = BLOCKS.register("gem_clusters_ore_stone", () -> new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.STONE)
            .requiresCorrectToolForDrops()
            .strength(1.8F, 6.0F)));
    public static final DeferredBlock<Block> CINNABAR_ORE_STONE_SLAB_BLOCK = BLOCKS.register("cinnabar_ore_stone_slab", () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(CINNABAR_ORE_STONE_BLOCK.get())));
    public static final DeferredBlock<Block> MIXED_METALS_ORE_STONE_SLAB_BLOCK = BLOCKS.register("mixed_metals_ore_stone_slab", () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(MIXED_METALS_ORE_STONE_BLOCK.get())));
    public static final DeferredBlock<Block> GEM_CLUSTERS_ORE_STONE_SLAB_BLOCK = BLOCKS.register("gem_clusters_ore_stone_slab", () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(GEM_CLUSTERS_ORE_STONE_BLOCK.get())));
    public static final DeferredItem<BlockItem> CINNABAR_ORE_STONE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("cinnabar_ore_stone", CINNABAR_ORE_STONE_BLOCK);
    public static final DeferredItem<BlockItem> MIXED_METALS_ORE_STONE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("mixed_metals_ore_stone", MIXED_METALS_ORE_STONE_BLOCK);
    public static final DeferredItem<BlockItem> GEM_CLUSTERS_ORE_STONE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("gem_clusters_ore_stone", GEM_CLUSTERS_ORE_STONE_BLOCK);
    public static final DeferredItem<BlockItem> CINNABAR_ORE_STONE_SLAB_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("cinnabar_ore_stone_slab", CINNABAR_ORE_STONE_SLAB_BLOCK);
    public static final DeferredItem<BlockItem> MIXED_METALS_ORE_STONE_SLAB_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("mixed_metals_ore_stone_slab", MIXED_METALS_ORE_STONE_SLAB_BLOCK);
    public static final DeferredItem<BlockItem> GEM_CLUSTERS_ORE_STONE_SLAB_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("gem_clusters_ore_stone_slab", GEM_CLUSTERS_ORE_STONE_SLAB_BLOCK);
    public static final DeferredBlock<Block> CRIMSITE_SURFACE_ORE_SPOT_BLOCK = BLOCKS.register("crimsite_surface_ore_spot", () -> new SurfaceOreSpotBlock(SurfaceOreType.CRIMSITE));
    public static final DeferredBlock<Block> VERIDIUM_SURFACE_ORE_SPOT_BLOCK = BLOCKS.register("veridium_surface_ore_spot", () -> new SurfaceOreSpotBlock(SurfaceOreType.VERIDIUM));
    public static final DeferredBlock<Block> ASURINE_SURFACE_ORE_SPOT_BLOCK = BLOCKS.register("asurine_surface_ore_spot", () -> new SurfaceOreSpotBlock(SurfaceOreType.ASURINE));
    public static final DeferredBlock<Block> OCHRUM_SURFACE_ORE_SPOT_BLOCK = BLOCKS.register("ochrum_surface_ore_spot", () -> new SurfaceOreSpotBlock(SurfaceOreType.OCHRUM));
    public static final DeferredBlock<Block> CINNABAR_SURFACE_ORE_SPOT_BLOCK = BLOCKS.register("cinnabar_surface_ore_spot", () -> new SurfaceOreSpotBlock(SurfaceOreType.CINNABAR));
    public static final DeferredBlock<Block> MIXED_METALS_SURFACE_ORE_SPOT_BLOCK = BLOCKS.register("mixed_metals_surface_ore_spot", () -> new SurfaceOreSpotBlock(SurfaceOreType.MIXED_METALS));
    public static final DeferredBlock<Block> GEM_CLUSTERS_SURFACE_ORE_SPOT_BLOCK = BLOCKS.register("gem_clusters_surface_ore_spot", () -> new SurfaceOreSpotBlock(SurfaceOreType.GEM_CLUSTERS));
    public static final DeferredBlock<Block> STONE_SURFACE_STONE_SPOT_BLOCK = BLOCKS.register("stone_surface_stone_spot", () -> new SurfaceStoneSpotBlock(SurfaceStoneType.STONE));
    public static final DeferredBlock<Block> DEEPSLATE_SURFACE_STONE_SPOT_BLOCK = BLOCKS.register("deepslate_surface_stone_spot", () -> new SurfaceStoneSpotBlock(SurfaceStoneType.DEEPSLATE));
    public static final DeferredBlock<Block> LIMESTONE_SURFACE_STONE_SPOT_BLOCK = BLOCKS.register("limestone_surface_stone_spot", () -> new SurfaceStoneSpotBlock(SurfaceStoneType.LIMESTONE));
    public static final DeferredBlock<Block> BASALT_SURFACE_STONE_SPOT_BLOCK = BLOCKS.register("basalt_surface_stone_spot", () -> new SurfaceStoneSpotBlock(SurfaceStoneType.BASALT));
    public static final DeferredBlock<Block> SCORIA_SURFACE_STONE_SPOT_BLOCK = BLOCKS.register("scoria_surface_stone_spot", () -> new SurfaceStoneSpotBlock(SurfaceStoneType.SCORIA));
    public static final Supplier<BlockEntityType<SurfaceOreSpotBlockEntity>> SURFACE_ORE_SPOT_BE = BLOCK_ENTITY_TYPES.register("surface_ore_spot", () -> BlockEntityType.Builder.of(
            SurfaceOreSpotBlockEntity::new,
            CRIMSITE_SURFACE_ORE_SPOT_BLOCK.get(),
            VERIDIUM_SURFACE_ORE_SPOT_BLOCK.get(),
            ASURINE_SURFACE_ORE_SPOT_BLOCK.get(),
            OCHRUM_SURFACE_ORE_SPOT_BLOCK.get(),
            CINNABAR_SURFACE_ORE_SPOT_BLOCK.get(),
            MIXED_METALS_SURFACE_ORE_SPOT_BLOCK.get(),
            GEM_CLUSTERS_SURFACE_ORE_SPOT_BLOCK.get()
    ).build(null));
    public static final DeferredItem<BlockItem> CRIMSITE_SURFACE_ORE_SPOT_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("crimsite_surface_ore_spot", CRIMSITE_SURFACE_ORE_SPOT_BLOCK);
    public static final DeferredItem<BlockItem> VERIDIUM_SURFACE_ORE_SPOT_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("veridium_surface_ore_spot", VERIDIUM_SURFACE_ORE_SPOT_BLOCK);
    public static final DeferredItem<BlockItem> ASURINE_SURFACE_ORE_SPOT_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("asurine_surface_ore_spot", ASURINE_SURFACE_ORE_SPOT_BLOCK);
    public static final DeferredItem<BlockItem> OCHRUM_SURFACE_ORE_SPOT_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("ochrum_surface_ore_spot", OCHRUM_SURFACE_ORE_SPOT_BLOCK);
    public static final DeferredItem<BlockItem> CINNABAR_SURFACE_ORE_SPOT_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("cinnabar_surface_ore_spot", CINNABAR_SURFACE_ORE_SPOT_BLOCK);
    public static final DeferredItem<BlockItem> MIXED_METALS_SURFACE_ORE_SPOT_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("mixed_metals_surface_ore_spot", MIXED_METALS_SURFACE_ORE_SPOT_BLOCK);
    public static final DeferredItem<BlockItem> GEM_CLUSTERS_SURFACE_ORE_SPOT_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("gem_clusters_surface_ore_spot", GEM_CLUSTERS_SURFACE_ORE_SPOT_BLOCK);
    public static final DeferredItem<BlockItem> STONE_SURFACE_STONE_SPOT_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("stone_surface_stone_spot", STONE_SURFACE_STONE_SPOT_BLOCK);
    public static final DeferredItem<BlockItem> DEEPSLATE_SURFACE_STONE_SPOT_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("deepslate_surface_stone_spot", DEEPSLATE_SURFACE_STONE_SPOT_BLOCK);
    public static final DeferredItem<BlockItem> LIMESTONE_SURFACE_STONE_SPOT_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("limestone_surface_stone_spot", LIMESTONE_SURFACE_STONE_SPOT_BLOCK);
    public static final DeferredItem<BlockItem> BASALT_SURFACE_STONE_SPOT_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("basalt_surface_stone_spot", BASALT_SURFACE_STONE_SPOT_BLOCK);
    public static final DeferredItem<BlockItem> SCORIA_SURFACE_STONE_SPOT_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("scoria_surface_stone_spot", SCORIA_SURFACE_STONE_SPOT_BLOCK);
    public static final DeferredHolder<Feature<?>, SurfaceOrePatchFeature> SURFACE_ORE_PATCH_FEATURE = FEATURES.register("surface_ore_patch", SurfaceOrePatchFeature::new);
    public static final DeferredHolder<Feature<?>, SurfaceStonePatchFeature> SURFACE_STONE_PATCH_FEATURE = FEATURES.register("surface_stone_patch", SurfaceStonePatchFeature::new);
    public static final DeferredBlock<Block> BURNER_LAB_BLOCK = BLOCKS.register("burner_lab", () -> new BurnerLabBlock());
    public static final DeferredBlock<Block> MECHANICAL_LAB_BLOCK = BLOCKS.register("mechanical_lab", MechanicalLabBlock::new);
    public static final DeferredBlock<Block> MODULAR_LAB_BLOCK = BLOCKS.register("modular_lab", ModularLabBlock::new);
    public static final Supplier<BlockEntityType<LabBlockEntity>> LAB_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "burner_lab",
            () -> BlockEntityType.Builder.of(
                    LabBlockEntity::new,
                    BURNER_LAB_BLOCK.get(),
                    MECHANICAL_LAB_BLOCK.get(),
                    MODULAR_LAB_BLOCK.get()
            ).build(null)
    );
    public static final Supplier<MenuType<LabMenu>> BURNER_LAB_MENU = MENU_TYPES.register("burner_lab", () -> IMenuTypeExtension.create((id, inv, data) -> new LabMenu(id, inv, (LabBlockEntity) inv.player.level().getBlockEntity(data.readBlockPos()))));
    public static final DeferredItem<BlockItem> BURNER_LAB_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("burner_lab", BURNER_LAB_BLOCK);
    public static final DeferredItem<BlockItem> MECHANICAL_LAB_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("mechanical_lab", MECHANICAL_LAB_BLOCK);
    public static final DeferredItem<BlockItem> MODULAR_LAB_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("modular_lab", MODULAR_LAB_BLOCK);

    public static final DeferredBlock<Block> ROGUELIKE_ALTAR_BLOCK = BLOCKS.register("roguelike_altar", RoguelikeAltarBlock::new);
    public static final Supplier<BlockEntityType<RoguelikeAltarBlockEntity>> ROGUELIKE_ALTAR_BE = BLOCK_ENTITY_TYPES.register("roguelike_altar", () -> BlockEntityType.Builder.of(RoguelikeAltarBlockEntity::new, ROGUELIKE_ALTAR_BLOCK.get()).build(null));
    public static final DeferredItem<BlockItem> ROGUELIKE_ALTAR_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("roguelike_altar", ROGUELIKE_ALTAR_BLOCK);
    public static final DeferredBlock<Block> CUSTOM_DUNGEON_CRYSTAL_FORGE_BLOCK = BLOCKS.register("custom_dungeon_crystal_forge", () -> new CustomDungeonCrystalForgeBlock());
    public static final Supplier<BlockEntityType<CustomDungeonCrystalForgeBlockEntity>> CUSTOM_DUNGEON_CRYSTAL_FORGE_BE = BLOCK_ENTITY_TYPES.register(
            "custom_dungeon_crystal_forge",
            () -> BlockEntityType.Builder.of(CustomDungeonCrystalForgeBlockEntity::new, CUSTOM_DUNGEON_CRYSTAL_FORGE_BLOCK.get()).build(null)
    );
    public static final Supplier<MenuType<CustomDungeonCrystalForgeMenu>> CUSTOM_DUNGEON_CRYSTAL_FORGE_MENU = MENU_TYPES.register(
            "custom_dungeon_crystal_forge",
            () -> IMenuTypeExtension.create((id, inv, data) -> new CustomDungeonCrystalForgeMenu(
                    id,
                    inv,
                    (CustomDungeonCrystalForgeBlockEntity) inv.player.level().getBlockEntity(data.readBlockPos())
            ))
    );
    public static final DeferredItem<BlockItem> CUSTOM_DUNGEON_CRYSTAL_FORGE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("custom_dungeon_crystal_forge", CUSTOM_DUNGEON_CRYSTAL_FORGE_BLOCK);
    public static final DeferredBlock<Block> DUNGEON_OBJECTIVE_ALTAR_BLOCK = BLOCKS.register("dungeon_objective_altar", DungeonObjectiveAltarBlock::new);
    public static final DeferredItem<BlockItem> DUNGEON_OBJECTIVE_ALTAR_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("dungeon_objective_altar", DUNGEON_OBJECTIVE_ALTAR_BLOCK);
    public static final DeferredBlock<Block> ROGUELIKE_PORTAL_BLOCK = BLOCKS.register("roguelike_portal", RoguelikePortalBlock::new);
    public static final Supplier<BlockEntityType<RoguelikePortalBlockEntity>> ROGUELIKE_PORTAL_BE = BLOCK_ENTITY_TYPES.register("roguelike_portal", () -> BlockEntityType.Builder.of(RoguelikePortalBlockEntity::new, ROGUELIKE_PORTAL_BLOCK.get()).build(null));
    public static final DeferredItem<BlockItem> ROGUELIKE_PORTAL_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("roguelike_portal", ROGUELIKE_PORTAL_BLOCK);
    public static final DeferredBlock<Block> DUNGEON_RETURN_PORTAL_BLOCK = BLOCKS.register("dungeon_return_portal", DungeonReturnPortalBlock::new);
    public static final DeferredItem<BlockItem> DUNGEON_RETURN_PORTAL_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("dungeon_return_portal", DUNGEON_RETURN_PORTAL_BLOCK);

    public static final DeferredItem<Item> ENCOUNTER_WAND_ITEM = ITEMS.registerItem("encounter_wand", EncounterWandItem::new);
    public static final DeferredItem<Item> EMPTY_DUNGEON_CRYSTAL_ITEM = ITEMS.registerItem("empty_dungeon_crystal", EmptyDungeonCrystalItem::new);
    public static final DeferredItem<Item> DUNGEON_CRYSTAL_ITEM = ITEMS.registerItem("dungeon_crystal", DungeonCrystalItem::new);
    public static final DeferredItem<Item> DUNGEON_COMPLETION_CRATE_ITEM = ITEMS.registerItem("dungeon_completion_crate", DungeonCompletionCrateItem::new);
    public static final DeferredItem<Item> DUNGEON_SCAVENGER_TOKEN_ITEM = ITEMS.registerItem("dungeon_scavenger_token", DungeonScavengerTokenItem::new);
    public static final DeferredItem<Item> SURFACE_ORE_DEBUG_COMPASS_ITEM = ITEMS.registerItem("surface_ore_debug_compass", SurfaceOreDebugCompassItem::new);
    public static final DeferredItem<Item> SURFACE_STONE_DEBUG_COMPASS_ITEM = ITEMS.registerItem("surface_stone_debug_compass", SurfaceStoneDebugCompassItem::new);
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
    public static final DeferredItem<Item> SPEED_MODULE_CARD_ITEM = ITEMS.registerItem("speed_module_card", properties -> new SpeedModuleCardItem(properties.stacksTo(16)));
    public static final DeferredItem<Item> PRODUCTIVITY_MODULE_CARD_ITEM = ITEMS.registerItem("productivity_module_card", properties -> new ProductivityModuleCardItem(properties.stacksTo(16)));
    public static final DeferredItem<Item> CARD_MODULE_ITEM = ITEMS.registerItem("card_module", CardModuleItem::new);
    public static final DeferredItem<Item> CARD_BOOSTER_ITEM = ITEMS.registerItem("card_booster", CardBoosterItem::new);
    public static final DeferredItem<Item> CARD_BOOSTER_BOX_ITEM = ITEMS.registerItem("card_booster_box", CardBoosterBoxItem::new);
    public static final DeferredItem<Item> CARD_DECK_CORE_ITEM = ITEMS.registerItem("card_deck_core", DeckCoreItem::new);
    public static final DeferredItem<Item> CARD_DECK_BOX_ITEM = ITEMS.registerItem("card_deck_box", DeckBoxItem::new);
    public static final DeferredItem<Item> CARD_DECK_ITEM = ITEMS.registerItem("card_deck", DeckItem::new);
    public static final DeferredItem<Item> CARD_SLEEVE_ITEM = ITEMS.registerItem("card_sleeve", CardSleeveItem::new);
    public static final DeferredItem<Item> CARD_TOKEN_ITEM = ITEMS.registerItem("card_token", CardTokenItem::new);
    public static final DeferredItem<Item> VENDOR_DISCOUNT_CHARM_ITEM = ITEMS.registerItem("vendor_discount_charm", VendorDiscountCharmItem::new);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> DUNGEON_CRYSTAL_THEME =
            DATA_COMPONENT_TYPES.registerComponentType(
                    "dungeon_crystal_theme",
                    builder -> builder.persistent(ResourceLocation.CODEC).networkSynchronized(ResourceLocation.STREAM_CODEC)
            );
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> DUNGEON_CRYSTAL_OBJECTIVE =
            DATA_COMPONENT_TYPES.registerComponentType(
                    "dungeon_crystal_objective",
                    builder -> builder.persistent(ResourceLocation.CODEC).networkSynchronized(ResourceLocation.STREAM_CODEC)
            );
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.incore"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> ENCOUNTER_WAND_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(ROGUELIKE_ALTAR_BLOCK_ITEM.get());
                output.accept(CUSTOM_DUNGEON_CRYSTAL_FORGE_BLOCK_ITEM.get());
                output.accept(DUNGEON_OBJECTIVE_ALTAR_BLOCK_ITEM.get());
                output.accept(ROGUELIKE_PORTAL_BLOCK_ITEM.get());
                output.accept(DUNGEON_RETURN_PORTAL_BLOCK_ITEM.get());
                output.accept(EMPTY_DUNGEON_CRYSTAL_ITEM.get());
                output.accept(DUNGEON_CRYSTAL_ITEM.get());
                output.accept(DUNGEON_COMPLETION_CRATE_ITEM.get());
                output.accept(DUNGEON_SCAVENGER_TOKEN_ITEM.get());
                if (!FMLEnvironment.production) {
                    output.accept(SURFACE_ORE_DEBUG_COMPASS_ITEM.get());
                    output.accept(SURFACE_STONE_DEBUG_COMPASS_ITEM.get());
                }
                output.accept(ENCOUNTER_SPAWNER_BLOCK_ITEM.get());
                output.accept(GACHA_CRATE_BLOCK_ITEM.get());
                output.accept(ARENA_ORB_BLOCK_ITEM.get());
                output.accept(ARENA_REWARD_CRATE_BLOCK_ITEM.get());
                output.accept(DECK_STATION_BLOCK_ITEM.get());
                output.accept(CARD_DECRYPTOR_BLOCK_ITEM.get());
                output.accept(VENDOR_BLOCK_ITEM.get());
                output.accept(MARKET_TERMINAL_BLOCK_ITEM.get());
                output.accept(SHIPMENT_TERMINAL_BLOCK_ITEM.get());
                output.accept(SHIPMENT_TERMINAL_MK2_BLOCK_ITEM.get());
                output.accept(MARKET_AUTOBUYER_BLOCK_ITEM.get());
                output.accept(MARKET_AUTOBUYER_MK2_BLOCK_ITEM.get());
                output.accept(CINNABAR_ORE_STONE_BLOCK_ITEM.get());
                output.accept(MIXED_METALS_ORE_STONE_BLOCK_ITEM.get());
                output.accept(GEM_CLUSTERS_ORE_STONE_BLOCK_ITEM.get());
                output.accept(CINNABAR_ORE_STONE_SLAB_BLOCK_ITEM.get());
                output.accept(MIXED_METALS_ORE_STONE_SLAB_BLOCK_ITEM.get());
                output.accept(GEM_CLUSTERS_ORE_STONE_SLAB_BLOCK_ITEM.get());
                output.accept(BURNER_LAB_BLOCK_ITEM.get());
                output.accept(MECHANICAL_LAB_BLOCK_ITEM.get());
                output.accept(MODULAR_LAB_BLOCK_ITEM.get());
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
                output.accept(SPEED_MODULE_CARD_ITEM.get());
                output.accept(PRODUCTIVITY_MODULE_CARD_ITEM.get());
                output.accept(CARD_MODULE_ITEM.get());
                output.accept(CARD_BOOSTER_ITEM.get());
                output.accept(CARD_BOOSTER_BOX_ITEM.get());
                output.accept(CARD_DECK_CORE_ITEM.get());
                output.accept(CARD_DECK_BOX_ITEM.get());
                output.accept(CARD_DECK_ITEM.get());
                output.accept(CARD_SLEEVE_ITEM.get());
                output.accept(CARD_TOKEN_ITEM.get());
                output.accept(VENDOR_DISCOUNT_CHARM_ITEM.get());
            }).build());
}
