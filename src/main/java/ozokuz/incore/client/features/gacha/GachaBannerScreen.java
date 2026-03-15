package ozokuz.incore.client.features.gacha;

import ozokuz.incore.Registration;
import ozokuz.incore.client.ui.UIScreenTheme;
import ozokuz.incore.client.ui.render.ThemedUi;
import ozokuz.incore.features.gacha.GachaPermitItem;
import ozokuz.incore.features.gacha.GachaService;
import ozokuz.incore.features.gacha.network.GachaNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

public class GachaBannerScreen extends Screen {
    private static final UIScreenTheme THEME = UIScreenTheme.OTHER_CONTENT;
    private static final float COST_SCALE = 0.75F;
    private final GachaService.ScreenData data;
    private final @Nullable Screen parent;
    private final long openedAtMs;
    private int page;
    private String selectedBannerId;
    private int bannersPerPage;
    private int totalPages;

    public GachaBannerScreen(GachaService.ScreenData data) {
        this(data, null);
    }

    public GachaBannerScreen(GachaService.ScreenData data, @Nullable Screen parent) {
        super(Component.translatable("screen.incore.gacha_banners.title"));
        this.data = data;
        this.parent = parent;
        this.openedAtMs = System.currentTimeMillis();
        this.page = 0;
        this.selectedBannerId = data.banners().isEmpty() ? null : data.banners().getFirst().id();
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
    protected void init() {
        this.clearWidgets();

        List<GachaService.BannerView> banners = data.banners();
        if (banners.isEmpty()) {
            this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                    .bounds(12, this.height - 28, 80, 20)
                    .build());
            return;
        }

        if (selectedBannerId == null || banners.stream().noneMatch(banner -> banner.id().equals(selectedBannerId))) {
            selectedBannerId = banners.getFirst().id();
        }

        int sidebarLeft = 12;
        int sidebarTop = 36;
        int sidebarWidth = 178;
        int rowHeight = 30;
        int footerTop = this.height - 46;
        int listHeight = Math.max(24, footerTop - sidebarTop - 6);
        bannersPerPage = Math.max(1, listHeight / rowHeight);
        totalPages = Math.max(1, (banners.size() + bannersPerPage - 1) / bannersPerPage);
        page = Math.clamp(page, 0, totalPages - 1);

        int start = page * bannersPerPage;
        int end = Math.min(banners.size(), start + bannersPerPage);

        for (int i = start; i < end; i++) {
            GachaService.BannerView banner = banners.get(i);
            int y = sidebarTop + (i - start) * rowHeight;

            this.addRenderableWidget(Button.builder(Component.empty(), button -> {
                        selectedBannerId = banner.id();
                        init();
                    })
                    .bounds(sidebarLeft, y, sidebarWidth, rowHeight - 2)
                    .build());
        }

        int exitY = this.height - 28;
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(12, exitY, 80, 20)
                .build());

        int buyWidth = 78;
        int infoWidth = 64;
        int buyX = this.width - 12 - buyWidth;
        int infoX = buyX - 6 - infoWidth;
        GachaService.BannerView selectedBanner = getSelectedBanner();
        boolean basicGuaranteeBlocked = selectedBanner != null && selectedBanner.basicGuaranteeBlocked();

        this.addRenderableWidget(Button.builder(Component.translatable("screen.incore.gacha_banners.info"), button -> {
                    GachaService.BannerView selected = getSelectedBanner();
                    if (selected != null) {
                        this.minecraft.setScreen(new GachaBannerInfoScreen(this, selected));
                    }
                })
                .bounds(infoX, exitY, infoWidth, 20)
                .build());

        Button pullButton = Button.builder(Component.translatable("screen.incore.gacha_banners.pull_x10"), button -> {
                    GachaService.BannerView selected = getSelectedBanner();
                    if (selected == null) {
                        return;
                    }
                    ResourceLocation bannerId = ResourceLocation.tryParse(selected.id());
                    if (bannerId != null) {
                        GachaNetworking.sendBannerPurchase(bannerId);
                    }
                })
                .bounds(buyX, exitY, buyWidth, 20)
                .build();
        pullButton.active = !basicGuaranteeBlocked && (selectedBanner == null || !selectedBanner.locked());
        this.addRenderableWidget(pullButton);

