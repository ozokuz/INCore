package io.github.ozokuz.incore;

import io.github.ozokuz.incore.features.machines.multiblock.*;

import appeng.api.parts.IPart;
import appeng.api.parts.IPartItem;
import appeng.api.parts.PartModels;
import appeng.items.parts.PartItem;
import appeng.items.parts.PartModelsHelper;
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
import io.github.ozokuz.incore.features.research.discovery.*;
import io.github.ozokuz.incore.features.research.station.*;
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
import io.github.ozokuz.incore.features.market.content.MarketAutoTraderBlock;
import io.github.ozokuz.incore.features.market.content.MarketAutoTraderBlockEntity;
import io.github.ozokuz.incore.features.market.content.MarketAutoTraderMk2Block;
import io.github.ozokuz.incore.features.market.content.MarketAutoTraderMk2BlockEntity;
import io.github.ozokuz.incore.features.market.content.MarketAutoTraderMenu;
import io.github.ozokuz.incore.features.market.content.MarketTerminalCardMenu;
import io.github.ozokuz.incore.features.market.content.MarketTerminalBlock;
import io.github.ozokuz.incore.features.market.content.MarketTerminalBlockEntity;
import io.github.ozokuz.incore.features.market.content.MarketTerminalMeBlock;
import io.github.ozokuz.incore.features.market.content.MarketTerminalMeBlockEntity;
import io.github.ozokuz.incore.features.market.content.MarketTerminalMeCardMenu;
import io.github.ozokuz.incore.features.market.content.ShipmentTerminalBlock;
import io.github.ozokuz.incore.features.market.content.ShipmentTerminalBlockEntity;
import io.github.ozokuz.incore.features.market.content.ShipmentTerminalMk2Block;
import io.github.ozokuz.incore.features.market.content.ShipmentTerminalMk2BlockEntity;
import io.github.ozokuz.incore.features.market.content.ShipmentTerminalMenu;
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
import io.github.ozokuz.incore.features.roguelike.content.DungeonCompletionCrateItem;
import io.github.ozokuz.incore.features.roguelike.content.DungeonCrystalItem;
import io.github.ozokuz.incore.features.roguelike.content.MeCrystalAutomationTerminalMenu;
import io.github.ozokuz.incore.features.roguelike.content.MeCrystalAutomationTerminalPart;
import io.github.ozokuz.incore.features.roguelike.content.DungeonObjectiveAltarBlock;
import io.github.ozokuz.incore.features.roguelike.content.DungeonCrystalModificationStationBlock;
import io.github.ozokuz.incore.features.roguelike.content.DungeonCrystalModificationStationBlockEntity;
import io.github.ozokuz.incore.features.roguelike.content.DungeonCrystalModificationStationMenu;
import io.github.ozokuz.incore.features.roguelike.content.DungeonReturnPortalBlock;
import io.github.ozokuz.incore.features.roguelike.content.DungeonScavengerTokenItem;
import io.github.ozokuz.incore.features.roguelike.content.EmptyDungeonCrystalItem;
import io.github.ozokuz.incore.features.roguelike.content.DungeonAltarBlock;
import io.github.ozokuz.incore.features.roguelike.content.DungeonAltarBlockEntity;
import io.github.ozokuz.incore.features.roguelike.content.LockedRecoveryStrongboxBlock;
import io.github.ozokuz.incore.features.roguelike.content.LockedRecoveryStrongboxBlockEntity;
import io.github.ozokuz.incore.features.roguelike.content.LockedRecoveryStrongboxItem;
import io.github.ozokuz.incore.features.roguelike.content.RecoveryStrongboxKeyItem;
import io.github.ozokuz.incore.features.roguelike.content.DungeonAltarAutomatorBlock;
import io.github.ozokuz.incore.features.roguelike.content.DungeonAltarAutomatorBlockEntity;
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
import net.minecraft.world.item.ItemStack;
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
import java.util.UUID;
import java.util.function.Supplier;
import java.util.function.Function;

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
    public static final DeferredBlock<Block> MARKET_TERMINAL_ME_BLOCK = BLOCKS.register("market_terminal_me", () -> new MarketTerminalMeBlock());
    public static final Supplier<BlockEntityType<MarketTerminalMeBlockEntity>> MARKET_TERMINAL_ME_BE = BLOCK_ENTITY_TYPES.register(
            "market_terminal_me",
            () -> BlockEntityType.Builder.of(MarketTerminalMeBlockEntity::new, MARKET_TERMINAL_ME_BLOCK.get()).build(null)
    );
    public static final Supplier<MenuType<MarketTerminalMeCardMenu>> MARKET_TERMINAL_ME_CARD_MENU = MENU_TYPES.register(
            "market_terminal_me_card",
            () -> IMenuTypeExtension.create((id, inv, data) -> new MarketTerminalMeCardMenu(
                    id,
                    inv,
                    (MarketTerminalMeBlockEntity) inv.player.level().getBlockEntity(data.readBlockPos())
            ))
    );
    public static final DeferredItem<BlockItem> MARKET_TERMINAL_ME_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("market_terminal_me", MARKET_TERMINAL_ME_BLOCK);
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
    public static final DeferredBlock<Block> MARKET_AUTOTRADER_BLOCK = BLOCKS.register("market_autotrader", () -> new MarketAutoTraderBlock());
    public static final Supplier<BlockEntityType<MarketAutoTraderBlockEntity>> MARKET_AUTOTRADER_BE = BLOCK_ENTITY_TYPES.register(
            "market_autotrader",
            () -> BlockEntityType.Builder.of(MarketAutoTraderBlockEntity::new, MARKET_AUTOTRADER_BLOCK.get()).build(null)
    );
    public static final Supplier<MenuType<MarketAutoTraderMenu>> MARKET_AUTOTRADER_MENU = MENU_TYPES.register(
            "market_autotrader",
            () -> IMenuTypeExtension.create((id, inv, data) -> new MarketAutoTraderMenu(
                    id,
                    inv,
                    (MarketAutoTraderBlockEntity) inv.player.level().getBlockEntity(data.readBlockPos())
            ))
    );
    public static final DeferredItem<BlockItem> MARKET_AUTOTRADER_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("market_autotrader", MARKET_AUTOTRADER_BLOCK);
    public static final DeferredBlock<Block> MARKET_AUTOTRADER_MK2_BLOCK = BLOCKS.register("market_autotrader_mk2", () -> new MarketAutoTraderMk2Block());
    public static final Supplier<BlockEntityType<MarketAutoTraderMk2BlockEntity>> MARKET_AUTOTRADER_MK2_BE = BLOCK_ENTITY_TYPES.register(
            "market_autotrader_mk2",
            () -> BlockEntityType.Builder.of(MarketAutoTraderMk2BlockEntity::new, MARKET_AUTOTRADER_MK2_BLOCK.get()).build(null)
    );
    public static final DeferredItem<BlockItem> MARKET_AUTOTRADER_MK2_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("market_autotrader_mk2", MARKET_AUTOTRADER_MK2_BLOCK);
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
    public static final DeferredBlock<Block> CRUDE_RESEARCH_STATION_BLOCK = BLOCKS.register("crude_research_station", () -> new CrudeResearchStationBlock());
    public static final DeferredBlock<Block> RESEARCH_STATION_CASING_BLOCK = BLOCKS.register("research_station_casing", () -> new MultiblockCasingBlock());
    public static final DeferredBlock<Block> LOGIC_HOUSING_BLOCK = BLOCKS.register("logic_housing", () -> new LogicHousingBlock());
    public static final DeferredBlock<Block> LOGIC_HOUSING_T2_BLOCK = BLOCKS.register("logic_housing_t2", () -> new LogicHousingBlock(2, BlockBehaviour.Properties.of()));
    public static final DeferredBlock<Block> LOGIC_HOUSING_T3_BLOCK = BLOCKS.register("logic_housing_t3", () -> new LogicHousingBlock(3, BlockBehaviour.Properties.of()));
    public static final DeferredBlock<Block> LOGIC_HOUSING_T4_BLOCK = BLOCKS.register("logic_housing_t4", () -> new LogicHousingBlock(4, BlockBehaviour.Properties.of()));
    public static final DeferredBlock<Block> RESEARCH_DRIVE_BLOCK = BLOCKS.register("research_drive", () -> new ResearchDriveBlock());
    public static final DeferredBlock<Block> ORCHESTRATION_DRIVE_BLOCK = BLOCKS.register("orchestration_drive", () -> new OrchestrationDriveBlock());
    public static final DeferredBlock<Block> MATERIAL_STORAGE_BLOCK = BLOCKS.register("material_storage", () -> new MaterialStorageBlock());
    public static final DeferredBlock<Block> MATERIAL_STORAGE_T2_BLOCK = BLOCKS.register("material_storage_t2", () -> new MaterialStorageBlock(2, BlockBehaviour.Properties.of()));
    public static final DeferredBlock<Block> MATERIAL_STORAGE_T3_BLOCK = BLOCKS.register("material_storage_t3", () -> new MaterialStorageBlock(3, BlockBehaviour.Properties.of()));
    public static final DeferredBlock<Block> MATERIAL_STORAGE_T4_BLOCK = BLOCKS.register("material_storage_t4", () -> new MaterialStorageBlock(4, BlockBehaviour.Properties.of()));
    public static final DeferredBlock<Block> OUTPUT_PORT_BLOCK = BLOCKS.register("output_port", () -> new OutputPortBlock());
    public static final DeferredBlock<Block> AUGMENTER_BLOCK = BLOCKS.register("augmenter", () -> new AugmenterBlock());
    public static final DeferredBlock<Block> LINKING_PORT_BLOCK = BLOCKS.register("linking_port", () -> new LinkingPortBlock());
    public static final DeferredBlock<Block> WIRELESS_LINK_BLOCK = BLOCKS.register("wireless_link", () -> new WirelessLinkBlock());
    public static final DeferredBlock<Block> RESEARCH_LINK_CABLE_BLOCK = BLOCKS.register("research_link_cable", () -> new ResearchLinkCableBlock());
    public static final DeferredBlock<Block> RESEARCH_ORCHESTRATOR_CONTROLLER_BLOCK = BLOCKS.register("research_orchestrator_controller", () -> new ResearchOrchestratorControllerBlock());
    public static final DeferredBlock<Block> RESEARCH_CONTROLLER_T1_BLOCK = BLOCKS.register("research_controller_t1", () -> new ResearchControllerTier1Block());
    public static final DeferredBlock<Block> RESEARCH_CONTROLLER_T2_BLOCK = BLOCKS.register("research_controller_t2", () -> new ResearchControllerTier2Block());
    public static final DeferredBlock<Block> RESEARCH_CONTROLLER_T3_BLOCK = BLOCKS.register("research_controller_t3", () -> new ResearchControllerTier3Block());
    public static final DeferredBlock<Block> RESEARCH_CONTROLLER_T4_BLOCK = BLOCKS.register("research_controller_t4", () -> new ResearchControllerTier4Block());
    public static final DeferredBlock<Block> MECHANICAL_POWER_INPUT_BLOCK = BLOCKS.register("mechanical_power_input", () -> new MechanicalPowerInputBlock());
    public static final DeferredBlock<Block> ELECTRIC_POWER_INPUT_BLOCK = BLOCKS.register("electric_power_input", () -> new ElectricPowerInputBlock());
    public static final DeferredBlock<Block> ELECTRIC_POWER_INPUT_T2_BLOCK = BLOCKS.register("electric_power_input_t2", () -> new ElectricPowerInputTier2Block());
    public static final DeferredBlock<Block> ELECTRIC_POWER_INPUT_T3_BLOCK = BLOCKS.register("electric_power_input_t3", () -> new ElectricPowerInputTier3Block());
    public static final DeferredBlock<Block> ELECTRIC_POWER_INPUT_T4_BLOCK = BLOCKS.register("electric_power_input_t4", () -> new ElectricPowerInputTier4Block());
    public static final DeferredBlock<Block> DATALOGGER_BLOCK = BLOCKS.register("datalogger", () -> new DataloggerBlock());
    public static final DeferredBlock<Block> TRANSLATOR_BLOCK = BLOCKS.register("translator", () -> new TranslatorBlock());
    public static final DeferredBlock<Block> RESEARCH_SAMPLE_FABRICATOR_BLOCK = BLOCKS.register("research_sample_fabricator", () -> new ResearchSampleFabricatorBlock());
    public static final Supplier<BlockEntityType<CrudeResearchStationBlockEntity>> CRUDE_RESEARCH_STATION_BE = BLOCK_ENTITY_TYPES.register(
            "crude_research_station",
            () -> BlockEntityType.Builder.of(CrudeResearchStationBlockEntity::new, CRUDE_RESEARCH_STATION_BLOCK.get()).build(null)
    );
    public static final Supplier<BlockEntityType<ResearchControllerBlockEntity>> RESEARCH_CONTROLLER_BE = BLOCK_ENTITY_TYPES.register(
            "research_controller",
            () -> BlockEntityType.Builder.of(
                    ResearchControllerBlockEntity::new,
                    RESEARCH_CONTROLLER_T1_BLOCK.get(),
                    RESEARCH_CONTROLLER_T2_BLOCK.get(),
                    RESEARCH_CONTROLLER_T3_BLOCK.get(),
                    RESEARCH_CONTROLLER_T4_BLOCK.get()
            ).build(null)
    );
    public static final Supplier<BlockEntityType<LogicHousingBlockEntity>> LOGIC_HOUSING_BE = BLOCK_ENTITY_TYPES.register(
            "logic_housing",
            () -> BlockEntityType.Builder.of(
                    LogicHousingBlockEntity::new,
                    LOGIC_HOUSING_BLOCK.get(),
                    LOGIC_HOUSING_T2_BLOCK.get(),
                    LOGIC_HOUSING_T3_BLOCK.get(),
                    LOGIC_HOUSING_T4_BLOCK.get()
            ).build(null)
    );
    public static final Supplier<BlockEntityType<ResearchDriveBlockEntity>> RESEARCH_DRIVE_BE = BLOCK_ENTITY_TYPES.register(
            "research_drive",
            () -> BlockEntityType.Builder.of(ResearchDriveBlockEntity::new, RESEARCH_DRIVE_BLOCK.get()).build(null)
    );
    public static final Supplier<BlockEntityType<OrchestrationDriveBlockEntity>> ORCHESTRATION_DRIVE_BE = BLOCK_ENTITY_TYPES.register(
            "orchestration_drive",
            () -> BlockEntityType.Builder.of(OrchestrationDriveBlockEntity::new, ORCHESTRATION_DRIVE_BLOCK.get()).build(null)
    );
    public static final Supplier<BlockEntityType<MaterialStorageBlockEntity>> MATERIAL_STORAGE_BE = BLOCK_ENTITY_TYPES.register(
            "material_storage",
            () -> BlockEntityType.Builder.of(
                    MaterialStorageBlockEntity::new,
                    MATERIAL_STORAGE_BLOCK.get(),
                    MATERIAL_STORAGE_T2_BLOCK.get(),
                    MATERIAL_STORAGE_T3_BLOCK.get(),
                    MATERIAL_STORAGE_T4_BLOCK.get()
            ).build(null)
    );
    public static final Supplier<BlockEntityType<OutputPortBlockEntity>> OUTPUT_PORT_BE = BLOCK_ENTITY_TYPES.register(
            "output_port",
            () -> BlockEntityType.Builder.of(OutputPortBlockEntity::new, OUTPUT_PORT_BLOCK.get()).build(null)
    );
    public static final Supplier<BlockEntityType<AugmenterBlockEntity>> AUGMENTER_BE = BLOCK_ENTITY_TYPES.register(
            "augmenter",
            () -> BlockEntityType.Builder.of(AugmenterBlockEntity::new, AUGMENTER_BLOCK.get()).build(null)
    );
    public static final Supplier<BlockEntityType<LinkingPortBlockEntity>> LINKING_PORT_BE = BLOCK_ENTITY_TYPES.register(
            "linking_port",
            () -> BlockEntityType.Builder.of(LinkingPortBlockEntity::new, LINKING_PORT_BLOCK.get()).build(null)
    );
    public static final Supplier<BlockEntityType<WirelessLinkBlockEntity>> WIRELESS_LINK_BE = BLOCK_ENTITY_TYPES.register(
            "wireless_link",
            () -> BlockEntityType.Builder.of(WirelessLinkBlockEntity::new, WIRELESS_LINK_BLOCK.get()).build(null)
    );
    public static final Supplier<BlockEntityType<ResearchOrchestratorControllerBlockEntity>> RESEARCH_ORCHESTRATOR_CONTROLLER_BE = BLOCK_ENTITY_TYPES.register(
            "research_orchestrator_controller",
            () -> BlockEntityType.Builder.of(ResearchOrchestratorControllerBlockEntity::new, RESEARCH_ORCHESTRATOR_CONTROLLER_BLOCK.get()).build(null)
    );
    public static final Supplier<BlockEntityType<MechanicalPowerInputBlockEntity>> MECHANICAL_POWER_INPUT_BE = BLOCK_ENTITY_TYPES.register(
            "mechanical_power_input",
            () -> BlockEntityType.Builder.of(MechanicalPowerInputBlockEntity::new, MECHANICAL_POWER_INPUT_BLOCK.get()).build(null)
    );
    public static final Supplier<BlockEntityType<ElectricPowerInputBlockEntity>> ELECTRIC_POWER_INPUT_BE = BLOCK_ENTITY_TYPES.register(
            "electric_power_input",
            () -> BlockEntityType.Builder.of(
                    ElectricPowerInputBlockEntity::new,
                    ELECTRIC_POWER_INPUT_BLOCK.get(),
                    ELECTRIC_POWER_INPUT_T2_BLOCK.get(),
                    ELECTRIC_POWER_INPUT_T3_BLOCK.get(),
                    ELECTRIC_POWER_INPUT_T4_BLOCK.get()
            ).build(null)
    );
    public static final Supplier<BlockEntityType<DataloggerBlockEntity>> DATALOGGER_BE = BLOCK_ENTITY_TYPES.register(
            "datalogger",
            () -> BlockEntityType.Builder.of(DataloggerBlockEntity::new, DATALOGGER_BLOCK.get()).build(null)
    );
    public static final Supplier<BlockEntityType<TranslatorBlockEntity>> TRANSLATOR_BE = BLOCK_ENTITY_TYPES.register(
            "translator",
            () -> BlockEntityType.Builder.of(TranslatorBlockEntity::new, TRANSLATOR_BLOCK.get()).build(null)
    );
    public static final Supplier<BlockEntityType<ResearchSampleFabricatorBlockEntity>> RESEARCH_SAMPLE_FABRICATOR_BE = BLOCK_ENTITY_TYPES.register(
            "research_sample_fabricator",
            () -> BlockEntityType.Builder.of(ResearchSampleFabricatorBlockEntity::new, RESEARCH_SAMPLE_FABRICATOR_BLOCK.get()).build(null)
    );
    public static final Supplier<MenuType<CrudeResearchStationMenu>> CRUDE_RESEARCH_STATION_MENU = MENU_TYPES.register(
            "crude_research_station",
            () -> IMenuTypeExtension.create((id, inv, data) -> new CrudeResearchStationMenu(
                    id,
                    inv,
                    (CrudeResearchStationBlockEntity) inv.player.level().getBlockEntity(data.readBlockPos())
            ))
    );
    public static final Supplier<MenuType<LogicHousingMenu>> LOGIC_HOUSING_MENU = MENU_TYPES.register(
            "logic_housing",
            () -> IMenuTypeExtension.create((id, inv, data) -> new LogicHousingMenu(id, inv, (LogicHousingBlockEntity) inv.player.level().getBlockEntity(data.readBlockPos())))
    );
    public static final Supplier<MenuType<ResearchDriveMenu>> RESEARCH_DRIVE_MENU = MENU_TYPES.register(
            "research_drive",
            () -> IMenuTypeExtension.create((id, inv, data) -> new ResearchDriveMenu(id, inv, (ResearchDriveBlockEntity) inv.player.level().getBlockEntity(data.readBlockPos())))
    );
    public static final Supplier<MenuType<OrchestrationDriveMenu>> ORCHESTRATION_DRIVE_MENU = MENU_TYPES.register(
            "orchestration_drive",
            () -> IMenuTypeExtension.create((id, inv, data) -> new OrchestrationDriveMenu(id, inv, (OrchestrationDriveBlockEntity) inv.player.level().getBlockEntity(data.readBlockPos())))
    );
    public static final Supplier<MenuType<MaterialStorageMenu>> MATERIAL_STORAGE_MENU = MENU_TYPES.register(
            "material_storage",
            () -> IMenuTypeExtension.create((id, inv, data) -> new MaterialStorageMenu(id, inv, (MaterialStorageBlockEntity) inv.player.level().getBlockEntity(data.readBlockPos())))
    );
    public static final Supplier<MenuType<OutputPortMenu>> OUTPUT_PORT_MENU = MENU_TYPES.register(
            "output_port",
            () -> IMenuTypeExtension.create((id, inv, data) -> new OutputPortMenu(id, inv, (OutputPortBlockEntity) inv.player.level().getBlockEntity(data.readBlockPos())))
    );
    public static final Supplier<MenuType<AugmenterMenu>> AUGMENTER_MENU = MENU_TYPES.register(
            "augmenter",
            () -> IMenuTypeExtension.create((id, inv, data) -> new AugmenterMenu(id, inv, (AugmenterBlockEntity) inv.player.level().getBlockEntity(data.readBlockPos())))
    );
    public static final Supplier<MenuType<WirelessLinkMenu>> WIRELESS_LINK_MENU = MENU_TYPES.register(
            "wireless_link",
            () -> IMenuTypeExtension.create((id, inv, data) -> new WirelessLinkMenu(id, inv, (WirelessLinkBlockEntity) inv.player.level().getBlockEntity(data.readBlockPos())))
    );
    public static final Supplier<MenuType<ResearchControllerMenu>> RESEARCH_CONTROLLER_MENU = MENU_TYPES.register(
            "research_controller",
            () -> IMenuTypeExtension.create((id, inv, data) -> new ResearchControllerMenu(id, inv, data.readBlockPos()))
    );
    public static final Supplier<MenuType<ResearchOrchestratorControllerMenu>> RESEARCH_ORCHESTRATOR_CONTROLLER_MENU = MENU_TYPES.register(
            "research_orchestrator_controller",
            () -> IMenuTypeExtension.create((id, inv, data) -> new ResearchOrchestratorControllerMenu(id, inv, data.readBlockPos()))
    );
    public static final Supplier<MenuType<PowerInputMenu>> POWER_INPUT_MENU = MENU_TYPES.register(
            "power_input",
            () -> IMenuTypeExtension.create((id, inv, data) -> new PowerInputMenu(id, inv, data.readBlockPos()))
    );
    public static final Supplier<MenuType<DataloggerMenu>> DATALOGGER_MENU = MENU_TYPES.register(
            "datalogger",
            () -> IMenuTypeExtension.create((id, inv, data) -> new DataloggerMenu(
                    id,
                    inv,
                    (DataloggerBlockEntity) inv.player.level().getBlockEntity(data.readBlockPos())
            ))
    );
    public static final Supplier<MenuType<TranslatorMenu>> TRANSLATOR_MENU = MENU_TYPES.register(
            "translator",
            () -> IMenuTypeExtension.create((id, inv, data) -> new TranslatorMenu(
                    id,
                    inv,
                    (TranslatorBlockEntity) inv.player.level().getBlockEntity(data.readBlockPos())
            ))
    );
    public static final Supplier<MenuType<ResearchSampleFabricatorMenu>> RESEARCH_SAMPLE_FABRICATOR_MENU = MENU_TYPES.register(
            "research_sample_fabricator",
            () -> IMenuTypeExtension.create((id, inv, data) -> new ResearchSampleFabricatorMenu(
                    id,
                    inv,
                    (ResearchSampleFabricatorBlockEntity) inv.player.level().getBlockEntity(data.readBlockPos())
            ))
    );
    public static final DeferredItem<BlockItem> CRUDE_RESEARCH_STATION_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("crude_research_station", CRUDE_RESEARCH_STATION_BLOCK);
    public static final DeferredItem<BlockItem> RESEARCH_STATION_CASING_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("research_station_casing", RESEARCH_STATION_CASING_BLOCK);
    public static final DeferredItem<BlockItem> LOGIC_HOUSING_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("logic_housing", LOGIC_HOUSING_BLOCK);
    public static final DeferredItem<BlockItem> LOGIC_HOUSING_T2_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("logic_housing_t2", LOGIC_HOUSING_T2_BLOCK);
    public static final DeferredItem<BlockItem> LOGIC_HOUSING_T3_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("logic_housing_t3", LOGIC_HOUSING_T3_BLOCK);
    public static final DeferredItem<BlockItem> LOGIC_HOUSING_T4_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("logic_housing_t4", LOGIC_HOUSING_T4_BLOCK);
    public static final DeferredItem<BlockItem> RESEARCH_DRIVE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("research_drive", RESEARCH_DRIVE_BLOCK);
    public static final DeferredItem<BlockItem> ORCHESTRATION_DRIVE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("orchestration_drive", ORCHESTRATION_DRIVE_BLOCK);
    public static final DeferredItem<BlockItem> MATERIAL_STORAGE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("material_storage", MATERIAL_STORAGE_BLOCK);
    public static final DeferredItem<BlockItem> MATERIAL_STORAGE_T2_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("material_storage_t2", MATERIAL_STORAGE_T2_BLOCK);
    public static final DeferredItem<BlockItem> MATERIAL_STORAGE_T3_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("material_storage_t3", MATERIAL_STORAGE_T3_BLOCK);
    public static final DeferredItem<BlockItem> MATERIAL_STORAGE_T4_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("material_storage_t4", MATERIAL_STORAGE_T4_BLOCK);
    public static final DeferredItem<BlockItem> OUTPUT_PORT_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("output_port", OUTPUT_PORT_BLOCK);
    public static final DeferredItem<BlockItem> AUGMENTER_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("augmenter", AUGMENTER_BLOCK);
    public static final DeferredItem<BlockItem> LINKING_PORT_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("linking_port", LINKING_PORT_BLOCK);
    public static final DeferredItem<BlockItem> WIRELESS_LINK_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("wireless_link", WIRELESS_LINK_BLOCK);
    public static final DeferredItem<BlockItem> RESEARCH_LINK_CABLE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("research_link_cable", RESEARCH_LINK_CABLE_BLOCK);
    public static final DeferredItem<BlockItem> RESEARCH_ORCHESTRATOR_CONTROLLER_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("research_orchestrator_controller", RESEARCH_ORCHESTRATOR_CONTROLLER_BLOCK);
    public static final DeferredItem<BlockItem> RESEARCH_CONTROLLER_T1_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("research_controller_t1", RESEARCH_CONTROLLER_T1_BLOCK);
    public static final DeferredItem<BlockItem> RESEARCH_CONTROLLER_T2_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("research_controller_t2", RESEARCH_CONTROLLER_T2_BLOCK);
    public static final DeferredItem<BlockItem> RESEARCH_CONTROLLER_T3_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("research_controller_t3", RESEARCH_CONTROLLER_T3_BLOCK);
    public static final DeferredItem<BlockItem> RESEARCH_CONTROLLER_T4_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("research_controller_t4", RESEARCH_CONTROLLER_T4_BLOCK);
    public static final DeferredItem<BlockItem> MECHANICAL_POWER_INPUT_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("mechanical_power_input", MECHANICAL_POWER_INPUT_BLOCK);
    public static final DeferredItem<BlockItem> ELECTRIC_POWER_INPUT_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("electric_power_input", ELECTRIC_POWER_INPUT_BLOCK);
    public static final DeferredItem<BlockItem> ELECTRIC_POWER_INPUT_T2_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("electric_power_input_t2", ELECTRIC_POWER_INPUT_T2_BLOCK);
    public static final DeferredItem<BlockItem> ELECTRIC_POWER_INPUT_T3_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("electric_power_input_t3", ELECTRIC_POWER_INPUT_T3_BLOCK);
    public static final DeferredItem<BlockItem> ELECTRIC_POWER_INPUT_T4_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("electric_power_input_t4", ELECTRIC_POWER_INPUT_T4_BLOCK);
    public static final DeferredItem<BlockItem> DATALOGGER_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("datalogger", DATALOGGER_BLOCK);
    public static final DeferredItem<BlockItem> TRANSLATOR_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("translator", TRANSLATOR_BLOCK);
    public static final DeferredItem<BlockItem> RESEARCH_SAMPLE_FABRICATOR_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("research_sample_fabricator", RESEARCH_SAMPLE_FABRICATOR_BLOCK);
    public static final DeferredBlock<Block> DUNGEON_ALTAR_BLOCK = BLOCKS.register("dungeon_altar", DungeonAltarBlock::new);
    public static final Supplier<BlockEntityType<DungeonAltarBlockEntity>> DUNGEON_ALTAR_BE = BLOCK_ENTITY_TYPES.register("dungeon_altar", () -> BlockEntityType.Builder.of(DungeonAltarBlockEntity::new, DUNGEON_ALTAR_BLOCK.get()).build(null));
    public static final DeferredItem<BlockItem> DUNGEON_ALTAR_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("dungeon_altar", DUNGEON_ALTAR_BLOCK);
    public static final DeferredBlock<Block> DUNGEON_ALTAR_AUTOMATOR_BLOCK = BLOCKS.register("dungeon_altar_automator", DungeonAltarAutomatorBlock::new);
    public static final Supplier<BlockEntityType<DungeonAltarAutomatorBlockEntity>> DUNGEON_ALTAR_AUTOMATOR_BE = BLOCK_ENTITY_TYPES.register(
            "dungeon_altar_automator",
            () -> BlockEntityType.Builder.of(DungeonAltarAutomatorBlockEntity::new, DUNGEON_ALTAR_AUTOMATOR_BLOCK.get()).build(null)
    );
    public static final DeferredItem<BlockItem> DUNGEON_ALTAR_AUTOMATOR_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("dungeon_altar_automator", DUNGEON_ALTAR_AUTOMATOR_BLOCK);
    public static final DeferredBlock<Block> DUNGEON_CRYSTAL_MODIFICATION_STATION_BLOCK = BLOCKS.register("dungeon_crystal_modification_station", () -> new DungeonCrystalModificationStationBlock());
    public static final Supplier<BlockEntityType<DungeonCrystalModificationStationBlockEntity>> DUNGEON_CRYSTAL_MODIFICATION_STATION_BE = BLOCK_ENTITY_TYPES.register(
            "dungeon_crystal_modification_station",
            () -> BlockEntityType.Builder.of(DungeonCrystalModificationStationBlockEntity::new, DUNGEON_CRYSTAL_MODIFICATION_STATION_BLOCK.get()).build(null)
    );
    public static final Supplier<MenuType<DungeonCrystalModificationStationMenu>> DUNGEON_CRYSTAL_MODIFICATION_STATION_MENU = MENU_TYPES.register(
            "dungeon_crystal_modification_station",
            () -> IMenuTypeExtension.create((id, inv, data) -> new DungeonCrystalModificationStationMenu(
                    id,
                    inv,
                    (DungeonCrystalModificationStationBlockEntity) inv.player.level().getBlockEntity(data.readBlockPos())
            ))
    );
    public static final DeferredItem<BlockItem> DUNGEON_CRYSTAL_MODIFICATION_STATION_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("dungeon_crystal_modification_station", DUNGEON_CRYSTAL_MODIFICATION_STATION_BLOCK);
    public static final DeferredItem<PartItem<MeCrystalAutomationTerminalPart>> ME_CRYSTAL_AUTOMATION_TERMINAL_ITEM =
            registerPartItem("me_crystal_automation_terminal", MeCrystalAutomationTerminalPart.class, MeCrystalAutomationTerminalPart::new);
    public static final Supplier<MenuType<MeCrystalAutomationTerminalMenu>> ME_CRYSTAL_AUTOMATION_TERMINAL_MENU = MENU_TYPES.register(
            "me_crystal_automation_terminal",
            () -> IMenuTypeExtension.create((id, inv, data) -> {
                var hostPos = data.readBlockPos();
                var side = net.minecraft.core.Direction.from3DDataValue(data.readByte());
                var resolvedPart = MeCrystalAutomationTerminalPart.resolve(inv.player.level(), hostPos, side);
                return new MeCrystalAutomationTerminalMenu(
                        id,
                        inv,
                        hostPos,
                        side,
                        resolvedPart
                );
            })
    );
    public static final DeferredBlock<Block> DUNGEON_OBJECTIVE_ALTAR_BLOCK = BLOCKS.register("dungeon_objective_altar", DungeonObjectiveAltarBlock::new);
    public static final DeferredItem<BlockItem> DUNGEON_OBJECTIVE_ALTAR_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("dungeon_objective_altar", DUNGEON_OBJECTIVE_ALTAR_BLOCK);
    public static final DeferredBlock<Block> LOCKED_RECOVERY_STRONGBOX_BLOCK = BLOCKS.register("locked_recovery_strongbox", () -> new LockedRecoveryStrongboxBlock());
    public static final Supplier<BlockEntityType<LockedRecoveryStrongboxBlockEntity>> LOCKED_RECOVERY_STRONGBOX_BE = BLOCK_ENTITY_TYPES.register(
            "locked_recovery_strongbox",
            () -> BlockEntityType.Builder.of(LockedRecoveryStrongboxBlockEntity::new, LOCKED_RECOVERY_STRONGBOX_BLOCK.get()).build(null)
    );
    public static final DeferredItem<BlockItem> LOCKED_RECOVERY_STRONGBOX_BLOCK_ITEM = ITEMS.registerItem(
            "locked_recovery_strongbox",
            properties -> new LockedRecoveryStrongboxItem(LOCKED_RECOVERY_STRONGBOX_BLOCK.get(), properties.stacksTo(1))
    );
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
    public static final DeferredItem<Item> RECOVERY_STRONGBOX_KEY_ITEM = ITEMS.registerItem(
            "recovery_strongbox_key",
            properties -> new RecoveryStrongboxKeyItem(properties.stacksTo(1))
    );
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
    public static final DeferredItem<Item> BASIC_LOGIC_MODULE_ITEM = ITEMS.registerItem(
            "basic_logic_module",
            properties -> new LogicModuleItem(properties, LogicModuleTier.T1, true, "fresh")
    );
    public static final DeferredItem<Item> LOGIC_MODULE_T2_ITEM = ITEMS.registerItem("logic_module_t2", properties -> new LogicModuleItem(properties, LogicModuleTier.T2, true, "fresh"));
    public static final DeferredItem<Item> LOGIC_MODULE_T3_ITEM = ITEMS.registerItem("logic_module_t3", properties -> new LogicModuleItem(properties, LogicModuleTier.T3, true, "fresh"));
    public static final DeferredItem<Item> LOGIC_MODULE_T4_ITEM = ITEMS.registerItem("logic_module_t4", properties -> new LogicModuleItem(properties, LogicModuleTier.T4, true, "fresh"));
    public static final DeferredItem<Item> BROKEN_LOGIC_MODULE_T2_ITEM = ITEMS.registerItem("broken_logic_module_t2", properties -> new LogicModuleItem(properties, LogicModuleTier.T2, false, "broken"));
    public static final DeferredItem<Item> BROKEN_LOGIC_MODULE_T3_ITEM = ITEMS.registerItem("broken_logic_module_t3", properties -> new LogicModuleItem(properties, LogicModuleTier.T3, false, "broken"));
    public static final DeferredItem<Item> USED_LOGIC_MODULE_T3_ITEM = ITEMS.registerItem("used_logic_module_t3", properties -> new LogicModuleItem(properties, LogicModuleTier.T3, false, "used"));
    public static final DeferredItem<Item> USED_LOGIC_MODULE_T4_ITEM = ITEMS.registerItem("used_logic_module_t4", properties -> new LogicModuleItem(properties, LogicModuleTier.T4, false, "used"));
    public static final DeferredItem<Item> STARTER_DATA_ITEM = ITEMS.registerSimpleItem("starter_data");
    public static final DeferredItem<Item> FIELD_PEN_ITEM = ITEMS.registerItem("field_pen", FieldPenItem::new);
    public static final DeferredItem<Item> FIELD_RESEARCH_NOTE_ITEM = ITEMS.registerItem("field_research_note", AbstractDiscoveryGrantItem::new);
    public static final DeferredItem<Item> RESEARCH_DATA_REPORT_ITEM = ITEMS.registerItem("research_data_report", AbstractDiscoveryGrantItem::new);
    public static final DeferredItem<Item> CONTINUUM_DATA_REPORT_ITEM = ITEMS.registerItem("continuum_data_report", ContinuumDataReportItem::new);
    public static final DeferredItem<Item> DECODED_CONTINUUM_REPORT_ITEM = ITEMS.registerItem("decoded_continuum_report", AbstractDiscoveryGrantItem::new);
    public static final DeferredItem<Item> BLANK_RESEARCH_SAMPLE_ITEM = ITEMS.registerSimpleItem("blank_research_sample");
    public static final DeferredItem<Item> RESEARCH_SAMPLE_ITEM = ITEMS.registerItem("research_sample", AbstractDiscoveryGrantItem::new);
    public static final DeferredItem<Item> RESEARCH_DISK_T1_ITEM = ITEMS.registerItem("research_disk_t1", properties -> new ResearchDiskItem(properties, ResearchDiskTier.T1));
    public static final DeferredItem<Item> RESEARCH_DISK_T2_ITEM = ITEMS.registerItem("research_disk_t2", properties -> new ResearchDiskItem(properties, ResearchDiskTier.T2));
    public static final DeferredItem<Item> RESEARCH_DISK_T3_ITEM = ITEMS.registerItem("research_disk_t3", properties -> new ResearchDiskItem(properties, ResearchDiskTier.T3));
    public static final DeferredItem<Item> RESEARCH_DISK_T4_ITEM = ITEMS.registerItem("research_disk_t4", properties -> new ResearchDiskItem(properties, ResearchDiskTier.T4));
    public static final DeferredItem<Item> ORCHESTRATION_DISK_ITEM = ITEMS.registerItem("orchestration_disk", OrchestrationDiskItem::new);
    public static final DeferredItem<Item> SIGNAL_TRANSMITTER_ITEM = ITEMS.registerItem("signal_transmitter", SignalTransmitterItem::new);
    public static final DeferredItem<Item> SPEED_AUGMENT_ITEM = ITEMS.registerItem("speed_augment", properties -> new MachineAugmentItem(properties, MachineAugmentType.SPEED, false, null));
    public static final DeferredItem<Item> PRODUCTIVITY_AUGMENT_ITEM = ITEMS.registerItem("productivity_augment", properties -> new MachineAugmentItem(properties, MachineAugmentType.PRODUCTIVITY, false, null));
    public static final DeferredItem<Item> STABILIZER_AUGMENT_ITEM = ITEMS.registerItem("stabilizer_augment", properties -> new MachineAugmentItem(properties, MachineAugmentType.STABILIZER, false, null));
    public static final DeferredItem<Item> CABLE_CAPACITY_AUGMENT_ITEM = ITEMS.registerItem("cable_capacity_augment", properties -> new MachineAugmentItem(properties, MachineAugmentType.CABLE_CAPACITY, false, null));
    public static final DeferredItem<Item> WIRELESS_CAPACITY_AUGMENT_ITEM = ITEMS.registerItem("wireless_capacity_augment", properties -> new MachineAugmentItem(properties, MachineAugmentType.WIRELESS_CAPACITY, false, null));
    public static final DeferredItem<Item> WIRELESS_RANGE_AUGMENT_ITEM = ITEMS.registerItem("wireless_range_augment", properties -> new MachineAugmentItem(properties, MachineAugmentType.WIRELESS_RANGE, false, null));
    public static final DeferredItem<Item> INFINITE_WIRELESS_AUGMENT_ITEM = ITEMS.registerItem("infinite_wireless_augment", properties -> new MachineAugmentItem(properties, MachineAugmentType.INFINITE_WIRELESS, false, null));
    public static final DeferredItem<Item> INTERDIMENSIONAL_WIRELESS_AUGMENT_ITEM = ITEMS.registerItem("interdimensional_wireless_augment", properties -> new MachineAugmentItem(properties, MachineAugmentType.INTERDIMENSIONAL_WIRELESS, false, null));
    public static final DeferredItem<Item> DUNGEON_SPEED_AUGMENT_ITEM = ITEMS.registerItem("dungeon_speed_augment", properties -> new MachineAugmentItem(properties, MachineAugmentType.SPEED, true, null));
    public static final DeferredItem<Item> DUNGEON_PRODUCTIVITY_AUGMENT_ITEM = ITEMS.registerItem("dungeon_productivity_augment", properties -> new MachineAugmentItem(properties, MachineAugmentType.PRODUCTIVITY, true, null));
    public static final DeferredItem<Item> DUNGEON_STABILIZER_AUGMENT_ITEM = ITEMS.registerItem("dungeon_stabilizer_augment", properties -> new MachineAugmentItem(properties, MachineAugmentType.STABILIZER, true, null));
    public static final DeferredItem<Item> DUNGEON_SPECIALIZER_FOUNDATIONS_ITEM = ITEMS.registerItem(
            "dungeon_specializer_foundations",
            properties -> new MachineAugmentItem(properties, MachineAugmentType.SPECIALIZER, true, ResourceLocation.fromNamespaceAndPath(INCore.MODID, "foundations"))
    );
    public static final DeferredItem<Item> DUNGEON_SPECIALIZER_EXPEDITION_ITEM = ITEMS.registerItem(
            "dungeon_specializer_expedition",
            properties -> new MachineAugmentItem(properties, MachineAugmentType.SPECIALIZER, true, ResourceLocation.fromNamespaceAndPath(INCore.MODID, "expedition"))
    );
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
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> RECOVERY_STRONGBOX_ID =
            DATA_COMPONENT_TYPES.registerComponentType(
                    "recovery_strongbox_id",
                    builder -> builder.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8)
            );
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> BATTLEPASS_LANE =
            DATA_COMPONENT_TYPES.registerComponentType(
                    "battlepass_lane",
                    builder -> builder.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8)
            );

    private static <T extends IPart> DeferredItem<PartItem<T>> registerPartItem(String id, Class<T> partClass, Function<IPartItem<T>, T> factory) {
        PartModels.registerModels(PartModelsHelper.createModels(partClass));
        return ITEMS.registerItem(id, properties -> new PartItem<>(properties, partClass, factory));
    }
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB = CREATIVE_MODE_TABS.register("main_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.incore"))
            .icon(() -> DUNGEON_CRYSTAL_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                // Crystal Making
                output.accept(DUNGEON_ALTAR_BLOCK_ITEM.get());
                output.accept(DUNGEON_CRYSTAL_MODIFICATION_STATION_BLOCK_ITEM.get());
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
                output.accept(MARKET_AUTOTRADER_BLOCK_ITEM.get());
                output.accept(MARKET_AUTOTRADER_MK2_BLOCK_ITEM.get());

                // Entropy
                output.accept(ENTROPY_BOOSTER_SMALL_ITEM.get());
                output.accept(ENTROPY_BOOSTER_LARGE_ITEM.get());

                // Gacha
                output.accept(BASIC_TIME_PIECE_ITEM.get());
                output.accept(CHARTERED_TIME_PIECE_ITEM.get());

                // Research
                output.accept(CRUDE_RESEARCH_STATION_BLOCK_ITEM.get());
                output.accept(RESEARCH_STATION_CASING_BLOCK_ITEM.get());
                output.accept(LOGIC_HOUSING_BLOCK_ITEM.get());
                output.accept(LOGIC_HOUSING_T2_BLOCK_ITEM.get());
                output.accept(LOGIC_HOUSING_T3_BLOCK_ITEM.get());
                output.accept(LOGIC_HOUSING_T4_BLOCK_ITEM.get());
                output.accept(RESEARCH_DRIVE_BLOCK_ITEM.get());
                output.accept(ORCHESTRATION_DRIVE_BLOCK_ITEM.get());
                output.accept(MATERIAL_STORAGE_BLOCK_ITEM.get());
                output.accept(MATERIAL_STORAGE_T2_BLOCK_ITEM.get());
                output.accept(MATERIAL_STORAGE_T3_BLOCK_ITEM.get());
                output.accept(MATERIAL_STORAGE_T4_BLOCK_ITEM.get());
                output.accept(OUTPUT_PORT_BLOCK_ITEM.get());
                output.accept(AUGMENTER_BLOCK_ITEM.get());
                output.accept(LINKING_PORT_BLOCK_ITEM.get());
                output.accept(WIRELESS_LINK_BLOCK_ITEM.get());
                output.accept(RESEARCH_LINK_CABLE_BLOCK_ITEM.get());
                output.accept(RESEARCH_ORCHESTRATOR_CONTROLLER_BLOCK_ITEM.get());
                output.accept(RESEARCH_CONTROLLER_T1_BLOCK_ITEM.get());
                output.accept(RESEARCH_CONTROLLER_T2_BLOCK_ITEM.get());
                output.accept(RESEARCH_CONTROLLER_T3_BLOCK_ITEM.get());
                output.accept(RESEARCH_CONTROLLER_T4_BLOCK_ITEM.get());
                output.accept(MECHANICAL_POWER_INPUT_BLOCK_ITEM.get());
                output.accept(ELECTRIC_POWER_INPUT_BLOCK_ITEM.get());
                output.accept(ELECTRIC_POWER_INPUT_T2_BLOCK_ITEM.get());
                output.accept(ELECTRIC_POWER_INPUT_T3_BLOCK_ITEM.get());
                output.accept(ELECTRIC_POWER_INPUT_T4_BLOCK_ITEM.get());
                output.accept(DATALOGGER_BLOCK_ITEM.get());
                output.accept(TRANSLATOR_BLOCK_ITEM.get());
                output.accept(RESEARCH_SAMPLE_FABRICATOR_BLOCK_ITEM.get());
                output.accept(BASIC_LOGIC_MODULE_ITEM.get());
                output.accept(LOGIC_MODULE_T2_ITEM.get());
                output.accept(LOGIC_MODULE_T3_ITEM.get());
                output.accept(LOGIC_MODULE_T4_ITEM.get());
                output.accept(BROKEN_LOGIC_MODULE_T2_ITEM.get());
                output.accept(BROKEN_LOGIC_MODULE_T3_ITEM.get());
                output.accept(USED_LOGIC_MODULE_T3_ITEM.get());
                output.accept(USED_LOGIC_MODULE_T4_ITEM.get());
                output.accept(STARTER_DATA_ITEM.get());
                output.accept(FIELD_PEN_ITEM.get());
                output.accept(FIELD_RESEARCH_NOTE_ITEM.get());
                output.accept(RESEARCH_DATA_REPORT_ITEM.get());
                output.accept(CONTINUUM_DATA_REPORT_ITEM.get());
                output.accept(DECODED_CONTINUUM_REPORT_ITEM.get());
                output.accept(BLANK_RESEARCH_SAMPLE_ITEM.get());
                output.accept(RESEARCH_SAMPLE_ITEM.get());
                output.accept(RESEARCH_DISK_T1_ITEM.get());
                output.accept(RESEARCH_DISK_T2_ITEM.get());
                output.accept(RESEARCH_DISK_T3_ITEM.get());
                output.accept(RESEARCH_DISK_T4_ITEM.get());
                output.accept(ORCHESTRATION_DISK_ITEM.get());
                output.accept(SIGNAL_TRANSMITTER_ITEM.get());
                output.accept(SPEED_AUGMENT_ITEM.get());
                output.accept(PRODUCTIVITY_AUGMENT_ITEM.get());
                output.accept(STABILIZER_AUGMENT_ITEM.get());
                output.accept(CABLE_CAPACITY_AUGMENT_ITEM.get());
                output.accept(WIRELESS_CAPACITY_AUGMENT_ITEM.get());
                output.accept(WIRELESS_RANGE_AUGMENT_ITEM.get());
                output.accept(INFINITE_WIRELESS_AUGMENT_ITEM.get());
                output.accept(INTERDIMENSIONAL_WIRELESS_AUGMENT_ITEM.get());
                output.accept(DUNGEON_SPEED_AUGMENT_ITEM.get());
                output.accept(DUNGEON_PRODUCTIVITY_AUGMENT_ITEM.get());
                output.accept(DUNGEON_STABILIZER_AUGMENT_ITEM.get());
                output.accept(DUNGEON_SPECIALIZER_FOUNDATIONS_ITEM.get());
                output.accept(DUNGEON_SPECIALIZER_EXPEDITION_ITEM.get());
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
                ItemStack debugRecoveryStrongbox = LOCKED_RECOVERY_STRONGBOX_BLOCK_ITEM.get().getDefaultInstance();
                ItemStack debugRecoveryKey = RECOVERY_STRONGBOX_KEY_ITEM.get().getDefaultInstance();
                String debugRecoveryId = UUID.fromString("00000000-0000-0000-0000-000000000001").toString();
                debugRecoveryStrongbox.set(RECOVERY_STRONGBOX_ID.get(), debugRecoveryId);
                debugRecoveryKey.set(RECOVERY_STRONGBOX_ID.get(), debugRecoveryId);
                output.accept(debugRecoveryStrongbox);
                output.accept(debugRecoveryKey);
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
