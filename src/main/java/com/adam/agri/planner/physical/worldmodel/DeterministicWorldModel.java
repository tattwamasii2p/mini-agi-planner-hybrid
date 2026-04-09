package com.adam.agri.planner.physical.worldmodel;

import com.adam.agri.planner.core.action.Action;
import com.adam.agri.planner.core.action.ActionOutcome;
import com.adam.agri.planner.core.state.PhysicalState;

/**
 * Simple deterministic world model.
 */
public class DeterministicWorldModel implements WorldModel {

    @Override
    public ActionOutcome simulate(PhysicalState state, Action action) {
        // Simplified: always returns current state
        return new ActionOutcome(state, 1.0, 1.0, 1.0, 0.0);
    }

    @Override
    public boolean isPhysicallyValid(PhysicalState state, Action action) {
        return true;
    }

    @Override
    public double evaluateCost(PhysicalState state, Action action) {
        return 1.0;
    }

    @Override
    public double evaluateRisk(PhysicalState state, Action action) {
        return 0.1;
    }

    @Override
    public double getTimeEstimate(PhysicalState state, Action action) {
        return 1.0;
    }

    @Override
    public boolean satisfiesConstraints(PhysicalState state) {
        return true;
    }
}
