package com.dnd.app.util;

import java.util.UUID;

/**
 * Orders UUIDs the way PostgreSQL's {@code uuid} type does — an unsigned, byte-wise comparison — so
 * pair normalization agrees with the {@code user_a_id < user_b_id} CHECK constraint. Java's
 * {@link UUID#compareTo} is a SIGNED comparison of the two longs and disagrees for high-bit UUIDs.
 */
public final class UuidOrdering {

    private UuidOrdering() {
    }

    public static int compareUnsigned(UUID first, UUID second) {
        int high = Long.compareUnsigned(first.getMostSignificantBits(), second.getMostSignificantBits());
        if (high != 0) {
            return high;
        }
        return Long.compareUnsigned(first.getLeastSignificantBits(), second.getLeastSignificantBits());
    }

    /** @return the two ids ordered so that result[0] < result[1] in PostgreSQL uuid ordering. */
    public static UUID[] normalizedPair(UUID first, UUID second) {
        return compareUnsigned(first, second) < 0
                ? new UUID[]{first, second}
                : new UUID[]{second, first};
    }
}
