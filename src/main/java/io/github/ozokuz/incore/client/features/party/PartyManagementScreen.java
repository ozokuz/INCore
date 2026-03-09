package io.github.ozokuz.incore.client.features.party;

import io.github.ozokuz.incore.client.ui.UIScreenTheme;
import io.github.ozokuz.incore.client.ui.render.ThemedUi;
import io.github.ozokuz.incore.features.party.network.PartyActionPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class PartyManagementScreen extends Screen {
    private static final UIScreenTheme THEME = UIScreenTheme.INFO;
    private static final int TARGET_WINDOW_WIDTH = 320;
    private static final int TARGET_WINDOW_HEIGHT = 400;
    private static final int BUTTON_HEIGHT = 20;
    private static final int SMALL_BUTTON_WIDTH = 60;
    private static final int ACTION_BUTTON_WIDTH = 100;
    private static final int LINE_HEIGHT = 14;
    private static final int PADDING = 8;
    private static final int SECTION_GAP = 12;

    private Integer previousMenuBlur;
    private final @Nullable Screen parent;
    private long lastSeenCacheVersion = Long.MIN_VALUE;
    private final Set<UUID> sendingInviteTargetIds = new HashSet<>();

    public PartyManagementScreen() {
        this(null);
    }

    public PartyManagementScreen(@Nullable Screen parent) {
        super(Component.translatable("screen.incore.party.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (this.previousMenuBlur == null) {
            this.previousMenuBlur = this.minecraft.options.getMenuBackgroundBlurriness();
            if (this.previousMenuBlur > 0) {
                this.minecraft.options.menuBackgroundBlurriness().set(0);
            }
        }

        PacketDistributor.sendToServer(new PartyActionPayload(PartyActionPayload.ActionType.REQUEST_SYNC, null));
        
        rebuildButtons();
    }

    @Override
    public void tick() {
        super.tick();

        long cacheVersion = PartyClientCache.getVersion();
        if (cacheVersion != this.lastSeenCacheVersion) {
            this.sendingInviteTargetIds.clear();
            rebuildButtons();
        }
    }

    private void rebuildButtons() {
        this.clearWidgets();
        this.lastSeenCacheVersion = PartyClientCache.getVersion();

        Layout layout = layout();
        int y = layout.contentY();
        
        y = layoutPendingInviteButtons(y);
        y = layoutPartyInfoButtons(y);
        y = layoutOnlinePlayersButtons(y);
        layoutMainActionButtons();
    }

    private int layoutPendingInviteButtons(int startY) {
        if (!PartyClientCache.hasPendingInvite()) {
            return startY;
        }

        Layout layout = layout();
        int x = layout.contentX();
        int y = startY + LINE_HEIGHT + PADDING + LINE_HEIGHT * 2 + PADDING;
        int width = layout.contentWidth();

        int acceptX = x;
        int declineX = x + ACTION_BUTTON_WIDTH + PADDING;
        
        this.addRenderableWidget(Button.builder(
                Component.translatable("screen.incore.party.accept"),
                btn -> sendAction(PartyActionPayload.ActionType.ACCEPT, null)
        ).bounds(acceptX, y, ACTION_BUTTON_WIDTH, BUTTON_HEIGHT).build());
        
        this.addRenderableWidget(Button.builder(
                Component.translatable("screen.incore.party.decline"),
                btn -> sendAction(PartyActionPayload.ActionType.DECLINE, null)
        ).bounds(declineX, y, ACTION_BUTTON_WIDTH, BUTTON_HEIGHT).build());

        return y + BUTTON_HEIGHT + SECTION_GAP;
    }

    private int layoutPartyInfoButtons(int startY) {
        Layout layout = layout();
        int x = layout.contentX();
        int y = startY + LINE_HEIGHT + PADDING;
        int width = layout.contentWidth();

        if (!PartyClientCache.isInParty()) {
            return y + LINE_HEIGHT + SECTION_GAP;
        }

        y += LINE_HEIGHT * 2 + PADDING;

        UUID myId = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getUUID() : null;
        boolean isLeader = myId != null && PartyClientCache.isLeader(myId);
        
        List<PartyClientCache.MemberView> members = PartyClientCache.getMembers();
        for (PartyClientCache.MemberView member : members) {
            boolean isThisLeader = PartyClientCache.isLeader(member.playerId());
            boolean isSelf = member.playerId().equals(myId);
            
            if (isLeader && !isSelf) {
                int btnX = x + width - SMALL_BUTTON_WIDTH - PADDING;
                
                if (!isThisLeader) {
                    this.addRenderableWidget(Button.builder(
                            Component.translatable("screen.incore.party.promote"),
                            btn -> sendAction(PartyActionPayload.ActionType.PROMOTE, member.playerId())
                    ).bounds(btnX - SMALL_BUTTON_WIDTH - PADDING / 2, y - 2, SMALL_BUTTON_WIDTH, BUTTON_HEIGHT - 4).build());
                }
                
                this.addRenderableWidget(Button.builder(
                        Component.translatable("screen.incore.party.kick"),
                        btn -> sendAction(PartyActionPayload.ActionType.KICK, member.playerId())
                ).bounds(btnX, y - 2, SMALL_BUTTON_WIDTH, BUTTON_HEIGHT - 4).build());
            }
            
            y += LINE_HEIGHT + 4;
        }

        return y + SECTION_GAP;
    }

    private int layoutOnlinePlayersButtons(int startY) {
        if (!PartyClientCache.isInParty()) {
            return startY;
        }

        UUID myId = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getUUID() : null;
        boolean isLeader = myId != null && PartyClientCache.isLeader(myId);
        
        if (!isLeader) {
            return startY;
        }

        Layout layout = layout();
        int x = layout.contentX();
        int y = startY + LINE_HEIGHT + PADDING;
        int width = layout.contentWidth();

        List<PartyClientCache.PlayerView> onlinePlayers = PartyClientCache.getOnlinePlayers();
        
        if (onlinePlayers.isEmpty()) {
            return y + LINE_HEIGHT + SECTION_GAP;
        }

        int btnX = x + width - ACTION_BUTTON_WIDTH;
        for (PartyClientCache.PlayerView player : onlinePlayers) {
            boolean sendingInvite = this.sendingInviteTargetIds.contains(player.playerId());
            boolean outgoingInvite = PartyClientCache.hasOutgoingInvite(player.playerId());

            Button button = Button.builder(
                    inviteButtonLabel(sendingInvite, outgoingInvite),
                    btn -> sendInvite(player.playerId())
            ).bounds(btnX, y - 2, ACTION_BUTTON_WIDTH, BUTTON_HEIGHT - 4).build();
            button.active = !sendingInvite && !outgoingInvite;
            this.addRenderableWidget(button);
            
            y += LINE_HEIGHT + 4;
        }

        return y + SECTION_GAP;
    }

    private void layoutMainActionButtons() {
        Layout layout = layout();
        int y = layout.windowTop() + layout.windowHeight() - BUTTON_HEIGHT - PADDING;
        int centerX = layout.windowLeft() + layout.windowWidth() / 2;

        if (!PartyClientCache.isInParty()) {
            int btnX = centerX - ACTION_BUTTON_WIDTH / 2;
            this.addRenderableWidget(Button.builder(
                    Component.translatable("screen.incore.party.create"),
                    btn -> sendAction(PartyActionPayload.ActionType.CREATE, null)
            ).bounds(btnX, y, ACTION_BUTTON_WIDTH, BUTTON_HEIGHT).build());
        } else {
            int leaveX = centerX - ACTION_BUTTON_WIDTH / 2;
            this.addRenderableWidget(Button.builder(
                    Component.translatable("screen.incore.party.leave"),
                    btn -> sendAction(PartyActionPayload.ActionType.LEAVE, null)
            ).bounds(leaveX, y, ACTION_BUTTON_WIDTH, BUTTON_HEIGHT).build());
        }
    }

    @Override
    public void removed() {
        if (this.minecraft != null && this.previousMenuBlur != null) {
            this.minecraft.options.menuBackgroundBlurriness().set(this.previousMenuBlur);
        }
        this.previousMenuBlur = null;
        super.removed();
    }

    @Override
    public void onClose() {
        if (this.parent != null && this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
            return;
        }
        super.onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        Layout layout = layout();
        ThemedUi ui = themed(guiGraphics);

        ui.drawBackdrop(this.width, this.height);
        drawMainPanel(guiGraphics, layout.windowLeft(), layout.windowTop(), layout.windowWidth(), layout.windowHeight());

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawString(this.font, this.title, layout.windowLeft() + 12, layout.windowTop() + 8, UIScreenTheme.Info.TITLE_TEXT, false);
        
        int y = layout.contentY();
        y = renderPendingInviteSection(guiGraphics, y);
        y = renderPartyInfoSection(guiGraphics, y);
        renderOnlinePlayersSection(guiGraphics, y);
    }

    private int renderPendingInviteSection(GuiGraphics guiGraphics, int startY) {
        if (!PartyClientCache.hasPendingInvite()) {
            return startY;
        }

        Layout layout = layout();
        int x = layout.contentX();
        int y = startY;

        guiGraphics.drawString(this.font,
                Component.translatable("screen.incore.party.section_pending_invite"),
                x, y, UIScreenTheme.Info.PRIMARY_TEXT, false);
        y += LINE_HEIGHT + PADDING;

        String inviterName = PartyClientCache.getInviteInviterName();
        long partyId = PartyClientCache.getInvitePartyId();
        
        guiGraphics.drawString(this.font, 
                Component.translatable("screen.incore.party.invite_from", inviterName), 
                x, y, UIScreenTheme.Info.TITLE_TEXT, false);
        y += LINE_HEIGHT;
        
        guiGraphics.drawString(this.font,
                Component.translatable("screen.incore.party.party_id", partyId),
                x, y, UIScreenTheme.Info.MUTED_TEXT, false);
        y += LINE_HEIGHT + PADDING;

        return y + BUTTON_HEIGHT + SECTION_GAP;
    }

    private int renderPartyInfoSection(GuiGraphics guiGraphics, int startY) {
        Layout layout = layout();
        int x = layout.contentX();
        int y = startY;

        guiGraphics.drawString(this.font,
                Component.translatable("screen.incore.party.section_party"),
                x, y, UIScreenTheme.Info.PRIMARY_TEXT, false);
        y += LINE_HEIGHT + PADDING;

        if (!PartyClientCache.isInParty()) {
            guiGraphics.drawString(this.font,
                    Component.translatable("screen.incore.party.not_in_party"),
                    x, y, UIScreenTheme.Info.MUTED_TEXT, false);
            return y + LINE_HEIGHT + SECTION_GAP;
        }

        long partyId = PartyClientCache.getPartyId();
        String leaderName = PartyClientCache.getLeaderName();
        UUID myId = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getUUID() : null;
        
        guiGraphics.drawString(this.font,
                Component.translatable("screen.incore.party.party_id", partyId),
                x, y, UIScreenTheme.Info.TITLE_TEXT, false);
        y += LINE_HEIGHT;

        guiGraphics.drawString(this.font,
                Component.translatable("screen.incore.party.leader", leaderName),
                x, y, UIScreenTheme.Info.SECONDARY_TEXT, false);
        y += LINE_HEIGHT + PADDING;

        List<PartyClientCache.MemberView> members = PartyClientCache.getMembers();
        for (PartyClientCache.MemberView member : members) {
            boolean isThisLeader = PartyClientCache.isLeader(member.playerId());
            
            String displayName = member.playerName() + (isThisLeader ? " ★" : "");
            guiGraphics.drawString(this.font, Component.literal(displayName), x + 8, y, 
                    isThisLeader ? UIScreenTheme.Info.LEADER_CROWN_TEXT : UIScreenTheme.Info.WHITE_TEXT, false);
            y += LINE_HEIGHT + 4;
        }

        return y + SECTION_GAP;
    }

    private void renderOnlinePlayersSection(GuiGraphics guiGraphics, int startY) {
        if (!PartyClientCache.isInParty()) {
            return;
        }

        UUID myId = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getUUID() : null;
        boolean isLeader = myId != null && PartyClientCache.isLeader(myId);
        
        if (!isLeader) {
            return;
        }

        Layout layout = layout();
        int x = layout.contentX();
        int y = startY;

        guiGraphics.drawString(this.font,
                Component.translatable("screen.incore.party.section_invite"),
                x, y, UIScreenTheme.Info.PRIMARY_TEXT, false);
        y += LINE_HEIGHT + PADDING;

        List<PartyClientCache.PlayerView> onlinePlayers = PartyClientCache.getOnlinePlayers();
        
        if (onlinePlayers.isEmpty()) {
            guiGraphics.drawString(this.font,
                    Component.translatable("screen.incore.party.no_online_players"),
                    x, y, UIScreenTheme.Info.MUTED_TEXT, false);
            return;
        }

        for (PartyClientCache.PlayerView player : onlinePlayers) {
            guiGraphics.drawString(this.font, Component.literal(player.playerName()), x + 8, y, UIScreenTheme.Info.WHITE_TEXT, false);
            y += LINE_HEIGHT + 4;
        }
    }

    private void drawMainPanel(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        themed(guiGraphics).drawWindow(x, y, width, height);
    }

    private void sendAction(PartyActionPayload.ActionType actionType, UUID targetPlayerId) {
        PacketDistributor.sendToServer(new PartyActionPayload(actionType, targetPlayerId));
    }

    private void sendInvite(UUID targetPlayerId) {
        this.sendingInviteTargetIds.add(targetPlayerId);
        rebuildButtons();
        sendAction(PartyActionPayload.ActionType.INVITE, targetPlayerId);
    }

    private Component inviteButtonLabel(boolean sendingInvite, boolean outgoingInvite) {
        if (sendingInvite) {
            return Component.translatable("screen.incore.party.inviting");
        }
        if (outgoingInvite) {
            return Component.translatable("screen.incore.party.invited");
        }
        return Component.translatable("screen.incore.party.invite");
    }

    private Layout layout() {
        int windowWidth = this.windowWidth();
        int windowHeight = this.windowHeight();
        int windowLeft = this.windowLeft();
        int windowTop = this.windowTop();

        return new Layout(
                windowLeft,
                windowTop,
                windowWidth,
                windowHeight,
                windowLeft + PADDING,
                windowTop + 28,
                windowWidth - PADDING * 2
        );
    }

    private int windowLeft() {
        return (this.width - this.windowWidth()) / 2;
    }

    private int windowTop() {
        return (this.height - this.windowHeight()) / 2;
    }

    private int windowWidth() {
        return Math.min(TARGET_WINDOW_WIDTH, Math.max(280, this.width - 24));
    }

    private int windowHeight() {
        return Math.min(TARGET_WINDOW_HEIGHT, Math.max(300, this.height - 24));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record Layout(
            int windowLeft,
            int windowTop,
            int windowWidth,
            int windowHeight,
            int contentX,
            int contentY,
            int contentWidth
    ) {
    }

    private ThemedUi themed(GuiGraphics guiGraphics) {
        return new ThemedUi(guiGraphics, this.font, THEME.theme());
    }
}
