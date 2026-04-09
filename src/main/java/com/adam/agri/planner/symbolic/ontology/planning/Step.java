package com.adam.agri.planner.symbolic.ontology.planning;

import com.adam.agri.planner.symbolic.ontology.upper.Abstract;
import com.adam.agri.planner.symbolic.ontology.upper.EntityId;
import com.adam.agri.planner.symbolic.ontology.upper.Property;
import com.adam.agri.planner.symbolic.ontology.upper.TimeInterval;

import java.util.Set;

/**
 * A single step in a plan.
 */
public class Step extends Abstract {
    private final int sequenceNumber;
    private final String action;
    private final TimeInterval duration;
    private final double expectedCost;
    private final double successProbability;

    public Step(EntityId id, Set<Property> properties, int sequenceNumber,
                String action, TimeInterval duration, double expectedCost, double successProbability) {
        super(id, properties);
        this.sequenceNumber = sequenceNumber;
        this.action = action;
        this.duration = duration;
        this.expectedCost = expectedCost;
        this.successProbability = successProbability;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public String getAction() {
        return action;
    }

    public TimeInterval getDuration() {
        return duration;
    }

    public double getExpectedCost() {
        return expectedCost;
    }

    public double getSuccessProbability() {
        return successProbability;
    }

    /**
     * Check if this step can be succeeded by another step.
     */
    public boolean isSuccededBy(Step other) {
        return other != null && other.sequenceNumber == this.sequenceNumber + 1;
    }

    @Override
    public String toString() {
        return "Step[" + sequenceNumber + ": " + action + "]";
    }
}