        if (basicGuaranteeBlocked && selectedBanner != null && !selectedBanner.locked()) {
            int selectorWidth = 136;
            int selectorX = infoX - 6 - selectorWidth;
            this.addRenderableWidget(Button.builder(
                            Component.translatable("screen.incore.gacha_banners.open_guaranteed_six_selector"),
                            button -> this.minecraft.setScreen(new GachaGuaranteedSixSelectionScreen(this, selectedBanner))
                    )
                    .bounds(selectorX, exitY, selectorWidth, 20)
                    .build());
        }

        if (totalPages > 1) {
            this.addRenderableWidget(Button.builder(Component.literal("<"), button -> {
                        page = Math.max(0, page - 1);
                        init();
                    })
                    .bounds(96, exitY, 20, 20)
                    .build());
            this.addRenderableWidget(Button.builder(Component.literal(">"), button -> {
                        page = Math.min(totalPages - 1, page + 1);
                        init();
                    })
                    .bounds(120, exitY, 20, 20)
                    .build());
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        themed(guiGraphics).drawBackdrop(this.width, this.height);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 14, UIScreenTheme.OtherContent.GACHA_TITLE_TEXT);

        int sidebarLeft = 12;
        int sidebarTop = 36;
        int sidebarWidth = 178;
        int footerY = this.height - 44;
        int sidebarBottom = footerY - 4;

        guiGraphics.fill(sidebarLeft, sidebarTop, sidebarLeft + sidebarWidth, sidebarBottom, UIScreenTheme.OtherContent.CATALOG_COLUMN_FILL);
        guiGraphics.drawString(this.font, Component.translatable("screen.incore.gacha_banners.sidebar"), sidebarLeft + 6, sidebarTop - 10, UIScreenTheme.OtherContent.GACHA_SIDEBAR_LABEL_TEXT);
        if (totalPages > 1) {
            guiGraphics.drawString(this.font, Component.translatable("screen.incore.gacha_banners.page", page + 1, totalPages), sidebarLeft + 88, sidebarTop - 10, UIScreenTheme.OtherContent.GACHA_PAGE_TEXT);
        }

        List<GachaService.BannerView> banners = data.banners();
        int rowHeight = 30;
        int start = page * bannersPerPage;
        int end = Math.min(banners.size(), start + bannersPerPage);
        for (int i = start; i < end; i++) {
            GachaService.BannerView banner = banners.get(i);
            int y = sidebarTop + (i - start) * rowHeight;
            boolean selected = banner.id().equals(selectedBannerId);
            int border = selected ? brightenColor(banner.sidebarColor(), 0.22F) : banner.sidebarColor();
            int fill = selected ? UIScreenTheme.OtherContent.CATALOG_ROW_SELECTED_FILL : UIScreenTheme.OtherContent.CATALOG_ROW_FILL;

            guiGraphics.fill(sidebarLeft + 1, y + 1, sidebarLeft + sidebarWidth - 1, y + rowHeight - 3, fill);
            guiGraphics.fill(sidebarLeft, y, sidebarLeft + sidebarWidth, y + 1, UIScreenTheme.OtherContent.GACHA_ROW_BORDER_MASK | border);
            guiGraphics.fill(sidebarLeft, y + rowHeight - 3, sidebarLeft + sidebarWidth, y + rowHeight - 2, UIScreenTheme.OtherContent.GACHA_ROW_BORDER_MASK | border);
            guiGraphics.fill(sidebarLeft, y, sidebarLeft + 1, y + rowHeight - 2, UIScreenTheme.OtherContent.GACHA_ROW_BORDER_MASK | border);
            guiGraphics.fill(sidebarLeft + sidebarWidth - 1, y, sidebarLeft + sidebarWidth, y + rowHeight - 2, UIScreenTheme.OtherContent.GACHA_ROW_BORDER_MASK | border);

            Item mainItem = itemFromId(banner.mainItemId());
            if (mainItem != Items.AIR) {
                guiGraphics.renderItem(mainItem.getDefaultInstance(), sidebarLeft + 4, y + 3);
            }

            int textX = sidebarLeft + 24;
            int textColor = selected ? UIScreenTheme.OtherContent.GACHA_TEXT_SELECTED : UIScreenTheme.OtherContent.CATALOG_TEXT_PRIMARY;
            guiGraphics.drawString(this.font, Component.literal(banner.name()), textX, y + 3, textColor, false);
            String remainingLabel = renderRemainingLabel(banner);
            if (!remainingLabel.isEmpty()) {
                guiGraphics.drawString(this.font, Component.literal(remainingLabel), textX, y + 15, UIScreenTheme.OtherContent.GACHA_TEXT_SECONDARY, false);
            } else if (banner.locked()) {
                guiGraphics.drawString(
                        this.font,
                        Component.translatable("screen.incore.gacha_banners.locked", banner.requiredLevel()),
                        textX,
                        y + 15,
                        UIScreenTheme.OtherContent.GACHA_ERROR_TEXT,
                        false
                );
            }
        }

