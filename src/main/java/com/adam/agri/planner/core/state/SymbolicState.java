package com.adam.agri.planner.core.state;

import java.util.*;

/**
 * Symbolic state - high-level goal representation.
 * Example: "in kitchen", "holding object X"
 */
public class SymbolicState implements State {
    private final StateId id;
    private final Set<Predicate> predicates;
    private final Map<String, Object> bindings;

    public SymbolicState(StateId id, Set<Predicate> predicates, Map<String, Object> bindings) {
        this.id = id;
        this.predicates = new HashSet<>(predicates);
        this.bindings = new HashMap<>(bindings);
    }

    public SymbolicState(StateId id) {
        this(id, new HashSet<>(), new HashMap<>());
    }

    @Override
    public StateId getId() {
        return id;
    }

    @Override
    public StateType getType() {
        return StateType.SYMBOLIC;
    }

    @Override
    public boolean isCompatible(State other) {
        return other.getType() == StateType.SYMBOLIC;
    }

    @Override
    public State copy() {
        return new SymbolicState(StateId.generate(), new HashSet<>(predicates), new HashMap<>(bindings));
    }

    public Set<Predicate> getPredicates() {
        return Collections.unmodifiableSet(predicates);
    }

    public boolean hasPredicate(Predicate p) {
        return predicates.contains(p);
    }

    public SymbolicState withPredicate(Predicate p) {
        Set<Predicate> newPredicates = new HashSet<>(predicates);
        newPredicates.add(p);
        return new SymbolicState(id, newPredicates, new HashMap<>(bindings));
    }

    public SymbolicState withoutPredicate(Predicate p) {
        Set<Predicate> newPredicates = new HashSet<>(predicates);
        newPredicates.remove(p);
        return new SymbolicState(id, newPredicates, new HashMap<>(bindings));
    }

    /**
     * Check if this state entails another (satisfies all its predicates).
     */
    public boolean entails(SymbolicState other) {
        return predicates.containsAll(other.predicates);
    }

    /**
     * Project to subset of variables.
     */
    public SymbolicState project(Set<String> variables) {
        Map<String, Object> projected = new HashMap<>();
        for (String var : variables) {
            if (bindings.containsKey(var)) {
                projected.put(var, bindings.get(var));
            }
        }
        return new SymbolicState(id, predicates, projected);
    }

    /**
     * Compose with another state (union of predicates and bindings).
     */
    public SymbolicState compose(SymbolicState other) {
        Set<Predicate> composed = new HashSet<>(predicates);
        composed.addAll(other.predicates);
        Map<String, Object> merged = new HashMap<>(bindings);
        merged.putAll(other.bindings);
        return new SymbolicState(StateId.generate(), composed, merged);
    }

    @Override
    public String toString() {
        return "SymbolicState{" + "id=" + id + ", predicates=" + predicates + ", bindings=" + bindings + '}';
    }
}
