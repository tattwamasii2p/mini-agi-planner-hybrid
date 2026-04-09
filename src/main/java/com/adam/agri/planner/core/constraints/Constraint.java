package com.adam.agri.planner.core.constraints;

import com.adam.agri.planner.core.trajectory.Trajectory;
import com.adam.agri.planner.core.state.State;

/**
 * Constraint representation for sheaf gluing and planning.
 */
public interface Constraint {
    boolean isSatisfiedBy(Trajectory trajectory);
    boolean isSatisfiedBy(State state);

    default String getDescription() {
        return "constraint";
    }

    default ConstraintType getType() {
        return ConstraintType.GENERAL;
    }
}
