package ozokuz.incore.client.ui;

public enum UIScreenTheme {
    INFO(new UITheme(
            new UITheme.Backdrop(0xD5090B10, 0xE0010206, true),
            new UITheme.Frame(0xCF10171F, 0xFF67DFFF, 0xFF1E2732, 0xFF4CAFCB, 0xFF1E2732),
            new UITheme.Frame(0xA516202B, 0xA058C7E6, 0x80131A22, 0x804A9EB9, 0x80131A22),
            new UITheme.Frame(0xA516202B, 0xA058C7E6, 0x80131A22, 0x804A9EB9, 0x80131A22),
            new UITheme.Frame(0xFF181F2A, 0xFF46566F, 0xFF46566F, 0xFF46566F, 0xFF46566F),
            new UITheme.Progress(0xFF242B34, 0xFF2F3540, 0xFF71C2FF, 0xFF5ED084),
            new UITheme.Text(0xFFF3F8FF, 0xFFD6F1FF, 0xB8C8D9, 0xFF67DFFF, 0xFF8EF7A0, 0xFFE2C777, 0xFFFF7A7A),
            new UITheme.Chip(0xA7283B4E, 0xFFEAF6FF)
    )),
    CRAFTING(new UITheme(
            new UITheme.Backdrop(0xD5090B10, 0xE0010206, true),
            new UITheme.Frame(0xCC111724, 0xFF4B596F, 0xFF4B596F, 0xFF4B596F, 0xFF4B596F),
            new UITheme.Frame(0x99202A3A, 0xFF4B596F, 0xFF4B596F, 0xFF4B596F, 0xFF4B596F),
            new UITheme.Frame(0x99202A3A, 0xFF4B596F, 0xFF4B596F, 0xFF4B596F, 0xFF4B596F),
            new UITheme.Frame(0xFF181F2A, 0xFF46566F, 0xFF46566F, 0xFF46566F, 0xFF46566F),
            new UITheme.Progress(0xFF242B34, 0xFF2F3540, 0xFF71C2FF, 0xFF5ED084),
            new UITheme.Text(0xFFE8EEF8, 0xFFD6E0EF, 0x98A6B8, 0x8CC5F3, 0x8EF7A0, 0xFFD9A7FF, 0xFFFF7A7A),
            new UITheme.Chip(0xA7243648, 0xFFEAF6FF)
    )),
    CONFIRMATION(new UITheme(
            new UITheme.Backdrop(0xD5090B10, 0xE0010206, true),
            new UITheme.Frame(0xE022252C, 0xFF8F959F, 0xFF8F959F, 0xFF8F959F, 0xFF8F959F),
            new UITheme.Frame(0xEE3C4048, 0xFF8F959F, 0xFF8F959F, 0xFF8F959F, 0xFF8F959F),
            new UITheme.Frame(0xEE3C4048, 0xFF8F959F, 0xFF8F959F, 0xFF8F959F, 0xFF8F959F),
            new UITheme.Frame(0xFF181F2A, 0xFF46566F, 0xFF46566F, 0xFF46566F, 0xFF46566F),
            new UITheme.Progress(0xFF242B34, 0xFF2F3540, 0xFF71C2FF, 0xFF5ED084),
            new UITheme.Text(0xFFF1F3F8, 0xFFD9DCE3, 0xAAB2BF, 0xFFCFE4FF, 0xBDE8BD, 0xFFE2C777, 0xFFFF7777),
            new UITheme.Chip(0xDD1D2127, 0xE6EDF9)
    )),
    MARKET_SHOP(new UITheme(
            new UITheme.Backdrop(0xD5090B10, 0xE0010206, true),
            new UITheme.Frame(0xAA151920, 0xFF454F63, 0xFF454F63, 0xFF454F63, 0xFF454F63),
            new UITheme.Frame(0xAA1B212B, 0xFF475063, 0xFF475063, 0xFF475063, 0xFF475063),
            new UITheme.Frame(0xFF1B212C, 0xFF3D4558, 0xFF3D4558, 0xFF3D4558, 0xFF3D4558),
            new UITheme.Frame(0xFF181D24, 0xFF4A5261, 0xFF4A5261, 0xFF4A5261, 0xFF4A5261),
            new UITheme.Progress(0xFF242B34, 0xFF3D4558, 0xFF71C2FF, 0xFF6F8FB3),
            new UITheme.Text(0xFFF2F2F2, 0xECF2FF, 0xB7C1D0, 0xCFE4FF, 0x6EE780, 0xFFE2C777, 0xFFFF8A8A),
            new UITheme.Chip(0xAA141414, 0xBDE8BD)
    )),
    VENDING_MACHINE(new UITheme(
            new UITheme.Backdrop(0xD5090B10, 0xE0010206, true),
            new UITheme.Frame(0xCC120E18, 0xFF6CE0FF, 0xFF6CE0FF, 0xFF6CE0FF, 0xFF6CE0FF),
            new UITheme.Frame(0x66233648, 0xFF66D9FF, 0xFF66D9FF, 0xFF66D9FF, 0xFF66D9FF),
            new UITheme.Frame(0x66233648, 0xFF66D9FF, 0xFF66D9FF, 0xFF66D9FF, 0xFF66D9FF),
            new UITheme.Frame(0xFF181D24, 0xFF4A5261, 0xFF4A5261, 0xFF4A5261, 0xFF4A5261),
            new UITheme.Progress(0xFF242B34, 0xFF2F3540, 0xFF71C2FF, 0xFF5ED084),
            new UITheme.Text(0xFFECF7FF, 0xFFA9E8FF, 0xFFA2BFD8, 0xFF66D9FF, 0xFFBDE8BD, 0xFFE2C777, 0xFFFF7777),
            new UITheme.Chip(0xAA141414, 0xBDE8BD)
    )),
    MACHINE(new UITheme(
            new UITheme.Backdrop(0xD5090B10, 0xE0010206, true),
            new UITheme.Frame(0xFF13161A, 0xFF4A4F5A, 0xFF4A4F5A, 0xFF4A4F5A, 0xFF4A4F5A),
            new UITheme.Frame(0xFF1A1F26, 0xFF363D49, 0xFF363D49, 0xFF363D49, 0xFF363D49),
            new UITheme.Frame(0xFF1A1F26, 0xFF363D49, 0xFF363D49, 0xFF363D49, 0xFF363D49),
            new UITheme.Frame(0xFF181D24, 0xFF4A5261, 0xFF4A5261, 0xFF4A5261, 0xFF4A5261),
            new UITheme.Progress(0xFF242B34, 0xFF2F3540, 0xFF71C2FF, 0xFF5ED084),
            new UITheme.Text(0xFFE6EBF4, 0xCDD3DE, 0xFFAAAAAA, 0xFF71C2FF, 0xFF7DD6A7, 0xFFE2C777, 0xFFD17C7C),
            new UITheme.Chip(0xAA141414, 0xBDE8BD)
    )),
    RESEARCH(new UITheme(
            new UITheme.Backdrop(0xD5090B10, 0xE0010206, true),
            new UITheme.Frame(0xE011161D, 0xFF44546B, 0xFF1B2330, 0xFF364455, 0xFF1B2330),
            new UITheme.Frame(0xCC171E28, 0xFF38485E, 0xFF121922, 0xFF2F3F52, 0xFF121922),
            new UITheme.Frame(0xCC171E28, 0xFF38485E, 0xFF121922, 0xFF2F3F52, 0xFF121922),
            new UITheme.Frame(0xFF141A23, 0xFF44556D, 0xFF44556D, 0xFF44556D, 0xFF44556D),
            new UITheme.Progress(0xFF101722, 0xFF243143, 0xFF67B5F5, 0xFF58C98A),
            new UITheme.Text(0xFFF2F6FA, 0xFFD4DEE9, 0xFF9DB0C3, 0xFF7CB9FF, 0xFF8FDF8A, 0xFFE4B36B, 0xFFD98C8C),
            new UITheme.Chip(0xAA223042, 0xFFEAF6FF)
    )),
    BATTLEPASS_TASKS(new UITheme(
            new UITheme.Backdrop(0xD5090B10, 0xE0010206, true),
            new UITheme.Frame(0xD114161A, 0xFF474B52, 0xFF000000, 0xFF474B52, 0xFF000000),
            new UITheme.Frame(0xCC0E1015, 0x66454D5B, 0x66454D5B, 0x66454D5B, 0x66454D5B),
            new UITheme.Frame(0xBB1A2029, 0x66454D5B, 0x66454D5B, 0x66454D5B, 0x66454D5B),
            new UITheme.Frame(0xFF181D24, 0xFF4A5261, 0xFF4A5261, 0xFF4A5261, 0xFF4A5261),
            new UITheme.Progress(0xAA111722, 0xFF3A4454, 0xFFFFD31A, 0xFF62D48A),
            new UITheme.Text(0xFFF2F4F8, 0xFFC3C9D3, 0xFFBCC3CF, 0xFFFFD31A, 0xFF62D48A, 0xFFE9DC87, 0xFFE87E7E),
            new UITheme.Chip(0xA82B425A, 0xFFDBEEFF)
    )),
    OTHER_CONTENT(new UITheme(
            new UITheme.Backdrop(0xD5090B10, 0xE0010206, true),
            new UITheme.Frame(0x99202020, 0xFF4D4D4D, 0xFF4D4D4D, 0xFF4D4D4D, 0xFF4D4D4D),
            new UITheme.Frame(0x99232323, 0xFF3D4558, 0xFF3D4558, 0xFF3D4558, 0xFF3D4558),
            new UITheme.Frame(0x99232323, 0xFF3D4558, 0xFF3D4558, 0xFF3D4558, 0xFF3D4558),
            new UITheme.Frame(0xFF181D24, 0xFF4A5261, 0xFF4A5261, 0xFF4A5261, 0xFF4A5261),
            new UITheme.Progress(0xAA111722, 0xFF3A4454, 0xFF6FA6FF, 0xFF7DDC88),
            new UITheme.Text(0xFFF6F6F6, 0xFFD8D8D8, 0xFFAEAEAE, 0xFFF3D26A, 0xFF7DDC88, 0xFFE5CA7A, 0xFFE87E7E),
            new UITheme.Chip(0xDD1D2127, 0xE6EDF9)
    ));

