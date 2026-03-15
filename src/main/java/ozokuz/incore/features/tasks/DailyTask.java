package ozokuz.incore.features.tasks;

public enum DailyTask {
    LOGIN("Log In", 1),
    SHOP_PURCHASE("Shop Purchases", 3),
    ARENA_COMPLETION("Arena Completions", 2),
    DUNGEON_COMPLETION("Dungeon Completion", 1),
    VENDING_MACHINE_PURCHASE("Vending Machine Purchase", 1),
    BUY_FROM_PLAYER("Buy from Player", 1),
    SELL_TO_PLAYER("Sell to Player", 1);

    private final String title;
    private final int goal;

    DailyTask(String title, int goal) {
        this.title = title;
        this.goal = goal;
    }

    public String title() {
        return title;
    }

    public int goal() {
        return goal;
    }

    public static DailyTask[] allTasks() {
        return values();
    }
}
