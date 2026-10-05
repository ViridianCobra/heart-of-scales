package net.basilisk.heartofscales.species.stats;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Swimming. AI swimming pushes itself by {@code accel} each tick and is slowed by {@code drag}, so it settles at
 * accel * drag / (1 - drag) blocks per tick. Ridden swimming cruises at that same speed and chases the rider's
 * wanted velocity at {@code riddenResponsiveness}. The rest drives the AI roam, rest and return-to-water behaviour.
 */
public record SwimStats(double accel, double drag, double riddenResponsiveness,
                        float aiMaxYawTurn, float aiMaxPitchTurn,
                        int roamMinTicks, int roamMaxTicks, int restMinTicks, int restMaxTicks,
                        double roamMinDistance, double roamMaxDistance,
                        int returnHorizontalRange, int returnVerticalRange) {
    public static final SwimStats DEFAULT = new SwimStats(0.06, 0.9, 0.15, 5.0f, 10.0f, 300, 600, 200, 400, 8.0, 16.0, 16, 4);

    public static final Codec<SwimStats> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.optionalFieldOf("accel", DEFAULT.accel()).forGetter(SwimStats::accel),
            Codec.DOUBLE.optionalFieldOf("drag", DEFAULT.drag()).forGetter(SwimStats::drag),
            Codec.DOUBLE.optionalFieldOf("ridden_responsiveness", DEFAULT.riddenResponsiveness()).forGetter(SwimStats::riddenResponsiveness),
            Codec.FLOAT.optionalFieldOf("ai_max_yaw_turn", DEFAULT.aiMaxYawTurn()).forGetter(SwimStats::aiMaxYawTurn),
            Codec.FLOAT.optionalFieldOf("ai_max_pitch_turn", DEFAULT.aiMaxPitchTurn()).forGetter(SwimStats::aiMaxPitchTurn),
            Codec.INT.optionalFieldOf("roam_min_ticks", DEFAULT.roamMinTicks()).forGetter(SwimStats::roamMinTicks),
            Codec.INT.optionalFieldOf("roam_max_ticks", DEFAULT.roamMaxTicks()).forGetter(SwimStats::roamMaxTicks),
            Codec.INT.optionalFieldOf("rest_min_ticks", DEFAULT.restMinTicks()).forGetter(SwimStats::restMinTicks),
            Codec.INT.optionalFieldOf("rest_max_ticks", DEFAULT.restMaxTicks()).forGetter(SwimStats::restMaxTicks),
            Codec.DOUBLE.optionalFieldOf("roam_min_distance", DEFAULT.roamMinDistance()).forGetter(SwimStats::roamMinDistance),
            Codec.DOUBLE.optionalFieldOf("roam_max_distance", DEFAULT.roamMaxDistance()).forGetter(SwimStats::roamMaxDistance),
            Codec.INT.optionalFieldOf("return_horizontal_range", DEFAULT.returnHorizontalRange()).forGetter(SwimStats::returnHorizontalRange),
            Codec.INT.optionalFieldOf("return_vertical_range", DEFAULT.returnVerticalRange()).forGetter(SwimStats::returnVerticalRange)
    ).apply(instance, SwimStats::new));

    /** The speed AI swimming settles at under its push and drag. */
    public double cruiseSpeed() {
        return accel * drag / (1 - drag);
    }
}
