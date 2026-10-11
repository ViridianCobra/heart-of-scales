package net.basilisk.heartofscales.species.stats;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.basilisk.heartofscales.entity.breath.BreathType;

import java.util.Optional;

/**
 * The breath attack. {@code type} is empty unless the file names one, and then the species group decides
 * (see DragonSpecies.breathType). A stream hits everything in its cone every {@code rateTicks}; rock fires one rock every
 * {@code rateTicks}. {@code width} is how wide the breath is at full {@code range}. {@code meter} is the breath bar, which
 * follows the same rules as sprint stamina.
 */
public record BreathStats(Optional<BreathType> type, float damage, double range, double width, int rateTicks, double push,
                          StaminaStats meter) {
    public static final BreathStats DEFAULT = new BreathStats(Optional.empty(), 3.0f, 8.0, 3.0, 10, 0.8, StaminaStats.DEFAULT);

    public static final Codec<BreathStats> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BreathType.CODEC.optionalFieldOf("type").forGetter(BreathStats::type),
            Codec.FLOAT.optionalFieldOf("damage", DEFAULT.damage()).forGetter(BreathStats::damage),
            Codec.doubleRange(0.5, Double.MAX_VALUE).optionalFieldOf("range", DEFAULT.range()).forGetter(BreathStats::range),
            Codec.DOUBLE.optionalFieldOf("width", DEFAULT.width()).forGetter(BreathStats::width),
            Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("rate_ticks", DEFAULT.rateTicks()).forGetter(BreathStats::rateTicks),
            Codec.DOUBLE.optionalFieldOf("push", DEFAULT.push()).forGetter(BreathStats::push),
            StaminaStats.CODEC.optionalFieldOf("meter", DEFAULT.meter()).forGetter(BreathStats::meter)
    ).apply(instance, BreathStats::new));
}
