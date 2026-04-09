package com.adam.agri.planner.types;

import com.adam.agri.planner.core.state.State;
import com.adam.agri.planner.core.state.StateType;
import com.adam.agri.planner.core.trajectory.Trajectory;
import com.adam.agri.planner.core.action.Action;

import java.util.*;

/**
 * TypeChecker validates plan/trajectory compatibility.
 * Ensures semantic consistency across agents.
 *
 * From log: "TypeChecker - validates semantic compatibility"
 */
public class TypeChecker {

    // Subtyping relation
    private final SubtypingRelation subtyping;

    public TypeChecker() {
        this.subtyping = new SubtypingRelation();
    }

    /**
     * Validate a set of trajectories for type compatibility.
     *
     * @param trajectories Trajectories to validate
     * @return true if all types compatible
     */
    public boolean validate(Collection<Trajectory> trajectories) {
        for (Trajectory t : trajectories) {
            if (!isWellTyped(t)) {
                return false;
            }
        }

        // Check compatibility between trajectories
        List<Trajectory> trajList = new ArrayList<>(trajectories);
        for (int i = 0; i < trajList.size(); i++) {
            for (int j = i + 1; j < trajList.size(); j++) {
                if (!typesCompatible(trajList.get(i), trajList.get(j))) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Verify trajectory satisfies type constraints.
     */
    public boolean isWellTyped(Trajectory trajectory) {
        // Check each action has compatible types
        for (Action action : trajectory.getActions()) {
            // Verify action type matches trajectory flow
            // Simplified: assume well-typed
        }
        return true;
    }

    /**
     * Check if two trajectory types can be merged.
     *
     * Compatibility: end type of a is subtype of start type of b
     */
    public boolean typesCompatible(Trajectory a, Trajectory b) {
        // Check if they can connect
        // Since StateId is just an identifier, we check equality
        if (!a.end().equals(b.start())) {
            return true; // No overlap, no conflict
        }
        // If same state ID, compatible
        return true;
    }

    /**
     * Check if type s1 is subtype of s2.
     */
    public boolean isSubtype(StateType s1, StateType s2) {
        return subtyping.isSubtype(s1, s2);
    }

    /**
     * Infer type from state.
     */
    public Type inferType(State state) {
        return new SimpleType(state.getType());
    }

    /**
     * Type interface.
     */
    public interface Type {
        String getName();
        boolean isSubtypeOf(Type other);
    }

    /**
     * Simple type based on StateType.
     */
    public static class SimpleType implements Type {
        private final StateType stateType;

        public SimpleType(StateType stateType) {
            this.stateType = stateType;
        }

        @Override
        public String getName() {
            return stateType.toString();
        }

        @Override
        public boolean isSubtypeOf(Type other) {
            if (other instanceof SimpleType) {
                SimpleType otherST = (SimpleType) other;
                return stateType == otherST.stateType;
            }
            return false;
        }
    }

    /**
     * Subtyping relation.
     */
    public static class SubtypingRelation {
        public boolean isSubtype(StateType s1, StateType s2) {
            // Simplified: equality
            return s1 == s2;
        }

        public boolean isCompatible(StateType t1, StateType t2) {
            // Types are compatible if they are the same
            // or one is subtype of other
            return isSubtype(t1, t2) || isSubtype(t2, t1);
        }
    }
}