    private final UITheme theme;

    UIScreenTheme(UITheme theme) {
        this.theme = theme;
    }

    public UITheme theme() {
        return this.theme;
    }

    public static final class Info {
        private Info() {
        }

        public static final int RGB_MASK = 0x00FFFFFF;
        public static final int PLR_SCROLL_TRACK_EMPTY_FILL = 0x334C6B80;
        public static final int PLR_SCROLL_TRACK_FILL = 0x4C101926;
        public static final int PLR_ROW_BORDER_IDLE = 0x66516D86;
        public static final int PLR_CARD_OUTLINE_GLOW = 0x79E9FF;
        public static final int PLR_HERO_GLOW = 0x7DE9FF;
        public static final int PLR_SCROLL_TRACK_BOTTOM = 0x80131A22;
        public static final int PLR_SCROLL_THUMB_BOTTOM = 0x805A8CA1;
        public static final int PLR_SCROLL_TRACK_TOP = 0x8060CAE8;
        public static final int PLR_ROW_BORDER_SELECTED_GLOW = 0x89EFFF;
        public static final int PLR_XP_PILL_FILL_IDLE = 0x8A2F4255;
        public static final int PLR_ROW_FILL_IDLE = 0x98202A36;
        public static final int PLR_ROW_FILL_REACHED = 0x580E141B;
        public static final int PLR_ROW_FILL_REACHED_HOVER = 0x6C151F28;
        public static final int PLR_ROW_FILL_REACHED_SELECTED = 0x7A1D2A35;
        public static final int PLR_CHIP_FILL_DEFAULT = 0xA7243648;
        public static final int PLR_CHIP_FILL_XP = 0xA82B425A;
        public static final int PLR_CHIP_FILL_LEVEL = 0xA9283B4E;
        public static final int PLR_ROW_ACCENT_IDLE = 0xAA4B9AB8;
        public static final int PLR_ROW_ACCENT_REACHED = 0x704A6A82;
        public static final int PLR_ROW_ACCENT_REACHED_SELECTED = 0xA06F9AB8;
        public static final int PLR_ROW_BORDER_HOVER = 0xAA6ABFDF;
        public static final int PLR_ROW_BORDER_REACHED = 0x44486178;
        public static final int PLR_ROW_BORDER_REACHED_HOVER = 0x6A6E93AE;
        public static final int PLR_ROW_BORDER_REACHED_SELECTED = 0x8FA9C9E0;
        public static final int PLR_SCROLL_THUMB_FILL = 0xAA7DE9FF;
        public static final int PLR_ROW_FILL_CLAIMED = 0xAE253446;
        public static final int PLR_REWARD_ENTROPY_FILL = 0xB0193A3A;
        public static final int PLR_REWARD_DEFAULT_FILL = 0xB01A2735;
        public static final int PLR_REWARD_COMMAND_FILL = 0xB03A301B;
        public static final int PLR_REWARD_FEATURE_FILL = 0xB02C3247;
        public static final int PLR_XP_PILL_FILL_SELECTED = 0xB03A607D;
        public static final int PLR_XP_PILL_FILL_REACHED = 0x4A1D2A35;
        public static final int PLR_XP_PILL_FILL_REACHED_SELECTED = 0x6C283A49;
        public static final int PLR_ROW_FILL_SELECTED = 0xB1263E58;
        public static final int STATUS_LINE_TEXT = 0xB2C6D8;
        public static final int MUTED_TEXT = 0xB8C8D9;
        public static final int STATUS_BALANCE_TEXT = 0xBDE8BD;
        public static final int PLR_QTY_CHIP_FILL = 0xC0141F2A;
        public static final int STATUS_TRACK_TEXT = 0xCCD5E1;
        public static final int SECONDARY_TEXT = 0xE2EBF5;
        public static final int PLR_ROW_ACCENT_SELECTED = 0xFF7AEFFF;
        public static final int STATUS_OVERFLOW_TEXT = 0xFF9BB8C8;
        public static final int PLR_TEXT_MUTED = 0xFFA6BED2;
        public static final int PLR_SCROLL_THUMB_TOP = 0xFFB7F2FF;
        public static final int PLR_FOCUS_LINE_TEXT = 0xFFD3ECFF;
        public static final int PLR_TEXT_SECONDARY = 0xFFD4E3F0;
        public static final int PRIMARY_TEXT = 0xFFD6F1FF;
        public static final int PLR_CHIP_TEXT_XP = 0xFFDBEEFF;
        public static final int PLR_ROW_TEXT_IDLE = 0xFFE1EDF8;
        public static final int PLR_ROW_TEXT_REACHED = 0xFFB1C0CD;
        public static final int PLR_ROW_TEXT_REACHED_SELECTED = 0xFFDCEAF4;
        public static final int PLR_XP_PILL_TEXT = 0xFFE6F6FF;
        public static final int PLR_XP_PILL_TEXT_REACHED = 0xFFC8D8E4;
        public static final int STATUS_SECTION_TEXT = 0xFFEAF4FF;
        public static final int PLR_LEVEL_MARKER_FILL = 0xCC1C2733;
        public static final int PLR_LEVEL_MARKER_FILL_REACHED = 0xB0182631;
        public static final int PLR_LEVEL_MARKER_BORDER = 0xAA5D7A8C;
        public static final int PLR_LEVEL_MARKER_BORDER_REACHED = 0xAA72A8CC;
        public static final int PLR_LEVEL_MARKER_CHECK = 0xFFA7E3FF;
        public static final int PLR_CHIP_TEXT_LIGHT = 0xFFEAF6FF;
        public static final int PLR_QTY_CHIP_TEXT = 0xFFF0F6FF;
        public static final int TITLE_TEXT = 0xFFF3F8FF;
        public static final int PLR_ROW_TEXT_SELECTED = 0xFFF3FBFF;
        public static final int LEADER_CROWN_TEXT = 0xFFFFD700;
        public static final int WHITE_TEXT = 0xFFFFFFFF;
    }

