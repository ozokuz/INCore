package io.github.ozokuz.incore;

import io.github.ozokuz.incore.features.arena.content.ArenaOrbBlock;
import io.github.ozokuz.incore.features.arena.content.ArenaRewardCrateBlock;
import io.github.ozokuz.incore.features.arena.content.ArenaRewardCrateBlockEntity;
import io.github.ozokuz.incore.features.arena.content.ArenaRewardCrateBlockItem;
import io.github.ozokuz.incore.features.encounter_spawner.EncounterSpawnerBE;
import io.github.ozokuz.incore.features.encounter_spawner.EncounterSpawnerBlock;
import io.github.ozokuz.incore.features.encounter_spawner.EncounterWandItem;
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
import io.github.ozokuz.incore.features.vendingmachine.VendingMachineBlock;
import io.github.ozokuz.incore.features.vendingmachine.VendingMachineBlockEntity;
import io.github.ozokuz.incore.features.vendingmachine.VendingMachineDiscountCharmItem;
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
import io.github.ozokuz.incore.features.surfaceore.SurfaceOreLocatorItem;
import io.github.ozokuz.incore.features.surfaceore.SurfaceOreSpotBlock;
import io.github.ozokuz.incore.features.surfaceore.SurfaceOreSpotBlockEntity;
import io.github.ozokuz.incore.features.surfaceore.SurfaceOreType;
import io.github.ozokuz.incore.features.surfaceore.SurfaceOreTypeLocatorItem;
import io.github.ozokuz.incore.features.surfaceore.SurfaceStoneDebugCompassItem;
import io.github.ozokuz.incore.features.surfaceore.SurfaceStoneLocatorItem;
import io.github.ozokuz.incore.features.surfaceore.SurfaceStonePatchFeature;
import io.github.ozokuz.incore.features.surfaceore.SurfaceStoneSpotBlock;
import io.github.ozokuz.incore.features.surfaceore.SurfaceStoneType;
import io.github.ozokuz.incore.features.surfaceore.SurfaceStoneTypeLocatorItem;
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
import io.github.ozokuz.incore.features.roguelike.worldgen.DungeonChunkGenerator;
import io.github.ozokuz.incore.features.entropy.EntropyBoosterItem;
import io.github.ozokuz.incore.features.entropy.EntropyCrateItem;
import io.github.ozokuz.incore.features.entropy.EntropyVesselItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.chunk.ChunkGenerator;
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

import com.mojang.serialization.Codec;
import java.util.function.Supplier;

