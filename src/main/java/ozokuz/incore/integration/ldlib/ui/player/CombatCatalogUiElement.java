package ozokuz.incore.integration.ldlib.ui.player;

import com.google.gson.Gson;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.IBindable;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.texture.RectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import ozokuz.incore.client.ui.UIScreenTheme;
import ozokuz.incore.features.arena.ArenaService;
import ozokuz.incore.features.arena.network.ArenaNetworking;
import ozokuz.incore.integration.ldlib.ui.INCoreLdLibUiScaffold;
import ozokuz.incore.integration.ldlib.ui.RequestBackIncoreUiPayload;
import ozokuz.incore.integration.ldlib.ui.texture.BeveledRectTexture;

final class CombatCatalogUiElement extends UIElement implements IBindable<String> {
    private static final Gson GSON = new Gson();
    private static final int PANEL_GAP = 16;
    private static final IGuiTexture SCROLL_TRACK_TEXTURE = new BeveledRectTexture(
            0x66101010,
            0xAA0C0C0C,
            0x66101010,
            0x66101010,
            1,
            0
    );
    private static final IGuiTexture SCROLL_THUMB_IDLE_TEXTURE = new BeveledRectTexture(
            0xFF444444,
            0xFF2A2A2A,
            0xFF444444,
            0xFF444444,
            1,
            0
    );
    private static final IGuiTexture SCROLL_THUMB_HOVER_TEXTURE = new BeveledRectTexture(
            0xFF5A6575,
            0xFF8BCFFF,
            0xFF5A6575,
            0xFF5A6575,
            1,
            0
    );
    private static final IGuiTexture SCROLL_THUMB_PRESSED_TEXTURE = new BeveledRectTexture(
            0xFF697587,
            0xFFAFE1FF,
            0xFF697587,
            0xFF697587,
            1,
            0
    );
    private static final IGuiTexture LIST_IDLE_TEXTURE = new BeveledRectTexture(
            0xFF2D2D2D,
            0xFF46566F,
            0xFF2D2D2D,
            0xFF2D2D2D,
            1,
            0
    );
    private static final IGuiTexture LIST_HOVER_TEXTURE = new BeveledRectTexture(
            0xFF323232,
            0xFF6F8AA9,
            0xFF323232,
            0xFF323232,
            1,
            0
    );
    private static final IGuiTexture LIST_SELECTED_TEXTURE = new BeveledRectTexture(
            0xFF313131,
            0xFF8BD3FF,
            0xFF313131,
            0xFF313131,
            1,
            0
    );
    private static final IGuiTexture FOOTER_BUTTON_IDLE_TEXTURE = new BeveledRectTexture(
            0xFF8C8C8C,
            0xFF5F5F5F,
            0xFFB3B3B3,
            0xFF696969,
            0,
            1
    );
    private static final IGuiTexture FOOTER_BUTTON_HOVER_TEXTURE = new BeveledRectTexture(
            0xFFA0A0A0,
            0xFF707070,
            0xFFD1D1D1,
            0xFF787878,
            0,
            1
    );
    private static final IGuiTexture FOOTER_BUTTON_PRESSED_TEXTURE = new BeveledRectTexture(
            0xFF7D7D7D,
            0xFF555555,
            0xFF707070,
            0xFFB1B1B1,
            0,
            1
    );

    private String currentJson = "";
    private ArenaService.ScreenData data = emptyData();
    private @Nullable String selectedCategoryId;
    private @Nullable String selectedEntryId;

    CombatCatalogUiElement() {
        layout(layout -> {
            layout.flex(1);
            layout.widthPercent(100);
            layout.heightPercent(100);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(10);
        });
        internalSetup();
        rebuild();
    }

    @Override
    public String getValue() {
        return currentJson;
    }

    @Override
    public CombatCatalogUiElement setValue(@Nullable String value) {
        String nextJson = value == null ? "" : value;
        if (nextJson.equals(currentJson) && !getChildren().isEmpty()) {
            return this;
        }

        currentJson = nextJson;
        data = parse(nextJson);
        syncSelection();
        rebuild();
        return this;
    }

