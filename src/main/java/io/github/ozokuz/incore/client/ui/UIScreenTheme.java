package io.github.ozokuz.incore.client.ui;

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
    VENDOR(new UITheme(
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
}
