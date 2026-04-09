package com.adam.agri.planner.deduction;

import com.adam.agri.planner.logic.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * LCF-style proof assistant - interactive proof construction with trusted kernel.
 *
 * The Prover maintains a proof state and allows step-by-step tactic application.
 * Each step is checked for correctness by the trusted kernel before being applied.
 *
 * Mathematical basis: Edinburgh LCF (Logic for Computable Functions)
 * - Tactics decompose goals
   - Inference rules verified by kernel
 * - Proof objects constructed incrementally
 *
 * This differs from DeductionEngine in that:
 * - DeductionEngine: automated search
 * - Prover: interactive construction with full proof tracking
 */
public class Prover {
    private List<Proof> goalStack;
    private final List<Tactic> history;
    private Proof currentProof;
    private boolean proofComplete;

    /**
     * Create prover with initial goal.
     */
    public Prover(Sequent initialGoal) {
        this.goalStack = new ArrayList<>();
        this.history = new ArrayList<>();
        this.currentProof = null;
        this.proofComplete = false;

        // Initialize with a goal to prove (incomplete proof stub)
        goalStack.add(new Proof(initialGoal, Tactics.UNKNOWN, List.of(), null));
    }

    /**
     * Create prover with empty goal stack for batch mode.
     */
    public Prover() {
        this.goalStack = new ArrayList<>();
        this.history = new ArrayList<>();
        this.currentProof = null;
        this.proofComplete = false;
    }

    /**
     * Start proving a new sequent.
     */
    public void start(Sequent goal) {
        reset();
        goalStack.add(new Proof(goal, Tactics.UNKNOWN, List.of(), null));
    }

    /**
     * Apply a tactic to the current goal.
     *
     * @param tactic The tactic to apply
     * @return true if successfully applied, false otherwise
     */
    public boolean apply(Tactic tactic) {
        if (proofComplete || goalStack.isEmpty()) {
            return false;
        }

        Proof current = goalStack.get(goalStack.size() - 1);
        Sequent goal = current.getConclusion();

        // Check tactic applicability
        if (!tactic.applicable(goal)) {
            return false;
        }

        // Apply tactic
        Optional<List<Sequent>> result = tactic.apply(goal);
        if (result.isEmpty()) {
            return false;
        }

        List<Sequent> premises = result.get();
        history.add(tactic);

        if (premises.isEmpty()) {
            // Axiom reached - this subgoal is complete
            Proof completed = Proof.axiom(goal, new Unit());
            replaceCurrentGoal(completed);
            tryComplete();
            return true;
        }

        // Replace current goal with new proof node, then add subgoals
        List<Proof> subproofs = new ArrayList<>();
        for (int i = 0; i < premises.size(); i++) {
            subproofs.add(new Proof(premises.get(i), Tactics.UNKNOWN, List.of(), null));
        }

        Proof node = new Proof(goal, tactic, subproofs, new Unit());
        replaceCurrentGoal(node);

        // Push subgoals onto stack (last first for DFS)
        for (int i = premises.size() - 1; i >= 0; i--) {
            goalStack.add(subproofs.get(i));
        }

        return true;
    }

    /**
     * Apply tactic by name (for interactive use).
     */
    public boolean apply(String tacticName) {
        Tactic tactic = findTactic(tacticName);
        if (tactic == null) {
            throw new IllegalArgumentException("Unknown tactic: " + tacticName);
        }
        return apply(tactic);
    }

    /**
     * Get list of applicable tactics for current goal.
     */
    public List<Tactic> applicableTactics() {
        if (proofComplete || goalStack.isEmpty()) {
            return Collections.emptyList();
        }

        List<Tactic> result = new ArrayList<>();
        Sequent goal = goalStack.get(goalStack.size() - 1).getConclusion();

        for (Tactic t : getStandardTactics()) {
            if (t.applicable(goal)) {
                // Create specific instance for parametric tactics
                if (t.name().equals(Tactics.AXIOM.name())) {
                    result.add(Tactics.AXIOM);
                } else if (t.name().equals("intro")) {
                    result.add(Tactics.intro("x"));
                } else if (t.name().equals("split")) {
                    result.add(Tactics.split());
                } else if (t.name().equals("apply")) {
                    result.add(Tactics.apply());
                } else {
                    result.add(t);
                }
            }
        }

        return result;
    }

