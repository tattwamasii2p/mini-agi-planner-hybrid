package com.adam.agri.planner.core.action;

import com.adam.agri.planner.core.state.*;
import com.adam.agri.planner.physical.worldmodel.WorldModel;

import java.util.Set;

/**
 * Action interface with preconditions and effects.
 * Supports both symbolic and physical action representations.
 */
public interface Action {
    ActionId getId();
    String getName();

    // Precondition checking
    boolean isApplicableIn(State state);
    boolean isApplicableIn(BeliefState belief);

    // Effect computation
    State apply(SymbolicState state);
    PhysicalState apply(PhysicalState state);
    BeliefState apply(BeliefState belief);

    // Physics-based evaluation
    ActionOutcome simulate(WorldModel world, State initial);

    // Get structural components
    Set<Precondition> getPreconditions();
    Set<Effect> getEffects();
}
