package net.basilisk.heartofscales.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Client-only options, in run/config/heart_of_scales-client.toml. */
public final class HeartOfScalesClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue FLIGHT_CAMERA_ZOOM;
    public static final ForgeConfigSpec.DoubleValue FLIGHT_CAMERA_DISTANCE_MULTIPLIER;
    public static final ForgeConfigSpec.BooleanValue BREATH_CAMERA;
    public static final ForgeConfigSpec.DoubleValue BREATH_CAMERA_LIFT;
    public static final ForgeConfigSpec.DoubleValue BREATH_CAMERA_EXTRA_DISTANCE;
    public static final ForgeConfigSpec.IntValue BREATH_CAMERA_EASE_IN;
    public static final ForgeConfigSpec.IntValue BREATH_CAMERA_EASE_OUT;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("flightCamera");
        FLIGHT_CAMERA_ZOOM = builder
                .comment("Pull the third-person camera further back while riding a flying dragon. Turn off if another camera mod conflicts.")
                .define("enabled", true);
        FLIGHT_CAMERA_DISTANCE_MULTIPLIER = builder
                .comment("How far back, as a multiple of the vanilla third-person distance.")
                .defineInRange("distanceMultiplier", 2.5, 1.0, 10.0);
        builder.pop();
        builder.push("breathCamera");
        BREATH_CAMERA = builder
                .comment("Lift the third-person camera over the rider and dragon while breathing, tilted so the crosshair stays on the breath's aim. Turn off if another camera mod conflicts.")
                .define("enabled", true);
        BREATH_CAMERA_LIFT = builder
                .comment("How far up the camera rises, in blocks.")
                .defineInRange("liftHeight", 1.5, 0.0, 4.0);
        BREATH_CAMERA_EXTRA_DISTANCE = builder
                .comment("How much further back the camera pulls, in blocks.")
                .defineInRange("extraDistance", 1.0, 0.0, 4.0);
        BREATH_CAMERA_EASE_IN = builder
                .comment("Ticks to rise once breathing starts.")
                .defineInRange("easeInTicks", 5, 1, 40);
        BREATH_CAMERA_EASE_OUT = builder
                .comment("Ticks to settle back once breathing stops.")
                .defineInRange("easeOutTicks", 10, 1, 40);
        builder.pop();
        SPEC = builder.build();
    }

    private HeartOfScalesClientConfig() {}
}
