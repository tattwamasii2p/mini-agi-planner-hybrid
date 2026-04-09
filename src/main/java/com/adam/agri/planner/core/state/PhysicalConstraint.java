package com.adam.agri.planner.core.state;

/**
 * Constraint on physical state.
 */
@FunctionalInterface
public interface PhysicalConstraint {
    boolean isSatisfiedBy(PhysicalState state);
}
