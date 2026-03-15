package ozokuz.incore.features.market.content;

import ozokuz.incore.Registration;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.items.wrapper.RangedWrapper;

public final class MarketMachineCapabilities {
    private MarketMachineCapabilities() {
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                Registration.SHIPMENT_TERMINAL_BE.get(),
                (be, side) -> insertOnly(new InvWrapper(be))
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                Registration.SHIPMENT_TERMINAL_MK2_BE.get(),
                (be, side) -> insertOnly(new InvWrapper(be))
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                Registration.MARKET_AUTOTRADER_BE.get(),
                (be, side) -> new RangedWrapper(
                        new InvWrapper(be),
                        MarketAutoTraderBlockEntity.OUTPUT_START,
                        MarketAutoTraderBlockEntity.SLOT_COUNT
                )
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                Registration.MARKET_AUTOTRADER_MK2_BE.get(),
                (be, side) -> new RangedWrapper(
                        new InvWrapper(be),
                        MarketAutoTraderBlockEntity.OUTPUT_START,
                        MarketAutoTraderBlockEntity.SLOT_COUNT
                )
        );
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                Registration.SHIPMENT_TERMINAL_MK2_BE.get(),
                ShipmentTerminalMk2BlockEntity::getEnergyStorage
        );
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                Registration.MARKET_AUTOTRADER_MK2_BE.get(),
                MarketAutoTraderMk2BlockEntity::getEnergyStorage
        );
    }

    private static IItemHandlerModifiable insertOnly(IItemHandlerModifiable delegate) {
        return new IItemHandlerModifiable() {
            @Override
            public int getSlots() {
                return delegate.getSlots();
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return delegate.getStackInSlot(slot);
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return delegate.insertItem(slot, stack, simulate);
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return ItemStack.EMPTY;
            }

            @Override
            public void setStackInSlot(int slot, ItemStack stack) {
                delegate.setStackInSlot(slot, stack);
            }

            @Override
            public int getSlotLimit(int slot) {
                return delegate.getSlotLimit(slot);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return delegate.isItemValid(slot, stack);
            }
        };
    }
}
