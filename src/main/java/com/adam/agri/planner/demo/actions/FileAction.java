package com.adam.agri.planner.demo.actions;

import com.adam.agri.planner.core.action.*;
import com.adam.agri.planner.core.state.*;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Abstract base class for file-based actions in code generation.
 */
public abstract class FileAction implements Action {
    protected final ActionId id;
    protected final String name;
    protected final Path targetPath;
    protected final Set<Precondition> preconditions;
    protected final Set<Effect> effects;

    protected FileAction(String name, Path targetPath) {
        this.id = ActionId.generate();
        this.name = name;
        this.targetPath = targetPath;
        this.preconditions = new HashSet<>();
        this.effects = new HashSet<>();
    }

    @Override
    public ActionId getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    public Path getTargetPath() {
        return targetPath;
    }

    @Override
    public Set<Precondition> getPreconditions() {
        return preconditions;
    }

    @Override
    public Set<Effect> getEffects() {
        return effects;
    }

    protected void addPrecondition(Precondition precondition) {
        preconditions.add(precondition);
    }

    protected void addEffect(Effect effect) {
        effects.add(effect);
    }

    @Override
    public boolean isApplicableIn(State state) {
        for (Precondition pre : preconditions) {
            if (!pre.isSatisfiedBy(state)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isApplicableIn(BeliefState belief) {
        return isApplicableIn((State) belief);
    }

    @Override
    public SymbolicState apply(SymbolicState state) {
        State result = state;
        for (Effect effect : effects) {
            result = effect.apply(result);
        }
        return (SymbolicState) result;
    }

    @Override
    public PhysicalState apply(PhysicalState state) {
        throw new UnsupportedOperationException("FileAction cannot be applied to PhysicalState directly");
    }

    @Override
    public BeliefState apply(BeliefState belief) {
        throw new UnsupportedOperationException("FileAction cannot be applied to BeliefState directly");
    }

    @Override
    public ActionOutcome simulate(com.adam.agri.planner.physical.worldmodel.WorldModel world,
                                  State initial) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public String toString() {
        return name + "[" + targetPath + "]";
    }
}