    public static final class Crafting {
        private Crafting() {
        }

        public static final int ACCENT_TEXT = 0x8CC5F3;
        public static final int SUCCESS_TEXT = 0x8EF7A0;
        public static final int MUTED_TEXT = 0x98A6B8;
        public static final int PANEL_FILL = 0x99202A3A;
        public static final int MODIFIER_LABEL_TEXT = 0xB0D6F2;
        public static final int WINDOW_FILL = 0xCC111724;
        public static final int BODY_TEXT = 0xD6E0EF;
        public static final int WARNING_TEXT = 0xD9A7FF;
        public static final int TITLE_TEXT = 0xE8EEF8;
        public static final int SLOT_FILL = 0xFF181F2A;
        public static final int HEADER_FILL = 0xFF1B2230;
        public static final int HEADER_BORDER = 0xFF38465D;
        public static final int SLOT_BORDER = 0xFF46566F;
        public static final int PANEL_BORDER = 0xFF4B596F;
        public static final int DANGER_TEXT = 0xFF7A7A;
        public static final int REASON_TEXT = 0xFFAFB5;
    }

    public static final class Confirmation {
        private Confirmation() {
        }

        public static final int DELTA_MUTED_TEXT = 0xAAB2BF;
        public static final int DELTA_POSITIVE_TEXT = 0xBDE8BD;
        public static final int BODY_MUTED_TEXT = 0xC9CDD6;
        public static final int LABEL_TEXT = 0xD9DCE3;
        public static final int CHIP_FILL = 0xDD1D2127;
        public static final int CHIP_TEXT = 0xE6EDF9;
        public static final int TITLE_TEXT = 0xF1F3F8;
        public static final int ARROW_TEXT = 0xF5F5F5;
        public static final int DELTA_NEGATIVE_TEXT = 0xFF7777;
        public static final int VALUE_TEXT = 0xFFFFFF;
    }

