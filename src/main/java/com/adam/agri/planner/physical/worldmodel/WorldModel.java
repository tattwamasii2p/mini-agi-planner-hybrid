package com.adam.agri.planner.physical.worldmodel;

import com.adam.agri.planner.core.action.Action;
import com.adam.agri.planner.core.action.ActionOutcome;
import com.adam.agri.planner.core.state.PhysicalState;

/**
 * World model for physics simulation and evaluation.
 * Can be implemented as a learned neural network (like MuZero) or explicit simulation.
 */
public interface WorldModel {

    /**
     * Simulate action in current state.
     * Returns probabilistic outcome.
     */
    ActionOutcome simulate(PhysicalState state, Action action);

    /**
     * Check if action is physically valid in given state.
     */
    boolean isPhysicallyValid(PhysicalState state, Action action);

    /**
     * Evaluate cost of action in state.
     */
    double evaluateCost(PhysicalState state, Action action);

    /**
     * Evaluate risk of action.
     */
    double evaluateRisk(PhysicalState state, Action action);

    /**
     * Get time estimate for action.
     */
    double getTimeEstimate(PhysicalState state, Action action);

    /**
     * Check if state satisfies physical constraints.
     */
    boolean satisfiesConstraints(PhysicalState state);
}
