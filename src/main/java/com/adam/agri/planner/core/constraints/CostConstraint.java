package com.adam.agri.planner.core.constraints;

import com.adam.agri.planner.core.trajectory.Trajectory;
import com.adam.agri.planner.core.state.State;

/**
 * Constraint on trajectory cost.
 */
public final class CostConstraint implements Constraint {
    private final double maxCost;

    public CostConstraint(double maxCost) {
        this.maxCost = maxCost;
    }

    @Override
    public boolean isSatisfiedBy(Trajectory t) {
        return t.cost() <= maxCost;
    }

    @Override
    public boolean isSatisfiedBy(State s) {
        return true; // Only applies to trajectories
    }

    @Override
    public String getDescription() {
        return "cost <= " + maxCost;
    }

    @Override
    public ConstraintType getType() {
        return ConstraintType.COST;
    }

    public double getMaxCost() {
        return maxCost;
    }
}