    public static final class MarketShop {
        private MarketShop() {
        }

        public static final int CHART_PANEL_FILL = 0x661A1F27;
        public static final int TEXT_POSITIVE = 0x6EE780;
        public static final int MODE_ACTIVE_TEXT = 0x9AE29A;
        public static final int SCROLLBAR_TRACK_FILL = 0xAA10161D;
        public static final int OVERLAY_PANEL_FILL = 0xAA141414;
        public static final int OUTER_PANEL_FILL = 0xAA151920;
        public static final int SECTION_PANEL_FILL = 0xAA1B212B;
        public static final int TEXT_MUTED = 0xB7C1D0;
        public static final int TEXT_SOFT = 0xB8C2D3;
        public static final int OVERLAY_VALUE_TEXT = 0xBDE8BD;
        public static final int CHART_MIN_TEXT = 0xBFBFBF;
        public static final int CHART_EMPTY_TEXT = 0xC6C6C6;
        public static final int TEXT_ACCENT = 0xCFE4FF;
        public static final int TEXT_NEUTRAL = 0xD0D0D0;
        public static final int CHART_MAX_TEXT = 0xD5D5D5;
        public static final int NO_DATA_TEXT = 0xDD8D8D;
        public static final int MODE_WARNING_TEXT = 0xE2C777;
        public static final int CHART_TITLE_TEXT = 0xE8E8E8;
        public static final int TEXT_PRIMARY = 0xECF2FF;
        public static final int TITLE_TEXT = 0xF2F2F2;
        public static final int ITEM_TILE_FILL = 0xFF1B212C;
        public static final int ITEM_TILE_FILL_HOVER = 0xFF202A37;
        public static final int ITEM_TILE_FILL_SELECTED = 0xFF283446;
        public static final int ITEM_TILE_BORDER = 0xFF3D4558;
        public static final int CHART_PANEL_BORDER = 0xFF424D5F;
        public static final int OUTER_PANEL_BORDER = 0xFF454F63;
        public static final int SECTION_PANEL_BORDER = 0xFF475063;
        public static final int SCROLLBAR_THUMB_FILL = 0xFF6F8FB3;
        public static final int CHART_LINE = 0xFF71C2FF;
        public static final int ITEM_TILE_BORDER_HOVER = 0xFF79A9DF;
        public static final int ITEM_TILE_BORDER_SELECTED = 0xFF89C9FF;
        public static final int TEXT_NEGATIVE = 0xFF8A8A;
        public static final int SCROLLBAR_THUMB_FILL_ACTIVE = 0xFF8FC8FF;
        public static final int TEXT_LOCKED = 0xFF9A9A9A;
        public static final int SCROLLBAR_THUMB_BORDER = 0xFFB7D6F6;
        public static final int ITEM_NAME_TEXT = 0xFFFFFF;
    }

