package net.basilisk.heartofscales.entity;

/** Where a ridden dragon is in its jump. Synced, so every player sees the crouch and the leap. */
public enum JumpState {
    NONE,
    /** The rider is holding Jump on the ground and the bar is filling. */
    CHARGING,
    /** Released and in the air, until it lands. */
    JUMPING;

    public static JumpState byOrdinal(int ordinal) {
        JumpState[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : NONE;
    }
}
