package net.basilisk.heartofscales.species.stats;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Every tunable number for a subspecies, read from the optional "stats" object of its dragon_species entry.
 * Each section and each field inside it is optional and defaults to the values the built-in dragons use.
 * docs/dragon-species-format.md has a complete example and describes every field.
 */
public record DragonStats(AttributeStats attributes, StaminaStats stamina, TamingStats taming, GroundStats ground,
                          FlightStats flight, GlideStats glide, SwimStats swim, HomeStats home) {
    public static final DragonStats DEFAULT = new DragonStats(AttributeStats.DEFAULT, StaminaStats.DEFAULT, TamingStats.DEFAULT,
            GroundStats.DEFAULT, FlightStats.DEFAULT, GlideStats.DEFAULT, SwimStats.DEFAULT, HomeStats.DEFAULT);

    public static final Codec<DragonStats> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            AttributeStats.CODEC.optionalFieldOf("attributes", AttributeStats.DEFAULT).forGetter(DragonStats::attributes),
            StaminaStats.CODEC.optionalFieldOf("stamina", StaminaStats.DEFAULT).forGetter(DragonStats::stamina),
            TamingStats.CODEC.optionalFieldOf("taming", TamingStats.DEFAULT).forGetter(DragonStats::taming),
            GroundStats.CODEC.optionalFieldOf("ground", GroundStats.DEFAULT).forGetter(DragonStats::ground),
            FlightStats.CODEC.optionalFieldOf("flight", FlightStats.DEFAULT).forGetter(DragonStats::flight),
            GlideStats.CODEC.optionalFieldOf("glide", GlideStats.DEFAULT).forGetter(DragonStats::glide),
            SwimStats.CODEC.optionalFieldOf("swim", SwimStats.DEFAULT).forGetter(DragonStats::swim),
            HomeStats.CODEC.optionalFieldOf("home", HomeStats.DEFAULT).forGetter(DragonStats::home)
    ).apply(instance, DragonStats::new));
}
