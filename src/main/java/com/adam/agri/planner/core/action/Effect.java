package com.adam.agri.planner.core.action;

import com.adam.agri.planner.core.state.State;

/**
 * Effect of action execution.
 */
public interface Effect {
    State apply(State state);

    default String getDescription() {
        return "effect";
    }
}
