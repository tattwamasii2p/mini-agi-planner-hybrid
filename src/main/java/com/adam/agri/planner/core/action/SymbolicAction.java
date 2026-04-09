package com.adam.agri.planner.core.action;

import com.adam.agri.planner.core.state.State;
import com.adam.agri.planner.core.state.SymbolicState;

import java.util.Set;

/**
 * Concrete action with symbolic preconditions and effects.
 */
public final class SymbolicAction implements Action {
    private final ActionId id;
    private final String name;
    private final Set<Precondition> preconditions;
    private final Set<Effect> effects;

    public SymbolicAction(String name, Set<Precondition> preconditions, Set<Effect> effects) {
        this.id = ActionId.generate();
        this.name = name;
        this.preconditions = preconditions;
        this.effects = effects;
    }

    @Override
    public ActionId getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isApplicableIn(State state) {
        if (state instanceof SymbolicState) {
            for (Precondition pre : preconditions) {
                if (!pre.isSatisfiedBy(state)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean isApplicableIn(com.adam.agri.planner.core.state.BeliefState belief) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public State apply(SymbolicState state) {
        for (Effect effect : effects) {
            state = (SymbolicState) effect.apply(state);
        }
        return state;
    }

    @Override
    public com.adam.agri.planner.core.state.PhysicalState apply(com.adam.agri.planner.core.state.PhysicalState state) {
        throw new UnsupportedOperationException("Cannot apply symbolic action to physical state directly");
    }

    @Override
    public com.adam.agri.planner.core.state.BeliefState apply(com.adam.agri.planner.core.state.BeliefState belief) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public ActionOutcome simulate(com.adam.agri.planner.physical.worldmodel.WorldModel world,
                                   State initial) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Set<Precondition> getPreconditions() {
        return preconditions;
    }

    @Override
    public Set<Effect> getEffects() {
        return effects;
    }
}