        int mainLeft = sidebarLeft + sidebarWidth + 8;
        int mainTop = sidebarTop;
        int mainRight = this.width - 12;
        int mainBottom = sidebarBottom;
        guiGraphics.fill(mainLeft, mainTop, mainRight, mainBottom, UIScreenTheme.OtherContent.CATALOG_DETAILS_FILL);

        GachaService.BannerView selected = getSelectedBanner();
        if (selected == null) {
            guiGraphics.drawCenteredString(this.font, Component.translatable("incore.gacha.banner.none_configured"), (mainLeft + mainRight) / 2, mainTop + 40, UIScreenTheme.OtherContent.GACHA_ERROR_TEXT);
            return;
        }

        guiGraphics.drawString(this.font, Component.literal(selected.name()), mainLeft + 10, mainTop + 8, UIScreenTheme.OtherContent.GACHA_TEXT_PRIMARY);
        if (selected.locked()) {
            guiGraphics.drawString(
                    this.font,
                    Component.translatable("screen.incore.gacha_banners.locked", selected.requiredLevel()),
                    mainLeft + 10,
                    mainTop + 20,
                    UIScreenTheme.OtherContent.GACHA_ERROR_TEXT
            );
        }
        guiGraphics.drawString(
                this.font,
                Component.translatable("screen.incore.gacha_banners.type." + selected.type()),
                mainLeft + 10,
                mainTop + (selected.locked() ? 32 : 20),
                "basic".equals(selected.type()) ? UIScreenTheme.OtherContent.GACHA_BANNER_TYPE_BASIC_TEXT : UIScreenTheme.OtherContent.GACHA_BANNER_TYPE_LIMITED_TEXT
        );
        int infoY = mainTop + (selected.locked() ? 44 : 32);
        guiGraphics.drawString(
                this.font,
                Component.translatable("screen.incore.gacha_banners.pity", selected.pityFive(), 40, selected.pitySix(), 80),
                mainLeft + 10,
                infoY,
                UIScreenTheme.OtherContent.GACHA_SIDEBAR_LABEL_TEXT
        );
        infoY += 12;
        if ("event".equals(selected.type())) {
            Component featuredLine = selected.eventFeaturedPityEnabled()
                    ? Component.translatable(
                            "screen.incore.gacha_banners.event_featured_pity",
                            selected.eventFeaturedPity(),
                            GachaService.EVENT_FEATURED_SIX_PITY_THRESHOLD
                    )
                    : Component.translatable("screen.incore.gacha_banners.event_featured_pity.unavailable");
            guiGraphics.drawString(this.font, featuredLine, mainLeft + 10, infoY, UIScreenTheme.OtherContent.GACHA_FEATURED_TEXT);
            infoY += 12;
        } else {
            guiGraphics.drawString(
                    this.font,
                    Component.translatable(
                            "screen.incore.gacha_banners.basic_guaranteed_pity",
                            selected.basicSelectedSixPity(),
                            GachaService.BASIC_SELECTED_SIX_THRESHOLD
                    ),
                    mainLeft + 10,
                    infoY,
                    UIScreenTheme.OtherContent.GACHA_DROP_RATE_TEXT
            );
            infoY += 12;
            if (selected.basicGuaranteeBlocked()) {
                guiGraphics.drawString(
                        this.font,
                        Component.translatable("screen.incore.gacha_banners.basic_guaranteed_locked"),
                        mainLeft + 10,
                        infoY,
                        UIScreenTheme.OtherContent.GACHA_PITY_VALUE_TEXT
                );
                infoY += 12;
            }
        }

        if (!selected.basicGuaranteeBlocked()) {
            guiGraphics.drawString(
                    this.font,
                    Component.translatable("screen.incore.gacha_banners.cost", GachaService.PULLS_PER_CRATE),
                    mainLeft + 10,
                    infoY,
                    UIScreenTheme.OtherContent.GACHA_TEXT_MUTED
            );
            infoY += 12;
        }

