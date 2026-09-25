package net.basilisk.heartofscales.species.stats;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Base values for the vanilla attributes a subspecies starts with. */
public record AttributeStats(double maxHealth, double movementSpeed, double flyingSpeed, double attackDamage, double followRange) {
    public static final AttributeStats DEFAULT = new AttributeStats(30.0, 0.25, 0.6, 4.0, 16.0);

    public static final Codec<AttributeStats> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.optionalFieldOf("max_health", DEFAULT.maxHealth()).forGetter(AttributeStats::maxHealth),
            Codec.DOUBLE.optionalFieldOf("movement_speed", DEFAULT.movementSpeed()).forGetter(AttributeStats::movementSpeed),
            Codec.DOUBLE.optionalFieldOf("flying_speed", DEFAULT.flyingSpeed()).forGetter(AttributeStats::flyingSpeed),
            Codec.DOUBLE.optionalFieldOf("attack_damage", DEFAULT.attackDamage()).forGetter(AttributeStats::attackDamage),
            Codec.DOUBLE.optionalFieldOf("follow_range", DEFAULT.followRange()).forGetter(AttributeStats::followRange)
    ).apply(instance, AttributeStats::new));
}
