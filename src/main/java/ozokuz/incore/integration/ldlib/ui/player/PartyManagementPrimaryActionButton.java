package ozokuz.incore.integration.ldlib.ui.player;

import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import ozokuz.incore.client.features.party.PartyClientCache;
import ozokuz.incore.features.party.network.PartyActionPayload;
import ozokuz.incore.integration.ldlib.ui.INCoreLdLibUiScaffold;

final class PartyManagementPrimaryActionButton extends Button {
    PartyManagementPrimaryActionButton() {
        INCoreLdLibUiScaffold.styleActionButton(this);
        setText(Component.translatable("screen.incore.party.create"));
        setOnClick(event -> PacketDistributor.sendToServer(new PartyActionPayload(primaryAction(), null)));
    }

    @Override
    public void screenTick() {
        setText(Component.translatable(
                PartyClientCache.isInParty()
                        ? "screen.incore.party.leave"
                        : "screen.incore.party.create"
        ));
        super.screenTick();
    }

    private static PartyActionPayload.ActionType primaryAction() {
        return PartyClientCache.isInParty()
                ? PartyActionPayload.ActionType.LEAVE
                : PartyActionPayload.ActionType.CREATE;
    }
}
