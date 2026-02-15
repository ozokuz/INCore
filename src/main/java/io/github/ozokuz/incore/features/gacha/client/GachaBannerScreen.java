package io.github.ozokuz.incore.features.gacha.client;

import io.github.ozokuz.incore.Registration;
import io.github.ozokuz.incore.features.gacha.GachaPermitItem;
import io.github.ozokuz.incore.features.gacha.GachaService;
import io.github.ozokuz.incore.features.gacha.network.GachaNetworking;
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
    private static final float COST_SCALE = 0.75F;
    private final GachaService.ScreenData data;
    private final long openedAtMs;
    private int page;
    private String selectedBannerId;
    private int bannersPerPage;
    private int totalPages;

    public GachaBannerScreen(GachaService.ScreenData data) {
        super(Component.translatable("screen.incore.gacha_banners.title"));
        this.data = data;
        this.openedAtMs = System.currentTimeMillis();
        this.page = 0;
        this.selectedBannerId = data.banners().isEmpty() ? null : data.banners().getFirst().id();
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

        this.addRenderableWidget(Button.builder(Component.translatable("screen.incore.gacha_banners.info"), button -> {
                    GachaService.BannerView selected = getSelectedBanner();
                    if (selected != null) {
                        this.minecraft.setScreen(new GachaBannerInfoScreen(this, selected));
                    }
                })
                .bounds(infoX, exitY, infoWidth, 20)
                .build());

        this.addRenderableWidget(Button.builder(Component.translatable("screen.incore.gacha_banners.pull_x10"), button -> {
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
                .build());

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
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 14, 0xF6F6F6);

        int sidebarLeft = 12;
        int sidebarTop = 36;
        int sidebarWidth = 178;
        int footerY = this.height - 44;
        int sidebarBottom = footerY - 4;

        guiGraphics.fill(sidebarLeft, sidebarTop, sidebarLeft + sidebarWidth, sidebarBottom, 0x991A1A1A);
        guiGraphics.drawString(this.font, Component.translatable("screen.incore.gacha_banners.sidebar"), sidebarLeft + 6, sidebarTop - 10, 0xD8D8D8);
        if (totalPages > 1) {
            guiGraphics.drawString(this.font, Component.translatable("screen.incore.gacha_banners.page", page + 1, totalPages), sidebarLeft + 88, sidebarTop - 10, 0xB6B6B6);
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
            int fill = selected ? 0xBB2A2A2A : 0x99232323;

            guiGraphics.fill(sidebarLeft + 1, y + 1, sidebarLeft + sidebarWidth - 1, y + rowHeight - 3, fill);
            guiGraphics.fill(sidebarLeft, y, sidebarLeft + sidebarWidth, y + 1, 0xFF000000 | border);
            guiGraphics.fill(sidebarLeft, y + rowHeight - 3, sidebarLeft + sidebarWidth, y + rowHeight - 2, 0xFF000000 | border);
            guiGraphics.fill(sidebarLeft, y, sidebarLeft + 1, y + rowHeight - 2, 0xFF000000 | border);
            guiGraphics.fill(sidebarLeft + sidebarWidth - 1, y, sidebarLeft + sidebarWidth, y + rowHeight - 2, 0xFF000000 | border);

            Item mainItem = itemFromId(banner.mainItemId());
            if (mainItem != Items.AIR) {
                guiGraphics.renderItem(mainItem.getDefaultInstance(), sidebarLeft + 4, y + 3);
            }

            int textX = sidebarLeft + 24;
            int textColor = selected ? 0xFFFFFF : 0xE8E8E8;
            guiGraphics.drawString(this.font, Component.literal(banner.name()), textX, y + 3, textColor, false);
            String remainingLabel = renderRemainingLabel(banner);
            if (!remainingLabel.isEmpty()) {
                guiGraphics.drawString(this.font, Component.literal(remainingLabel), textX, y + 15, 0xBFBFBF, false);
            }
        }

        int mainLeft = sidebarLeft + sidebarWidth + 8;
        int mainTop = sidebarTop;
        int mainRight = this.width - 12;
        int mainBottom = sidebarBottom;
        guiGraphics.fill(mainLeft, mainTop, mainRight, mainBottom, 0x99202020);

        GachaService.BannerView selected = getSelectedBanner();
        if (selected == null) {
            guiGraphics.drawCenteredString(this.font, Component.translatable("incore.gacha.banner.none_configured"), (mainLeft + mainRight) / 2, mainTop + 40, 0xE66F6F);
            return;
        }

        guiGraphics.drawString(this.font, Component.literal(selected.name()), mainLeft + 10, mainTop + 8, 0xF2F2F2);
        guiGraphics.drawString(
                this.font,
                Component.translatable("screen.incore.gacha_banners.type." + selected.type()),
                mainLeft + 10,
                mainTop + 20,
                "basic".equals(selected.type()) ? 0x9AE6FF : 0xFFD98A
        );
        guiGraphics.drawString(
                this.font,
                Component.translatable("screen.incore.gacha_banners.pity", selected.pityFive(), 40, selected.pitySix(), 80),
                mainLeft + 10,
                mainTop + 32,
                0xD8D8D8
        );
        guiGraphics.drawString(
                this.font,
                Component.translatable("screen.incore.gacha_banners.cost", GachaService.PULLS_PER_CRATE),
                mainLeft + 10,
                mainTop + 44,
                0xCECECE
        );
        String selectedRemaining = renderRemainingLabel(selected);
        if (!selectedRemaining.isEmpty()) {
            guiGraphics.drawString(
                    this.font,
                    Component.translatable("screen.incore.gacha_banners.time_left", selectedRemaining),
                    mainLeft + 10,
                    mainTop + 56,
                    0xD8D8D8
            );
        }
        guiGraphics.drawCenteredString(
                this.font,
                Component.translatable("screen.incore.gacha_banners.high_rarity_showcase"),
                (mainLeft + mainRight) / 2,
                mainTop + 74,
                0xF3D26A
        );

        renderPullCostPanel(guiGraphics, selected);
        ItemStack hoveredHighlight = renderHighRarityShowcase(guiGraphics, selected, mainLeft, mainTop, mainRight, mainBottom, mouseX, mouseY);
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

        guiGraphics.fill(panelLeft, panelTop, panelRight, panelBottom, 0xAA141414);

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
                    new CostRenderLine(Registration.BASIC_BANNER_PERMIT_ITEM.get(), GachaService.PULLS_PER_CRATE, 0xFF5555)
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

            if (stack.getItem() == Registration.BASIC_BANNER_PERMIT_ITEM.get()) {
                basicCount += stack.getCount();
                continue;
            }

            if (stack.getItem() == Registration.CHARTERED_BANNER_PERMIT_ITEM.get()) {
                charteredCount += stack.getCount();
                continue;
            }

            if (stack.getItem() == Registration.BANNER_PERMIT_ITEM.get() && bannerId != null && GachaPermitItem.matchesBanner(stack, bannerId)) {
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
                lines.add(new CostRenderLine(Registration.BANNER_PERMIT_ITEM.get(), useSpecific, 0xBDE8BD));
            }
            if (useBasic > 0) {
                lines.add(new CostRenderLine(Registration.BASIC_BANNER_PERMIT_ITEM.get(), useBasic, 0xBDE8BD));
            }
            if (missing > 0) {
                lines.add(new CostRenderLine(Registration.BASIC_BANNER_PERMIT_ITEM.get(), missing, 0xFF5555));
            }
            return new PermitUsage(lines);
        }

        int useSpecific = Math.min(required, specificCount);
        int remaining = required - useSpecific;
        int useChartered = Math.min(remaining, charteredCount);
        int missing = required - useSpecific - useChartered;
        List<CostRenderLine> lines = new ArrayList<>();
        if (useSpecific > 0) {
            lines.add(new CostRenderLine(Registration.BANNER_PERMIT_ITEM.get(), useSpecific, 0xBDE8BD));
        }
        if (useChartered > 0) {
            lines.add(new CostRenderLine(Registration.CHARTERED_BANNER_PERMIT_ITEM.get(), useChartered, 0xBDE8BD));
        }
        if (missing > 0) {
            lines.add(new CostRenderLine(Registration.CHARTERED_BANNER_PERMIT_ITEM.get(), missing, 0xFF5555));
        }
        return new PermitUsage(lines);
    }

    private void renderCostLine(GuiGraphics guiGraphics, int x, int y, Item item, int count, int color) {
        if (count <= 0) {
            return;
        }

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0.0F);
        guiGraphics.pose().scale(COST_SCALE, COST_SCALE, 1.0F);
        guiGraphics.renderItem(item.getDefaultInstance(), 0, 0);
        guiGraphics.drawString(this.font, Component.literal("x" + count), 20, 4, color, false);
        guiGraphics.pose().popPose();
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
            int mainTop,
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
                    mainTop + 110,
                    0xD88B8B
            );
            return null;
        }

        int showcaseTop = mainTop + 86;
        int showcaseBottom = mainBottom - 8;
        int centerX = (mainLeft + mainRight) / 2;
        int maxWidth = Math.max(80, (mainRight - mainLeft) - 28);
        int y = showcaseTop;

        ItemStack hovered = null;
        if (!sixStars.isEmpty()) {
            guiGraphics.drawCenteredString(this.font, Component.translatable("screen.incore.gacha_banners.showcase.six"), centerX, y, 0xFF8C8C);
            y += 10;
            GridRenderResult sixRender = renderItemGrid(guiGraphics, sixStars, centerX, y, maxWidth, 1.55F, 5, 3, mouseX, mouseY);
            y = sixRender.nextY();
            hovered = sixRender.hoveredStack();
        }

        if (!fiveStars.isEmpty()) {
            if (y + 6 < showcaseBottom) {
                y += 6;
            }
            guiGraphics.drawCenteredString(this.font, Component.translatable("screen.incore.gacha_banners.showcase.five"), centerX, y, 0xE5CA7A);
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
}
