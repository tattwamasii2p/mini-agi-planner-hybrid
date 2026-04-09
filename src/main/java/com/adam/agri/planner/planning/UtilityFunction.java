package com.adam.agri.planner.planning;

import com.adam.agri.planner.core.state.State;

/**
 * Utility function for goal achievement.
 */
@FunctionalInterface
public interface UtilityFunction {
    double compute(State from, Goal to);

    static UtilityFunction standard() {
        return (from, to) -> 100.0; // Fixed reward
    }

    static UtilityFunction discounted(double gamma) {
        return (from, to) -> 100.0 * Math.pow(gamma, 1); // Placeholder
    }
}
