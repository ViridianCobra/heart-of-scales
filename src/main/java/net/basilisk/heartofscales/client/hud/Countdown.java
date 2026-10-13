package net.basilisk.heartofscales.client.hud;

/** Formats a tick count as minutes and seconds, rounding part seconds up so it only reads 0:00 when done. */
public final class Countdown {
    private static final int TICKS_PER_SECOND = 20;

    public static String format(int ticks) {
        int seconds = Math.max(0, (ticks + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND);
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    private Countdown() {}
}
