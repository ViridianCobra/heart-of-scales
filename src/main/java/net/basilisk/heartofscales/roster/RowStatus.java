package net.basilisk.heartofscales.roster;

/** What a beacon's list says about a dragon, worked out when the list is opened. */
public enum RowStatus {
    HOME("home"),
    AWAY("away"),
    NOT_LOADED("not_loaded"),
    DIED("died"),
    REMOVED("removed");

    private final String id;

    RowStatus(String id) {
        this.id = id;
    }

    public static RowStatus of(RosterState state, boolean loaded, boolean insideHomeArea) {
        return switch (state) {
            case DIED -> RowStatus.DIED;
            case REMOVED -> RowStatus.REMOVED;
            case ALIVE -> !loaded ? NOT_LOADED : insideHomeArea ? HOME : AWAY;
        };
    }

    /** The dragon is gone from the world, so only its owner's dismissal clears the row. */
    public boolean hasEnded() {
        return this == DIED || this == REMOVED;
    }

    /** Whether the row names the dimension after its coordinates. Elsewhere means not the viewer's, who is at the beacon. */
    public boolean showsDimension(boolean elsewhere) {
        return this == NOT_LOADED || (this != HOME && elsewhere);
    }

    public String translationKey() {
        return "beacon.heart_of_scales.status." + id;
    }
}
