package com.adam.agri.planner.logic;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Sequent = Judgment Γ ⊢ A (Layer 12: Type Theory as Logic)
 *
 * Γ (context) = hypotheses/states
 * A (goal) = proposition/type
 *
 * Proof = Morphism in category of proofs (term witness)
 */
public record Sequent(List<Proposition> context, Proposition goal) {

    /**
     * Create sequent with empty context.
     */
    public static Sequent of(Proposition goal) {
        return new Sequent(List.of(), goal);
    }

    /**
     * Create sequent with context.
     */
    public static Sequent of(List<Proposition> context, Proposition goal) {
        return new Sequent(context, goal);
    }

    /**
     * Weakening: add hypothesis to context.
     * Rule: Γ ⊢ A → Γ, B ⊢ A
     */
    public Sequent weaken(Proposition p) {
        List<Proposition> newCtx = new ArrayList<>(context);
        newCtx.add(p);
        return new Sequent(newCtx, goal);
    }

    /**
     * Cut rule: use lemma to reduce context.
     * Rule: Γ, A ⊢ B and Γ ⊢ A → Γ ⊢ B
     */
    public Sequent cut(Proposition lemma, Proof proof) {
        if (!context.contains(lemma)) {
            throw new IllegalStateException("Lemma not in context: " + lemma);
        }
        // Γ, A ⊢ B with ⊢ A → Γ ⊢ B
        return new Sequent(
            context.stream()
                .filter(p -> !p.equals(lemma))
                .collect(Collectors.toList()),
            goal
        );
    }

    /**
     * Check if sequent is provable by axiom (goal in context).
     */
    public boolean isAxiom() {
        return context.contains(goal);
    }

    /**
     * Get context size.
     */
    public int contextSize() {
        return context.size();
    }

    @Override
    public String toString() {
        String ctx = context.isEmpty()
            ? "⊢"
            : context.stream()
                .map(Object::toString)
                .collect(Collectors.joining(", ")) + " ⊢";
        return ctx + " " + goal;
    }
}
