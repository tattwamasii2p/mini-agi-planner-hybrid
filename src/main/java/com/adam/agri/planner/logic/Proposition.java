package com.adam.agri.planner.logic;

/**
 * Proposition = Type (Curry-Howard correspondence)
 *
 * Represents logical propositions that can be proven or refuted.
 * Modal operators (Layer 9): □ (necessity) and ◇ (possibility)
 */
public sealed interface Proposition permits
    Atomic, Implication, Conjunction, Disjunction, Forall, Exists, Modal {

    /**
     * Path to ⊤ (true) in HoTT (Layer 8) = existence of proof.
     * Check if proposition is provable.
     */
    boolean isProvable();

    /**
     * Sheaf semantics (Layer 10): locally true / globally true.
     * Check if proposition holds in given context.
     */
    boolean isLocallyTrue(Context ctx);

    /**
     * Get the type of this proposition (for Curry-Howard).
     */
    PropositionType getType();

    enum PropositionType {
        ATOMIC, IMPLICATION, CONJUNCTION, DISJUNCTION,
        FORALL, EXISTS, MODAL
    }
}
