package io.github.ozokuz.incore.features.market.content;

import io.github.ozokuz.incore.Registration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MarketAutoBuyerMenu extends AbstractContainerMenu {
    public static final int CARD_X = 24;
    public static final int CARD_Y = 176;
    public static final int OUTPUT_X = 56;
    public static final int OUTPUT_Y = 158;
    public static final int PLAYER_INVENTORY_X = 34;
    public static final int PLAYER_INVENTORY_Y = 238;
    public static final int HOTBAR_Y = 296;

    private final MarketAutoBuyerBlockEntity blockEntity;
    private final ContainerData data;

    public MarketAutoBuyerMenu(int containerId, Inventory playerInventory, MarketAutoBuyerBlockEntity blockEntity) {
        super(Registration.MARKET_AUTOBUYER_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.data = blockEntity.data;

        for (int i = 0; i < data.getCount(); i++) {
            addDataSlot(net.minecraft.world.inventory.DataSlot.forContainer(data, i));
        }

        addSlot(new Slot(blockEntity, MarketAutoBuyerBlockEntity.CARD_SLOT, CARD_X, CARD_Y));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slot = MarketAutoBuyerBlockEntity.OUTPUT_START + col + row * 9;
                addSlot(new Slot(blockEntity, slot, OUTPUT_X + col * 18, OUTPUT_Y + row * 18));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, PLAYER_INVENTORY_X + col * 18, PLAYER_INVENTORY_Y + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, PLAYER_INVENTORY_X + col * 18, HOTBAR_Y));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        int machineSlots = MarketAutoBuyerBlockEntity.SLOT_COUNT;
        if (index < machineSlots) {
            if (!moveItemStackTo(stack, machineSlots, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!moveItemStackTo(stack, 0, machineSlots, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity.stillValid(player) && blockEntity.canAccess(player);
    }

    public int progress() {
        return data.get(0);
    }

    public int maxProgress() {
        return Math.max(1, data.get(1));
    }

    public int status() {
        return data.get(2);
    }

    public int priceCap() {
        return data.get(3);
    }

    public int batchSize() {
        return data.get(4);
    }

    public boolean enabled() {
        return data.get(5) != 0;
    }

    public int progressScaled(int width) {
        return Math.clamp((progress() * width) / maxProgress(), 0, width);
    }

    public BlockPosAccessor positionAccessor() {
        return new BlockPosAccessor(blockEntity.getBlockPos().asLong());
    }

    public String targetItemId() {
        ResourceLocation id = blockEntity.targetItemId();
        return id == null ? "" : id.toString();
    }

    public record BlockPosAccessor(long asLong) {
    }
}
