package net.basilisk.heartofscales.species.stats;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Free flight and the flight AI. The JSON is one flat {@code flight} object; the codec is split into a rider group
 * and an AI group only because a record codec holds at most sixteen fields.
 */
public record FlightStats(Rider rider, Ai ai) {
    public static final FlightStats DEFAULT = new FlightStats(Rider.DEFAULT, Ai.DEFAULT);

    public static final Codec<FlightStats> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Rider.CODEC.forGetter(FlightStats::rider),
            Ai.CODEC.forGetter(FlightStats::ai)
    ).apply(instance, FlightStats::new));

    /**
     * Ridden free flight. Velocity is set straight from the rider's look rather than through vanilla air physics.
     * Roll and reverse pitch are render-only leans driven by yaw and position changes.
     */
    public record Rider(double riddenSpeedFactor, double ascendInput, double freeResponsiveness, int landingGraceTicks,
                        float rollPerYawDegree, float maxRoll, float rollSmoothing, float freeStrafeRoll, float freeReversePitch,
                        float freeCamKeyPitchRate, float freeCamBankTurnRate, float freeCamBankSmoothing,
                        float freeCamLevelRate, float freeCamReleaseTurnRate, float freeCamHeadYawLimit) {
        public static final Rider DEFAULT = new Rider(1.0, 0.8, 0.3, 10,
                8.0f, 50.0f, 0.15f, 25.0f, 20.0f,
                2.5f, 3.0f, 0.15f, 4.0f, 8.0f, 70.0f);

        static final MapCodec<Rider> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.DOUBLE.optionalFieldOf("ridden_speed_factor", DEFAULT.riddenSpeedFactor()).forGetter(Rider::riddenSpeedFactor),
                Codec.DOUBLE.optionalFieldOf("ascend_input", DEFAULT.ascendInput()).forGetter(Rider::ascendInput),
                Codec.DOUBLE.optionalFieldOf("free_responsiveness", DEFAULT.freeResponsiveness()).forGetter(Rider::freeResponsiveness),
                Codec.INT.optionalFieldOf("landing_grace_ticks", DEFAULT.landingGraceTicks()).forGetter(Rider::landingGraceTicks),
                Codec.FLOAT.optionalFieldOf("roll_per_yaw_degree", DEFAULT.rollPerYawDegree()).forGetter(Rider::rollPerYawDegree),
                Codec.FLOAT.optionalFieldOf("max_roll", DEFAULT.maxRoll()).forGetter(Rider::maxRoll),
                Codec.FLOAT.optionalFieldOf("roll_smoothing", DEFAULT.rollSmoothing()).forGetter(Rider::rollSmoothing),
                Codec.FLOAT.optionalFieldOf("free_strafe_roll", DEFAULT.freeStrafeRoll()).forGetter(Rider::freeStrafeRoll),
                Codec.FLOAT.optionalFieldOf("free_reverse_pitch", DEFAULT.freeReversePitch()).forGetter(Rider::freeReversePitch),
                Codec.FLOAT.optionalFieldOf("free_cam_key_pitch_rate", DEFAULT.freeCamKeyPitchRate()).forGetter(Rider::freeCamKeyPitchRate),
                Codec.FLOAT.optionalFieldOf("free_cam_bank_turn_rate", DEFAULT.freeCamBankTurnRate()).forGetter(Rider::freeCamBankTurnRate),
                Codec.FLOAT.optionalFieldOf("free_cam_bank_smoothing", DEFAULT.freeCamBankSmoothing()).forGetter(Rider::freeCamBankSmoothing),
                Codec.FLOAT.optionalFieldOf("free_cam_level_rate", DEFAULT.freeCamLevelRate()).forGetter(Rider::freeCamLevelRate),
                Codec.FLOAT.optionalFieldOf("free_cam_release_turn_rate", DEFAULT.freeCamReleaseTurnRate()).forGetter(Rider::freeCamReleaseTurnRate),
                Codec.FLOAT.optionalFieldOf("free_cam_head_yaw_limit", DEFAULT.freeCamHeadYawLimit()).forGetter(Rider::freeCamHeadYawLimit)
        ).apply(instance, Rider::new));
    }

    /** How the flight move control turns and how idle flights circle their centre. */
    public record Ai(double speedFactor, float maxYawTurn, float maxPitchTurn, int roamTakeoffChance, double circleMinRadius,
                     double circleMaxRadius, double ownerCircleMinRadius, double ownerCircleMaxRadius, int circleMinHeight,
                     int circleMaxHeight, int circleMinLaps, int circleMaxLaps) {
        public static final Ai DEFAULT = new Ai(0.5, 3.0f, 10.0f, 600, 16.0, 32.0, 10.0, 16.0, 8, 20, 1, 3);

        static final MapCodec<Ai> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.DOUBLE.optionalFieldOf("ai_speed_factor", DEFAULT.speedFactor()).forGetter(Ai::speedFactor),
                Codec.FLOAT.optionalFieldOf("ai_max_yaw_turn", DEFAULT.maxYawTurn()).forGetter(Ai::maxYawTurn),
                Codec.FLOAT.optionalFieldOf("ai_max_pitch_turn", DEFAULT.maxPitchTurn()).forGetter(Ai::maxPitchTurn),
                Codec.INT.optionalFieldOf("roam_takeoff_chance", DEFAULT.roamTakeoffChance()).forGetter(Ai::roamTakeoffChance),
                Codec.DOUBLE.optionalFieldOf("circle_min_radius", DEFAULT.circleMinRadius()).forGetter(Ai::circleMinRadius),
                Codec.DOUBLE.optionalFieldOf("circle_max_radius", DEFAULT.circleMaxRadius()).forGetter(Ai::circleMaxRadius),
                Codec.DOUBLE.optionalFieldOf("owner_circle_min_radius", DEFAULT.ownerCircleMinRadius()).forGetter(Ai::ownerCircleMinRadius),
                Codec.DOUBLE.optionalFieldOf("owner_circle_max_radius", DEFAULT.ownerCircleMaxRadius()).forGetter(Ai::ownerCircleMaxRadius),
                Codec.INT.optionalFieldOf("circle_min_height", DEFAULT.circleMinHeight()).forGetter(Ai::circleMinHeight),
                Codec.INT.optionalFieldOf("circle_max_height", DEFAULT.circleMaxHeight()).forGetter(Ai::circleMaxHeight),
                Codec.INT.optionalFieldOf("circle_min_laps", DEFAULT.circleMinLaps()).forGetter(Ai::circleMinLaps),
                Codec.INT.optionalFieldOf("circle_max_laps", DEFAULT.circleMaxLaps()).forGetter(Ai::circleMaxLaps)
        ).apply(instance, Ai::new));
    }

    // Flat accessors so callers read flight().roamMaxHeight() without caring about the internal split

    public double riddenSpeedFactor() { return rider.riddenSpeedFactor(); }
    public double ascendInput() { return rider.ascendInput(); }
    public double freeResponsiveness() { return rider.freeResponsiveness(); }
    public int landingGraceTicks() { return rider.landingGraceTicks(); }
    public float rollPerYawDegree() { return rider.rollPerYawDegree(); }
    public float maxRoll() { return rider.maxRoll(); }
    public float rollSmoothing() { return rider.rollSmoothing(); }
    public float freeStrafeRoll() { return rider.freeStrafeRoll(); }
    public float freeReversePitch() { return rider.freeReversePitch(); }
    public float freeCamKeyPitchRate() { return rider.freeCamKeyPitchRate(); }
    public float freeCamBankTurnRate() { return rider.freeCamBankTurnRate(); }
    public float freeCamBankSmoothing() { return rider.freeCamBankSmoothing(); }
    public float freeCamLevelRate() { return rider.freeCamLevelRate(); }
    public float freeCamReleaseTurnRate() { return rider.freeCamReleaseTurnRate(); }
    public float freeCamHeadYawLimit() { return rider.freeCamHeadYawLimit(); }
    public double aiSpeedFactor() { return ai.speedFactor(); }
    public float aiMaxYawTurn() { return ai.maxYawTurn(); }
    public float aiMaxPitchTurn() { return ai.maxPitchTurn(); }
    public int roamTakeoffChance() { return ai.roamTakeoffChance(); }
    public double circleMinRadius() { return ai.circleMinRadius(); }
    public double circleMaxRadius() { return ai.circleMaxRadius(); }
    public double ownerCircleMinRadius() { return ai.ownerCircleMinRadius(); }
    public double ownerCircleMaxRadius() { return ai.ownerCircleMaxRadius(); }
    public int circleMinHeight() { return ai.circleMinHeight(); }
    public int circleMaxHeight() { return ai.circleMaxHeight(); }
    public int circleMinLaps() { return ai.circleMinLaps(); }
    public int circleMaxLaps() { return ai.circleMaxLaps(); }
}
