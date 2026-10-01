package net.basilisk.heartofscales.roster;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/** Trims a sorted list so it fits a size limit, keeping the rows that sort first. */
public final class RowBudget {
    public static <T> List<T> prefixWithin(List<T> rows, ToIntFunction<T> size, int budget) {
        int used = 0;
        for (int i = 0; i < rows.size(); i++) {
            used += size.applyAsInt(rows.get(i));
            if (used > budget) return new ArrayList<>(rows.subList(0, i));
        }
        return rows;
    }

    private RowBudget() {}
}
