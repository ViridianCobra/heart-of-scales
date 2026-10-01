package net.basilisk.heartofscales.roster;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Real-world times stored in the roster, in epoch milliseconds. */
public final class RosterTime {
    /** Stored when no time was recorded, such as a dragon tamed before tame times existed. */
    public static final long UNKNOWN = 0L;
    private static final String PATTERN = "d MMM, HH:mm";

    public static String format(long epochMillis, ZoneId zone, Locale locale) {
        return DateTimeFormatter.ofPattern(PATTERN, locale).format(Instant.ofEpochMilli(epochMillis).atZone(zone));
    }

    private RosterTime() {}
}
