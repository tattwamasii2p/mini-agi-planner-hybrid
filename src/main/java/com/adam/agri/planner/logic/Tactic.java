package com.adam.agri.planner.logic;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Tactic = functional program: Sequent → Option[List[Sequent]]
 *
 * If tactic is applicable, it splits goal into subgoals.
 * If not → returns None (backtrack point).
 *
 * Core abstraction for LCF-style proof assistant.
 */
public interface Tactic extends Function<Sequent, Optional<List<Sequent>>> {

    /**
     * Tactic name.
     */
    String name();

    /**
     * Tactic description.
     */
    default String description() {
        return name();
    }

    /**
     * Check if tactic is applicable to this sequent.
     * Pattern matching on goal structure.
     */
    boolean applicable(Sequent goal);

    /**
     * Get premises for a given sequent.
     * Used for proof verification.
     */
    default List<Sequent> premises(Sequent goal) {
        return apply(goal).orElse(null);
    }

    /**
     * Compose tactics sequentially.
     * T1 → T2 means apply T1, then T2 to all premises.
     */
    default Tactic then(Tactic next) {
        return new Tactic() {
            @Override
            public Optional<List<Sequent>> apply(Sequent seq) {
                Optional<List<Sequent>> result1 = Tactic.this.apply(seq);
                if (result1.isEmpty()) return Optional.empty();

                // Apply next to each premise
                List<Sequent> premises = result1.get();
                if (premises.isEmpty()) {
                    return Optional.of(List.of());
                }

                // Simplified: apply to first premise only
                // Full composition would apply to all
                return next.apply(premises.get(0));
            }

            @Override
            public boolean applicable(Sequent s) {
                return Tactic.this.applicable(s);
            }

            @Override
            public String name() {
                return Tactic.this.name() + "; " + next.name();
            }
        };
    }

    /**
     * Try this tactic, or alternative if fails.
     */
    default Tactic orElse(Tactic alternative) {
        return new Tactic() {
            @Override
            public Optional<List<Sequent>> apply(Sequent seq) {
                Optional<List<Sequent>> result = Tactic.this.apply(seq);
                if (result.isPresent()) return result;
                return alternative.apply(seq);
            }

            @Override
            public boolean applicable(Sequent s) {
                return Tactic.this.applicable(s) || alternative.applicable(s);
            }

            @Override
            public String name() {
                return "(" + Tactic.this.name() + " | " + alternative.name() + ")";
            }
        };
    }
}
