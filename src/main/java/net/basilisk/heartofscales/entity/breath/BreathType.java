package net.basilisk.heartofscales.entity.breath;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/** Which breath a dragon has. Plain data so it loads without vanilla's registries; Breaths maps each one to its behaviour. */
public enum BreathType implements StringRepresentable {
    ROCK("rock", 0xFFA0A0A0),
    AIR("air", 0xFFFFFFFF),
    WATER("water", 0xFF4A90FF);

    public static final Codec<BreathType> CODEC = StringRepresentable.fromEnum(BreathType::values);

    private final String name;
    private final int barColour;

    BreathType(String name, int barColour) {
        this.name = name;
        this.barColour = barColour;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    /** ARGB colour of the breath meter on the HUD. */
    public int barColour() {
        return barColour;
    }
}