    public static final class VendingMachine {
        private VendingMachine() {
        }

        public static final int LIST_ROW_FILL_AVAILABLE = 0x66233648;
        public static final int LIST_ROW_FILL_BLOCKED = 0x662C2832;
        public static final int LIST_ROW_FILL_SOLD_OUT = 0x663A1D25;
        public static final int OFFER_COUNT_TEXT = 0xA2BFD8;
        public static final int PAGE_TEXT = 0xA9E8FF;
        public static final int STRESS_CHIP_FILL_OK = 0xAA132739;
        public static final int OVERLAY_FILL = 0xAA141414;
        public static final int STRESS_CHIP_FILL_BAD = 0xAA472222;
        public static final int STRIKE_TEXT = 0xAA9C9DA3;
        public static final int STOCK_OK_TEXT = 0xBDE8BD;
        public static final int DISCOUNT_CHIP_FILL_CURIO = 0xCC0F3A32;
        public static final int DISCOUNT_CHIP_FILL_DEFAULT = 0xCC3B2F10;
        public static final int STRIKE_LINE = 0xCC909197;
        public static final int QUANTITY_TEXT = 0xDDE7F2;
        public static final int TITLE_TEXT = 0xECF7FF;
        public static final int OFFER_NAME_TEXT = 0xF0F0F0;
        public static final int COST_MISSING_TEXT = 0xFF5555;
        public static final int LIST_ROW_HEADER_AVAILABLE = 0xFF66D9FF;
        public static final int STOCK_EMPTY_TEXT = 0xFF7777;
        public static final int DISCOUNT_CHIP_TEXT_CURIO = 0xFF84FFD7;
        public static final int STRESS_CHIP_TEXT_OK = 0xFF9EDCFF;
        public static final int LIST_ROW_HEADER_BLOCKED = 0xFF9FA7B5;
        public static final int LIST_ROW_HEADER_SOLD_OUT = 0xFFCE6D6D;
        public static final int STRESS_CHIP_TEXT_BAD = 0xFFE8A3A3;
        public static final int DISCOUNT_CHIP_TEXT_DEFAULT = 0xFFF9CF7A;
    }

    public static final class Machine {
        private Machine() {
        }

        public static final int GHOST_SLOT_OVERLAY = 0x55FFFFFF;
        public static final int TITLE_TEXT = 0xE6EBF4;
        public static final int PROGRESS_FRAME_FILL = 0xFF101318;
        public static final int SECTION_FILL = 0xFF1A1F26;
        public static final int HEADER_FILL = 0xFF20252C;
        public static final int PROGRESS_TRACK_FILL = 0xFF242B34;
        public static final int PROGRESS_FRAME_BORDER = 0xFF2F3540;
        public static final int SECTION_BORDER = 0xFF363D49;
        public static final int HEADER_BORDER = 0xFF3D4350;
        public static final int PROGRESS_FILL_SUCCESS = 0xFF5ED084;
        public static final int PROGRESS_FILL_PRIMARY = 0xFF71C2FF;
        public static final int STATUS_READY_TEXT = 0xFF7DD6A7;
        public static final int STATUS_DISABLED_TEXT = 0xFFAAAAAA;
        public static final int STATUS_ERROR_TEXT = 0xFFD17C7C;
        public static final int STATUS_WARNING_TEXT = 0xFFE2C777;
    }

