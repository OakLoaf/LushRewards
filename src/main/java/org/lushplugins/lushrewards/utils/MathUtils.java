package org.lushplugins.lushrewards.utils;

import org.jetbrains.annotations.Nullable;

public class MathUtils {

    public static @Nullable Integer findFirstNumInSequence(int start, int increment, int lowerBound) {
        if (increment <= 0) {
            return start > lowerBound ? start : null;
        }

        return (int) (start + increment * Math.ceil((lowerBound - start) / (double) increment));
    }

}
