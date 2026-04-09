package com.adam.agri.planner.symbolic.ontology.upper;

import java.util.Set;

/**
 * Abstract entity - conceptual, no physical location.
 *
 * Examples: numbers, ideas, plans, algorithms.
 * From upper ontology (SUMO/BFO/DOLCE inspired).
 */
public abstract class Abstract extends EntityImpl {

    public Abstract(EntityId id, Set<Property> properties) {
        super(id, properties);
    }

    /**
     * Abstract entities can be instantiated in physical form.
     * Returns true if this abstract has a physical representation.
     */
    public boolean hasPhysicalRepresentation() {
        return this instanceof Representation;
    }

    /**
     * Get the physical representation if one exists.
     */
    public Representation getPhysicalRepresentation() {
        if (this instanceof Representation) {
            return (Representation) this;
        }
        return null;
    }
}
