package com.adam.agri.planner.planning;

import java.util.List;

import com.adam.agri.planner.core.action.Action;
import com.adam.agri.planner.core.state.StateId;
import com.adam.agri.planner.core.trajectory.Trajectory;

/**
 * Complete plan consisting of trajectory and metadata.
 */
public final class Plan {
    private final Trajectory trajectory;
    private final Goal goal;
    private final double expectedCost;
    private final double expectedUtility;
    private final double successProbability;

    public Plan(Trajectory trajectory, Goal goal,
                  double expectedCost, double expectedUtility, double successProbability) {
        this.trajectory = trajectory;
        this.goal = goal;
        this.expectedCost = expectedCost;
        this.expectedUtility = expectedUtility;
        this.successProbability = successProbability;
    }

    public Plan(Trajectory trajectory, Goal goal) {
        this(trajectory, goal, trajectory.cost(), 0, trajectory.probability());
    }

    public Plan(Trajectory trajectory) {
        this(trajectory, null, trajectory.cost(), 0, trajectory.probability());
    }

    public Trajectory toTrajectory() {
        return trajectory;
    }

    public List<Action> getActions() {
        return trajectory.getActions();
    }

    public StateId getStartState() {
        return trajectory.start();
    }

    public StateId getEndState() {
        return trajectory.end();
    }

    public Goal getGoal() {
        return goal;
    }

    public double getExpectedCost() {
        return expectedCost;
    }

    public double getExpectedUtility() {
        return expectedUtility;
    }

    public double getSuccessProbability() {
        return successProbability;
    }

    /**
     * Compute net value: utility - cost, weighted by success probability.
     */
    public double getNetValue() {
        return successProbability * expectedUtility - expectedCost;
    }

    /**
     * Get plan length in number of actions.
     */
    public int length() {
        return trajectory.getActions().size();
    }

    /**
     * Check if plan uses external tools/computers.
     */
    public boolean hasExternalActions() {
        return trajectory.getActions().stream()
            .anyMatch(a -> a instanceof ExternalAction);
    }

    @Override
    public String toString() {
        return "Plan{" +
               "length=" + length() +
               ", cost=" + expectedCost +
               ", prob=" + successProbability +
               ", value=" + getNetValue() +
               '}';
    }

    // Marker interface for actions affecting external systems
    public interface ExternalAction extends Action {
        boolean isRemote();
        boolean requiresAuthentication();
    }
}
