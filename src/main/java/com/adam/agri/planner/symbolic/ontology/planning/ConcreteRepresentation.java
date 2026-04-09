package com.adam.agri.planner.symbolic.ontology.planning;

import com.adam.agri.planner.symbolic.ontology.upper.*;

import java.util.Set;

/**
 * A physical representation of a Plan.
 *
 * This class links the abstract Plan entity to a physical representation
 * (e.g., written document, digital file, memory storage).
 */
public class ConcreteRepresentation extends Physical implements Representation {
    private final Abstract represents;
    private final String medium;
    private final boolean isCurrent;

    public ConcreteRepresentation(EntityId id, Set<Property> properties, Abstract represents,
                                  String medium, Location location, TimeInterval timeInterval) {
        super(id, properties, location, timeInterval);
        this.represents = represents;
        this.medium = medium;
        this.isCurrent = true;
    }

    @Override
    public Abstract getRepresents() {
        return represents;
    }

    @Override
    public String getMedium() {
        return medium;
    }

    @Override
    public boolean isCurrent() {
        return isCurrent;
    }

    /**
     * Get the Plan that this represents, if it represents a Plan.
     */
    public Plan getPlan() {
        if (represents instanceof Plan) {
            return (Plan) represents;
        }
        return null;
    }

    @Override
    public String toString() {
        return "ConcreteRepresentation[" + represents + " on " + medium + "]";
    }
}