        String selectedRemaining = renderRemainingLabel(selected);
        if (!selectedRemaining.isEmpty()) {
            guiGraphics.drawString(
                    this.font,
                    Component.translatable("screen.incore.gacha_banners.time_left", selectedRemaining),
                    mainLeft + 10,
                    infoY,
                    UIScreenTheme.OtherContent.GACHA_SIDEBAR_LABEL_TEXT
            );
            infoY += 12;
        }
        int showcaseTitleY = Math.max(mainTop + 74, infoY + 6);
        guiGraphics.drawCenteredString(
                this.font,
                Component.translatable("screen.incore.gacha_banners.high_rarity_showcase"),
                (mainLeft + mainRight) / 2,
                showcaseTitleY,
                UIScreenTheme.OtherContent.GACHA_PITY_LABEL_TEXT
        );

        if (!selected.basicGuaranteeBlocked()) {
            renderPullCostPanel(guiGraphics, selected);
        }
        ItemStack hoveredHighlight = renderHighRarityShowcase(
                guiGraphics,
                selected,
                mainLeft,
                showcaseTitleY + 12,
                mainRight,
                mainBottom,
                mouseX,
                mouseY
        );
        if (hoveredHighlight != null && !hoveredHighlight.isEmpty()) {
            guiGraphics.renderTooltip(this.font, hoveredHighlight, mouseX, mouseY);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private @Nullable GachaService.BannerView getSelectedBanner() {
        if (selectedBannerId == null) {
            return null;
        }
        return data.banners().stream().filter(banner -> selectedBannerId.equals(banner.id())).findFirst().orElse(null);
    }

    private static int brightenColor(int color, float amount) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        r = Math.min(255, (int) (r + (255 - r) * amount));
        g = Math.min(255, (int) (g + (255 - g) * amount));
        b = Math.min(255, (int) (b + (255 - b) * amount));
        return (r << 16) | (g << 8) | b;
    }

    private static Item itemFromId(String itemIdString) {
        if (itemIdString == null || itemIdString.isBlank()) {
            return Items.AIR;
        }
        ResourceLocation itemId = ResourceLocation.tryParse(itemIdString);
        if (itemId == null) {
            return Items.AIR;
        }
        return BuiltInRegistries.ITEM.get(itemId);
    }

    private String renderRemainingLabel(GachaService.BannerView banner) {
        if (!"event".equals(banner.type())) {
            return Component.translatable("screen.incore.gacha_banners.rotation.static").getString();
        }

        if (banner.remainingMillis() < 0L) {
            return "";
        }

        long elapsed = Math.max(0L, System.currentTimeMillis() - openedAtMs);
        long remaining = Math.max(0L, banner.remainingMillis() - elapsed);
        return formatDuration(remaining);
    }

