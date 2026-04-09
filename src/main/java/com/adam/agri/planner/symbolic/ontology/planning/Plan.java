package com.adam.agri.planner.symbolic.ontology.planning;

import com.adam.agri.planner.symbolic.ontology.upper.Abstract;
import com.adam.agri.planner.symbolic.ontology.upper.EntityId;
import com.adam.agri.planner.symbolic.ontology.upper.Location;
import com.adam.agri.planner.symbolic.ontology.upper.Property;
import com.adam.agri.planner.symbolic.ontology.upper.Representation;
import com.adam.agri.planner.symbolic.ontology.upper.TimeInterval;

import java.util.List;
import java.util.Set;

/**
 * A plan - an abstract sequence of steps to achieve a goal.
 *
 * Plans are abstract representations that can be refined into trajectories.
 */
public class Plan extends Abstract {
    private final Goal goal;
    private final List<Step> steps;
    private final double estimatedCost;
    private final double estimatedTime;
    private final boolean isExecutable;

    public Plan(EntityId id, Set<Property> properties, Goal goal,
                List<Step> steps, double estimatedCost, double estimatedTime) {
        super(id, properties);
        this.goal = goal;
        this.steps = steps;
        this.estimatedCost = estimatedCost;
        this.estimatedTime = estimatedTime;
        this.isExecutable = validateSteps();
    }

    public Goal getGoal() {
        return goal;
    }

    public List<Step> getSteps() {
        return steps;
    }

    public double getEstimatedCost() {
        return estimatedCost;
    }

    public double getEstimatedTime() {
        return estimatedTime;
    }

    public boolean isExecutable() {
        return isExecutable;
    }

    /**
     * Get number of steps in the plan.
     */
    public int getStepCount() {
        return steps != null ? steps.size() : 0;
    }

    /**
     * Check if plan is empty (no steps).
     */
    public boolean isEmpty() {
        return steps == null || steps.isEmpty();
    }

    /**
     * Validate that all steps form a coherent sequence.
     */
    private boolean validateSteps() {
        if (steps == null || steps.isEmpty()) {
            return true;
        }
        for (int i = 0; i < steps.size() - 1; i++) {
            if (!steps.get(i).isSuccededBy(steps.get(i + 1))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Create a concrete plan representation for a file.
     */
    public Representation asRepresentation(EntityId repId, Set<Property> repProps,
                                            String medium, Location location,
                                            TimeInterval timeInterval) {
        return new ConcreteRepresentation(repId, repProps, this, medium, location, timeInterval);
    }

    @Override
    public String toString() {
        return "Plan[" + goal + ": " + getStepCount() + " steps, cost=" + estimatedCost + "]";
    }
}