    public static final class Research {
        private Research() {
        }

        public static final int TITLE_TEXT = 0xFFF2F6FA;
        public static final int TEXT_PRIMARY = 0xFFD4DEE9;
        public static final int TEXT_MUTED = 0xFF9DB0C3;
        public static final int TEXT_ACCENT = 0xFF7CB9FF;
        public static final int TEXT_SUCCESS = 0xFF8FDF8A;
        public static final int TEXT_WARNING = 0xFFE4B36B;
        public static final int TEXT_DANGER = 0xFFD98C8C;
        public static final int WINDOW_FILL = 0xE011161D;
        public static final int WINDOW_BORDER_TOP = 0xFF44546B;
        public static final int WINDOW_BORDER_SIDE = 0xFF364455;
        public static final int WINDOW_BORDER_BOTTOM = 0xFF1B2330;
        public static final int PANEL_FILL = 0xCC171E28;
        public static final int PANEL_BORDER_TOP = 0xFF38485E;
        public static final int PANEL_BORDER_SIDE = 0xFF2F3F52;
        public static final int PANEL_BORDER_BOTTOM = 0xFF121922;
        public static final int SLOT_FILL = 0xFF141A23;
        public static final int SLOT_BORDER = 0xFF44556D;
        public static final int PROGRESS_TRACK_FILL = 0xFF101722;
        public static final int PROGRESS_TRACK_BORDER = 0xFF243143;
        public static final int PROGRESS_FILL_PRIMARY = 0xFF67B5F5;
        public static final int PROGRESS_FILL_ALT = 0xFF58C98A;
    }

    public static final class BattlepassTasks {
        private BattlepassTasks() {
        }