public class Registration {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(INCore.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(INCore.MODID);
    public static final DeferredRegister.DataComponents DATA_COMPONENT_TYPES = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, INCore.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, INCore.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,  INCore.MODID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, INCore.MODID);
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, INCore.MODID);
    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS = DeferredRegister.create(Registries.CHUNK_GENERATOR, INCore.MODID);

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        DATA_COMPONENT_TYPES.register(bus);
        CREATIVE_MODE_TABS.register(bus);
        BLOCK_ENTITY_TYPES.register(bus);
        MENU_TYPES.register(bus);
        FEATURES.register(bus);
        CHUNK_GENERATORS.register(bus);
    }

    public static final Supplier<MapCodec<DungeonChunkGenerator>> DUNGEON_CHUNK_GENERATOR = CHUNK_GENERATORS.register("dungeon", () -> DungeonChunkGenerator.CODEC);

    public static final DeferredBlock<Block> ENCOUNTER_SPAWNER_BLOCK = BLOCKS.register("encounter_spawner", EncounterSpawnerBlock::new);
    public static final Supplier<BlockEntityType<EncounterSpawnerBE>> ENCOUNTER_SPAWNER_BE = BLOCK_ENTITY_TYPES.register("encounter_spawner", () -> BlockEntityType.Builder.of(EncounterSpawnerBE::new, ENCOUNTER_SPAWNER_BLOCK.get()).build(null));
    public static final DeferredItem<BlockItem> ENCOUNTER_SPAWNER_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("encounter_spawner", ENCOUNTER_SPAWNER_BLOCK);
    public static final DeferredBlock<Block> GACHA_RIFT_BLOCK = BLOCKS.register("gacha_rift", GachaCrateBlock::new);
    public static final Supplier<BlockEntityType<GachaCrateBlockEntity>> GACHA_RIFT_BE = BLOCK_ENTITY_TYPES.register("gacha_rift", () -> BlockEntityType.Builder.of(GachaCrateBlockEntity::new, GACHA_RIFT_BLOCK.get()).build(null));
    public static final DeferredItem<BlockItem> GACHA_RIFT_BLOCK_ITEM = ITEMS.registerItem("gacha_rift", properties -> new GachaCrateBlockItem(GACHA_RIFT_BLOCK.get(), properties));
    public static final DeferredBlock<Block> ARENA_ORB_BLOCK = BLOCKS.register("arena_orb", ArenaOrbBlock::new);
    public static final DeferredItem<BlockItem> ARENA_ORB_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("arena_orb", ARENA_ORB_BLOCK);
    public static final DeferredBlock<Block> ARENA_REWARD_RIFT_BLOCK = BLOCKS.register("arena_reward_rift", ArenaRewardCrateBlock::new);
    public static final Supplier<BlockEntityType<ArenaRewardCrateBlockEntity>> ARENA_REWARD_RIFT_BE =
            BLOCK_ENTITY_TYPES.register("arena_reward_rift", () -> BlockEntityType.Builder.of(ArenaRewardCrateBlockEntity::new, ARENA_REWARD_RIFT_BLOCK.get()).build(null));
    public static final DeferredItem<BlockItem> ARENA_REWARD_RIFT_BLOCK_ITEM =
            ITEMS.registerItem("arena_reward_rift", properties -> new ArenaRewardCrateBlockItem(ARENA_REWARD_RIFT_BLOCK.get(), properties));
    public static final DeferredBlock<Block> DECK_STATION_BLOCK = BLOCKS.register("deck_station", () -> new DeckStationBlock());
    public static final Supplier<BlockEntityType<DeckStationBlockEntity>> DECK_STATION_BE = BLOCK_ENTITY_TYPES.register("deck_station", () -> BlockEntityType.Builder.of(DeckStationBlockEntity::new, DECK_STATION_BLOCK.get()).build(null));
    public static final Supplier<MenuType<DeckStationMenu>> DECK_STATION_MENU = MENU_TYPES.register("deck_station", () -> IMenuTypeExtension.create((id, inv, data) -> new DeckStationMenu(id, inv, (DeckStationBlockEntity) inv.player.level().getBlockEntity(data.readBlockPos()))));
    public static final DeferredItem<BlockItem> DECK_STATION_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("deck_station", DECK_STATION_BLOCK);
    public static final DeferredBlock<Block> CARD_DECRYPTOR_BLOCK = BLOCKS.register("card_decryptor", DecryptorBlock::new);
    public static final DeferredItem<BlockItem> CARD_DECRYPTOR_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("card_decryptor", CARD_DECRYPTOR_BLOCK);
    public static final DeferredBlock<Block> VENDING_MACHINE_BLOCK = BLOCKS.register("vending_machine", () -> new VendingMachineBlock());
    public static final Supplier<BlockEntityType<VendingMachineBlockEntity>> VENDING_MACHINE_BE = BLOCK_ENTITY_TYPES.register("vending_machine", () -> BlockEntityType.Builder.of(VendingMachineBlockEntity::new, VENDING_MACHINE_BLOCK.get()).build(null));
    public static final DeferredItem<BlockItem> VENDING_MACHINE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("vending_machine", VENDING_MACHINE_BLOCK);
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
    public static final DeferredBlock<Block> QUARTZ_ORE_STONE_BLOCK = BLOCKS.register("quartz_ore_stone", () -> new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.QUARTZ)
            .requiresCorrectToolForDrops()
            .strength(1.8F, 6.0F)));
    public static final DeferredBlock<Block> QUARTZ_ORE_STONE_SLAB_BLOCK = BLOCKS.register("quartz_ore_stone_slab", () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(QUARTZ_ORE_STONE_BLOCK.get())));
    public static final DeferredItem<BlockItem> CINNABAR_ORE_STONE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("cinnabar_ore_stone", CINNABAR_ORE_STONE_BLOCK);
    public static final DeferredItem<BlockItem> MIXED_METALS_ORE_STONE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("mixed_metals_ore_stone", MIXED_METALS_ORE_STONE_BLOCK);
    public static final DeferredItem<BlockItem> GEM_CLUSTERS_ORE_STONE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("gem_clusters_ore_stone", GEM_CLUSTERS_ORE_STONE_BLOCK);
    public static final DeferredItem<BlockItem> CINNABAR_ORE_STONE_SLAB_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("cinnabar_ore_stone_slab", CINNABAR_ORE_STONE_SLAB_BLOCK);
    public static final DeferredItem<BlockItem> MIXED_METALS_ORE_STONE_SLAB_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("mixed_metals_ore_stone_slab", MIXED_METALS_ORE_STONE_SLAB_BLOCK);
    public static final DeferredItem<BlockItem> GEM_CLUSTERS_ORE_STONE_SLAB_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("gem_clusters_ore_stone_slab", GEM_CLUSTERS_ORE_STONE_SLAB_BLOCK);
    public static final DeferredItem<BlockItem> QUARTZ_ORE_STONE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("quartz_ore_stone", QUARTZ_ORE_STONE_BLOCK);
    public static final DeferredItem<BlockItem> QUARTZ_ORE_STONE_SLAB_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("quartz_ore_stone_slab", QUARTZ_ORE_STONE_SLAB_BLOCK);
    public static final DeferredBlock<Block> CRIMSITE_SURFACE_ORE_SPOT_BLOCK = BLOCKS.register("crimsite_surface_ore_spot", () -> new SurfaceOreSpotBlock(SurfaceOreType.CRIMSITE));
    public static final DeferredBlock<Block> VERIDIUM_SURFACE_ORE_SPOT_BLOCK = BLOCKS.register("veridium_surface_ore_spot", () -> new SurfaceOreSpotBlock(SurfaceOreType.VERIDIUM));
    public static final DeferredBlock<Block> ASURINE_SURFACE_ORE_SPOT_BLOCK = BLOCKS.register("asurine_surface_ore_spot", () -> new SurfaceOreSpotBlock(SurfaceOreType.ASURINE));
    public static final DeferredBlock<Block> OCHRUM_SURFACE_ORE_SPOT_BLOCK = BLOCKS.register("ochrum_surface_ore_spot", () -> new SurfaceOreSpotBlock(SurfaceOreType.OCHRUM));
    public static final DeferredBlock<Block> CINNABAR_SURFACE_ORE_SPOT_BLOCK = BLOCKS.register("cinnabar_surface_ore_spot", () -> new SurfaceOreSpotBlock(SurfaceOreType.CINNABAR));
    public static final DeferredBlock<Block> MIXED_METALS_SURFACE_ORE_SPOT_BLOCK = BLOCKS.register("mixed_metals_surface_ore_spot", () -> new SurfaceOreSpotBlock(SurfaceOreType.MIXED_METALS));
    public static final DeferredBlock<Block> GEM_CLUSTERS_SURFACE_ORE_SPOT_BLOCK = BLOCKS.register("gem_clusters_surface_ore_spot", () -> new SurfaceOreSpotBlock(SurfaceOreType.GEM_CLUSTERS));
    public static final DeferredBlock<Block> NETHER_QUARTZ_SURFACE_ORE_SPOT_BLOCK = BLOCKS.register("nether_quartz_surface_ore_spot", () -> new SurfaceOreSpotBlock(SurfaceOreType.NETHER_QUARTZ));
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
            GEM_CLUSTERS_SURFACE_ORE_SPOT_BLOCK.get(),
            NETHER_QUARTZ_SURFACE_ORE_SPOT_BLOCK.get()
    ).build(null));
    public static final DeferredItem<BlockItem> CRIMSITE_SURFACE_ORE_SPOT_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("crimsite_surface_ore_spot", CRIMSITE_SURFACE_ORE_SPOT_BLOCK);
    public static final DeferredItem<BlockItem> VERIDIUM_SURFACE_ORE_SPOT_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("veridium_surface_ore_spot", VERIDIUM_SURFACE_ORE_SPOT_BLOCK);
    public static final DeferredItem<BlockItem> ASURINE_SURFACE_ORE_SPOT_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("asurine_surface_ore_spot", ASURINE_SURFACE_ORE_SPOT_BLOCK);
    public static final DeferredItem<BlockItem> OCHRUM_SURFACE_ORE_SPOT_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("ochrum_surface_ore_spot", OCHRUM_SURFACE_ORE_SPOT_BLOCK);
    public static final DeferredItem<BlockItem> CINNABAR_SURFACE_ORE_SPOT_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("cinnabar_surface_ore_spot", CINNABAR_SURFACE_ORE_SPOT_BLOCK);
    public static final DeferredItem<BlockItem> MIXED_METALS_SURFACE_ORE_SPOT_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("mixed_metals_surface_ore_spot", MIXED_METALS_SURFACE_ORE_SPOT_BLOCK);
    public static final DeferredItem<BlockItem> GEM_CLUSTERS_SURFACE_ORE_SPOT_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("gem_clusters_surface_ore_spot", GEM_CLUSTERS_SURFACE_ORE_SPOT_BLOCK);
    public static final DeferredItem<BlockItem> NETHER_QUARTZ_SURFACE_ORE_SPOT_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("nether_quartz_surface_ore_spot", NETHER_QUARTZ_SURFACE_ORE_SPOT_BLOCK);
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

    public static final DeferredItem<Item> CRIMSITE_ORE_LOCATOR = ITEMS.registerItem("crimsite_ore_locator", properties -> new SurfaceOreLocatorItem(properties, SurfaceOreType.CRIMSITE, "Cr", 1));
    public static final DeferredItem<Item> VERIDIUM_ORE_LOCATOR = ITEMS.registerItem("veridium_ore_locator", properties -> new SurfaceOreLocatorItem(properties, SurfaceOreType.VERIDIUM, "Ve", 5));
    public static final DeferredItem<Item> ASURINE_ORE_LOCATOR = ITEMS.registerItem("asurine_ore_locator", properties -> new SurfaceOreLocatorItem(properties, SurfaceOreType.ASURINE, "As", 11));
    public static final DeferredItem<Item> OCHRUM_ORE_LOCATOR = ITEMS.registerItem("ochrum_ore_locator", properties -> new SurfaceOreLocatorItem(properties, SurfaceOreType.OCHRUM, "Oc", 4));
    public static final DeferredItem<Item> CINNABAR_ORE_LOCATOR = ITEMS.registerItem("cinnabar_ore_locator", properties -> new SurfaceOreLocatorItem(properties, SurfaceOreType.CINNABAR, "Ci", 1));
    public static final DeferredItem<Item> MIXED_METALS_ORE_LOCATOR = ITEMS.registerItem("mixed_metals_ore_locator", properties -> new SurfaceOreLocatorItem(properties, SurfaceOreType.MIXED_METALS, "Mm", 7));
    public static final DeferredItem<Item> GEM_CLUSTERS_ORE_LOCATOR = ITEMS.registerItem("gem_clusters_ore_locator", properties -> new SurfaceOreLocatorItem(properties, SurfaceOreType.GEM_CLUSTERS, "Gc", 6));
    public static final DeferredItem<Item> NETHER_QUARTZ_ORE_LOCATOR = ITEMS.registerItem("nether_quartz_ore_locator", properties -> new SurfaceOreLocatorItem(properties, SurfaceOreType.NETHER_QUARTZ, "Nq", 9));

    public static final DeferredItem<Item> STONE_LOCATOR = ITEMS.registerItem("stone_locator", properties -> new SurfaceStoneLocatorItem(properties, SurfaceStoneType.STONE, "St", 8));
    public static final DeferredItem<Item> DEEPSLATE_LOCATOR = ITEMS.registerItem("deepslate_locator", properties -> new SurfaceStoneLocatorItem(properties, SurfaceStoneType.DEEPSLATE, "Ds", 0));
    public static final DeferredItem<Item> LIMESTONE_LOCATOR = ITEMS.registerItem("limestone_locator", properties -> new SurfaceStoneLocatorItem(properties, SurfaceStoneType.LIMESTONE, "Li", 15));
    public static final DeferredItem<Item> BASALT_LOCATOR = ITEMS.registerItem("basalt_locator", properties -> new SurfaceStoneLocatorItem(properties, SurfaceStoneType.BASALT, "Ba", 0));
    public static final DeferredItem<Item> SCORIA_LOCATOR = ITEMS.registerItem("scoria_locator", properties -> new SurfaceStoneLocatorItem(properties, SurfaceStoneType.SCORIA, "Sc", 14));

    public static final DeferredItem<Item> UNIVERSAL_ORE_LOCATOR = ITEMS.registerItem("universal_ore_locator", properties -> new SurfaceOreTypeLocatorItem(properties, "Or", 7));
    public static final DeferredItem<Item> UNIVERSAL_STONE_LOCATOR = ITEMS.registerItem("universal_stone_locator", properties -> new SurfaceStoneTypeLocatorItem(properties, "St", 8));

    public static final DeferredItem<Item> ENTROPY_CRATE_ITEM = ITEMS.registerItem("entropy_crate", EntropyCrateItem::new);
    public static final DeferredItem<Item> ENTROPY_BOOSTER_SMALL_ITEM = ITEMS.registerItem("entropy_booster_small", properties -> new EntropyBoosterItem(properties, 50));
    public static final DeferredItem<Item> ENTROPY_BOOSTER_LARGE_ITEM = ITEMS.registerItem("entropy_booster_large", properties -> new EntropyBoosterItem(properties, 100));
    public static final DeferredItem<Item> ENTROPY_VESSEL_ITEM = ITEMS.registerItem("entropy_vessel", EntropyVesselItem::new);
    public static final DeferredItem<Item> BATTLEPASS_LANE_UNLOCK_ITEM = ITEMS.registerItem(
            "battlepass_lane_unlock",
            properties -> new BattlePassLaneUnlockItem(properties)
    );
    public static final DeferredItem<Item> BASIC_TIME_PIECE_ITEM = ITEMS.registerItem("basic_time_piece", properties -> new GachaPermitItem(properties, GachaPermitItem.PermitMode.BASIC));
    public static final DeferredItem<Item> CHARTERED_TIME_PIECE_ITEM = ITEMS.registerItem("chartered_time_piece", properties -> new GachaPermitItem(properties, GachaPermitItem.PermitMode.CHARTERED));
    public static final DeferredItem<Item> TIME_PIECE_ITEM = ITEMS.registerItem("time_piece", properties -> new GachaPermitItem(properties, GachaPermitItem.PermitMode.SPECIFIC));
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
    public static final DeferredItem<Item> VENDING_MACHINE_DISCOUNT_CHARM_ITEM = ITEMS.registerItem("vending_machine_discount_charm", VendingMachineDiscountCharmItem::new);

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
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> BATTLEPASS_LANE =
            DATA_COMPONENT_TYPES.registerComponentType(
                    "battlepass_lane",
                    builder -> builder.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8)
            );
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB = CREATIVE_MODE_TABS.register("main_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.incore"))
            .icon(() -> DUNGEON_CRYSTAL_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                // Crystal Making
                output.accept(ROGUELIKE_ALTAR_BLOCK_ITEM.get());
                output.accept(CUSTOM_DUNGEON_CRYSTAL_FORGE_BLOCK_ITEM.get());
                output.accept(EMPTY_DUNGEON_CRYSTAL_ITEM.get());

                // Ore Spots
                // Specific Locators
                output.accept(CRIMSITE_ORE_LOCATOR.get());
                output.accept(VERIDIUM_ORE_LOCATOR.get());
                output.accept(ASURINE_ORE_LOCATOR.get());
                output.accept(OCHRUM_ORE_LOCATOR.get());
                output.accept(CINNABAR_ORE_LOCATOR.get());
                output.accept(MIXED_METALS_ORE_LOCATOR.get());
                output.accept(GEM_CLUSTERS_ORE_LOCATOR.get());
                output.accept(NETHER_QUARTZ_ORE_LOCATOR.get());
                output.accept(STONE_LOCATOR.get());
                output.accept(DEEPSLATE_LOCATOR.get());
                output.accept(LIMESTONE_LOCATOR.get());
                output.accept(BASALT_LOCATOR.get());
                output.accept(SCORIA_LOCATOR.get());
                // Universal Locators
                output.accept(UNIVERSAL_ORE_LOCATOR.get());
                output.accept(UNIVERSAL_STONE_LOCATOR.get());
                // Ore Stones
                output.accept(CINNABAR_ORE_STONE_BLOCK_ITEM.get());
                output.accept(CINNABAR_ORE_STONE_SLAB_BLOCK_ITEM.get());
                output.accept(MIXED_METALS_ORE_STONE_BLOCK_ITEM.get());
                output.accept(MIXED_METALS_ORE_STONE_SLAB_BLOCK_ITEM.get());
                output.accept(GEM_CLUSTERS_ORE_STONE_BLOCK_ITEM.get());
                output.accept(GEM_CLUSTERS_ORE_STONE_SLAB_BLOCK_ITEM.get());
                output.accept(QUARTZ_ORE_STONE_BLOCK_ITEM.get());
                output.accept(QUARTZ_ORE_STONE_SLAB_BLOCK_ITEM.get());

                // VendingMachine
                output.accept(VENDING_MACHINE_BLOCK_ITEM.get());

                // Market
                output.accept(MARKET_TERMINAL_BLOCK_ITEM.get());
                output.accept(SHIPMENT_TERMINAL_BLOCK_ITEM.get());
                output.accept(SHIPMENT_TERMINAL_MK2_BLOCK_ITEM.get());
                output.accept(MARKET_AUTOBUYER_BLOCK_ITEM.get());
                output.accept(MARKET_AUTOBUYER_MK2_BLOCK_ITEM.get());

                // Entropy
                output.accept(ENTROPY_BOOSTER_SMALL_ITEM.get());
                output.accept(ENTROPY_BOOSTER_LARGE_ITEM.get());

                // Gacha
                output.accept(BASIC_TIME_PIECE_ITEM.get());
                output.accept(CHARTERED_TIME_PIECE_ITEM.get());

                // Research
                output.accept(BURNER_LAB_BLOCK_ITEM.get());
                output.accept(MECHANICAL_LAB_BLOCK_ITEM.get());
                output.accept(MODULAR_LAB_BLOCK_ITEM.get());
                output.accept(SPEED_MODULE_CARD_ITEM.get());
                output.accept(PRODUCTIVITY_MODULE_CARD_ITEM.get());

                // Card Deck
                output.accept(DECK_STATION_BLOCK_ITEM.get());
                output.accept(CARD_DECRYPTOR_BLOCK_ITEM.get());
                output.accept(CARD_SLEEVE_ITEM.get());
                output.accept(CARD_TOKEN_ITEM.get());
                output.accept(VENDING_MACHINE_DISCOUNT_CHARM_ITEM.get());
            }).build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> DEBUG_TAB = CREATIVE_MODE_TABS.register("debug_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.incore.debug"))
            .icon(() -> ENCOUNTER_WAND_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                // Dungeon
                output.accept(DUNGEON_OBJECTIVE_ALTAR_BLOCK_ITEM.get());
                output.accept(ROGUELIKE_PORTAL_BLOCK_ITEM.get());
                output.accept(DUNGEON_RETURN_PORTAL_BLOCK_ITEM.get());
                output.accept(DUNGEON_CRYSTAL_ITEM.get());
                output.accept(DUNGEON_COMPLETION_CRATE_ITEM.get());
                output.accept(DUNGEON_SCAVENGER_TOKEN_ITEM.get());
                output.accept(ENCOUNTER_SPAWNER_BLOCK_ITEM.get());
                output.accept(ENCOUNTER_WAND_ITEM.get());

                // Surface Spots
                output.accept(SURFACE_ORE_DEBUG_COMPASS_ITEM.get());
                output.accept(SURFACE_STONE_DEBUG_COMPASS_ITEM.get());
                output.accept(CRIMSITE_SURFACE_ORE_SPOT_BLOCK_ITEM.get());
                output.accept(VERIDIUM_SURFACE_ORE_SPOT_BLOCK_ITEM.get());
                output.accept(ASURINE_SURFACE_ORE_SPOT_BLOCK_ITEM.get());
                output.accept(OCHRUM_SURFACE_ORE_SPOT_BLOCK_ITEM.get());
                output.accept(CINNABAR_SURFACE_ORE_SPOT_BLOCK_ITEM.get());
                output.accept(MIXED_METALS_SURFACE_ORE_SPOT_BLOCK_ITEM.get());
                output.accept(GEM_CLUSTERS_SURFACE_ORE_SPOT_BLOCK_ITEM.get());
                output.accept(NETHER_QUARTZ_SURFACE_ORE_SPOT_BLOCK_ITEM.get());
                output.accept(STONE_SURFACE_STONE_SPOT_BLOCK_ITEM.get());
                output.accept(DEEPSLATE_SURFACE_STONE_SPOT_BLOCK_ITEM.get());
                output.accept(LIMESTONE_SURFACE_STONE_SPOT_BLOCK_ITEM.get());
                output.accept(BASALT_SURFACE_STONE_SPOT_BLOCK_ITEM.get());
                output.accept(SCORIA_SURFACE_STONE_SPOT_BLOCK_ITEM.get());

                // Gacha
                output.accept(GACHA_RIFT_BLOCK_ITEM.get());

                // Arena
                output.accept(ARENA_ORB_BLOCK_ITEM.get());
                output.accept(ARENA_REWARD_RIFT_BLOCK_ITEM.get());

                // Entropy Something
                output.accept(ENTROPY_CRATE_ITEM.get());
                output.accept(ENTROPY_VESSEL_ITEM.get());

                // NBT Items
                output.accept(BATTLEPASS_LANE_UNLOCK_ITEM.get());
                output.accept(TIME_PIECE_ITEM.get());
                output.accept(CARD_MODULE_ITEM.get());
                output.accept(CARD_BOOSTER_ITEM.get());
                output.accept(CARD_BOOSTER_BOX_ITEM.get());
                output.accept(CARD_DECK_CORE_ITEM.get());
                output.accept(CARD_DECK_BOX_ITEM.get());
                output.accept(CARD_DECK_ITEM.get());
            }).build());
}