    private void rebuild() {
        clearAllChildren();
        addChildren(createMainRow(), createFooterRow());
    }

    private UIElement createMainRow() {
        return new UIElement()
                .layout(layout -> {
                    layout.flex(1);
                    layout.widthPercent(100);
                    layout.heightPercent(100);
                    layout.flexDirection(FlexDirection.ROW);
                    layout.gapAll(PANEL_GAP);
                })
                .addChildren(
                        createListColumn(
                                Component.translatable("screen.incore.arena_catalog.categories"),
                                categoryScroller()
                        ).layout(layout -> {
                            layout.flex(1);
                            layout.heightPercent(100);
                        }),
                        createListColumn(
                                Component.translatable("screen.incore.arena_catalog.difficulties"),
                                entryScroller()
                        ).layout(layout -> {
                            layout.flex(1);
                            layout.heightPercent(100);
                        }),
                        createDetailsPanel().layout(layout -> {
                            layout.flex(2);
                            layout.heightPercent(100);
                        })
                );
    }

    private UIElement createFooterRow() {
        Button doneButton = footerButton(Component.translatable("gui.done"));
        doneButton.layout(layout -> {
            layout.width(128);
            layout.height(24);
        });
        doneButton.setOnClick(event -> PacketDistributor.sendToServer(RequestBackIncoreUiPayload.INSTANCE));

        Button startButton = footerButton(Component.translatable("screen.incore.arena_catalog.start"));
        startButton.layout(layout -> {
            layout.width(196);
            layout.height(24);
        });
        startButton.setActive(canStartSelectedEntry());
        startButton.setOnClick(event -> {
            ResourceLocation entryId = selectedEntryResource();
            if (entryId != null) {
                ArenaNetworking.requestStartRun(entryId);
            }
        });

        return new UIElement()
                .layout(layout -> {
                    layout.widthPercent(100);
                    layout.flexDirection(FlexDirection.ROW);
                    layout.justifyContent(AlignContent.SPACE_BETWEEN);
                    layout.alignItems(AlignItems.CENTER);
                })
                .addChildren(doneButton, startButton);
    }

    private UIElement createListColumn(Component heading, ScrollerView scrollerView) {
        return new UIElement()
                .layout(layout -> {
                    layout.widthPercent(100);
                    layout.heightPercent(100);
                    layout.flexDirection(FlexDirection.COLUMN);
                    layout.gapAll(6);
                })
                .addChildren(
                        columnHeading(heading),
                        new UIElement()
                                .layout(layout -> {
                                    layout.flex(1);
                                    layout.widthPercent(100);
                                    layout.paddingAll(2);
                                })
                                .style(style -> style.backgroundTexture(RectTexture.of(0xCC232323)))
                                .addChild(scrollerView)
                );
    }

    private ScrollerView categoryScroller() {
        ScrollerView scrollerView = createScroller();
        if (data.categories().isEmpty()) {
            scrollerView.addScrollViewChild(emptyLabel(Component.translatable("screen.incore.arena_catalog.none")));
        } else {
            for (ArenaService.CategoryView category : data.categories()) {
                scrollerView.addScrollViewChild(categoryButton(category));
            }
        }
        return scrollerView;
    }

    private ScrollerView entryScroller() {
        ScrollerView scrollerView = createScroller();
        List<ArenaService.ScreenEntry> entries = entriesForSelectedCategory();
        if (entries.isEmpty()) {
            scrollerView.addScrollViewChild(emptyLabel(Component.translatable("screen.incore.arena_catalog.none")));
        } else {
            for (ArenaService.ScreenEntry entry : entries) {
                scrollerView.addScrollViewChild(entryButton(entry));
            }
        }
        return scrollerView;
    }

