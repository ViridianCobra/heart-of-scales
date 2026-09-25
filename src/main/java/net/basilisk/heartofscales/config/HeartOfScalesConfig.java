package net.basilisk.heartofscales.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class HeartOfScalesConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue HATCH_TIME_TICKS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        HATCH_TIME_TICKS = builder
                .comment("How long an egg takes to hatch in a nest, in ticks (20 ticks = 1 second). Default is 8 minutes.")
                .defineInRange("hatchTimeTicks", 9600, 1, Integer.MAX_VALUE);
        SPEC = builder.build();
    }

    private HeartOfScalesConfig() {}
}
