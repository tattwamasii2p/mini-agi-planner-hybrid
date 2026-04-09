package com.adam.agri.planner.core.state;

/**
 * Base interface for all state types (symbolic and physical).
 * Implements the core state abstraction for the hybrid planner.
 */
public interface State {
    StateId getId();
    StateType getType();
    boolean isCompatible(State other);
    State copy();

    /**
     * Get timestamp of this state (for temporal ordering).
     * Default returns 0.0 for states without temporal information.
     */
    default double getTimestamp() {
        return 0.0;
    }
}
