package io.github.ozokuz.incore.features.gacha.client;

import io.github.ozokuz.incore.features.gacha.GachaService;
import io.github.ozokuz.incore.features.gacha.network.GachaNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.List;

public class GachaBannerScreen extends Screen {
    private static final int BANNERS_PER_PAGE = 4;

    private final GachaService.ScreenData data;
    private int page;

    public GachaBannerScreen(GachaService.ScreenData data) {
        super(Component.translatable("screen.incore.gacha_banners.title"));
        this.data = data;
        this.page = 0;
    }

    @Override
    protected void init() {
        this.clearWidgets();

        List<GachaService.BannerView> banners = data.banners();
        int totalPages = Math.max(1, (banners.size() + BANNERS_PER_PAGE - 1) / BANNERS_PER_PAGE);
        page = Math.clamp(page, 0, totalPages - 1);

        int start = page * BANNERS_PER_PAGE;
        int end = Math.min(banners.size(), start + BANNERS_PER_PAGE);
        int rowHeight = 52;
        int baseY = 50;
        int cardLeft = this.width / 2 - 150;
        int selectX = cardLeft + 212;
        int infoX = cardLeft + 264;

        for (int i = start; i < end; i++) {
            GachaService.BannerView banner = banners.get(i);
            int row = i - start;
            int y = baseY + row * rowHeight + 16;

            this.addRenderableWidget(Button.builder(Component.translatable("screen.incore.gacha_banners.select"), button -> {
                        ResourceLocation bannerId = ResourceLocation.tryParse(banner.id());
                        if (bannerId != null) {
                            GachaNetworking.sendBannerSelection(bannerId);
                        }
                    })
                    .bounds(selectX, y, 48, 20)
                    .build());

            this.addRenderableWidget(Button.builder(Component.translatable("screen.incore.gacha_banners.info"), button -> this.minecraft.setScreen(new GachaBannerInfoScreen(this, banner)))
                    .bounds(infoX, y, 36, 20)
                    .build());
        }

        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(this.width / 2 - 40, this.height - 28, 80, 20)
                .build());

        if (totalPages > 1) {
            this.addRenderableWidget(Button.builder(Component.literal("<"), button -> {
                        page = Math.max(0, page - 1);
                        init();
                    })
                    .bounds(this.width / 2 - 120, this.height - 28, 20, 20)
                    .build());
            this.addRenderableWidget(Button.builder(Component.literal(">"), button -> {
                        page = Math.min(totalPages - 1, page + 1);
                        init();
                    })
                    .bounds(this.width / 2 + 100, this.height - 28, 20, 20)
                    .build());
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 14, 0xF6F6F6);

        List<GachaService.BannerView> banners = data.banners();
        int totalPages = Math.max(1, (banners.size() + BANNERS_PER_PAGE - 1) / BANNERS_PER_PAGE);
        guiGraphics.drawCenteredString(
                this.font,
                Component.translatable("screen.incore.gacha_banners.page", page + 1, totalPages),
                this.width / 2,
                28,
                0xB8B8B8
        );
        guiGraphics.drawCenteredString(
                this.font,
                Component.translatable("screen.incore.gacha_banners.cost", GachaService.PULLS_PER_CRATE),
                this.width / 2,
                38,
                0xD9D9D9
        );

        int start = page * BANNERS_PER_PAGE;
        int end = Math.min(banners.size(), start + BANNERS_PER_PAGE);
        int rowHeight = 52;
        int baseY = 50;
        int cardLeft = this.width / 2 - 150;
        int cardRight = this.width / 2 + 150;

        for (int i = start; i < end; i++) {
            GachaService.BannerView banner = banners.get(i);
            int row = i - start;
            int y = baseY + row * rowHeight;

            int bgColor = 0xAA1C1C1C;
            guiGraphics.fill(cardLeft, y, cardRight, y + 46, bgColor);

            Component title = Component.literal(banner.name());
            guiGraphics.drawString(this.font, title, cardLeft + 6, y + 6, 0xF0F0F0);
            guiGraphics.drawString(
                    this.font,
                    Component.translatable("screen.incore.gacha_banners.pity", banner.pityFive(), 40, banner.pitySix(), 80),
                    cardLeft + 6,
                    y + 18,
                    0xCFCFCF
            );
            guiGraphics.drawString(
                    this.font,
                    Component.translatable("screen.incore.gacha_banners.type." + banner.type()),
                    cardLeft + 6,
                    y + 30,
                    "basic".equals(banner.type()) ? 0x9AE6FF : 0xFFD98A
            );

            int itemX = cardLeft + 94;
            int itemY = y + 24;
            int featuredCount = Math.min(3, banner.featuredItems().size());
            for (int featuredIndex = 0; featuredIndex < featuredCount; featuredIndex++) {
                ResourceLocation itemId = ResourceLocation.tryParse(banner.featuredItems().get(featuredIndex));
                if (itemId == null) {
                    continue;
                }
                Item item = BuiltInRegistries.ITEM.get(itemId);
                if (item == Items.AIR) {
                    continue;
                }
                guiGraphics.renderItem(item.getDefaultInstance(), itemX + featuredIndex * 18, itemY);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
