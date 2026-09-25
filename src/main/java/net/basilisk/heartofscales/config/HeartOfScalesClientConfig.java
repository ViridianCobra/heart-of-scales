package net.basilisk.heartofscales.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client-only options, in run/config/heart_of_scales-client.toml. */
public final class HeartOfScalesClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue FLIGHT_CAMERA_ZOOM;
    public static final ModConfigSpec.DoubleValue FLIGHT_CAMERA_DISTANCE_MULTIPLIER;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("flightCamera");
        FLIGHT_CAMERA_ZOOM = builder
                .comment("Pull the third-person camera further back while riding a flying dragon. Turn off if another camera mod conflicts.")
                .define("enabled", true);
        FLIGHT_CAMERA_DISTANCE_MULTIPLIER = builder
                .comment("How far back, as a multiple of the vanilla third-person distance.")
                .defineInRange("distanceMultiplier", 2.5, 1.0, 10.0);
        builder.pop();
        SPEC = builder.build();
    }

    private HeartOfScalesClientConfig() {}
}
