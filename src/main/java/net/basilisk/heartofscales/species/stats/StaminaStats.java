package net.basilisk.heartofscales.species.stats;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Sprint stamina. The bar holds {@code max} points; sprinting spends a full bar over {@code drainSeconds}, and it refills
 * at {@code regen} per tick after {@code regenDelayTicks} without sprinting. Once empty, sprint stays locked until
 * {@code recoveredFraction} of the bar is back.
 */
public record StaminaStats(float max, float drainSeconds, float regen, int regenDelayTicks, float recoveredFraction) {
    public static final StaminaStats DEFAULT = new StaminaStats(100.0f, 5.0f, 0.5f, 20, 0.25f);

    public static final Codec<StaminaStats> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.optionalFieldOf("max", DEFAULT.max()).forGetter(StaminaStats::max),
            Codec.FLOAT.optionalFieldOf("drain_seconds", DEFAULT.drainSeconds()).forGetter(StaminaStats::drainSeconds),
            Codec.FLOAT.optionalFieldOf("regen", DEFAULT.regen()).forGetter(StaminaStats::regen),
            Codec.INT.optionalFieldOf("regen_delay_ticks", DEFAULT.regenDelayTicks()).forGetter(StaminaStats::regenDelayTicks),
            Codec.FLOAT.optionalFieldOf("recovered_fraction", DEFAULT.recoveredFraction()).forGetter(StaminaStats::recoveredFraction)
    ).apply(instance, StaminaStats::new));

    /** Stamina spent each tick of sprinting. */
    public float drainPerTick() {
        return max / (drainSeconds * 20.0f);
    }

    /** Stamina needed before an exhausted dragon may sprint again. */
    public float recoveredThreshold() {
        return max * recoveredFraction;
    }
}
