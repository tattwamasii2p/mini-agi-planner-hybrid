package com.adam.agri.planner.core.constraints;

import com.adam.agri.planner.core.trajectory.Trajectory;
import com.adam.agri.planner.core.state.State;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Set of constraints for plan validation.
 */
public final class ConstraintSet {
    private final Set<Constraint> constraints;
    private final Set<Constraint> hardConstraints;
    private final Set<Constraint> softConstraints;

    public ConstraintSet() {
        this.constraints = new HashSet<>();
        this.hardConstraints = new HashSet<>();
        this.softConstraints = new HashSet<>();
    }

    public void addHard(Constraint c) {
        constraints.add(c);
        hardConstraints.add(c);
    }

    public void addSoft(Constraint c) {
        constraints.add(c);
        softConstraints.add(c);
    }

    public Set<Constraint> getHardConstraints() {
        return Collections.unmodifiableSet(hardConstraints);
    }

    public Set<Constraint> getSoftConstraints() {
        return Collections.unmodifiableSet(softConstraints);
    }

    public boolean allSatisfiedBy(Trajectory t) {
        return hardConstraints.stream().allMatch(c -> c.isSatisfiedBy(t));
    }

    public boolean allSatisfiedBy(State s) {
        return hardConstraints.stream().allMatch(c -> c.isSatisfiedBy(s));
    }

    public long countViolations(Trajectory t) {
        return hardConstraints.stream().filter(c -> !c.isSatisfiedBy(t)).count();
    }

    public double softSatisfaction(Trajectory t) {
        if (softConstraints.isEmpty()) return 1.0;
        long satisfied = softConstraints.stream().filter(c -> c.isSatisfiedBy(t)).count();
        return (double) satisfied / softConstraints.size();
    }

    public ConstraintSet copy() {
        ConstraintSet copy = new ConstraintSet();
        for (Constraint c : hardConstraints) {
            copy.addHard(c);
        }
        for (Constraint c : softConstraints) {
            copy.addSoft(c);
        }
        return copy;
    }

    public boolean isEmpty() {
        return constraints.isEmpty();
    }
}
