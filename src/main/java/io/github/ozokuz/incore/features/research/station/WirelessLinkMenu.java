package io.github.ozokuz.incore.features.research.station;

import io.github.ozokuz.incore.features.machines.multiblock.*;

import io.github.ozokuz.incore.Registration;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;

public class WirelessLinkMenu extends AbstractMachineInventoryMenu {
    private final WirelessLinkBlockEntity wirelessLink;
    private int ownerKindOrdinal;
    private int bindingStatus;
    private int hasStoredChannel;

    public WirelessLinkMenu(int containerId, Inventory playerInventory, WirelessLinkBlockEntity wirelessLink) {
        super(Registration.WIRELESS_LINK_MENU.get(), containerId, playerInventory, wirelessLink, 1);
        this.wirelessLink = wirelessLink;
        ownerKindOrdinal = wirelessLink.ownerKind().ordinal();
        bindingStatus = wirelessLink.bindingStatus();
        hasStoredChannel = wirelessLink.hasStoredChannel() ? 1 : 0;

        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return wirelessLink.ownerKind().ordinal();
            }

            @Override
            public void set(int value) {
                ownerKindOrdinal = value;
            }
        });
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return wirelessLink.bindingStatus();
            }

            @Override
            public void set(int value) {
                bindingStatus = Math.max(0, value);
            }
        });
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return wirelessLink.hasStoredChannel() ? 1 : 0;
            }

            @Override
            public void set(int value) {
                hasStoredChannel = value > 0 ? 1 : 0;
            }
        });
    }

    public MultiblockOwnerKind ownerKind() {
        return ownerKindOrdinal < 0 || ownerKindOrdinal >= MultiblockOwnerKind.values().length
                ? MultiblockOwnerKind.NONE
                : MultiblockOwnerKind.values()[ownerKindOrdinal];
    }

    public int bindingStatus() {
        return bindingStatus;
    }

    public boolean hasStoredChannel() {
        return hasStoredChannel > 0;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != 0 || ownerKind() != MultiblockOwnerKind.ORCHESTRATOR || !stillValid(player)) {
            return false;
        }
        wirelessLink.clearStoredBinding();
        broadcastChanges();
        return true;
    }
}
