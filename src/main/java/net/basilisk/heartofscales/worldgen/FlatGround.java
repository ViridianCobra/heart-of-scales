package net.basilisk.heartofscales.worldgen;

import java.util.List;
import java.util.OptionalInt;

/**
 * Decides whether sampled ground heights are flat enough to build on.
 * The highest sample is the build height: nothing then pokes up through the template's air,
 * and the small dips beneath it are filled by terrain adaptation.
 */
public final class FlatGround {
    public static OptionalInt highestIfFlat(List<Integer> heights, int maxSlope) {
        if (heights.isEmpty()) return OptionalInt.empty();
        int min = heights.stream().mapToInt(Integer::intValue).min().getAsInt();
        int max = heights.stream().mapToInt(Integer::intValue).max().getAsInt();
        return max - min <= maxSlope ? OptionalInt.of(max) : OptionalInt.empty();
    }

    private FlatGround() {}
}
