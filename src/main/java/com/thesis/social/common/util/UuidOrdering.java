package com.thesis.social.common.util;

import java.util.Comparator;
import java.util.UUID;

// Java's UUID.compareTo treats the 64-bit halves as signed longs, so UUIDs whose
// high bit differs sort opposite from PostgreSQL's unsigned byte-wise UUID order.
// Always use this comparator when ordering UUIDs that must match a Postgres
// constraint or index (e.g. friendships.chk_friendships_sorted_pair).
public final class UuidOrdering {

    public static final Comparator<UUID> UNSIGNED = (a, b) -> {
        int cmp = Long.compareUnsigned(a.getMostSignificantBits(), b.getMostSignificantBits());
        return cmp != 0 ? cmp : Long.compareUnsigned(a.getLeastSignificantBits(), b.getLeastSignificantBits());
    };

    private UuidOrdering() {
    }
}
