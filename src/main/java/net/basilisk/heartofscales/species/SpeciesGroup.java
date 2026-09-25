package net.basilisk.heartofscales.species;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/** The parent species a subspecies belongs to. Dragons only breed within a group. */
public enum SpeciesGroup implements StringRepresentable {
    LAND("land"),
    AIR("air"),
    WATER("water");

    public static final Codec<SpeciesGroup> CODEC = StringRepresentable.fromEnum(SpeciesGroup::values);

    private final String name;

    SpeciesGroup(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
