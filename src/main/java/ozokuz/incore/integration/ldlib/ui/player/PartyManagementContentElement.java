package ozokuz.incore.integration.ldlib.ui.player;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import ozokuz.incore.client.features.party.PartyClientCache;
import ozokuz.incore.client.ui.UIScreenTheme;
import ozokuz.incore.features.party.network.PartyActionPayload;
import ozokuz.incore.integration.ldlib.ui.INCoreLdLibUiScaffold;

final class PartyManagementContentElement extends UIElement {
    private static final int SMALL_BUTTON_WIDTH = 60;
    private static final int ACTION_BUTTON_WIDTH = 100;
    private static final int ROW_BUTTON_HEIGHT = 18;

    private long lastSeenCacheVersion = Long.MIN_VALUE;

    private final Set<UUID> sendingInviteTargetIds = new HashSet<>();

    PartyManagementContentElement() {
        layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(8);
        });
        internalSetup();
    }

    @Override
    public void screenTick() {
        if (!isClientUi()) {
            super.screenTick();
            return;
        }

        long cacheVersion = PartyClientCache.getVersion();
        if (cacheVersion != lastSeenCacheVersion || getChildren().isEmpty()) {
            if (cacheVersion != lastSeenCacheVersion) {
                sendingInviteTargetIds.clear();
                lastSeenCacheVersion = cacheVersion;
            }
            rebuild();
        }
        super.screenTick();
    }

    private boolean isClientUi() {
        return getModularUI() != null
                && getModularUI().player != null
                && getModularUI().player.level().isClientSide();
    }

    private void rebuild() {
        clearAllChildren();

        if (PartyClientCache.hasPendingInvite()) {
            addChild(createPendingInviteSection().root());
        }
        addChild(createPartySection().root());
        if (shouldShowInviteSection()) {
            addChild(createInviteSection().root());
        }
    }

    private INCoreLdLibUiScaffold.SectionScaffold createPendingInviteSection() {
        var section = INCoreLdLibUiScaffold.createSection(Component.translatable("screen.incore.party.section_pending_invite"));

        section.body().addChildren(
                bodyLabel(Component.translatable("screen.incore.party.invite_from", PartyClientCache.getInviteInviterName()), UIScreenTheme.Info.TITLE_TEXT),
                bodyLabel(Component.translatable("screen.incore.party.party_id", PartyClientCache.getInvitePartyId()), UIScreenTheme.Info.MUTED_TEXT),
                actionRow(
                        actionButton(
                                Component.translatable("screen.incore.party.accept"),
                                PartyActionPayload.ActionType.ACCEPT,
                                null,
                                ACTION_BUTTON_WIDTH
                        ),
                        actionButton(
                                Component.translatable("screen.incore.party.decline"),
                                PartyActionPayload.ActionType.DECLINE,
                                null,
                                ACTION_BUTTON_WIDTH
                        )
                )
        );

        return section;
    }

    private INCoreLdLibUiScaffold.SectionScaffold createPartySection() {
        var section = INCoreLdLibUiScaffold.createSection(Component.translatable("screen.incore.party.section_party"));

        if (!PartyClientCache.isInParty()) {
            section.body().addChild(bodyLabel(
                    Component.translatable("screen.incore.party.not_in_party"),
                    UIScreenTheme.Info.MUTED_TEXT
            ));
            return section;
        }

        UUID myId = currentPlayerId();
        boolean isLeader = myId != null && PartyClientCache.isLeader(myId);

        section.body().addChildren(
                bodyLabel(Component.translatable("screen.incore.party.party_id", PartyClientCache.getPartyId()), UIScreenTheme.Info.TITLE_TEXT),
                bodyLabel(Component.translatable("screen.incore.party.leader", PartyClientCache.getLeaderName()), UIScreenTheme.Info.SECONDARY_TEXT)
        );

        for (PartyClientCache.MemberView member : PartyClientCache.getMembers()) {
            boolean memberIsLeader = PartyClientCache.isLeader(member.playerId());
            boolean self = member.playerId().equals(myId);

            UIElement row = INCoreLdLibUiScaffold.row().layout(layout -> {
                layout.widthPercent(100);
                layout.alignItems(AlignItems.CENTER);
                layout.gapAll(6);
            });

            String displayName = member.playerName() + (memberIsLeader ? " ★" : "");
            row.addChildren(
                    bodyLabel(
                            Component.literal(displayName),
                            memberIsLeader ? UIScreenTheme.Info.LEADER_CROWN_TEXT : UIScreenTheme.Info.WHITE_TEXT
                    ).layout(layout -> layout.flex(1))
            );

            if (isLeader && !self) {
                if (!memberIsLeader) {
                    row.addChild(actionButton(
                            Component.translatable("screen.incore.party.promote"),
                            PartyActionPayload.ActionType.PROMOTE,
                            member.playerId(),
                            SMALL_BUTTON_WIDTH
                    ));
                }
                row.addChild(actionButton(
                        Component.translatable("screen.incore.party.kick"),
                        PartyActionPayload.ActionType.KICK,
                        member.playerId(),
                        SMALL_BUTTON_WIDTH
                ));
            }

            section.body().addChild(row);
        }

        return section;
    }

    private INCoreLdLibUiScaffold.SectionScaffold createInviteSection() {
        var section = INCoreLdLibUiScaffold.createSection(Component.translatable("screen.incore.party.section_invite"));
        List<PartyClientCache.PlayerView> onlinePlayers = PartyClientCache.getOnlinePlayers();

        if (onlinePlayers.isEmpty()) {
            section.body().addChild(bodyLabel(
                    Component.translatable("screen.incore.party.no_online_players"),
                    UIScreenTheme.Info.MUTED_TEXT
            ));
            return section;
        }

        for (PartyClientCache.PlayerView player : onlinePlayers) {
            boolean sendingInvite = sendingInviteTargetIds.contains(player.playerId());
            boolean outgoingInvite = PartyClientCache.hasOutgoingInvite(player.playerId());

            Button inviteButton = INCoreLdLibUiScaffold.actionButton(
                    inviteButtonLabel(sendingInvite, outgoingInvite),
                    ROW_BUTTON_HEIGHT
            );
            inviteButton.layout(layout -> layout.width(ACTION_BUTTON_WIDTH));
            inviteButton.setActive(!sendingInvite && !outgoingInvite);
            inviteButton.setOnClick(event -> sendInvite(player.playerId()));

            section.body().addChild(
                    INCoreLdLibUiScaffold.row()
                            .layout(layout -> {
                                layout.widthPercent(100);
                                layout.alignItems(AlignItems.CENTER);
                                layout.gapAll(6);
                            })
                            .addChildren(
                                    bodyLabel(Component.literal(player.playerName()), UIScreenTheme.Info.WHITE_TEXT)
                                            .layout(layout -> layout.flex(1)),
                                    inviteButton
                            )
            );
        }

        return section;
    }

    private static boolean shouldShowInviteSection() {
        UUID myId = currentPlayerId();
        return PartyClientCache.isInParty() && myId != null && PartyClientCache.isLeader(myId);
    }

    private static UUID currentPlayerId() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player == null ? null : minecraft.player.getUUID();
    }

    private static Label bodyLabel(Component text, int color) {
        Label label = INCoreLdLibUiScaffold.wrappedLabel(text);
        label.textStyle(style -> style.textColor(color).textWrap(TextWrap.HIDE));
        return label;
    }

    private static UIElement actionRow(Button... buttons) {
        return INCoreLdLibUiScaffold.row()
                .layout(layout -> {
                    layout.widthPercent(100);
                    layout.alignItems(AlignItems.CENTER);
                    layout.gapAll(6);
                })
                .addChildren(buttons);
    }

    private static Button actionButton(
            Component text,
            PartyActionPayload.ActionType actionType,
            UUID targetPlayerId,
            int width
    ) {
        Button button = INCoreLdLibUiScaffold.actionButton(text, ROW_BUTTON_HEIGHT);
        button.layout(layout -> layout.width(width));
        button.setOnClick(event -> sendAction(actionType, targetPlayerId));
        return button;
    }

    private void sendInvite(UUID targetPlayerId) {
        sendingInviteTargetIds.add(targetPlayerId);
        rebuild();
        sendAction(PartyActionPayload.ActionType.INVITE, targetPlayerId);
    }

    private static void sendAction(PartyActionPayload.ActionType actionType, UUID targetPlayerId) {
        PacketDistributor.sendToServer(new PartyActionPayload(actionType, targetPlayerId));
    }

    private static Component inviteButtonLabel(boolean sendingInvite, boolean outgoingInvite) {
        if (sendingInvite) {
            return Component.translatable("screen.incore.party.inviting");
        }
        if (outgoingInvite) {
            return Component.translatable("screen.incore.party.invited");
        }
        return Component.translatable("screen.incore.party.invite");
    }
}