        public static final int TIER_SLOT_BORDER = 0x33454D5B;
        public static final int TAB_DIVIDER = 0x334A4F57;
        public static final int SCROLL_TRACK_FILL = 0x4C101926;
        public static final int SCROLL_TRACK_EMPTY_FILL = 0x661A2029;
        public static final int TAB_FILL_DEFAULT = 0x6626282E;
        public static final int BORDER_MUTED = 0x66454D5B;
        public static final int REWARD_OVERLAY_LOCKED = 0x7405070C;
        public static final int BADGE_EDGE_INACTIVE = 0x80454D5B;
        public static final int ROW_BORDER_COMPLETE = 0x8062D48A;
        public static final int HEADER_CHIP_FILL = 0x882D313A;
        public static final int HEADER_BORDER_TOP = 0x88484D56;
        public static final int REWARD_OVERLAY_CLAIMED = 0xA01A070F;
        public static final int HEADER_BORDER_BOTTOM = 0xAA0A0D11;
        public static final int PROGRESS_BG_AVAILABLE = 0xAA111722;
        public static final int TIER_SLOT_FILL = 0xAA141820;
        public static final int PROGRESS_BG_LOCKED = 0xAA1A1E26;
        public static final int LIST_FILL_DEFAULT = 0xAA1A2029;
        public static final int ROW_FILL_COMPLETE = 0xAA243127;
        public static final int CHIP_FILL_COMPLETE = 0xAA2B4A34;
        public static final int CHIP_FILL_DEFAULT = 0xAA2D3440;
        public static final int TIER_ROW_UNLOCKED = 0xAA302813;
        public static final int TAB_UNDERLINE_DEFAULT = 0xAA3D4149;
        public static final int CHIP_BORDER_DEFAULT = 0xAA5A6372;
        public static final int CHIP_BORDER_COMPLETE = 0xAA62D48A;
        public static final int REWARD_FILL_LOCKED = 0xBB141820;
        public static final int CARD_FILL_DEFAULT = 0xBB1A2029;
        public static final int CARD_FILL_COMPLETE = 0xBB1E2A24;
        public static final int QUANTITY_CHIP_FILL = 0xC0181E27;
        public static final int EMPTY_TEXT = 0xC9CED7;
        public static final int PANEL_FILL = 0xCC0E1015;
        public static final int REWARD_FILL_UNLOCKED = 0xCC20262E;
        public static final int BADGE_FILL_DEFAULT = 0xCC242A35;
        public static final int BADGE_FILL_COMPLETE = 0xCC253A30;
        public static final int TASK_ROW_FILL_SELECTED = 0xCC323946;
        public static final int REQUIREMENT_FILL_BLOCKED = 0xCC4B1D25;
        public static final int REQUIREMENT_FILL_WARNING = 0xCC8A6D14;
        public static final int REQUIREMENT_FILL_DANGER = 0xCC8E2323;
        public static final int SCROLL_THUMB_FILL = 0xCCBFC7D3;
        public static final int WINDOW_FILL = 0xD114161A;
        public static final int NONE_TEXT = 0xE0E0E0;
        public static final int WINDOW_BORDER_DARK = 0xFF000000;
        public static final int BORDER_DARK = 0xFF0A0C10;
        public static final int TAB_TEXT_SELECTED = 0xFF1A1D22;
        public static final int REWARD_BORDER_EPIC = 0xFF34C7FF;
        public static final int PROGRESS_BORDER = 0xFF3A4454;
        public static final int WINDOW_BORDER_LIGHT = 0xFF474B52;
        public static final int REWARD_BORDER_COMMON = 0xFF4B515D;
        public static final int SCROLL_THUMB_BORDER = 0xFF5A6372;
        public static final int PROGRESS_FILL_LOCKED = 0xFF5A6474;
        public static final int PROGRESS_FILL_COMPLETE = 0xFF62D48A;
        public static final int PROGRESS_FILL_AVAILABLE = 0xFF6FA6FF;
        public static final int REWARD_PLACEHOLDER_TEXT = 0xFF717887;
        public static final int REWARD_BORDER_MYTHIC = 0xFF72D8FF;
        public static final int REWARD_BORDER_LOCKED = 0xFF733239;
        public static final int PROGRESS_FILL_CLAIMED = 0xFF7DDC88;
        public static final int LEAGUE_PLATINUM = 0xFF85E2E5;
        public static final int LEAGUE_DIAMOND = 0xFF89A8FF;
        public static final int TASK_STATUS_COMPLETE = 0xFF99E19D;
        public static final int FOOTER_HINT_TEXT = 0xFF9EA4AF;
        public static final int PROGRESS_BORDER_CLAIMED = 0xFF9EF0A9;
        public static final int TASK_STATUS_LOCKED = 0xFFAAAEB8;
        public static final int LEAGUE_DEFAULT = 0xFFBCBEC6;
        public static final int TEXT_MUTED = 0xFFBCC3CF;
        public static final int HEADER_SEASON_TEXT = 0xFFBFC5CF;
        public static final int TAB_TEXT_DEFAULT = 0xFFC2C6CF;
        public static final int TEXT_SECONDARY = 0xFFC3C9D3;
        public static final int HEADER_META_TEXT = 0xFFC8CED9;
        public static final int FOOTER_TASK_ID_TEXT = 0xFFC9CED8;
        public static final int LEAGUE_SILVER = 0xFFC9D1DA;
        public static final int HEADER_TOTAL_XP_TEXT = 0xFFCAD0DC;
        public static final int TASK_TYPE_TEXT = 0xFFD0D5DF;
        public static final int DETAILS_TEXT = 0xFFD4D9E3;
        public static final int LEAGUE_BRONZE = 0xFFD89A4B;
        public static final int TIER_POINTS_COMPLETE = 0xFFD8EEE0;
        public static final int REWARD_LEVEL_TEXT = 0xFFE2E6EE;
        public static final int TEXT_WARNING = 0xFFE9DC87;
        public static final int QUANTITY_TEXT = 0xFFE9EDF5;
        public static final int TEXT_SOFT = 0xFFECEFF5;
        public static final int HEADER_XP_TEXT = 0xFFF0F0F0;
        public static final int TAB_FILL_SELECTED = 0xFFF1F1F1;
        public static final int HEADER_TITLE_TEXT = 0xFFF2F2F2;
        public static final int TEXT_PRIMARY = 0xFFF2F4F8;
        public static final int TIER_POINTS_UNLOCKED = 0xFFF8E8A3;
        public static final int TAB_UNDERLINE_SELECTED = 0xFFFECE21;
        public static final int REWARD_BORDER_RARE = 0xFFFFB347;
        public static final int ACCENT_GOLD = 0xFFFFD31A;
        public static final int LEAGUE_GOLD = 0xFFFFD966;
        public static final int TASK_XP_TEXT = 0xFFFFE070;
        public static final int TEXT_WHITE = 0xFFFFFFFF;
    }

    public static final class OtherContent {
        private OtherContent() {
        }

