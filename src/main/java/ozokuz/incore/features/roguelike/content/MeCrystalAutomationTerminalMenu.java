package ozokuz.incore.features.roguelike.content;

import ozokuz.incore.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;


public class MeCrystalAutomationTerminalMenu extends AbstractContainerMenu {
    private final MeCrystalAutomationTerminalPart part;
    private final BlockPos hostPos;
    private final Direction side;
    private final ContainerLevelAccess access;

    public MeCrystalAutomationTerminalMenu(int containerId, Inventory inventory, BlockPos hostPos, Direction side, MeCrystalAutomationTerminalPart part) {
        super(Registration.ME_CRYSTAL_AUTOMATION_TERMINAL_MENU.get(), containerId);
        this.hostPos = hostPos;
        this.side = side;
        this.part = part;
        this.access = ContainerLevelAccess.create(inventory.player.level(), hostPos);
    }

    public MeCrystalAutomationTerminalPart part() {
        return part;
    }

    public BlockPos hostPos() {
        return hostPos;
    }

    public Direction side() {
        return side;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // Quick move (shift-click) is intentionally disabled to prevent accidental transfers
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return access.evaluate((level, pos) -> {
            if (MeCrystalAutomationTerminalPart.resolve(level, pos, side) == null) {
                return false;
            }
            double dx = player.getX() - (pos.getX() + 0.5D);
            double dy = player.getY() - (pos.getY() + 0.5D);
            double dz = player.getZ() - (pos.getZ() + 0.5D);
            return dx * dx + dy * dy + dz * dz <= 64.0D;
        }, false);
    }
}
