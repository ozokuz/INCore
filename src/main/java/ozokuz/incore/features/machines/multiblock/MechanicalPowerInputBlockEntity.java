package ozokuz.incore.features.machines.multiblock;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import ozokuz.incore.Config;
import ozokuz.incore.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MechanicalPowerInputBlockEntity extends KineticBlockEntity implements IMechanicalPowerAdapter, IMachinePowerInput, MenuProvider {
    public MechanicalPowerInputBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.MECHANICAL_POWER_INPUT_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MechanicalPowerInputBlockEntity input) {
        input.tick();
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public float availableRpm() {
        return isOperational() ? Math.abs(getSpeed()) : 0.0F;
    }

    @Override
    public boolean isOperational() {
        return !isOverStressed() && Math.abs(getSpeed()) > 0.5F;
    }

    @Override
    public int availablePower(int maxRp) {
        int rpPerRpm = Math.max(0, Config.MECHANICAL_CORE_RP_PER_RPM.get());
        int rpFromInput = (int) Math.floor(availableRpm()) * rpPerRpm;
        int rpLimit = Math.min(Math.max(0, maxRp), Math.max(1, Config.MECHANICAL_CORE_MAX_RP_PER_TICK.get()));
        return Math.max(0, Math.min(rpLimit, rpFromInput));
    }

    @Override
    public int pullPower(int maxRp) {
        return availablePower(maxRp);
    }

    @Override
    public MachinePowerFamily family() {
        return MachinePowerFamily.MECHANICAL;
    }

    @Override
    public int powerTier() {
        return 1;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory, @NotNull Player player) {
        return new PowerInputMenu(containerId, playerInventory, worldPosition);
    }
}
