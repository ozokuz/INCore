package io.github.ozokuz.incore.features.roguelike;

import io.github.ozokuz.incore.Registration;
import io.github.ozokuz.incore.features.roguelike.content.RoguelikeAltarBlockEntity;
import io.github.ozokuz.incore.features.roguelike.content.DungeonCrystalDataUtil;
import io.github.ozokuz.incore.features.roguelike.content.RoguelikePortalBlockEntity;
import io.github.ozokuz.incore.features.roguelike.data.AltarOfferingData;
import io.github.ozokuz.incore.features.roguelike.data.AltarOfferingManager;
import io.github.ozokuz.incore.features.roguelike.data.DungeonObjectiveData;
import io.github.ozokuz.incore.features.roguelike.data.DungeonObjectiveManager;
import io.github.ozokuz.incore.features.roguelike.data.DungeonThemeData;
import io.github.ozokuz.incore.features.roguelike.data.DungeonThemeManager;
import io.github.ozokuz.incore.features.roguelike.data.DungeonModifierManager;
import io.github.ozokuz.incore.features.roguelike.instance.DungeonInstanceManager;
import io.github.ozokuz.incore.features.roguelike.state.RoguelikeSavedData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class RoguelikeService {
    private static final int MIN_ALTAR_ITEM_VARIANTS = 3;
    private static final int MAX_ALTAR_ITEM_VARIANTS = 5;
    private static final int ALTAR_VARIANT_GROWTH_STEP = 10;
    private static final double ALTAR_COLLECTION_RADIUS = 1.8D;

    private RoguelikeService() {
    }

    public static void tickAltar(ServerLevel level, BlockPos altarPos, RoguelikeAltarBlockEntity altar) {
        UUID ownerId = altar.ownerId();
        if (ownerId == null) {
            syncAltarDisplay(List.of(), altar);
            return;
        }

        MinecraftServer server = level.getServer();
        RoguelikeSavedData data = RoguelikeSavedData.get(server);
        ensureAltarRequirement(server, data, ownerId);

        boolean crystalPlaced = data.isCrystalPlaced(ownerId);
        altar.setCrystalPlaced(crystalPlaced);

        if (crystalPlaced) {
            absorbDroppedItems(level, altarPos, data, ownerId);
        }
        syncAltarDisplay(data.altarRequirements(ownerId), altar);
    }

    public static boolean placeCrystal(Player player, InteractionHand hand, BlockPos altarPos) {
        if (!(player instanceof ServerPlayer serverPlayer) || serverPlayer.getServer() == null) {
            return false;
        }

        UUID ownerId = resolveAltarOwner(serverPlayer, altarPos);
        if (ownerId == null) {
            return false;
        }

        MinecraftServer server = serverPlayer.getServer();
        RoguelikeSavedData data = RoguelikeSavedData.get(server);

        if (data.isCrystalPlaced(ownerId)) {
            serverPlayer.sendSystemMessage(Component.translatable("incore.roguelike.altar.crystal_already_placed"));
            return false;
        }

        ItemStack held = serverPlayer.getItemInHand(hand);
        if (!held.is(Registration.EMPTY_DUNGEON_CRYSTAL_ITEM.get())) {
            return false;
        }

        if (!serverPlayer.isCreative()) {
            held.shrink(1);
        }

        data.setCrystalPlaced(ownerId, true);
        ensureAltarRequirement(server, data, ownerId);

        serverPlayer.sendSystemMessage(
                Component.translatable("incore.roguelike.altar.crystal_placed").withStyle(ChatFormatting.AQUA));
        showAltarRequirement(serverPlayer, altarPos);

        BlockEntity blockEntity = serverPlayer.serverLevel().getBlockEntity(altarPos);
        if (blockEntity instanceof RoguelikeAltarBlockEntity altar) {
            altar.setCrystalPlaced(true);
            syncAltarDisplay(data.altarRequirements(ownerId), altar);
        }

        return true;
    }

    public static boolean tryFinalizeAltar(Player player, @Nullable InteractionHand hand, BlockPos altarPos) {
        if (!(player instanceof ServerPlayer serverPlayer) || serverPlayer.getServer() == null) {
            return false;
        }

        UUID ownerId = resolveAltarOwner(serverPlayer, altarPos);
        if (ownerId == null) {
            return false;
        }

        MinecraftServer server = serverPlayer.getServer();
        RoguelikeSavedData data = RoguelikeSavedData.get(server);
        ensureAltarRequirement(server, data, ownerId);

        if (!data.isCrystalPlaced(ownerId)) {
            serverPlayer.sendSystemMessage(Component.translatable("incore.roguelike.altar.need_crystal"));
            showAltarRequirement(serverPlayer, altarPos);
            return false;
        }

        if (!data.isAltarComplete(ownerId)) {
            showAltarRequirement(serverPlayer, altarPos);
            serverPlayer.sendSystemMessage(Component.translatable("incore.roguelike.altar.not_ready"));
            return false;
        }

        ItemStack crystal = new ItemStack(Registration.DUNGEON_CRYSTAL_ITEM.get());
        if (!serverPlayer.addItem(crystal)) {
            serverPlayer.drop(crystal, false);
        }

        data.incrementCrystalsCrafted(ownerId);
        data.setCrystalPlaced(ownerId, false);
        chooseNextAltarRequirement(server, data, ownerId);

        serverPlayer.sendSystemMessage(
                Component.translatable("incore.roguelike.altar.created_crystal").withStyle(ChatFormatting.AQUA));
        showAltarRequirement(serverPlayer, altarPos);

        BlockEntity blockEntity = serverPlayer.serverLevel().getBlockEntity(altarPos);
        if (blockEntity instanceof RoguelikeAltarBlockEntity altar) {
            altar.setCrystalPlaced(false);
            syncAltarDisplay(data.altarRequirements(ownerId), altar);
        }

        return true;
    }

    public static void showAltarRequirement(Player player, BlockPos altarPos) {
        if (!(player instanceof ServerPlayer serverPlayer) || serverPlayer.getServer() == null) {
            return;
        }

        UUID ownerId = altarPos == null
                ? serverPlayer.getUUID()
                : resolveAltarOwner(serverPlayer, altarPos);
        if (ownerId == null) {
            return;
        }

        MinecraftServer server = serverPlayer.getServer();
        RoguelikeSavedData data = RoguelikeSavedData.get(server);
        ensureAltarRequirement(server, data, ownerId);

        List<RoguelikeSavedData.AltarRequirement> requirements = data.altarRequirements(ownerId);
        if (requirements.size() < MIN_ALTAR_ITEM_VARIANTS) {
            serverPlayer.sendSystemMessage(Component.translatable("incore.roguelike.altar.no_offerings"));
            return;
        }

        serverPlayer.sendSystemMessage(Component.translatable("incore.roguelike.altar.requirement.header"));

        for (RoguelikeSavedData.AltarRequirement requirement : requirements) {
            AltarOfferingData offering = AltarOfferingManager.OFFERINGS.get(requirement.offeringId());
            if (offering == null) {
                continue;
            }

            serverPlayer.sendSystemMessage(
                    Component.translatable(
                            "incore.roguelike.altar.requirement.item",
                            offering.item().getDescription(),
                            requirement.submittedAmount(),
                            requirement.requiredAmount())
                            .withStyle(requirement.isComplete() ? ChatFormatting.GREEN : ChatFormatting.WHITE));
        }

        boolean crystalPlaced = data.isCrystalPlaced(ownerId);
        if (!crystalPlaced) {
            serverPlayer.sendSystemMessage(Component.translatable("incore.roguelike.altar.need_crystal"));
        } else if (data.isAltarComplete(ownerId)) {
            serverPlayer.sendSystemMessage(Component.translatable("incore.roguelike.altar.ready"));
        } else {
            serverPlayer.sendSystemMessage(Component.translatable("incore.roguelike.altar.drop_items_hint"));
        }
    }

    public static int getAltarDifficulty(MinecraftServer server, UUID ownerId) {
        return RoguelikeSavedData.get(server).crystalsCrafted(ownerId);
    }

    public static void setAltarDifficulty(MinecraftServer server, UUID ownerId, int value) {
        RoguelikeSavedData data = RoguelikeSavedData.get(server);
        data.setCrystalsCrafted(ownerId, value);
        chooseNextAltarRequirement(server, data, ownerId);
    }

    public static ItemStack createDungeonCrystal(int count, ResourceLocation themeId, ResourceLocation objectiveId) {
        return createDungeonCrystal(count, themeId, objectiveId, List.of(), themeId != null || objectiveId != null);
    }

    public static ItemStack createDungeonCrystal(
            int count,
            ResourceLocation themeId,
            ResourceLocation objectiveId,
            List<ResourceLocation> modifiers,
            boolean customConfigured) {
        ItemStack stack = new ItemStack(Registration.DUNGEON_CRYSTAL_ITEM.get(), Math.max(1, count));
        if (themeId == null && objectiveId == null && (modifiers == null || modifiers.isEmpty())) {
            return stack;
        }

        if (themeId != null) {
            stack.set(Registration.DUNGEON_CRYSTAL_THEME.get(), themeId);
        }
        if (objectiveId != null) {
            stack.set(Registration.DUNGEON_CRYSTAL_OBJECTIVE.get(), objectiveId);
        }
        List<ResourceLocation> normalizedModifiers = modifiers == null ? List.of()
                : modifiers.stream()
                        .filter(id -> id != null && DungeonModifierManager.MODIFIERS.containsKey(id))
                        .distinct()
                        .toList();
        DungeonCrystalDataUtil.writeModifiers(stack, normalizedModifiers);
        DungeonCrystalDataUtil.setCustomCrystal(stack,
                customConfigured || themeId != null || objectiveId != null || !normalizedModifiers.isEmpty());

        return stack;
    }

    public static boolean onPortalInteracted(Player player, InteractionHand hand, RoguelikePortalBlockEntity portal,
            BlockPos portalPos) {
        return tryEnterPortal(player, portal, portalPos);
    }

    public static boolean tryActivatePortalFromFrame(Player player, InteractionHand hand, BlockPos clickedPos,
            Direction clickedFace) {
        if (!(player instanceof ServerPlayer serverPlayer) || serverPlayer.getServer() == null) {
            return false;
        }

        ItemStack crystalStack = serverPlayer.getItemInHand(hand);
        if (!crystalStack.is(Registration.DUNGEON_CRYSTAL_ITEM.get())) {
            return false;
        }

        ServerLevel level = serverPlayer.serverLevel();
        Optional<RoguelikePortalShape> shape = findFrameShape(level, clickedPos, clickedFace);
        if (shape.isEmpty()) {
            serverPlayer.sendSystemMessage(Component.translatable("incore.roguelike.portal.frame_invalid"));
            return false;
        }

        RandomSource random = level.random;
        Optional<DungeonThemeManager.PickedTheme> themePick = pickThemeForCrystal(serverPlayer, crystalStack, random);
        Optional<DungeonObjectiveManager.PickedObjective> objectivePick = pickObjectiveForCrystal(serverPlayer,
                crystalStack, random);
        List<ResourceLocation> modifiers = pickModifiersForCrystal(serverPlayer, crystalStack);
        if (themePick.isEmpty() || objectivePick.isEmpty()) {
            if (themePick.isEmpty() && objectivePick.isEmpty()) {
                serverPlayer.sendSystemMessage(Component.translatable("incore.roguelike.portal.data_missing"));
            }
            return false;
        }

        return DungeonInstanceManager.activatePortal(
                serverPlayer,
                shape.get(),
                crystalStack,
                themePick.get().id(),
                objectivePick.get().id(),
                modifiers,
                themePick.get().data());
    }

    public static boolean tryEnterPortal(Player player, RoguelikePortalBlockEntity portal, BlockPos portalPos) {
        if (!(player instanceof ServerPlayer serverPlayer) || serverPlayer.getServer() == null) {
            return false;
        }

        return DungeonInstanceManager.tryEnterPortal(serverPlayer, portal, portalPos);
    }

    public static boolean tryUseReturnPlaceholder(Player player, BlockPos interactionPos) {
        return DungeonInstanceManager.tryUseReturnPlaceholder(player, interactionPos);
    }

    public static void onServerTick(MinecraftServer server) {
        DungeonInstanceManager.onServerTick(server);
    }

    public static void onPlayerDeath(ServerPlayer player) {
        DungeonInstanceManager.onPlayerDeath(player);
    }

    public static void onPlayerRespawn(ServerPlayer player) {
        DungeonInstanceManager.onPlayerRespawn(player);
    }

    public static void onPlayerLogin(ServerPlayer player) {
        DungeonInstanceManager.onPlayerLogin(player);
    }

    public static void onPlayerLogout(ServerPlayer player) {
        DungeonInstanceManager.onPlayerLogout(player);
    }

    public static void onDungeonMobDeath(LivingEntity ignored) {
        DungeonInstanceManager.onDungeonMobDeath(ignored);
    }

    public static void onSpawnPositionCheck(MobSpawnEvent.PositionCheck event) {
        DungeonInstanceManager.onSpawnPositionCheck(event);
    }

    public static void onDungeonBlockInteracted(ServerPlayer player, BlockPos pos, BlockState state) {
        DungeonInstanceManager.onDungeonBlockInteracted(player, pos, state);
    }

    public static boolean trySubmitScavengerToken(Player player, @Nullable InteractionHand hand, BlockPos altarPos) {
        return DungeonInstanceManager.trySubmitScavengerToken(player, hand, altarPos);
    }

    public static Component themeDisplayName(ResourceLocation themeId) {
        return Component.translatable(resourceNameTranslationKey("incore.roguelike.theme", themeId));
    }

    public static Component objectiveDisplayName(ResourceLocation objectiveId) {
        return Component.translatable(resourceNameTranslationKey("incore.roguelike.objective", objectiveId));
    }

    private static UUID resolveAltarOwner(ServerPlayer player, BlockPos altarPos) {
        BlockEntity blockEntity = player.serverLevel().getBlockEntity(altarPos);
        if (!(blockEntity instanceof RoguelikeAltarBlockEntity altar)) {
            return null;
        }

        UUID ownerId = altar.ownerId();
        if (ownerId != null) {
            return ownerId;
        }

        ownerId = player.getUUID();
        altar.setOwner(ownerId);
        return ownerId;
    }

    private static Optional<RoguelikePortalShape> findFrameShape(ServerLevel level, BlockPos clickedPos,
            Direction clickedFace) {
        BlockPos[] seeds = new BlockPos[] {
                clickedPos,
                clickedPos.relative(clickedFace),
                clickedPos.relative(clickedFace.getOpposite())
        };

        Set<BlockPos> checked = new HashSet<>();
        for (BlockPos seed : seeds) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dy = -3; dy <= 2; dy++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        BlockPos candidate = seed.offset(dx, dy, dz);
                        if (!checked.add(candidate)) {
                            continue;
                        }

                        Optional<RoguelikePortalShape> shape = RoguelikePortalShape.findEmptyPortalShape(level,
                                candidate);
                        if (shape.isPresent()) {
                            return shape;
                        }
                    }
                }
            }
        }

        return Optional.empty();
    }

    private static Optional<DungeonThemeManager.PickedTheme> pickThemeForCrystal(ServerPlayer player,
            ItemStack crystalStack, RandomSource random) {
        ResourceLocation customThemeId = crystalStack.get(Registration.DUNGEON_CRYSTAL_THEME.get());
        if (customThemeId == null) {
            return DungeonThemeManager.pickRandom(random);
        }

        DungeonThemeData themeData = DungeonThemeManager.THEMES.get(customThemeId);
        if (themeData == null) {
            player.sendSystemMessage(
                    Component.translatable("incore.roguelike.portal.invalid_theme", customThemeId.toString()));
            return Optional.empty();
        }

        return Optional.of(new DungeonThemeManager.PickedTheme(customThemeId, themeData));
    }

    private static Optional<DungeonObjectiveManager.PickedObjective> pickObjectiveForCrystal(ServerPlayer player,
            ItemStack crystalStack, RandomSource random) {
        ResourceLocation customObjectiveId = crystalStack.get(Registration.DUNGEON_CRYSTAL_OBJECTIVE.get());
        if (customObjectiveId == null) {
            return DungeonObjectiveManager.pickRandom(random);
        }

        DungeonObjectiveData objectiveData = DungeonObjectiveManager.OBJECTIVES.get(customObjectiveId);
        if (objectiveData == null) {
            player.sendSystemMessage(
                    Component.translatable("incore.roguelike.portal.invalid_objective", customObjectiveId.toString()));
            return Optional.empty();
        }

        return Optional.of(new DungeonObjectiveManager.PickedObjective(customObjectiveId, objectiveData));
    }

    private static List<ResourceLocation> pickModifiersForCrystal(ServerPlayer player, ItemStack crystalStack) {
        List<ResourceLocation> selected = DungeonCrystalDataUtil.readModifiers(crystalStack);
        if (selected.isEmpty()) {
            return List.of();
        }

        List<ResourceLocation> valid = new ArrayList<>(selected.size());
        for (ResourceLocation id : selected) {
            if (!DungeonModifierManager.MODIFIERS.containsKey(id)) {
                player.sendSystemMessage(
                        Component.translatable("incore.roguelike.portal.invalid_modifier", id.toString()));
                continue;
            }
            valid.add(id);
        }
        return List.copyOf(valid);
    }

    private static void ensureAltarRequirement(MinecraftServer server, RoguelikeSavedData data, UUID ownerId) {
        List<RoguelikeSavedData.AltarRequirement> current = data.altarRequirements(ownerId);
        if (current.size() >= MIN_ALTAR_ITEM_VARIANTS && hasOnlyValidOfferings(current)) {
            return;
        }

        chooseNextAltarRequirement(server, data, ownerId);
    }

    private static boolean hasOnlyValidOfferings(List<RoguelikeSavedData.AltarRequirement> requirements) {
        for (RoguelikeSavedData.AltarRequirement requirement : requirements) {
            AltarOfferingData offering = AltarOfferingManager.OFFERINGS.get(requirement.offeringId());
            if (offering == null || requirement.requiredAmount() <= 0) {
                return false;
            }
        }

        return true;
    }

    private static void chooseNextAltarRequirement(MinecraftServer server, RoguelikeSavedData data, UUID ownerId) {
        int uniqueItems = (int) AltarOfferingManager.OFFERINGS.values().stream()
                .map(AltarOfferingData::item)
                .distinct()
                .count();

        if (uniqueItems < MIN_ALTAR_ITEM_VARIANTS) {
            data.setAltarRequirements(ownerId, List.of());
            return;
        }

        int desiredVariants = Math.min(
                Math.min(MAX_ALTAR_ITEM_VARIANTS, uniqueItems),
                MIN_ALTAR_ITEM_VARIANTS + (data.crystalsCrafted(ownerId) / ALTAR_VARIANT_GROWTH_STEP));

        List<AltarOfferingManager.PickedOffering> picked = pickDistinctOfferings(server.overworld().random,
                desiredVariants);
        if (picked.size() < MIN_ALTAR_ITEM_VARIANTS) {
            data.setAltarRequirements(ownerId, List.of());
            return;
        }

        List<RoguelikeSavedData.AltarRequirement> requirements = new ArrayList<>(picked.size());
        for (AltarOfferingManager.PickedOffering offering : picked) {
            int requiredAmount = offering.data().requiredAmount(data.crystalsCrafted(ownerId));
            requirements.add(new RoguelikeSavedData.AltarRequirement(offering.id(), requiredAmount, 0));
        }

        data.setAltarRequirements(ownerId, requirements);
    }

    private static List<AltarOfferingManager.PickedOffering> pickDistinctOfferings(RandomSource random, int count) {
        List<Map.Entry<ResourceLocation, AltarOfferingData>> pool = new ArrayList<>(
                AltarOfferingManager.OFFERINGS.entrySet());
        List<AltarOfferingManager.PickedOffering> picked = new ArrayList<>(count);
        Set<Item> usedItems = new HashSet<>();

        while (!pool.isEmpty() && picked.size() < count) {
            int totalWeight = 0;
            for (Map.Entry<ResourceLocation, AltarOfferingData> entry : pool) {
                if (usedItems.contains(entry.getValue().item())) {
                    continue;
                }
                totalWeight += entry.getValue().weight();
            }

            if (totalWeight <= 0) {
                break;
            }

            int roll = random.nextInt(totalWeight);
            Map.Entry<ResourceLocation, AltarOfferingData> choice = null;
            for (Map.Entry<ResourceLocation, AltarOfferingData> entry : pool) {
                if (usedItems.contains(entry.getValue().item())) {
                    continue;
                }

                roll -= entry.getValue().weight();
                if (roll < 0) {
                    choice = entry;
                    break;
                }
            }

            if (choice == null) {
                break;
            }

            picked.add(new AltarOfferingManager.PickedOffering(choice.getKey(), choice.getValue()));
            Item chosenItem = choice.getValue().item();
            usedItems.add(chosenItem);
            pool.removeIf(entry -> entry.getValue().item() == chosenItem);
        }

        return picked;
    }

    private static void absorbDroppedItems(ServerLevel level, BlockPos altarPos, RoguelikeSavedData data,
            UUID ownerId) {
        if (data.isAltarComplete(ownerId)) {
            return;
        }

        AABB area = new AABB(altarPos).inflate(ALTAR_COLLECTION_RADIUS, 1.0D, ALTAR_COLLECTION_RADIUS);
        List<ItemEntity> itemEntities = level.getEntitiesOfClass(ItemEntity.class, area,
                itemEntity -> itemEntity.isAlive() && !itemEntity.getItem().isEmpty());

        for (ItemEntity itemEntity : itemEntities) {
            if (data.isAltarComplete(ownerId)) {
                return;
            }

            ItemStack stack = itemEntity.getItem();
            int consumed = consumeStackForRequirements(stack, data, ownerId);
            if (consumed <= 0) {
                continue;
            }

            if (stack.isEmpty()) {
                itemEntity.discard();
            } else {
                itemEntity.setItem(stack);
            }
        }
    }

    private static int consumeStackForRequirements(ItemStack stack, RoguelikeSavedData data, UUID ownerId) {
        if (stack.isEmpty()) {
            return 0;
        }

        for (RoguelikeSavedData.AltarRequirement requirement : data.altarRequirements(ownerId)) {
            if (requirement.isComplete()) {
                continue;
            }

            AltarOfferingData offering = AltarOfferingManager.OFFERINGS.get(requirement.offeringId());
            if (offering == null || offering.item() != stack.getItem()) {
                continue;
            }

            int toConsume = Math.min(requirement.remaining(), stack.getCount());
            int consumed = data.submitOffering(ownerId, requirement.offeringId(), toConsume);
            if (consumed > 0) {
                stack.shrink(consumed);
                return consumed;
            }

            return 0;
        }

        return 0;
    }

    private static void syncAltarDisplay(List<RoguelikeSavedData.AltarRequirement> requirements,
            RoguelikeAltarBlockEntity altar) {
        List<RoguelikeAltarBlockEntity.DisplayEntry> entries = new ArrayList<>();
        for (RoguelikeSavedData.AltarRequirement requirement : requirements) {
            AltarOfferingData offering = AltarOfferingManager.OFFERINGS.get(requirement.offeringId());
            if (offering == null) {
                continue;
            }

            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(offering.item());
            entries.add(new RoguelikeAltarBlockEntity.DisplayEntry(itemId, requirement.submittedAmount(),
                    requirement.requiredAmount()));
        }

        altar.setDisplayEntries(entries);
    }

    private static String resourceNameTranslationKey(String baseKey, ResourceLocation id) {
        String path = id.getPath().replace('/', '.');
        String suffix = "incore".equals(id.getNamespace())
                ? path
                : id.getNamespace() + "." + path;
        return baseKey + "." + suffix;
    }
}
