package net.basilisk.heartofscales.entity;

import net.minecraft.network.chat.Component;

/** What a tamed dragon has been told to do. Given with the staff. */
public enum DragonCommand {
    FOLLOW("follow"),
    SIT("sit"),
    /** Roam around the home beacon. Only valid for a dragon that has a home. */
    WANDER("wander");

    private final String id;

    DragonCommand(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public Component displayName() {
        return Component.translatable("command.heart_of_scales." + id);
    }

    public static DragonCommand byId(String id) {
        for (DragonCommand command : values()) {
            if (command.id.equals(id)) return command;
        }
        return FOLLOW;
    }

    public static DragonCommand byOrdinal(int ordinal) {
        DragonCommand[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : FOLLOW;
    }
}