    private static String formatDuration(long millis) {
        long totalSeconds = Math.max(0L, millis / 1000L);
        long days = totalSeconds / 86_400L;
        long hours = (totalSeconds % 86_400L) / 3_600L;
        long minutes = (totalSeconds % 3_600L) / 60L;
        long seconds = totalSeconds % 60L;

        if (days > 0L) {
            return String.format(Locale.ROOT, "%dd %02d:%02d:%02d", days, hours, minutes, seconds);
        }
        return String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds);
    }

    private void renderPullCostPanel(GuiGraphics guiGraphics, GachaService.BannerView selected) {
        int exitY = this.height - 28;
        int buyWidth = 78;
        int buyX = this.width - 12 - buyWidth;

        PermitUsage usage = calculatePermitUsage(selected);
        if (usage.lines().isEmpty()) {
            return;
        }

        int maxScaledLineWidth = 0;
        int scaledLineHeight = (int) Math.ceil(16 * COST_SCALE);
        for (CostRenderLine line : usage.lines()) {
            maxScaledLineWidth = Math.max(maxScaledLineWidth, scaledCostLineWidth(line.count()));
        }

        int totalHeight = usage.lines().size() * scaledLineHeight + Math.max(0, usage.lines().size() - 1);
        int rowY = exitY - 2 - totalHeight;
        int panelLeft = buyX;
        int panelRight = buyX + buyWidth;
        int panelTop = rowY - 2;
        int panelBottom = exitY;

        guiGraphics.fill(panelLeft, panelTop, panelRight, panelBottom, UIScreenTheme.OtherContent.GACHA_BALANCE_PANEL_FILL);

        for (CostRenderLine line : usage.lines()) {
            int lineX = panelLeft + (buyWidth - scaledCostLineWidth(line.count())) / 2;
            renderCostLine(guiGraphics, lineX, rowY, line.item(), line.count(), line.color());
            rowY += scaledLineHeight + 1;
        }
    }

    private PermitUsage calculatePermitUsage(GachaService.BannerView selected) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return new PermitUsage(List.of(
                    new CostRenderLine(Registration.BASIC_TIME_PIECE_ITEM.get(), GachaService.PULLS_PER_CRATE, UIScreenTheme.OtherContent.GACHA_COST_MISSING_TEXT)
            ));
        }

        int required = GachaService.PULLS_PER_CRATE;
        boolean isBasic = "basic".equals(selected.type());
        int basicCount = 0;
        int specificCount = 0;
        int charteredCount = 0;
        ResourceLocation bannerId = ResourceLocation.tryParse(selected.id());

        for (int i = 0; i < minecraft.player.getInventory().getContainerSize(); i++) {
            ItemStack stack = minecraft.player.getInventory().getItem(i);
            if (stack.isEmpty()) {
                continue;
            }

            if (stack.getItem() == Registration.BASIC_TIME_PIECE_ITEM.get()) {
                basicCount += stack.getCount();
                continue;
            }

            if (stack.getItem() == Registration.CHARTERED_TIME_PIECE_ITEM.get()) {
                charteredCount += stack.getCount();
                continue;
            }

            if (stack.getItem() == Registration.TIME_PIECE_ITEM.get() && bannerId != null && GachaPermitItem.matchesBanner(stack, bannerId)) {
                specificCount += stack.getCount();
            }
        }

        if (isBasic) {
            int useSpecific = Math.min(required, specificCount);
            int remaining = required - useSpecific;
            int useBasic = Math.min(remaining, basicCount);
            int missing = required - useSpecific - useBasic;
            List<CostRenderLine> lines = new ArrayList<>();
            if (useSpecific > 0) {
                lines.add(new CostRenderLine(Registration.TIME_PIECE_ITEM.get(), useSpecific, UIScreenTheme.OtherContent.GACHA_COST_OK_TEXT));
            }
            if (useBasic > 0) {
                lines.add(new CostRenderLine(Registration.BASIC_TIME_PIECE_ITEM.get(), useBasic, UIScreenTheme.OtherContent.GACHA_COST_OK_TEXT));
            }
            if (missing > 0) {
                lines.add(new CostRenderLine(Registration.BASIC_TIME_PIECE_ITEM.get(), missing, UIScreenTheme.OtherContent.GACHA_COST_MISSING_TEXT));
            }
            return new PermitUsage(lines);
        }

        int useSpecific = Math.min(required, specificCount);
        int remaining = required - useSpecific;
        int useChartered = Math.min(remaining, charteredCount);
        int missing = required - useSpecific - useChartered;
        List<CostRenderLine> lines = new ArrayList<>();
        if (useSpecific > 0) {
            lines.add(new CostRenderLine(Registration.TIME_PIECE_ITEM.get(), useSpecific, UIScreenTheme.OtherContent.GACHA_COST_OK_TEXT));
        }
        if (useChartered > 0) {
            lines.add(new CostRenderLine(Registration.CHARTERED_TIME_PIECE_ITEM.get(), useChartered, UIScreenTheme.OtherContent.GACHA_COST_OK_TEXT));
        }
        if (missing > 0) {
            lines.add(new CostRenderLine(Registration.CHARTERED_TIME_PIECE_ITEM.get(), missing, UIScreenTheme.OtherContent.GACHA_COST_MISSING_TEXT));
        }
        return new PermitUsage(lines);
    }

    private void renderCostLine(GuiGraphics guiGraphics, int x, int y, Item item, int count, int color) {
        if (count <= 0) {
            return;
        }
        themed(guiGraphics).drawScaledItemLine(item.getDefaultInstance(), "x" + count, x, y, COST_SCALE, color);
    }

    private int scaledCostLineWidth(int count) {
        int unscaledWidth = 20 + this.font.width("x" + count);
        return (int) Math.ceil(unscaledWidth * COST_SCALE);
    }

    private record PermitUsage(List<CostRenderLine> lines) {
    }

    private record CostRenderLine(Item item, int count, int color) {
    }

    private @Nullable ItemStack renderHighRarityShowcase(
            GuiGraphics guiGraphics,
            GachaService.BannerView banner,
            int mainLeft,
            int showcaseTop,
            int mainRight,
            int mainBottom,
            int mouseX,
            int mouseY
    ) {
        List<Item> sixStars = collectUniqueRewardsByRarity(banner, 6);
        List<Item> fiveStars = collectUniqueRewardsByRarity(banner, 5);

        if (sixStars.isEmpty() && fiveStars.isEmpty()) {
            guiGraphics.drawCenteredString(
                    this.font,
                    Component.translatable("screen.incore.gacha_banners.high_rarity_none"),
                    (mainLeft + mainRight) / 2,
                    showcaseTop + 24,
                    UIScreenTheme.OtherContent.GACHA_SHOWCASE_CHANCE_TEXT
            );
            return null;
        }

        int showcaseBottom = mainBottom - 8;
        int centerX = (mainLeft + mainRight) / 2;
        int maxWidth = Math.max(80, (mainRight - mainLeft) - 28);
        int y = showcaseTop;

        ItemStack hovered = null;
        if (!sixStars.isEmpty()) {
            guiGraphics.drawCenteredString(this.font, Component.translatable("screen.incore.gacha_banners.showcase.six"), centerX, y, UIScreenTheme.OtherContent.GACHA_SHOWCASE_SIX_TEXT);
            y += 10;
            GridRenderResult sixRender = renderItemGrid(guiGraphics, sixStars, centerX, y, maxWidth, 1.55F, 5, 3, mouseX, mouseY);
            y = sixRender.nextY();
            hovered = sixRender.hoveredStack();
        }

        if (!fiveStars.isEmpty()) {
            if (y + 6 < showcaseBottom) {
                y += 6;
            }
            guiGraphics.drawCenteredString(this.font, Component.translatable("screen.incore.gacha_banners.showcase.five"), centerX, y, UIScreenTheme.OtherContent.GACHA_SHOWCASE_FIVE_TEXT);
            y += 10;
            GridRenderResult fiveRender = renderItemGrid(guiGraphics, fiveStars, centerX, y, maxWidth, 1.15F, 6, 2, mouseX, mouseY);
            if (hovered == null) {
                hovered = fiveRender.hoveredStack();
            }
        }
        return hovered;
    }

    private List<Item> collectUniqueRewardsByRarity(GachaService.BannerView banner, int rarity) {
        LinkedHashSet<Item> items = new LinkedHashSet<>();
        for (GachaService.RewardView reward : banner.rewards()) {
            if (reward.rarity() != rarity) {
                continue;
            }

            ResourceLocation itemId = ResourceLocation.tryParse(reward.itemId());
            if (itemId == null) {
                continue;
            }
            Item item = BuiltInRegistries.ITEM.get(itemId);
            if (item != Items.AIR) {
                items.add(item);
            }
        }
        return new ArrayList<>(items);
    }

    private GridRenderResult renderItemGrid(
            GuiGraphics guiGraphics,
            List<Item> items,
            int centerX,
            int topY,
            int maxWidth,
            float scale,
            int preferredMaxColumns,
            int gap,
            int mouseX,
            int mouseY
    ) {
        if (items.isEmpty()) {
            return new GridRenderResult(topY, null);
        }

        int iconSize = Math.max(1, Math.round(16.0F * scale));
        int cell = iconSize + gap;
        int maxColumns = Math.max(1, Math.min(preferredMaxColumns, maxWidth / Math.max(1, cell)));
        int columns = Math.max(1, Math.min(items.size(), maxColumns));
        int rows = (items.size() + columns - 1) / columns;
        int rowWidth = columns * cell - gap;
        int startX = centerX - rowWidth / 2;
        ItemStack hovered = null;

        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            ItemStack displayStack = item.getDefaultInstance();
            int row = i / columns;
            int col = i % columns;
            int x = startX + col * cell;
            int y = topY + row * cell;
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(x, y, 0.0F);
            guiGraphics.pose().scale(scale, scale, 1.0F);
            guiGraphics.renderItem(displayStack, 0, 0);
            guiGraphics.pose().popPose();

            if (hovered == null
                    && mouseX >= x
                    && mouseX < x + iconSize
                    && mouseY >= y
                    && mouseY < y + iconSize) {
                hovered = displayStack;
            }
        }

        return new GridRenderResult(topY + rows * cell, hovered);
    }

    private record GridRenderResult(int nextY, @Nullable ItemStack hoveredStack) {
    }

    private ThemedUi themed(GuiGraphics guiGraphics) {
        return new ThemedUi(guiGraphics, this.font, THEME.theme());
    }
}
