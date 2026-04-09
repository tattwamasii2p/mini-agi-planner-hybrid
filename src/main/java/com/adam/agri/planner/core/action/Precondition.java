package com.adam.agri.planner.core.action;

import com.adam.agri.planner.core.state.State;

/**
 * Precondition for action applicability.
 */
@FunctionalInterface
public interface Precondition {
    boolean isSatisfiedBy(State state);

    default String getDescription() {
        return "precondition";
    }
}
