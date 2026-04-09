package com.adam.agri.planner.logic;

import java.util.List;
import java.util.Optional;

/**
 * Proof = Tree of judgments (derivation)
 * Or as Path in type space (HoTT: identity types)
 *
 * Layer 8: Homotopy Type Theory correspondence.
 */
public class Proof {
    private final Sequent conclusion;
    private final Tactic tacticApplied;
    private final List<Proof> subproofs;
    private final Term witness;

    /**
     * Create proof node.
     */
    public Proof(Sequent conclusion, Tactic tactic, List<Proof> subproofs, Term witness) {
        this.conclusion = conclusion;
        this.tacticApplied = tactic;
        this.subproofs = List.copyOf(subproofs);
        this.witness = witness;
    }

    /**
     * Create axiom (leaf node).
     * Rule: A ∈ Γ → Γ ⊢ A
     */
    public static Proof axiom(Sequent seq, Term witness) {
        return new Proof(seq, Tactics.AXIOM, List.of(), witness);
    }

    /**
     * Verification: check that proof is valid.
     * Structural check of tactic soundness.
     */
    public boolean isValid() {
        // Axiom is always valid
        if (tacticApplied == Tactics.AXIOM) {
            return conclusion.isAxiom();
        }

        if (tacticApplied == Tactics.UNKNOWN) {
            return false; // incomplete proof
        }

        // Check tactic soundness: premises ⊢ conclusion via rule
        var expectedPremises = tacticApplied.premises(conclusion);
        if (expectedPremises == null) {
            return false; // tactic not applicable
        }

        if (subproofs.size() != expectedPremises.size()) {
            return false;
        }

        for (int i = 0; i < subproofs.size(); i++) {
            Proof sub = subproofs.get(i);
            if (!sub.conclusion.equals(expectedPremises.get(i))) {
                return false; // Structure mismatch
            }
            if (!sub.isValid()) {
                return false; // Recursive check
            }
        }

        return true;
    }

    /**
     * Extract the computational content (program/witness).
     */
    public Term extractWitness() {
        return witness;
    }

    /**
     * Get conclusion sequent.
     */
    public Sequent getConclusion() {
        return conclusion;
    }

    /**
     * Get tactic applied at this node.
     */
    public Tactic getTactic() {
        return tacticApplied;
    }

    /**
     * Get subproofs (premises).
     */
    public List<Proof> getSubproofs() {
        return subproofs;
    }

    /**
     * Get proof depth.
     */
    public int depth() {
        if (subproofs.isEmpty()) return 1;
        int maxSubDepth = subproofs.stream()
            .mapToInt(Proof::depth)
            .max()
            .orElse(0);
        return 1 + maxSubDepth;
    }

    /**
     * Get proof size (number of nodes).
     */
    public int size() {
        return 1 + subproofs.stream()
            .mapToInt(Proof::size)
            .sum();
    }

    /**
     * Convertproof to natural deduction format.
     */
    public String toNaturalDeduction() {
        StringBuilder sb = new StringBuilder();
        toNDString(sb, 0);
        return sb.toString();
    }

    private void toNDString(StringBuilder sb, int indent) {
        String prefix = "  ".repeat(indent);

        if (tacticApplied == Tactics.AXIOM) {
            sb.append(prefix).append(conclusion).append(" (axiom)\n");
        } else {
            sb.append(prefix).append(conclusion)
              .append(" (").append(tacticApplied.name()).append(")\n");
            for (Proof sub : subproofs) {
                sub.toNDString(sb, indent + 1);
            }
        }
    }

    @Override
    public String toString() {
        return "Proof{" + conclusion + ", " + tacticApplied.name() +
            ", subproofs=" + subproofs.size() + "}";
    }
}

/**
 * Proof construction result.
 */
record ProofResult(boolean success, Proof proof, String error) {
    static ProofResult ok(Proof proof) {
        return new ProofResult(true, proof, null);
    }

    static ProofResult fail(String error) {
        return new ProofResult(false, null, error);
    }

    Optional<Proof> getProof() {
        return Optional.ofNullable(proof);
    }
}
