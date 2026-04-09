package com.adam.agri.planner.core.constraints;

import com.adam.agri.planner.core.trajectory.Trajectory;
import com.adam.agri.planner.core.state.State;

import java.util.concurrent.TimeUnit;

/**
 * Constraint on trajectory time.
 */
public final class TimeConstraint implements Constraint {
    private final double maxTime;
    private final TimeUnit unit;

    public TimeConstraint(double maxTime) {
        this(maxTime, TimeUnit.SECONDS);
    }

    public TimeConstraint(double maxTime, TimeUnit unit) {
        this.maxTime = maxTime;
        this.unit = unit;
    }

    @Override
    public boolean isSatisfiedBy(Trajectory t) {
        return t.time() <= maxTime;
    }

    @Override
    public boolean isSatisfiedBy(State s) {
        return true;
    }

    @Override
    public String getDescription() {
        return "time <= " + maxTime + " " + unit;
    }

    @Override
    public ConstraintType getType() {
        return ConstraintType.TIME;
    }

    public double getMaxTime() {
        return maxTime;
    }
}
