package com.adam.agri.planner.planning;

import com.adam.agri.planner.core.state.BeliefState;
import com.adam.agri.planner.core.state.State;
import com.adam.agri.planner.core.state.StateId;

/**
 * Goal specification for planning.
 */
public final class Goal {
    private final Condition targetCondition;
    private final UtilityFunction utility;
    private final StateId targetState;

    public Goal(Condition targetCondition, UtilityFunction utility) {
        this.targetCondition = targetCondition;
        this.utility = utility;
        this.targetState = null;
    }

    public Goal(StateId targetState, UtilityFunction utility) {
        this.targetState = targetState;
        this.utility = utility;
        this.targetCondition = null;
    }

    public Goal(StateId targetState) {
        this(targetState, (from, to) -> 100.0); // default utility
    }

    /**
     * Check if goal is satisfied by a concrete state.
     */
    public boolean isSatisfiedBy(State state) {
        if (targetState != null) {
            return state.getId().equals(targetState);
        }
        if (targetCondition != null) {
            return targetCondition.isSatisfiedBy(state);
        }
        return false;
    }

    /**
     * Check if goal is satisfied by a belief state (probabilistic).
     */
    public boolean isSatisfiedBy(BeliefState belief) {
        if (targetState != null) {
            // Check if goal state is in support with high probability
            return belief.getSupport().stream()
                .anyMatch(s -> s.getId().equals(targetState) && belief.probability(s) > 0.5);
        }
        // For condition goals, check expected satisfaction
        double expectedSatisfaction = belief.getSupport().stream()
            .mapToDouble(s -> belief.probability(s) * (
                targetCondition != null && targetCondition.isSatisfiedBy(s) ? 1.0 : 0.0
            ))
            .sum();
        return expectedSatisfaction > 0.5;
    }

    public StateId getTargetState() {
        return targetState;
    }

    public Condition getTargetCondition() {
        return targetCondition;
    }

    public UtilityFunction getUtility() {
        return utility;
    }

    /**
     * Compute utility of achieving goal from current state.
     */
    public double computeUtility(State current) {
        return utility.compute(current, this);
    }

    @Override
    public String toString() {
        if (targetState != null) {
            return "Goal{state=" + targetState + '}';
        }
        return "Goal{condition=" + targetCondition + '}';
    }
}