    private UIElement createDetailsPanel() {
        UIElement panel = new UIElement()
                .layout(layout -> {
                    layout.widthPercent(100);
                    layout.flex(1);
                    layout.paddingAll(10);
                    layout.flexDirection(FlexDirection.COLUMN);
                    layout.gapAll(6);
                })
                .style(style -> style.backgroundTexture(RectTexture.of(0xCC232323)));

        ArenaService.ScreenEntry selectedEntry = selectedEntry();
        if (selectedEntry == null) {
            return panel.addChild(emptyLabel(Component.translatable("screen.incore.arena_catalog.none")));
        }

        panel.addChildren(
                detailHeading(Component.literal(selectedEntry.categoryName())),
                detailLine(Component.literal(selectedEntry.difficultyName()), UIScreenTheme.OtherContent.CATALOG_TEXT_HEADING),
                detailLine(
                        Component.translatable("screen.incore.arena_catalog.gateway", selectedEntry.gatewayId()),
                        UIScreenTheme.OtherContent.CATALOG_TEXT_HEADING
                ),
                detailLine(
                        Component.translatable("screen.incore.arena_catalog.entropy", selectedEntry.rewardEntropyCost()),
                        UIScreenTheme.OtherContent.CATALOG_TEXT_WARNING
                )
        );

        if (selectedEntry.locked()) {
            panel.addChild(detailLine(
                    Component.translatable("screen.incore.arena_catalog.locked_details", selectedEntry.requiredLevel()),
                    UIScreenTheme.OtherContent.CATALOG_TEXT_WARNING
            ));
        }

        panel.addChildren(
                spacer(10),
                detailHeading(Component.translatable("screen.incore.arena_catalog.rewards")),
                createRewardsGrid(selectedEntry)
        );

        if (!selectedEntry.rewardSummary().isBlank()) {
            panel.addChildren(
                    spacer(10),
                    detailLine(
                            Component.literal(selectedEntry.rewardSummary()),
                            UIScreenTheme.OtherContent.CATALOG_TEXT_HEADING
                    )
            );
        }

        return new UIElement()
                .layout(layout -> {
                    layout.widthPercent(100);
                    layout.heightPercent(100);
                    layout.flexDirection(FlexDirection.COLUMN);
                    layout.gapAll(6);
                })
                .addChildren(columnHeading(Component.translatable("screen.incore.arena_catalog.details")), panel);
    }

