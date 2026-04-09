package com.adam.agri.planner.planning;

import com.adam.agri.planner.core.state.State;
import com.adam.agri.planner.core.state.BeliefState;
import com.adam.agri.planner.physical.worldmodel.WorldModel;

import java.util.List;
import java.util.Optional;

/**
 * Base planner interface with support for different search strategies.
 */
public interface Planner {

    /**
     * Main planning method.
     * Returns a complete plan from initial state to goal.
     *
     * @param initial Initial state
     * @param goal Goal specification
     * @param context Planning constraints and preferences
     * @return Complete plan if found
     */
    Optional<Plan> plan(State initial, Goal goal, PlanningContext context);

    /**
     * Plan with belief states (POMDP support).
     *
     * @param initial Initial belief state
     * @param goal Goal specification
     * @param context Planning constraints
     * @return Complete plan if found
     */
    Optional<Plan> plan(BeliefState initial, Goal goal, PlanningContext context);

    /**
     * Generate candidate plans (for hybrid evaluation).
     *
     * @param initial Initial state
     * @param goal Goal
     * @param maxCandidates Maximum number of candidates
     * @return List of candidate plans
     */
    List<Plan> generateCandidates(State initial, Goal goal, int maxCandidates);

    /**
     * Evaluate plan in physics model.
     *
     * @param plan Plan to evaluate
     * @param world Physics world model
     * @return Evaluation result
     */
    PlanEvaluation evaluate(Plan plan, WorldModel world);

    /**
     * Get statistics from last run.
     *
     * @return Planning statistics
     */
    PlanningStats getLastRunStats();
}
