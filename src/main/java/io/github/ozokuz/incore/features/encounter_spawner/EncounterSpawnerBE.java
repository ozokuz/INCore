package io.github.ozokuz.incore.features.encounter_spawner;

import io.github.ozokuz.incore.Config;
import io.github.ozokuz.incore.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

public class EncounterSpawnerBE extends BlockEntity {
    private static final int CHECK_INTERVAL_TICKS = 12;
    private static final int SPAWN_DELAY_TICKS = 20;
    private static final ResourceLocation TRIGGER_SOUND_ID = ResourceLocation.withDefaultNamespace("block.fire.extinguish");

    private String encounterId = "";
    private EncounterData encounterData;
    private Vec3i spawnOffset = Vec3i.ZERO;
    private boolean triggered;
    private long spawnDueGameTime = -1L;

    public EncounterSpawnerBE(BlockPos pos, BlockState blockState) {
        super(Registration.ENCOUNTER_SPAWNER_BE.get(), pos, blockState);
    }

    public void setSpawnOffset(BlockPos pos) {
        spawnOffset = new Vec3i(pos.getX(), pos.getY(), pos.getZ());
        setChanged();
    }

    public void setEncounterId(String encounterId) {
        this.encounterId = encounterId == null ? "" : encounterId;
        encounterData = EncounterManager.ENCOUNTERS.get(ResourceLocation.tryParse(this.encounterId));
        setChanged();
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);

        encounterId = tag.getString("encounter");
        encounterData = EncounterManager.ENCOUNTERS.get(ResourceLocation.tryParse(encounterId));

        int[] spawnOffsetArr = tag.getIntArray("spawn_offset");
        if (spawnOffsetArr.length >= 3) {
            spawnOffset = new Vec3i(spawnOffsetArr[0], spawnOffsetArr[1], spawnOffsetArr[2]);
        } else {
            spawnOffset = Vec3i.ZERO;
        }

        triggered = tag.getBoolean("triggered");
        spawnDueGameTime = tag.contains("spawn_due_game_time", Tag.TAG_LONG)
                ? tag.getLong("spawn_due_game_time")
                : -1L;
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);

        tag.putString("encounter", encounterId == null ? "" : encounterId);
        tag.putIntArray("spawn_offset", new int[]{spawnOffset.getX(), spawnOffset.getY(), spawnOffset.getZ()});
        tag.putBoolean("triggered", triggered);
        tag.putLong("spawn_due_game_time", spawnDueGameTime);
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T blockEntity) {
        if (level == null || level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!(blockEntity instanceof EncounterSpawnerBE be)) {
            return;
        }

        if (be.encounterData == null && be.encounterId != null && !be.encounterId.isEmpty()) {
            be.encounterData = EncounterManager.ENCOUNTERS.get(ResourceLocation.tryParse(be.encounterId));
        }

        if (be.triggered) {
            if (be.spawnDueGameTime >= 0L && serverLevel.getGameTime() >= be.spawnDueGameTime) {
                be.spawnMobs(serverLevel);
                serverLevel.removeBlock(pos, false);
            }
            return;
        }

        if (!be.shouldRunProximityCheck(serverLevel.getGameTime())) {
            return;
        }

        int triggerRadius = Config.ENCOUNTER_TRIGGER_RADIUS.get();
        var players = serverLevel.getNearbyPlayers(TargetingConditions.DEFAULT, null, new AABB(pos).inflate(triggerRadius));
        if (players.isEmpty() || players.stream().allMatch(Player::isCreative)) {
            return;
        }

        be.triggered = true;
        be.spawnDueGameTime = serverLevel.getGameTime() + SPAWN_DELAY_TICKS;
        be.setChanged();

        serverLevel.playSound(
                null,
                be.worldPosition,
                SoundEvent.createVariableRangeEvent(TRIGGER_SOUND_ID),
                SoundSource.BLOCKS,
                1.0F,
                1.0F
        );
    }

    private void spawnMobs(ServerLevel serverLevel) {
        if (encounterData == null) {
            return;
        }

        for (var mob : encounterData.mobs()) {
            spawn(serverLevel, mob);
        }
    }

    private boolean shouldRunProximityCheck(long gameTime) {
        long phase = Math.floorMod(worldPosition.asLong(), CHECK_INTERVAL_TICKS);
        return Math.floorMod(gameTime, CHECK_INTERVAL_TICKS) == phase;
    }

    private void spawn(ServerLevel level, EncounterData.MobEntry entry) {
        if (entry == null || entry.type() == null || entry.count() <= 0) {
            return;
        }

        BlockPos anchor = worldPosition.offset(spawnOffset);
        for (int i = 0; i < entry.count(); i++) {
            Mob mob = entry.type().create(level);

            if (mob == null) {
                continue;
            }

            double x = anchor.getX() + 0.5 + level.random.nextGaussian() * 0.6;
            double z = anchor.getZ() + 0.5 + level.random.nextGaussian() * 0.6;
            double y = findGroundY(level, anchor, x, z);

            mob.moveTo(x, y, z, level.random.nextFloat() * 360F, 0);

            mob.getPersistentData().putString("incore:loot_table", encounterData.lootTable());

            mob.setCustomName(Component.translatable("incore.encounter_spawner.mob", mob.getDisplayName()));

            level.addFreshEntity(mob);
        }
    }

    private double findGroundY(ServerLevel level, BlockPos anchor, double x, double z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos((int) x, anchor.getY(), (int) z);

        while (pos.getY() > level.getMinBuildHeight() && level.getBlockState(pos.below()).isAir()) {
            pos.move(Direction.DOWN);
        }

        return pos.getY() + 1;
    }
}
