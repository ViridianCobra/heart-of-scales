package net.basilisk.heartofscales.species.stats;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Hand-feeding a wild dragon adds points per item; it is tamed once the total reaches the threshold. */
public record TamingStats(int threshold, int foodPoints, int favouriteFoodPoints) {
    public static final TamingStats DEFAULT = new TamingStats(300, 10, 30);

    public static final Codec<TamingStats> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("threshold", DEFAULT.threshold()).forGetter(TamingStats::threshold),
            Codec.INT.optionalFieldOf("food_points", DEFAULT.foodPoints()).forGetter(TamingStats::foodPoints),
            Codec.INT.optionalFieldOf("favourite_food_points", DEFAULT.favouriteFoodPoints()).forGetter(TamingStats::favouriteFoodPoints)
    ).apply(instance, TamingStats::new));
}
