package net.basilisk.heartofscales.roster;

/** What the roster knows for certain about a dragon. Whether it is home or away is worked out live, see {@link RowStatus}. */
public enum RosterState {
    ALIVE,
    DIED,
    REMOVED;

    public static RosterState byName(String name) {
        for (RosterState state : values()) {
            if (state.name().equals(name)) return state;
        }
        return ALIVE;
    }
}
