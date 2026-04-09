package com.adam.agri.planner.bridge;

import com.adam.agri.planner.core.state.PhysicalState;
import com.adam.agri.planner.core.state.SymbolicState;
import com.adam.agri.planner.core.action.Action;
import com.adam.agri.planner.core.action.SymbolicAction;
import com.adam.agri.planner.core.trajectory.Trajectory;
import com.adam.agri.planner.physical.worldmodel.WorldModel;

import java.util.List;

/**
 * Refines symbolic plans to physical trajectories.
 * Symbolic → Physical refinement.
 *
 * ρ: SymbolicPlan → Trajectory
 *
 * From log (line 4763):
 * class Refinement {
 *     Trajectory toPhysical(SymbolicPlan p);
 * }
 */
public interface Refinement {

    /**
     * Refine symbolic plan to physical trajectory.
     * ρ(p) = concrete trajectory in physical space
     *
     * @param symbolicPlan High-level symbolic plan
     * @return Physical trajectory
     */
    Trajectory toPhysical(SymbolicPlan symbolicPlan);

    /**
     * Refine symbolic action to concrete physical actions.
     *
     * @param action Symbolic action
     * @param context Current physical context
     * @return List of concrete physical actions
     */
    List<Action> toPhysicalActions(SymbolicAction action, PhysicalState context);

    /**
     * Check if symbolic plan is physically realizable.
     *
     * @param plan Symbolic plan to check
     * @param initial Initial physical state
     * @return true if physically realizable
     */
    boolean isRealizable(SymbolicPlan plan, PhysicalState initial);

    /**
     * Estimate physical feasibility.
     * Returns probability of successful execution.
     *
     * @param plan Plan to assess
     * @param world World model
     * @return Feasibility estimate
     */
    FeasibilityEstimate assessFeasibility(SymbolicPlan plan, WorldModel world);

    /**
     * Feasibility estimate with metrics.
     */
    class FeasibilityEstimate {
        private final double probability;
        private final double estimatedCost;
        private final double estimatedTime;
        private final String failureReason;

        public FeasibilityEstimate(double probability, double estimatedCost,
                                    double estimatedTime, String failureReason) {
            this.probability = probability;
            this.estimatedCost = estimatedCost;
            this.estimatedTime = estimatedTime;
            this.failureReason = failureReason;
        }

        public static FeasibilityEstimate success(double cost, double time) {
            return new FeasibilityEstimate(1.0, cost, time, null);
        }

        public static FeasibilityEstimate failure(String reason) {
            return new FeasibilityEstimate(0.0, Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY, reason);
        }

        public double getProbability() { return probability; }
        public double getEstimatedCost() { return estimatedCost; }
        public double getEstimatedTime() { return estimatedTime; }
        public String getFailureReason() { return failureReason; }
        public boolean isFeasible() { return probability > 0.5; }
    }

    /**
     * Simple symbolic plan (list of symbolic states).
     */
    class SymbolicPlan {
        private final List<SymbolicState> states;

        public SymbolicPlan(List<SymbolicState> states) {
            this.states = states;
        }

        public List<SymbolicState> getStates() { return states; }

        public SymbolicState getStart() { return states.get(0); }
        public SymbolicState getGoal() { return states.get(states.size() - 1); }

        /**
         * Approximate equality check.
         */
        public boolean approximatelyEquals(SymbolicPlan other, double tolerance) {
            if (states.size() != other.states.size()) return false;
            for (int i = 0; i < states.size(); i++) {
                if (!states.get(i).getId().equals(other.states.get(i).getId())) {
                    return false;
                }
            }
            return true;
        }
    }
}
