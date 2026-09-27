package net.basilisk.heartofscales.entity;

import java.util.List;

/** How a ridden dragon answers the rider's look direction. Toggled by the rider with a key. */
public enum FlightMode {
    /** Goes exactly where the rider looks. */
    FREE("free"),
    /** Carries momentum: dives gain speed, climbs lose it, turns roll the body. */
    GLIDE("glide");

    private final String id;

    FlightMode(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    /**
     * The modes a species offers, in toggle order. A species with neither still gets free flight so the synced mode
     * always names something, even though such a dragon never leaves the ground.
     */
    public static List<FlightMode> available(boolean flies, boolean glides) {
        if (flies && glides) return List.of(FREE, GLIDE);
        if (glides) return List.of(GLIDE);
        return List.of(FREE);
    }

    /** The mode after this one in the list, wrapping around. A mode not in the list resolves to the first. */
    public FlightMode next(List<FlightMode> available) {
        int index = available.indexOf(this);
        return available.get(index < 0 ? 0 : (index + 1) % available.size());
    }

    public static FlightMode byId(String id) {
        for (FlightMode mode : values()) {
            if (mode.id.equals(id)) return mode;
        }
        return FREE;
    }

    public static FlightMode byOrdinal(int ordinal) {
        FlightMode[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : FREE;
    }
}
