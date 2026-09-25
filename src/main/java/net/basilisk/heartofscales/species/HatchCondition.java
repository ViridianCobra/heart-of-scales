package net.basilisk.heartofscales.species;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;

/** What a nest needs before the egg in it can hatch. Checked at the nest's position. */
public enum HatchCondition implements StringRepresentable {
    DARK("dark") {
        @Override
        public boolean test(Level level, BlockPos nest) {
            return level.getMaxLocalRawBrightness(nest) <= DARK_MAX_LIGHT;
        }
    },
    /** Looser than DARK so a nest lit by nearby mutation mushrooms still qualifies. */
    DIM("dim") {
        @Override
        public boolean test(Level level, BlockPos nest) {
            return level.getMaxLocalRawBrightness(nest) <= DIM_MAX_LIGHT;
        }
    },
    OPEN_AIR("open_air") {
        @Override
        public boolean test(Level level, BlockPos nest) {
            for (int i = 1; i <= AIR_BLOCKS_ABOVE; i++) {
                if (!level.getBlockState(nest.above(i)).isAir()) return false;
            }
            return true;
        }
    },
    WATERLOGGED("waterlogged") {
        @Override
        public boolean test(Level level, BlockPos nest) {
            return level.getFluidState(nest).is(FluidTags.WATER);
        }
    };

    public static final Codec<HatchCondition> CODEC = StringRepresentable.fromEnum(HatchCondition::values);

    private static final int DARK_MAX_LIGHT = 5;
    private static final int DIM_MAX_LIGHT = 7;
    private static final int AIR_BLOCKS_ABOVE = 8;

    private final String name;

    HatchCondition(String name) {
        this.name = name;
    }

    public abstract boolean test(Level level, BlockPos nest);

    public String translationKey() {
        return "hatch.heart_of_scales.needs." + name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
