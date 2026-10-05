package net.basilisk.heartofscales.roster;

import java.util.Comparator;
import java.util.UUID;

/** Orders a beacon's list: the viewer's dragons first, then oldest tamed first, unknown tame times last. */
public record RosterSortKey(boolean yours, long tamedAt, UUID dragon) implements Comparable<RosterSortKey> {
    private static final Comparator<RosterSortKey> ORDER = Comparator
            .comparing((RosterSortKey key) -> !key.yours())
            .thenComparingLong(key -> key.tamedAt() == RosterTime.UNKNOWN ? Long.MAX_VALUE : key.tamedAt())
            .thenComparing(RosterSortKey::dragon);

    @Override
    public int compareTo(RosterSortKey other) {
        return ORDER.compare(this, other);
    }
}
