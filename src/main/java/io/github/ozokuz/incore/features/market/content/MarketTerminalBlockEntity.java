package io.github.ozokuz.incore.features.market.content;

import io.github.ozokuz.incore.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MarketTerminalBlockEntity extends BlockEntity {
    private @Nullable UUID owner;
    private final Set<UUID> trustedPlayers = new HashSet<>();

    public MarketTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.MARKET_TERMINAL_BE.get(), pos, state);
    }

    public void setOwner(@Nullable UUID owner) {
        this.owner = owner;
        setChanged();
    }

    public @Nullable UUID owner() {
        return owner;
    }

    public boolean canManageTrust(UUID playerId) {
        return owner != null && owner.equals(playerId);
    }

    public boolean canTrade(net.minecraft.world.entity.player.Player player) {
        if (owner == null) {
            return true;
        }

        UUID playerId = player.getUUID();
        return owner.equals(playerId) || trustedPlayers.contains(playerId);
    }

    public Set<UUID> trustedPlayers() {
        return Set.copyOf(trustedPlayers);
    }

    public void addTrusted(UUID id) {
        if (id == null) {
            return;
        }
        if (trustedPlayers.add(id)) {
            setChanged();
        }
    }

    public void removeTrusted(UUID id) {
        if (id == null) {
            return;
        }
        if (trustedPlayers.remove(id)) {
            setChanged();
        }
    }

    public boolean toggleTrusted(UUID id) {
        if (id == null) {
            return false;
        }

        if (trustedPlayers.contains(id)) {
            trustedPlayers.remove(id);
            setChanged();
            return false;
        }

        trustedPlayers.add(id);
        setChanged();
        return true;
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) {
            tag.putUUID("owner", owner);
        }

        ListTag trustedTag = new ListTag();
        for (UUID trusted : trustedPlayers) {
            trustedTag.add(StringTag.valueOf(trusted.toString()));
        }
        tag.put("trustedPlayers", trustedTag);
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
        trustedPlayers.clear();

        ListTag trustedTag = tag.getList("trustedPlayers", Tag.TAG_STRING);
        for (Tag row : trustedTag) {
            try {
                trustedPlayers.add(UUID.fromString(row.getAsString()));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }
}
