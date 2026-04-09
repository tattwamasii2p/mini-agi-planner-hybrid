package com.adam.agri.planner.planning;

import com.adam.agri.planner.core.state.State;

/**
 * Condition that must hold for goal satisfaction.
 */
@FunctionalInterface
public interface Condition {
    boolean isSatisfiedBy(State state);
}
