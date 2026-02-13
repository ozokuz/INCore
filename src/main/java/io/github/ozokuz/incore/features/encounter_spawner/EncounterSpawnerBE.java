package io.github.ozokuz.incore.features.encounter_spawner;

import io.github.ozokuz.incore.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
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
    private static final int TRIGGER_RADIUS = 8;
    private State state = State.IDLE;
    private EncounterData encounterData;
    private String encounterId;
    private int timer = 10;
    private BlockPos anchor;
    private Vec3i spawnOffset;

    enum State {
        IDLE,
        WARN,
        SPAWN
    }

    public EncounterSpawnerBE(BlockPos pos, BlockState blockState) {
        super(Registration.ENCOUNTER_SPAWNER_BE.get(), pos, blockState);
    }

    public void setSpawnOffset(BlockPos pos) {
        spawnOffset = pos;
        anchor = worldPosition.offset(pos);
    }

    public void setEncounterId(String encounterId) {
        this.encounterId = encounterId;
        encounterData = EncounterManager.ENCOUNTERS.get(ResourceLocation.tryParse(encounterId));
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);

        encounterId = tag.getString("encounter");
        if (encounterId.isEmpty()) return;

        encounterData = EncounterManager.ENCOUNTERS.get(ResourceLocation.tryParse(encounterId));
        var spawnOffsetArr = tag.getIntArray("spawn_offset");
        spawnOffset = new Vec3i(spawnOffsetArr[0], spawnOffsetArr[1], spawnOffsetArr[2]);
        anchor = worldPosition.offset(spawnOffset);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);

        if (encounterId == null || encounterId.isEmpty() || spawnOffset == null) return;

        tag.putString("encounter", encounterId);
        tag.putIntArray("spawn_offset", new int[]{spawnOffset.getX(), spawnOffset.getY(), spawnOffset.getZ()});
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T blockEntity) {
        var be = (EncounterSpawnerBE) blockEntity;
        if (be.state == State.SPAWN || level == null || level.isClientSide) return;

        if (be.state == State.IDLE) {
            var players = level.getNearbyPlayers(TargetingConditions.DEFAULT, null, new AABB(pos).inflate(TRIGGER_RADIUS));

            if (players.isEmpty()) return;
            if (players.stream().allMatch(Player::isCreative)) return;

            be.state = State.WARN;

            level.playSound(null, be.worldPosition, SoundEvent.createVariableRangeEvent(ResourceLocation.parse("minecraft:block.fire.extinguish")), SoundSource.BLOCKS, 1f, 1f);

            return;
        }

        if (--be.timer > 0) {
            return;
        }

        be.state = State.SPAWN;
        be.spawnMobs((ServerLevel) level);
        level.removeBlock(pos, false);
    }

    private void spawnMobs(ServerLevel serverLevel) {
        for (var mob : encounterData.mobs()) {
            spawn(serverLevel, mob);
        }
    }

    private void spawn(ServerLevel level, EncounterData.MobEntry entry) {
        for (int i = 0; i < entry.count(); i++) {
            Mob mob = entry.type().create(level);

            if (mob == null) continue;

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

        return pos.getY();
    }
}
