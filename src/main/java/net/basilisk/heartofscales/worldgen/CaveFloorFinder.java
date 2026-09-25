package net.basilisk.heartofscales.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;

/**
 * Finds cave floors in a vertical column.
 * A floor is a solid block at y with at least {@code headroom} air blocks directly above it.
 */
public final class CaveFloorFinder {
    public static List<Integer> floors(int minY, int maxY, int headroom, IntPredicate solid, IntPredicate air) {
        List<Integer> floors = new ArrayList<>();
        for (int y = minY; y <= maxY; y++) {
            if (solid.test(y) && hasHeadroom(y, headroom, air)) {
                floors.add(y);
            }
        }
        return floors;
    }

    /** True if no y in [fromY, toY] is blocked. Used to reject spots with water or lava in the way. */
    public static boolean isClear(int fromY, int toY, IntPredicate blocked) {
        for (int y = fromY; y <= toY; y++) {
            if (blocked.test(y)) return false;
        }
        return true;
    }

    private static boolean hasHeadroom(int floorY, int headroom, IntPredicate air) {
        for (int y = floorY + 1; y <= floorY + headroom; y++) {
            if (!air.test(y)) return false;
        }
        return true;
    }

    private CaveFloorFinder() {}
}
