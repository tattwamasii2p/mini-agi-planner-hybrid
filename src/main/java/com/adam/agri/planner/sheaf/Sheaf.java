package com.adam.agri.planner.sheaf;

import java.util.List;
import java.util.Optional;

/**
 * Sheaf interface for generic type T.
 * Represents a sheaf of sections over a topological space.
 * Mathematical basis: §heaf theory (Čech cohomology).
 */
public interface Sheaf<T> {

    /**
     * Local section of the sheaf.
     */
    interface LocalSection<T> {
        T getElement();
        boolean isExact();
        boolean isCompatibleWith(LocalSection<T> other);
        Optional<LocalSection<T>> glueWith(LocalSection<T> other);
    }

    /**
     * Compatibility pair for checking global consistency.
     */
    interface CompatibilityPair<T> {
        LocalSection<T> first();
        LocalSection<T> second();
        boolean areCompatible();
    }

    /**
     * Get all compatible pairs for consistency checking.
     */
    List<CompatibilityPair<T>> getCompatiblePairs();
}
