package net.basilisk.heartofscales.species.stats;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** The box around a home beacon a wandering dragon stays inside, and how often it checks the beacon is still there. */
public record HomeStats(int rangeHorizontal, int rangeDown, int rangeUp, int checkIntervalTicks) {
    public static final HomeStats DEFAULT = new HomeStats(16, 3, 17, 20);

    public static final Codec<HomeStats> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("range_horizontal", DEFAULT.rangeHorizontal()).forGetter(HomeStats::rangeHorizontal),
            Codec.INT.optionalFieldOf("range_down", DEFAULT.rangeDown()).forGetter(HomeStats::rangeDown),
            Codec.INT.optionalFieldOf("range_up", DEFAULT.rangeUp()).forGetter(HomeStats::rangeUp),
            Codec.INT.optionalFieldOf("check_interval_ticks", DEFAULT.checkIntervalTicks()).forGetter(HomeStats::checkIntervalTicks)
    ).apply(instance, HomeStats::new));
}
