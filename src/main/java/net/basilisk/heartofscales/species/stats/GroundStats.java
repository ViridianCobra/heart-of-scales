package net.basilisk.heartofscales.species.stats;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Ridden movement shared by every mode. Sprint multiplies speed in free flight and swimming, and walk sprint on the
 * ground; strafe and reverse factors scale the rider's sideways and backward input.
 */
public record GroundStats(double sprintSpeedFactor, double walkSprintSpeedFactor, float riddenStrafeFactor, float riddenReverseFactor) {
    public static final GroundStats DEFAULT = new GroundStats(3.0, 1.5, 0.5f, 0.25f);

    public static final Codec<GroundStats> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.optionalFieldOf("sprint_speed_factor", DEFAULT.sprintSpeedFactor()).forGetter(GroundStats::sprintSpeedFactor),
            Codec.DOUBLE.optionalFieldOf("walk_sprint_speed_factor", DEFAULT.walkSprintSpeedFactor()).forGetter(GroundStats::walkSprintSpeedFactor),
            Codec.FLOAT.optionalFieldOf("ridden_strafe_factor", DEFAULT.riddenStrafeFactor()).forGetter(GroundStats::riddenStrafeFactor),
            Codec.FLOAT.optionalFieldOf("ridden_reverse_factor", DEFAULT.riddenReverseFactor()).forGetter(GroundStats::riddenReverseFactor)
    ).apply(instance, GroundStats::new));
}
