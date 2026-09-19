package net.basilisk.heartofscales.entity;

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

    public FlightMode next() {
        return this == FREE ? GLIDE : FREE;
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
