package net.basilisk.heartofscales.species;

import com.mojang.serialization.Codec;
import net.basilisk.heartofscales.entity.breath.BreathType;
import net.minecraft.util.StringRepresentable;

/** The parent species a subspecies belongs to. Dragons only breed within a group. */
public enum SpeciesGroup implements StringRepresentable {
    LAND("land", BreathType.ROCK),
    AIR("air", BreathType.AIR),
    WATER("water", BreathType.WATER);

    public static final Codec<SpeciesGroup> CODEC = StringRepresentable.fromEnum(SpeciesGroup::values);

    private final String name;
    private final BreathType defaultBreath;

    SpeciesGroup(String name, BreathType defaultBreath) {
        this.name = name;
        this.defaultBreath = defaultBreath;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    /** The breath a subspecies of this group has unless its file names another. */
    public BreathType defaultBreath() {
        return defaultBreath;
    }
}
