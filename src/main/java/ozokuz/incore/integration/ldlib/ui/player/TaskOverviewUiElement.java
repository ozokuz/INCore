package ozokuz.incore.integration.ldlib.ui.player;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.IBindable;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.texture.RectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import ozokuz.incore.client.ui.UIScreenTheme;
import ozokuz.incore.features.tasks.DailyTaskService;
import ozokuz.incore.features.tasks.TaskService;
import ozokuz.incore.features.tasks.network.TaskNetworking;

final class TaskOverviewUiElement extends UIElement implements IBindable<String> {
    private String currentJson = "";
    private TaskOverviewUiSupport.TaskOverviewSnapshot data = TaskOverviewUiSupport.TaskOverviewSnapshot.EMPTY;

    TaskOverviewUiElement() {
        layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
        });
        internalSetup();
        rebuild();
    }

    @Override
    public String getValue() {
        return currentJson;
    }

    @Override
    public TaskOverviewUiElement setValue(@Nullable String value) {
        String nextJson = value == null ? "" : value;
        if (nextJson.equals(currentJson) && !getChildren().isEmpty()) {
            return this;
        }
        currentJson = nextJson;
        data = TaskOverviewUiSupport.parseSnapshot(nextJson);
        rebuild();
        return this;
    }

    private void rebuild() {
        clearAllChildren();
        addChild(
                new UIElement()
                        .layout(layout -> {
                            layout.widthPercent(100);
                            layout.heightPercent(100);
                            layout.justifyContent(AlignContent.CENTER);
                            layout.alignItems(AlignItems.CENTER);
                        })
                        .style(style -> style.backgroundTexture(RectTexture.of(0x72000000)))
                        .addChild(createWindow())
        );
    }

    private UIElement createWindow() {
        var window = framedSurface(
                TaskOverviewUiSupport.WINDOW_FILL,
                TaskOverviewUiSupport.WINDOW_BORDER_LIGHT,
                TaskOverviewUiSupport.WINDOW_BORDER_DARK,
                10
        );
        window.root().layout(layout -> {
            layout.widthPercent(98);
            layout.heightPercent(98);
            layout.maxWidth(TaskOverviewUiSupport.TARGET_WINDOW_WIDTH);
            layout.maxHeight(TaskOverviewUiSupport.TARGET_WINDOW_HEIGHT);
            layout.minWidth(TaskOverviewUiSupport.MIN_WINDOW_WIDTH);
            layout.minHeight(TaskOverviewUiSupport.MIN_WINDOW_HEIGHT);
        });

        window.body().addChildren(
                titleLabel(),
                contentRow()
        );
        return window.root();
    }

    private UIElement titleLabel() {
        return TaskOverviewUiSupport.lineLabel(
                Component.translatable("screen.incore.tasks.title"),
                UIScreenTheme.BattlepassTasks.HEADER_TITLE_TEXT
        ).textStyle(style -> style
                .textWrap(TextWrap.HIDE)
                .textAlignHorizontal(Horizontal.LEFT)
        );
    }

    private UIElement contentRow() {
        return new UIElement()
                .layout(layout -> {
                    layout.flex(1);
                    layout.widthPercent(100);
                    layout.minHeight(0);
                    layout.flexDirection(FlexDirection.ROW);
                    layout.gapAll(8);
                })
                .addChildren(
                        createSidebarPanel(),
                        createMainPanel()
                );
    }

    private UIElement createSidebarPanel() {
        var panel = framedPanel(TaskOverviewUiSupport.PANEL_FILL, TaskOverviewUiSupport.PANEL_BORDER, 8);
        panel.root().layout(layout -> {
            layout.flexBasis(TaskOverviewUiSupport.SIDEBAR_TARGET_WIDTH);
            layout.minWidth(132);
            layout.maxWidth(190);
            layout.heightPercent(100);
        });
        panel.body().layout(layout -> {
            layout.flex(1);
            layout.heightPercent(100);
            layout.minHeight(0);
            layout.gapAll(8);
        });

        panel.body().addChildren(
                TaskOverviewUiSupport.lineLabel(
                        Component.literal("Daily: " + data.fixedDailyCompleted() + "/" + TaskOverviewUiSupport.TOTAL_DAILY_TASKS),
                        TaskOverviewUiSupport.TEXT_PRIMARY
                ),
                createDailyTaskArea(),
                spacer(),
                TaskOverviewUiSupport.lineLabel(
                        Component.translatable("screen.incore.tasks.daily_reward"),
                        TaskOverviewUiSupport.TEXT_SECONDARY
                ),
                createRewardStrip(data.dailyRewards(), 4, false),
                createDailyClaimButton()
        );
        return panel.root();
    }

    private UIElement createDailyTaskArea() {
        UIElement rows = new UIElement().layout(layout -> {
            layout.flex(1);
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.ROW);
            layout.gapAll(4);
            layout.alignItems(AlignItems.STRETCH);
        });

        rows.addChildren(
                createDailyProgressBar(),
                createDailyTaskList().layout(layout -> {
                    layout.flex(1);
                    layout.heightPercent(100);
                })
        );
        return rows;
    }

    private UIElement createDailyProgressBar() {
        int completed = Math.clamp(data.fixedDailyCompleted(), 0, TaskOverviewUiSupport.TOTAL_DAILY_TASKS);
        UIElement bar = new UIElement().layout(layout -> {
            layout.width(8);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(2);
        });
        for (int i = 0; i < TaskOverviewUiSupport.TOTAL_DAILY_TASKS; i++) {
            boolean filled = i >= TaskOverviewUiSupport.TOTAL_DAILY_TASKS - completed;
            bar.addChild(new UIElement()
                    .layout(layout -> {
                        layout.widthPercent(100);
                        layout.height(22);
                    })
                    .style(style -> style.backgroundTexture(RectTexture.of(
                            filled ? TaskOverviewUiSupport.PROGRESS_FILL : TaskOverviewUiSupport.PROGRESS_BG
                    )))
            );
        }
        return bar;
    }

    private UIElement createDailyTaskList() {
        List<DailyTaskService.DailyTaskView> tasks = TaskOverviewUiSupport.sortedDailyTasks(data.fixedDailyTasks());
        if (tasks.isEmpty()) {
            return TaskOverviewUiSupport.lineLabel(
                    Component.translatable("screen.incore.tasks.no_daily"),
                    TaskOverviewUiSupport.TEXT_SECONDARY
            );
        }

        UIElement list = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(2);
        });
        for (DailyTaskService.DailyTaskView task : tasks) {
            boolean complete = task.progress() >= task.goal();
            var row = framedPanel(
                    complete ? TaskOverviewUiSupport.CARD_FILL_COMPLETE : TaskOverviewUiSupport.CARD_FILL,
                    complete ? TaskOverviewUiSupport.ROW_BORDER_COMPLETE : TaskOverviewUiSupport.PANEL_BORDER,
                    4
            );
            row.root().layout(layout -> {
                layout.widthPercent(100);
                layout.height(22);
            });
            row.body().layout(layout -> {
                layout.widthPercent(100);
                layout.flexDirection(FlexDirection.ROW);
                layout.alignItems(AlignItems.CENTER);
                layout.gapAll(4);
            });

            Label title = TaskOverviewUiSupport.lineLabel(
                    Component.literal(task.title()),
                    complete ? TaskOverviewUiSupport.TEXT_COMPLETE : TaskOverviewUiSupport.TEXT_SOFT
            );
            title.layout(layout -> {
                layout.flex(1);
                layout.minWidth(0);
            });
            title.textStyle(style -> style
                    .adaptiveWidth(false)
                    .textWrap(TextWrap.HIDE)
                    .textAlignHorizontal(Horizontal.LEFT)
            );

            Label progressLabel = TaskOverviewUiSupport.lineLabel(
                    Component.literal(complete ? "\u2713" : task.progress() + "/" + task.goal()),
                    TaskOverviewUiSupport.TEXT_SECONDARY
            );
            progressLabel.layout(layout -> layout.width(28));
            progressLabel.textStyle(style -> style
                    .adaptiveWidth(false)
                    .textWrap(TextWrap.HIDE)
                    .textAlignHorizontal(Horizontal.RIGHT)
            );

            row.body().addChildren(
                    title,
                    progressLabel
            );
            list.addChild(row.root());
        }
        return list;
    }

    private UIElement createDailyClaimButton() {
        boolean claimable = data.fixedDailyAllCompleted() && !data.fixedDailyRewardClaimed();
        Component text;
        if (claimable) {
            text = Component.translatable("screen.incore.tasks.claim_daily");
        } else if (data.fixedDailyRewardClaimed()) {
            text = Component.translatable("screen.incore.tasks.claimed_daily");
        } else {
            text = Component.translatable("screen.incore.tasks.claim_daily_locked");
        }
        Button button = TaskOverviewUiSupport.createButton(text, 140, claimable);
        button.layout(layout -> layout.widthPercent(100));
        button.setOnClick(event -> TaskNetworking.requestDailyRewardClaim());
        return button;
    }

    private UIElement createMainPanel() {
        var panel = framedPanel(TaskOverviewUiSupport.PANEL_FILL, TaskOverviewUiSupport.PANEL_BORDER, 8);
        panel.root().layout(layout -> {
            layout.flex(1);
            layout.heightPercent(100);
        });
        panel.body().layout(layout -> {
            layout.flex(1);
            layout.heightPercent(100);
            layout.minHeight(0);
            layout.gapAll(8);
        });
        panel.body().addChildren(
                createWeeklyHeader(),
                createWeeklyList(),
                createTierTrack()
        );
        return panel.root();
    }

    private UIElement createWeeklyHeader() {
        int claimableTiers = TaskOverviewUiSupport.countClaimableWeeklyTiers(data);
        Button weeklyClaimButton = TaskOverviewUiSupport.createButton(
                claimableTiers > 0
                        ? Component.translatable("screen.incore.tasks.claim_weekly_count", claimableTiers)
                        : Component.translatable("screen.incore.tasks.claim_weekly_locked"),
                166,
                claimableTiers > 0
        );
        weeklyClaimButton.setOnClick(event -> TaskNetworking.requestWeeklyRewardsClaim());

        return new UIElement()
                .layout(layout -> {
                    layout.widthPercent(100);
                    layout.flexDirection(FlexDirection.ROW);
                    layout.justifyContent(AlignContent.SPACE_BETWEEN);
                    layout.alignItems(AlignItems.FLEX_START);
                    layout.gapAll(8);
                })
                .addChildren(
                        new UIElement()
                                .layout(layout -> {
                                    layout.flex(1);
                                    layout.flexDirection(FlexDirection.COLUMN);
                                    layout.gapAll(2);
                                })
                                .addChildren(
                                        TaskOverviewUiSupport.lineLabel(
                                                Component.translatable("screen.incore.tasks.weekly_routine"),
                                                TaskOverviewUiSupport.TEXT_PRIMARY
                                        ),
                                        TaskOverviewUiSupport.lineLabel(
                                                Component.translatable("screen.incore.tasks.refreshes_weekly"),
                                                TaskOverviewUiSupport.TEXT_SECONDARY
                                        )
                                ),
                        weeklyClaimButton
                );
    }

    private UIElement createWeeklyList() {
        List<TaskService.TaskView> tasks = TaskOverviewUiSupport.sortedWeeklyTasks(data.weekly());
        ScrollerView scroller = new ScrollerView()
                .scrollerStyle(style -> style
                        .mode(ScrollerMode.VERTICAL)
                        .horizontalScrollDisplay(ScrollDisplay.NEVER)
                        .verticalScrollDisplay(ScrollDisplay.AUTO)
                        .minScrollPixel(0.0F)
                        .maxScrollPixel(18.0F)
                );
        scroller.layout(layout -> {
            layout.flex(1);
            layout.widthPercent(100);
            layout.minHeight(0);
        });
        scroller.viewPort
                .style(style -> style.backgroundTexture(RectTexture.of(0x00000000)))
                .layout(layout -> {
                    layout.flex(1);
                    layout.minHeight(0);
                    layout.paddingAll(0);
                });
        scroller.viewContainer.layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(6);
        });
        scroller.verticalContainer.layout(layout -> layout.gapColumn(6));
        scroller.verticalScroller.layout(layout -> layout.width(6));
        scroller.verticalScroller.headButton.setDisplay(false);
        scroller.verticalScroller.tailButton.setDisplay(false);
        scroller.horizontalScroller.headButton.setDisplay(false);
        scroller.horizontalScroller.tailButton.setDisplay(false);
        scroller.verticalScroller.scrollContainer.style(style -> style.backgroundTexture(TaskOverviewUiSupport.SCROLL_TRACK_TEXTURE));
        scroller.verticalScroller.scrollBar.buttonStyle(style -> style
                .baseTexture(TaskOverviewUiSupport.SCROLL_THUMB_IDLE_TEXTURE)
                .hoverTexture(TaskOverviewUiSupport.SCROLL_THUMB_HOVER_TEXTURE)
                .pressedTexture(TaskOverviewUiSupport.SCROLL_THUMB_PRESSED_TEXTURE)
        );

        if (tasks.isEmpty()) {
            scroller.addScrollViewChild(TaskOverviewUiSupport.lineLabel(
                    Component.translatable("screen.incore.tasks.no_weekly"),
                    TaskOverviewUiSupport.TEXT_SECONDARY
            ));
        } else {
            for (TaskService.TaskView task : tasks) {
                scroller.addScrollViewChild(createWeeklyCard(task));
            }
        }
        return scroller;
    }

    private UIElement createWeeklyCard(TaskService.TaskView task) {
        boolean complete = task.progress() >= task.goal();
        int progress = Math.min(task.progress(), task.goal());
        Label titleLabel = TaskOverviewUiSupport.lineLabel(
                Component.literal(task.title()),
                TaskOverviewUiSupport.TEXT_SOFT
        );
        titleLabel.layout(layout -> {
            layout.widthPercent(100);
            layout.minWidth(0);
        });
        titleLabel.textStyle(style -> style
                .adaptiveWidth(false)
                .textWrap(TextWrap.HIDE)
                .textAlignHorizontal(Horizontal.LEFT)
        );

        Label progressLabel = TaskOverviewUiSupport.lineLabel(
                Component.literal(progress + "/" + task.goal()),
                TaskOverviewUiSupport.TEXT_SECONDARY
        );
        progressLabel.layout(layout -> layout.widthPercent(100));
        progressLabel.textStyle(style -> style
                .adaptiveWidth(false)
                .textWrap(TextWrap.HIDE)
                .textAlignHorizontal(Horizontal.LEFT)
        );

        var card = framedPanel(
                complete ? TaskOverviewUiSupport.CARD_FILL_COMPLETE : TaskOverviewUiSupport.CARD_FILL,
                complete ? TaskOverviewUiSupport.ROW_BORDER_COMPLETE : TaskOverviewUiSupport.PANEL_BORDER,
                0
        );
        card.root().layout(layout -> {
            layout.widthPercent(100);
            layout.height(TaskOverviewUiSupport.WEEKLY_CARD_HEIGHT);
        });
        card.body().layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.STRETCH);
        });

        card.body().addChildren(
                new UIElement()
                        .layout(layout -> {
                            layout.width(52);
                            layout.heightPercent(100);
                            layout.justifyContent(AlignContent.CENTER);
                            layout.alignItems(AlignItems.CENTER);
                        })
                        .style(style -> style.backgroundTexture(RectTexture.of(
                                complete ? TaskOverviewUiSupport.BADGE_FILL_COMPLETE : TaskOverviewUiSupport.BADGE_FILL
                        )))
                        .addChild(TaskOverviewUiSupport.lineLabel(
                                Component.literal("▲ " + task.points()),
                                TaskOverviewUiSupport.TEXT_ACCENT
                        )),
                new UIElement()
                        .layout(layout -> {
                            layout.flex(1);
                            layout.minWidth(0);
                            layout.heightPercent(100);
                            layout.paddingLeft(8);
                            layout.paddingRight(8);
                            layout.paddingTop(4);
                            layout.paddingBottom(4);
                            layout.flexDirection(FlexDirection.COLUMN);
                            layout.gapAll(2);
                        })
                        .addChildren(
                                titleLabel,
                                progressLabel,
                                progressBar(
                                        progress,
                                        task.goal(),
                                        complete ? TaskOverviewUiSupport.PROGRESS_FILL_COMPLETE : TaskOverviewUiSupport.PROGRESS_FILL
                                )
                        ),
                createStatusPill(complete)
        );
        return card.root();
    }

    private UIElement createStatusPill(boolean complete) {
        var pill = framedPanel(
                complete ? TaskOverviewUiSupport.CHIP_FILL_COMPLETE : TaskOverviewUiSupport.CHIP_FILL,
                complete ? TaskOverviewUiSupport.CHIP_BORDER_COMPLETE : TaskOverviewUiSupport.CHIP_BORDER,
                4
        );
        pill.root().layout(layout -> {
            layout.width(92);
            layout.height(16);
            layout.marginTop(10);
            layout.marginRight(8);
        });
        pill.body().layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
            layout.justifyContent(AlignContent.CENTER);
            layout.alignItems(AlignItems.CENTER);
        });
        pill.body().addChild(
                TaskOverviewUiSupport.lineLabel(
                        Component.translatable(
                                complete
                                        ? "screen.incore.tasks.status_complete"
                                        : "screen.incore.tasks.status_in_progress"
                        ),
                        TaskOverviewUiSupport.TEXT_PRIMARY
                ).textStyle(style -> style.textWrap(TextWrap.HIDE).textAlignHorizontal(Horizontal.CENTER))
        );
        return pill.root();
    }

    private UIElement createTierTrack() {
        int maxPoints = data.tiers().stream().mapToInt(TaskService.TierView::requiredPoints).max().orElse(0);
        int clampedPoints = maxPoints > 0 ? Math.min(data.weeklyPoints(), maxPoints) : data.weeklyPoints();

        return new UIElement()
                .layout(layout -> {
                    layout.widthPercent(100);
                    layout.flexShrink(0);
                    layout.flexDirection(FlexDirection.COLUMN);
                    layout.gapAll(4);
                })
                .addChildren(
                        new UIElement()
                                .layout(layout -> {
                                    layout.widthPercent(100);
                                    layout.flexDirection(FlexDirection.ROW);
                                    layout.justifyContent(AlignContent.SPACE_BETWEEN);
                                    layout.alignItems(AlignItems.CENTER);
                                })
                                .addChildren(
                                        TaskOverviewUiSupport.lineLabel(
                                                Component.translatable("screen.incore.tasks.tiers"),
                                                TaskOverviewUiSupport.TEXT_SECONDARY
                                        ),
                                        new UIElement()
                                                .layout(layout -> {
                                                    layout.flexDirection(FlexDirection.ROW);
                                                    layout.gapAll(3);
                                                    layout.alignItems(AlignItems.CENTER);
                                                })
                                                .addChildren(
                                                        TaskOverviewUiSupport.lineLabel(Component.literal("▲"), TaskOverviewUiSupport.TEXT_ACCENT),
                                                        TaskOverviewUiSupport.lineLabel(
                                                                Component.translatable("screen.incore.tasks.tier_points_progress", clampedPoints, maxPoints),
                                                                TaskOverviewUiSupport.TEXT_SECONDARY
                                                        )
                                                )
                                ),
                        createTierCards(),
                        progressBar(clampedPoints, Math.max(1, maxPoints), TaskOverviewUiSupport.PROGRESS_FILL)
                );
    }

    private UIElement createTierCards() {
        UIElement row = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.ROW);
            layout.gapAll(4);
            layout.alignItems(AlignItems.STRETCH);
        });

        for (TaskService.TierView tier : data.tiers()) {
            int fill = tier.claimed()
                    ? TaskOverviewUiSupport.CARD_FILL_COMPLETE
                    : tier.unlocked() ? TaskOverviewUiSupport.TIER_FILL_UNLOCKED : TaskOverviewUiSupport.TIER_SLOT_FILL;
            int border = tier.claimed()
                    ? TaskOverviewUiSupport.ROW_BORDER_COMPLETE
                    : tier.unlocked() ? TaskOverviewUiSupport.TEXT_ACCENT : TaskOverviewUiSupport.TIER_SLOT_BORDER;
            int pointsColor = tier.claimed()
                    ? UIScreenTheme.BattlepassTasks.TIER_POINTS_COMPLETE
                    : tier.unlocked() ? UIScreenTheme.BattlepassTasks.TIER_POINTS_UNLOCKED : TaskOverviewUiSupport.TEXT_SECONDARY;

            var card = framedPanel(fill, border, 4);
            card.root().layout(layout -> {
                layout.flex(1);
                layout.minWidth(56);
                layout.height(36);
            });
            card.body().layout(layout -> {
                layout.widthPercent(100);
                layout.heightPercent(100);
                layout.flexDirection(FlexDirection.COLUMN);
                layout.gapAll(4);
            });
            card.body().addChildren(
                    new UIElement()
                            .layout(layout -> {
                                layout.widthPercent(100);
                                layout.flexDirection(FlexDirection.ROW);
                                layout.justifyContent(AlignContent.SPACE_BETWEEN);
                                layout.alignItems(AlignItems.CENTER);
                            })
                            .addChildren(
                                    TaskOverviewUiSupport.lineLabel(Component.literal("T" + tier.tier()), TaskOverviewUiSupport.TEXT_PRIMARY),
                                    TaskOverviewUiSupport.lineLabel(Component.literal("▲ " + tier.requiredPoints()), pointsColor)
                            ),
                    createRewardStrip(tier.rewards(), 2, true)
            );
            row.addChild(card.root());
        }
        return row;
    }

    private UIElement createRewardStrip(List<TaskService.RewardView> rewards, int maxVisible, boolean compact) {
        if (rewards == null || rewards.isEmpty()) {
            return TaskOverviewUiSupport.lineLabel(Component.literal("-"), TaskOverviewUiSupport.TEXT_SECONDARY);
        }

        UIElement row = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.ROW);
            if (!compact) {
                layout.flexWrap(FlexWrap.WRAP);
            }
            layout.gapAll(4);
            layout.alignItems(AlignItems.CENTER);
            layout.justifyContent(AlignContent.CENTER);
        });

        int visibleRewards = Math.min(maxVisible, rewards.size());
        for (int i = 0; i < visibleRewards; i++) {
            row.addChild(createRewardEntry(rewards.get(i), compact));
        }
        if (rewards.size() > visibleRewards) {
            row.addChild(TaskOverviewUiSupport.lineLabel(
                    Component.literal("+" + (rewards.size() - visibleRewards)),
                    TaskOverviewUiSupport.TEXT_PRIMARY
            ));
        }
        return row;
    }

    private UIElement createRewardEntry(TaskService.RewardView reward, boolean compact) {
        ItemStack stack = TaskOverviewUiSupport.iconStackFor(reward);
        int amount = TaskOverviewUiSupport.displayAmount(reward);

        UIElement entry = new UIElement().layout(layout -> {
            layout.widthAuto();
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.gapAll(compact ? 2 : 3);
        });

        entry.addChild(
                new UIElement()
                        .layout(layout -> {
                            layout.width(TaskOverviewUiSupport.REWARD_ICON_SIZE);
                            layout.height(TaskOverviewUiSupport.REWARD_ICON_SIZE);
                        })
                        .style(style -> style
                                .backgroundTexture(new ItemStackTexture(stack))
                                .tooltips(TaskOverviewUiSupport.tooltipForReward(reward, stack))
                        )
        );

        if (amount > 1) {
            entry.addChild(
                    TaskOverviewUiSupport.lineLabel(
                            Component.literal(compact ? "x" + amount : Integer.toString(amount)),
                            compact ? TaskOverviewUiSupport.TEXT_SECONDARY : TaskOverviewUiSupport.TEXT_PRIMARY
                    )
            );
        }
        return entry;
    }

    private ProgressBar progressBar(int progress, int goal, int fillColor) {
        ProgressBar progressBar = TaskOverviewUiSupport.progressBar(
                TaskOverviewUiSupport.PROGRESS_BG,
                fillColor,
                5
        );
        progressBar.setProgress(goal <= 0 ? 0.0F : (float) Math.max(0, progress) / (float) goal);
        return progressBar;
    }

    private static UIElement spacer() {
        return new UIElement().layout(layout -> layout.flex(1));
    }

    private static PanelScaffold framedPanel(int fillColor, int borderColor, int padding) {
        UIElement root = new UIElement()
                .layout(layout -> {
                    layout.widthPercent(100);
                    layout.paddingAll(1);
                })
                .style(style -> style.backgroundTexture(RectTexture.of(borderColor)));
        UIElement body = new UIElement()
                .layout(layout -> {
                    layout.widthPercent(100);
                    layout.flex(1);
                    layout.paddingAll(padding);
                })
                .style(style -> style.backgroundTexture(RectTexture.of(fillColor)));
        root.addChild(body);
        return new PanelScaffold(root, body);
    }

    private static PanelScaffold framedSurface(int fillColor, int lightBorderColor, int darkBorderColor, int padding) {
        UIElement outer = new UIElement()
                .layout(layout -> {
                    layout.widthPercent(100);
                    layout.paddingAll(1);
                })
                .style(style -> style.backgroundTexture(RectTexture.of(darkBorderColor)));
        UIElement middle = new UIElement()
                .layout(layout -> {
                    layout.widthPercent(100);
                    layout.flex(1);
                    layout.paddingAll(1);
                })
                .style(style -> style.backgroundTexture(RectTexture.of(lightBorderColor)));
        UIElement body = new UIElement()
                .layout(layout -> {
                    layout.widthPercent(100);
                    layout.flex(1);
                    layout.paddingAll(padding);
                    layout.flexDirection(FlexDirection.COLUMN);
                    layout.gapAll(10);
                })
                .style(style -> style.backgroundTexture(RectTexture.of(fillColor)));
        middle.addChild(body);
        outer.addChild(middle);
        return new PanelScaffold(outer, body);
    }

    private record PanelScaffold(UIElement root, UIElement body) {
    }
}
