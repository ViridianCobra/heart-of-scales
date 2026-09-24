package net.basilisk.heartofscales.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.function.IntPredicate;

/** The vertical run of water a point sits in: top water block and bottom water block, both inclusive. */
public record WaterColumn(int surfaceY, int floorY) {
    private static final int SCAN_LIMIT = 32;

    public int depth() {
        return surfaceY - floorY + 1;
    }

    public boolean isAtLeast(int blocks) {
        return depth() >= blocks;
    }

    /**
     * Walks up and down from startY while isWater holds. Empty if startY itself is not water.
     * maxUp and maxDown cap how far each direction is searched.
     */
    public static Optional<WaterColumn> scan(int startY, IntPredicate isWater, int maxUp, int maxDown) {
        if (!isWater.test(startY)) return Optional.empty();
        int surface = startY;
        while (surface - startY < maxUp && isWater.test(surface + 1)) surface++;
        int floor = startY;
        while (startY - floor < maxDown && isWater.test(floor - 1)) floor--;
        return Optional.of(new WaterColumn(surface, floor));
    }

    /** The water column through pos in the level, or empty if pos is not water. */
    public static Optional<WaterColumn> at(Level level, BlockPos pos) {
        BlockPos.MutableBlockPos cursor = pos.mutable();
        return scan(pos.getY(), y -> level.getFluidState(cursor.setY(y)).is(FluidTags.WATER), SCAN_LIMIT, SCAN_LIMIT);
    }
}
