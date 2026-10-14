package dev.tas69.hovernt;

public final class Marquee {
    private static final long PAUSE_MS = 1800;

    private Marquee() {}

    public static int offset(int textWidth, int viewportWidth, long timeMs) {
        int overflow = Math.max(0, textWidth - viewportWidth);
        if (overflow == 0) return 0;
        long travel = overflow * 45L;
        long phase = Math.floorMod(timeMs, 2 * PAUSE_MS + 2 * travel);
        if (phase < PAUSE_MS) return 0;
        if (phase < PAUSE_MS + travel) return (int) ((phase - PAUSE_MS) / 45);
        if (phase < 2 * PAUSE_MS + travel) return overflow;
        return overflow - (int) ((phase - 2 * PAUSE_MS - travel) / 45);
    }
}
