package com.adam.agri.planner.symbolic.ontology.upper;

/**
 * A physical representation of an abstract entity.
 *
 * Example: a written plan (physical) representing a strategy (abstract).
 * Links the abstract and physical realms.
 */
public interface Representation {

    /**
     * Get the abstract entity that this physically represents.
     */
    Abstract getRepresents();

    /**
     * Get the medium of representation (paper, digital, memory, etc.).
     */
    String getMedium();

    /**
     * Check if this representation is current/valid.
     */
    boolean isCurrent();
}
