package dev.tas69.hovernt;

public final class SidebarLayout {
    public static boolean expanded = true;

    private SidebarLayout() {}

    public static int width(int screenWidth, int merchantWidth) {
        return expanded ? Math.min(190, Math.max(0, screenWidth - merchantWidth - 12)) : 0;
    }

    public static int merchantLeft(int screenWidth, int merchantWidth) {
        return (screenWidth + width(screenWidth, merchantWidth) - merchantWidth) / 2;
    }
}
