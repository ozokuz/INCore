package io.github.ozokuz.incore.features.market.content;

import io.github.ozokuz.incore.Registration;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public final class MarketMachineCapabilities {
    private MarketMachineCapabilities() {
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                Registration.SHIPMENT_TERMINAL_MK2_BE.get(),
                ShipmentTerminalMk2BlockEntity::getEnergyStorage
        );
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                Registration.MARKET_AUTOBUYER_MK2_BE.get(),
                MarketAutoBuyerMk2BlockEntity::getEnergyStorage
        );
    }
}
