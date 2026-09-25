package net.basilisk.heartofscales.species.stats;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Glide mode: the heading chases the rider's look at a limited rate and speed is carried as momentum. Speeds are
 * factors of the flying speed attribute unless noted. The JSON is one flat {@code glide} object; the codec is split
 * into a speed group and a stall group only because a record codec holds at most sixteen fields.
 */
public record GlideStats(Speed speed, Stall stall) {
    public static final GlideStats DEFAULT = new GlideStats(Speed.DEFAULT, Stall.DEFAULT);

    public static final Codec<GlideStats> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Speed.CODEC.forGetter(GlideStats::speed),
            Stall.CODEC.forGetter(GlideStats::stall)
    ).apply(instance, GlideStats::new));

    /**
     * Turning, and how pitch changes speed. Inside the neutral pitch band (degrees below the horizon) speed holds;
     * nose above it loses speed, nose below gains. Sprint extra speed is in blocks per tick above the normal cap.
     */
    public record Speed(float yawRate, float pitchRate, double maxSpeedFactor, double stallSpeedFactor,
                        float neutralPitchMin, float neutralPitchMax, double diveAccel, double climbDecel,
                        double sprintExtraSpeed, double sprintAccel, double sprintExcessBleed, double strafeResponsiveness) {
        public static final Speed DEFAULT = new Speed(4.0f, 3.0f, 3.5, 0.5, 4.0f, 6.0f, 0.06, 0.042, 10.0 / 20.0, 0.025, 0.05, 0.2);

        static final MapCodec<Speed> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.FLOAT.optionalFieldOf("yaw_rate", DEFAULT.yawRate()).forGetter(Speed::yawRate),
                Codec.FLOAT.optionalFieldOf("pitch_rate", DEFAULT.pitchRate()).forGetter(Speed::pitchRate),
                Codec.DOUBLE.optionalFieldOf("max_speed_factor", DEFAULT.maxSpeedFactor()).forGetter(Speed::maxSpeedFactor),
                Codec.DOUBLE.optionalFieldOf("stall_speed_factor", DEFAULT.stallSpeedFactor()).forGetter(Speed::stallSpeedFactor),
                Codec.FLOAT.optionalFieldOf("neutral_pitch_min", DEFAULT.neutralPitchMin()).forGetter(Speed::neutralPitchMin),
                Codec.FLOAT.optionalFieldOf("neutral_pitch_max", DEFAULT.neutralPitchMax()).forGetter(Speed::neutralPitchMax),
                Codec.DOUBLE.optionalFieldOf("dive_accel", DEFAULT.diveAccel()).forGetter(Speed::diveAccel),
                Codec.DOUBLE.optionalFieldOf("climb_decel", DEFAULT.climbDecel()).forGetter(Speed::climbDecel),
                Codec.DOUBLE.optionalFieldOf("sprint_extra_speed", DEFAULT.sprintExtraSpeed()).forGetter(Speed::sprintExtraSpeed),
                Codec.DOUBLE.optionalFieldOf("sprint_accel", DEFAULT.sprintAccel()).forGetter(Speed::sprintAccel),
                Codec.DOUBLE.optionalFieldOf("sprint_excess_bleed", DEFAULT.sprintExcessBleed()).forGetter(Speed::sprintExcessBleed),
                Codec.DOUBLE.optionalFieldOf("strafe_responsiveness", DEFAULT.strafeResponsiveness()).forGetter(Speed::strafeResponsiveness)
        ).apply(instance, Speed::new));
    }

    /** Below stall speed the wings stop carrying the dragon; these govern the fall and the recovery from it. */
    public record Stall(double fallAccel, double fallMax, double fallRecovery, float pitchRate, double fallToSpeed,
                        int assistDelayTicks, float freeCamAssistPitch, float freeCamAssistRate) {
        public static final Stall DEFAULT = new Stall(0.015, 0.6, 0.8, 9.0f, 0.25, 10, 30.0f, 4.0f);

        static final MapCodec<Stall> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.DOUBLE.optionalFieldOf("stall_fall_accel", DEFAULT.fallAccel()).forGetter(Stall::fallAccel),
                Codec.DOUBLE.optionalFieldOf("stall_fall_max", DEFAULT.fallMax()).forGetter(Stall::fallMax),
                Codec.DOUBLE.optionalFieldOf("stall_fall_recovery", DEFAULT.fallRecovery()).forGetter(Stall::fallRecovery),
                Codec.FLOAT.optionalFieldOf("stall_pitch_rate", DEFAULT.pitchRate()).forGetter(Stall::pitchRate),
                Codec.DOUBLE.optionalFieldOf("stall_fall_to_speed", DEFAULT.fallToSpeed()).forGetter(Stall::fallToSpeed),
                Codec.INT.optionalFieldOf("stall_assist_delay_ticks", DEFAULT.assistDelayTicks()).forGetter(Stall::assistDelayTicks),
                Codec.FLOAT.optionalFieldOf("free_cam_stall_assist_pitch", DEFAULT.freeCamAssistPitch()).forGetter(Stall::freeCamAssistPitch),
                Codec.FLOAT.optionalFieldOf("free_cam_stall_assist_rate", DEFAULT.freeCamAssistRate()).forGetter(Stall::freeCamAssistRate)
        ).apply(instance, Stall::new));
    }

    // Flat accessors so callers read glide().maxSpeedFactor() without caring about the internal split

    public float yawRate() { return speed.yawRate(); }
    public float pitchRate() { return speed.pitchRate(); }
    public double maxSpeedFactor() { return speed.maxSpeedFactor(); }
    public double stallSpeedFactor() { return speed.stallSpeedFactor(); }
    public float neutralPitchMin() { return speed.neutralPitchMin(); }
    public float neutralPitchMax() { return speed.neutralPitchMax(); }
    public double diveAccel() { return speed.diveAccel(); }
    public double climbDecel() { return speed.climbDecel(); }
    public double sprintExtraSpeed() { return speed.sprintExtraSpeed(); }
    public double sprintAccel() { return speed.sprintAccel(); }
    public double sprintExcessBleed() { return speed.sprintExcessBleed(); }
    public double strafeResponsiveness() { return speed.strafeResponsiveness(); }
    public double stallFallAccel() { return stall.fallAccel(); }
    public double stallFallMax() { return stall.fallMax(); }
    public double stallFallRecovery() { return stall.fallRecovery(); }
    public float stallPitchRate() { return stall.pitchRate(); }
    public double stallFallToSpeed() { return stall.fallToSpeed(); }
    public int stallAssistDelayTicks() { return stall.assistDelayTicks(); }
    public float freeCamStallAssistPitch() { return stall.freeCamAssistPitch(); }
    public float freeCamStallAssistRate() { return stall.freeCamAssistRate(); }
}