    /**
     * Undo last tactic application.
     */
    public boolean undo() {
        if (history.isEmpty()) {
            return false;
        }

        // Simplified undo - in full implementation would restore previous state
        // For now, just remove from history
        history.remove(history.size() - 1);
        return true;
    }

    /**
     * Check if proof is complete and valid.
     */
    public boolean isComplete() {
        return proofComplete && currentProof != null && currentProof.isValid();
    }

    /**
     * Get the completed proof.
     */
    public Optional<Proof> getProof() {
        return Optional.ofNullable(currentProof);
    }

    /**
     * Get current goal (the one being worked on).
     */
    public Optional<Sequent> currentGoal() {
        if (goalStack.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(goalStack.get(goalStack.size() - 1).getConclusion());
    }

    /**
     * Get remaining subgoals.
     */
    public List<Sequent> remainingGoals() {
        List<Sequent> goals = new ArrayList<>();
        for (Proof p : goalStack) {
            if (p.getTactic() == Tactics.UNKNOWN) {
                goals.add(p.getConclusion());
            }
        }
        return goals;
    }

    /**
     * Get tactic application history.
     */
    public List<Tactic> getHistory() {
        return Collections.unmodifiableList(history);
    }

    /**
     * Get depth of current proof tree.
     */
    public int getDepth() {
        return currentProof != null ? currentProof.depth() : 0;
    }

    /**
     * Get size of current proof (number of nodes).
     */
    public int getSize() {
        return currentProof != null ? currentProof.size() : 0;
    }

    /**
     * Get current proof as string (for debugging).
     */
    public String proofToString() {
        if (currentProof == null) {
            return "Incomplete";
        }
        return currentProof.toNaturalDeduction();
    }

    /**
     * Export proof to Coq format (placeholder).
     */
    public String exportToCoq() {
        if (currentProof == null) {
            return "(* Incomplete proof *)";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("(* Generated Coq proof *)\n");
        sb.append("Theorem goal : ").append(currentProof.getConclusion().toString()).append(".\n");
        sb.append("Proof.\n");
        for (Tactic t : history) {
            sb.append("  ").append(tacticToCoq(t)).append(".\n");
        }
        sb.append("Qed.\n");
        return sb.toString();
    }

    /**
     * Export proof to Lean format (placeholder).
     */
    public String exportToLean() {
        if (currentProof == null) {
            return "-- Incomplete proof";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("-- Generated Lean proof\n");
        sb.append("theorem goal : ").append(currentProof.getConclusion().toString()).append(" := by\n");
        for (Tactic t : history) {
            sb.append("  ").append(tacticToLean(t)).append("\n");
        }
        return sb.toString();
    }

    private void reset() {
        goalStack.clear();
        history.clear();
        currentProof = null;
        proofComplete = false;
    }

    private void replaceCurrentGoal(Proof completed) {
        if (goalStack.isEmpty()) {
            return;
        }
        goalStack.remove(goalStack.size() - 1);
        currentProof = completed;
    }

    private void tryComplete() {
        // Check if we have a valid complete proof
        if (currentProof != null && currentProof.isValid()) {
            proofComplete = true;
            goalStack.clear();
        }
    }

    private Tactic findTactic(String name) {
        for (Tactic t : getStandardTactics()) {
            if (t.name().equals(name)) {
                return t;
            }
        }
        return null;
    }

    private List<Tactic> getStandardTactics() {
        return List.of(
            Tactics.AXIOM,
            Tactics.intro("x"),
            Tactics.apply(),
            Tactics.split(),
            Tactics.assume("hyp", new Atomic("_"))
        );
    }

    private String tacticToCoq(Tactic t) {
        return switch (t.name()) {
            case "axiom" -> "exact H";
            case "intro" -> "intros";
            case "apply" -> "apply H";
            case "split" -> "split";
            default -> t.name();
        };
    }

    private String tacticToLean(Tactic t) {
        return switch (t.name()) {
            case "axiom" -> "exact h";
            case "intro" -> "intro x";
            case "apply" -> "apply h";
            case "split" -> "constructor";
            default -> t.name() + " -- ";
        };
    }
}