    private UIElement createRewardsGrid(ArenaService.ScreenEntry selectedEntry) {
        if (selectedEntry.rewardItems().isEmpty()) {
            return emptyLabel(Component.translatable("screen.incore.arena_catalog.none"));
        }

        UIElement grid = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.ROW);
            layout.flexWrap(FlexWrap.WRAP);
            layout.gapAll(16);
            layout.alignItems(AlignItems.CENTER);
        });
        for (ArenaService.RewardView reward : selectedEntry.rewardItems()) {
            grid.addChild(rewardLine(reward));
        }
        return grid;
    }

    private Button categoryButton(ArenaService.CategoryView category) {
        return listButton(Component.literal(category.name()), category.id().equals(selectedCategoryId), () -> {
            selectedCategoryId = category.id();
            selectedEntryId = entriesForSelectedCategory().stream()
                    .map(ArenaService.ScreenEntry::id)
                    .findFirst()
                    .orElse(null);
            rebuild();
        });
    }

    private Button entryButton(ArenaService.ScreenEntry entry) {
        Button button = listButton(
                Component.literal(entry.difficultyName()),
                entry.id().equals(selectedEntryId),
                () -> {
                    selectedEntryId = entry.id();
                    rebuild();
                }
        );
        button.style(style -> style.tooltips(
                entry.locked()
                        ? Component.translatable("screen.incore.arena_catalog.locked", entry.requiredLevel())
                        : Component.translatable("screen.incore.arena_catalog.start")
        ));
        return button;
    }

    private static Button listButton(Component text, boolean selected, Runnable onClick) {
        Button button = new Button().setText(text);
        button.layout(layout -> {
            layout.widthPercent(100);
            layout.height(32);
            layout.alignItems(AlignItems.CENTER);
            layout.justifyContent(AlignContent.FLEX_START);
            layout.paddingHorizontal(10);
        });
        button.text.getLayout().flex(1);
        button.text.getLayout().heightPercent(100);
        button.textStyle(style -> style
                .adaptiveWidth(false)
                .textWrap(TextWrap.HIDE)
                .textAlignHorizontal(Horizontal.LEFT)
                .textColor(UIScreenTheme.OtherContent.CATALOG_TEXT_HEADING)
        );
        button.buttonStyle(style -> style
                .baseTexture(selected ? LIST_SELECTED_TEXTURE : LIST_IDLE_TEXTURE)
                .hoverTexture(selected ? LIST_SELECTED_TEXTURE : LIST_HOVER_TEXTURE)
                .pressedTexture(LIST_SELECTED_TEXTURE)
        );
        button.setOnClick(event -> onClick.run());
        return button;
    }

    private static Button footerButton(Component text) {
        Button button = new Button().setText(text);
        button.layout(layout -> {
            layout.alignItems(AlignItems.CENTER);
            layout.justifyContent(AlignContent.CENTER);
        });
        button.text.getLayout().flex(1);
        button.text.getLayout().heightPercent(100);
        button.textStyle(style -> style
                .adaptiveWidth(false)
                .textWrap(TextWrap.HIDE)
                .textColor(UIScreenTheme.Info.WHITE_TEXT)
        );
        button.buttonStyle(style -> style
                .baseTexture(FOOTER_BUTTON_IDLE_TEXTURE)
                .hoverTexture(FOOTER_BUTTON_HOVER_TEXTURE)
                .pressedTexture(FOOTER_BUTTON_PRESSED_TEXTURE)
        );
        return button;
    }

    private static Label columnHeading(Component text) {
        Label label = INCoreLdLibUiScaffold.wrappedLabel(text);
        label.textStyle(style -> style
                .textWrap(TextWrap.HIDE)
                .textColor(UIScreenTheme.OtherContent.CATALOG_TEXT_HEADING)
        );
        return label;
    }

    private static Label detailHeading(Component text) {
        Label label = INCoreLdLibUiScaffold.wrappedLabel(text);
        label.textStyle(style -> style
                .textWrap(TextWrap.HIDE)
                .textColor(UIScreenTheme.OtherContent.CATALOG_TEXT_HEADING)
        );
        return label;
    }

    private static Label detailLine(Component text, int color) {
        Label label = INCoreLdLibUiScaffold.wrappedLabel(text);
        label.textStyle(style -> style
                .textWrap(TextWrap.HIDE)
                .textColor(color)
        );
        return label;
    }

    private static Label emptyLabel(Component text) {
        return detailLine(text, UIScreenTheme.OtherContent.CATALOG_TEXT_META);
    }

    private static UIElement rewardLine(ArenaService.RewardView reward) {
        ItemStack stack = displayStack(reward);
        return new UIElement()
                .layout(layout -> {
                    layout.widthAuto();
                    layout.alignItems(AlignItems.CENTER);
                    layout.gapAll(6);
                })
                .style(style -> style.tooltips(
                        stack.getHoverName(),
                        Component.literal("x" + stack.getCount())
                ))
                .addChildren(
                        new UIElement()
                                .layout(layout -> {
                                    layout.width(16);
                                    layout.height(16);
                                })
                                .style(style -> style.backgroundTexture(new ItemStackTexture(stack)))
                                .setAllowHitTest(false)
                );
    }

    private static ScrollerView createScroller() {
        ScrollerView scrollerView = new ScrollerView()
                .scrollerStyle(style -> style
                        .mode(ScrollerMode.VERTICAL)
                        .horizontalScrollDisplay(ScrollDisplay.NEVER)
                        .verticalScrollDisplay(ScrollDisplay.AUTO)
                        .minScrollPixel(0.0F)
                        .maxScrollPixel(18.0F)
                );
        scrollerView.layout(layout -> {
            layout.flex(1);
            layout.widthPercent(100);
        });
        scrollerView.viewPort
                .style(style -> style.backgroundTexture(IGuiTexture.EMPTY))
                .layout(layout -> {
                    layout.flex(1);
                    layout.paddingAll(0);
                });
        scrollerView.viewContainer.layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(4);
        });
        scrollerView.verticalContainer.layout(layout -> layout.gapColumn(4));
        scrollerView.verticalScroller.layout(layout -> layout.width(5));
        scrollerView.verticalScroller.headButton.setDisplay(false);
        scrollerView.verticalScroller.tailButton.setDisplay(false);
        scrollerView.horizontalScroller.headButton.setDisplay(false);
        scrollerView.horizontalScroller.tailButton.setDisplay(false);
        scrollerView.verticalScroller.scrollContainer.style(style -> style.backgroundTexture(SCROLL_TRACK_TEXTURE));
        scrollerView.verticalScroller.scrollBar.buttonStyle(style -> style
                .baseTexture(SCROLL_THUMB_IDLE_TEXTURE)
                .hoverTexture(SCROLL_THUMB_HOVER_TEXTURE)
                .pressedTexture(SCROLL_THUMB_PRESSED_TEXTURE)
        );
        return scrollerView;
    }

    private boolean canStartSelectedEntry() {
        ArenaService.ScreenEntry entry = selectedEntry();
        return entry != null && !entry.locked();
    }

    private @Nullable ResourceLocation selectedEntryResource() {
        ArenaService.ScreenEntry entry = selectedEntry();
        return entry == null ? null : ResourceLocation.tryParse(entry.id());
    }

    private @Nullable ArenaService.ScreenEntry selectedEntry() {
        if (selectedEntryId == null) {
            return null;
        }
        return data.entries().stream()
                .filter(entry -> selectedEntryId.equals(entry.id()))
                .findFirst()
                .orElse(null);
    }

    private List<ArenaService.ScreenEntry> entriesForSelectedCategory() {
        if (selectedCategoryId == null) {
            return List.of();
        }
        return data.entries().stream()
                .filter(entry -> selectedCategoryId.equals(entry.categoryId()))
                .toList();
    }

    private void syncSelection() {
        if (data.categories().stream().noneMatch(category -> category.id().equals(selectedCategoryId))) {
            selectedCategoryId = data.categories().stream()
                    .map(ArenaService.CategoryView::id)
                    .findFirst()
                    .orElseGet(() -> data.entries().stream()
                            .map(ArenaService.ScreenEntry::categoryId)
                            .findFirst()
                            .orElse(null));
        }

        List<ArenaService.ScreenEntry> entries = entriesForSelectedCategory();
        if (entries.stream().noneMatch(entry -> entry.id().equals(selectedEntryId))) {
            selectedEntryId = entries.stream()
                    .map(ArenaService.ScreenEntry::id)
                    .findFirst()
                    .orElse(null);
        }
    }

    private static UIElement spacer(float height) {
        return new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.height(height);
        });
    }

    private static ArenaService.ScreenData parse(String json) {
        if (json.isBlank()) {
            return emptyData();
        }
        try {
            ArenaService.ScreenData parsed = GSON.fromJson(json, ArenaService.ScreenData.class);
            return parsed == null ? emptyData() : parsed;
        } catch (Exception ignored) {
            return emptyData();
        }
    }

    private static ArenaService.ScreenData emptyData() {
        return new ArenaService.ScreenData(List.of(), List.of());
    }

    private static ItemStack displayStack(ArenaService.RewardView reward) {
        String itemId = reward.itemId();
        if (itemId != null && !itemId.isBlank()) {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(itemId));
            if (item != null && item != Items.AIR) {
                return new ItemStack(item, Math.max(1, reward.count()));
            }
        }
        return new ItemStack(Items.BARRIER, Math.max(1, reward.count()));
    }
}
