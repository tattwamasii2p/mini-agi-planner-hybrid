package com.adam.agri.planner.deduction;

import com.adam.agri.planner.logic.Sequent;
import com.adam.agri.planner.logic.Tactic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * State for proof search - tracks the current sequent and path of tactics applied.
 * Used by DeductionEngine for depth-first search with backtracking.
 *
 * Mathematical correspondence: Node in proof search tree
 * where edges are tactic applications.
 */
public final class ProofSearchState {
    private final Sequent sequent;
    private final List<Tactic> path;
    private final int depth;

    /**
     * Create initial search state with empty path.
     */
    public ProofSearchState(Sequent sequent) {
        this(sequent, List.of(), 0);
    }

    /**
     * Create search state with given path.
     */
    public ProofSearchState(Sequent sequent, List<Tactic> path, int depth) {
        this.sequent = Objects.requireNonNull(sequent);
        this.path = List.copyOf(path);
        this.depth = depth;
    }

    /**
     * Get the current sequent to prove.
     */
    public Sequent sequent() {
        return sequent;
    }

    /**
     * Get the path of tactics applied so far.
     */
    public List<Tactic> path() {
        return path;
    }

    /**
     * Get search depth.
     */
    public int depth() {
        return depth;
    }

    /**
     * Check if this is an axiom state (goal in context).
     */
    public boolean isAxiom() {
        return sequent.isAxiom();
    }

    /**
     * Create child state with additional tactic in path.
     */
    public ProofSearchState child(Sequent newSequent, Tactic tactic) {
        List<Tactic> newPath = new ArrayList<>(path);
        newPath.add(tactic);
        return new ProofSearchState(newSequent, newPath, depth + 1);
    }

    /**
     * Create multiple child states from tactic application results.
     */
    public List<ProofSearchState> children(List<Sequent> premises, Tactic tactic) {
        if (premises.isEmpty()) {
            // Axiom reached
            return Collections.singletonList(
                new ProofSearchState(sequent, extendPath(tactic), depth + 1)
            );
        }

        List<ProofSearchState> result = new ArrayList<>();
        for (Sequent premise : premises) {
            result.add(new ProofSearchState(premise, extendPath(tactic), depth + 1));
        }
        return result;
    }

    public List<Tactic> extendPath(Tactic tactic) {
        List<Tactic> newPath = new ArrayList<>(path);
        newPath.add(tactic);
        return newPath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProofSearchState that = (ProofSearchState) o;
        return depth == that.depth &&
               sequent.equals(that.sequent) &&
               path.equals(that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sequent, path, depth);
    }

    @Override
    public String toString() {
        return "ProofSearchState{" +
               "depth=" + depth +
               ", sequent=" + sequent +
               ", path=" + path.stream().map(Tactic::name).toList() +
               '}';
    }
}