        public static final int PACK_RARITY_THREE_TEXT = 0x64D8FF;
        public static final int INFO_ROW_FILL_B = 0x66202020;
        public static final int INFO_ROW_FILL_A = 0x66303030;
        public static final int PACK_FOIL_LABEL_TEXT = 0x79F7FF;
        public static final int PACK_NAME_FOIL_TEXT = 0x8AEEFF;
        public static final int PACK_UNKNOWN_TEXT = 0x8B93A6;
        public static final int PACK_TYPE_TEXT = 0x8FC3E0;
        public static final int CATALOG_COLUMN_FILL = 0x991A1A1A;
        public static final int CATALOG_DETAILS_FILL = 0x99202020;
        public static final int CATALOG_ROW_FILL = 0x99232323;
        public static final int GACHA_BANNER_TYPE_BASIC_TEXT = 0x9AE6FF;
        public static final int GUARANTEE_ROW_FILL = 0xA01D1D1D;
        public static final int GUARANTEE_ROW_FILL_SELECTED = 0xA0223A4A;
        public static final int PACK_SUBTITLE_TEXT = 0xA9DBEC;
        public static final int PACK_CARD_FILL_HIDDEN = 0xAA101018;
        public static final int PACK_CARD_FILL_REVEALED = 0xAA111D2C;
        public static final int GACHA_BALANCE_PANEL_FILL = 0xAA141414;
        public static final int INFO_NOTE_TEXT = 0xAEAEAE;
        public static final int GUARANTEE_ID_TEXT = 0xAFAFAF;
        public static final int GACHA_DROP_RATE_TEXT = 0xB0E0FF;
        public static final int GACHA_PAGE_TEXT = 0xB6B6B6;
        public static final int CATALOG_ROW_SELECTED_FILL = 0xBB2A2A2A;
        public static final int PACK_RARITY_FOUR_TEXT = 0xBC8EFF;
        public static final int GACHA_COST_OK_TEXT = 0xBDE8BD;
        public static final int GACHA_TEXT_SECONDARY = 0xBFBFBF;
        public static final int INFO_RATE_LABEL_TEXT = 0xC2E9FF;
        public static final int PACK_MODAL_FILL = 0xCC0A131B;
        public static final int GACHA_TEXT_MUTED = 0xCECECE;
        public static final int INFO_TOOLTIP_TEXT = 0xCFCFCF;
        public static final int PACK_RARITY_DEFAULT_TEXT = 0xD0D9E8;
        public static final int GACHA_SHOWCASE_CHANCE_TEXT = 0xD88B8B;
        public static final int GACHA_SIDEBAR_LABEL_TEXT = 0xD8D8D8;
        public static final int CATALOG_TEXT_META = 0xD9D9D9;
        public static final int PACK_NAME_TEXT = 0xE0E7F2;
        public static final int GACHA_SHOWCASE_FIVE_TEXT = 0xE5CA7A;
        public static final int GACHA_ERROR_TEXT = 0xE66F6F;
        public static final int INFO_ITEM_MISSING_TEXT = 0xE86E6E;
        public static final int GUARANTEE_ERROR_TEXT = 0xE87E7E;
        public static final int CATALOG_TEXT_PRIMARY = 0xE8E8E8;
        public static final int INFO_CHANCE_TEXT = 0xEDEDED;
        public static final int INFO_ITEM_TEXT = 0xEFEFEF;
        public static final int CATALOG_TEXT_HEADING = 0xF0F0F0;
        public static final int GACHA_TEXT_PRIMARY = 0xF2F2F2;
        public static final int PACK_TITLE_TEXT = 0xF2F9FF;
        public static final int GACHA_PITY_LABEL_TEXT = 0xF3D26A;
        public static final int GACHA_TITLE_TEXT = 0xF6F6F6;
        public static final int GACHA_ROW_BORDER_MASK = 0xFF000000;
        public static final int CATALOG_ROW_BORDER = 0xFF3D4558;
        public static final int PACK_MODAL_BORDER = 0xFF3FC6D5;
        public static final int PACK_CARD_BORDER_HIDDEN = 0xFF444E63;
        public static final int GUARANTEE_ROW_BORDER = 0xFF4D4D4D;
        public static final int PACK_CARD_BORDER_REVEALED = 0xFF4FCDE3;
        public static final int GACHA_COST_MISSING_TEXT = 0xFF5555;
        public static final int GUARANTEE_ROW_BORDER_SELECTED = 0xFF6BD5FF;
        public static final int CATALOG_ROW_BORDER_SELECTED = 0xFF89C9FF;
        public static final int GACHA_SHOWCASE_SIX_TEXT = 0xFF8C8C;
        public static final int PACK_RARITY_FIVE_TEXT = 0xFF8E4B;
        public static final int GUARANTEE_ROW_BORDER_HOVER = 0xFF8F8F8F;
        public static final int GACHA_PITY_VALUE_TEXT = 0xFF9696;
        public static final int PACK_RARITY_SIX_TEXT = 0xFFCE5E;
        public static final int INFO_FEATURED_TEXT = 0xFFD4A4;
        public static final int GACHA_BANNER_TYPE_LIMITED_TEXT = 0xFFD98A;
        public static final int GACHA_FEATURED_TEXT = 0xFFDB9A;
        public static final int CATALOG_TEXT_WARNING = 0xFFE6CC;
        public static final int GACHA_TEXT_SELECTED = 0xFFFFFF;
    }

}
