package com.adam.agri.planner.core.constraints;

import com.adam.agri.planner.core.trajectory.Trajectory;
import com.adam.agri.planner.core.state.State;

/**
 * Constraint on trajectory risk.
 */
public final class RiskConstraint implements Constraint {
    private final double maxRisk;

    public RiskConstraint(double maxRisk) {
        this.maxRisk = maxRisk;
    }

    @Override
    public boolean isSatisfiedBy(Trajectory t) {
        return t.risk() <= maxRisk;
    }

    @Override
    public boolean isSatisfiedBy(State s) {
        return true;
    }

    @Override
    public String getDescription() {
        return "risk <= " + maxRisk;
    }

    @Override
    public ConstraintType getType() {
        return ConstraintType.RISK;
    }

    public double getMaxRisk() {
        return maxRisk;
    }
}
