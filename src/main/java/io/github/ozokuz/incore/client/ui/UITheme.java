package io.github.ozokuz.incore.client.ui;

public record UITheme(
        Backdrop backdrop,
        Frame window,
        Frame panel,
        Frame card,
        Frame slot,
        Progress progress,
        Text text,
        Chip chip
) {
    public record Backdrop(int top, int bottom, boolean gradient) {
    }

    public record Frame(int fill, int borderTop, int borderBottom, int borderLeft, int borderRight) {
    }

    public record Progress(int trackFill, int trackBorder, int fill, int fillAlt) {
    }

    public record Text(int primary, int secondary, int muted, int accent, int success, int warning, int danger) {
    }

    public record Chip(int fill, int text) {
    }
}
