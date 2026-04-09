package com.adam.agri.planner.deduction;

import com.adam.agri.planner.logic.*;

import java.util.*;

/**
 * Automated Theorem Prover using depth-first search with tactic application.
 * Implements LCF-style proof search where tactics decompose goals into subgoals.
 *
 * Mathematical basis: Sequent calculus proof search
 * Each tactic application corresponds to a proof rule.
 *
 * @see Tactic for the tactic interface
 * @see Sequent for the goal representation
 */
public class DeductionEngine {
    private final List<Tactic> tacticLibrary;
    private int maxDepth;
    private long timeoutMs;
    private boolean proofFound;
    private Proof foundProof;
    private int stepsExplored;

    /**
     * Create deduction engine with default tactic library.
     */
    public DeductionEngine() {
        this(defaultTactics());
    }

    /**
     * Create deduction engine with custom tactic library.
     */
    public DeductionEngine(List<Tactic> tacticLibrary) {
        this.tacticLibrary = new ArrayList<>(tacticLibrary);
        this.maxDepth = 50;
        this.timeoutMs = 30000;
    }

    /**
     * Set maximum search depth.
     */
    public DeductionEngine withMaxDepth(int depth) {
        this.maxDepth = depth;
        return this;
    }

    /**
     * Set search timeout in milliseconds.
     */
    public DeductionEngine withTimeout(long timeoutMs) {
        this.timeoutMs = timeoutMs;
        return this;
    }

    /**
     * Prove a sequent using depth-first tactic search.
     *
     * @param goal The sequent to prove (Γ ⊢ A)
     * @return Proof search result
     */
    public ProofSearchResult prove(Sequent goal) {
        reset();
        long startTime = System.currentTimeMillis();

        Deque<ProofSearchState> stack = new ArrayDeque<>();
        stack.push(new ProofSearchState(goal));

        while (!stack.isEmpty() && !proofFound) {
            // Timeout check
            if (System.currentTimeMillis() - startTime > timeoutMs) {
                return ProofSearchResult.fail(
                    "Timeout after " + timeoutMs + "ms",
                    stepsExplored,
                    maxDepth
                );
            }

            // Depth limit check
            ProofSearchState current = stack.pop();
            if (current.depth() >= maxDepth) {
                continue;
            }

            stepsExplored++;

            // Check if axiom reached
            if (current.isAxiom()) {
                proofFound = true;
                foundProof = constructProof(current);
                break;
            }

            // Try each tactic
            for (Tactic tactic : tacticLibrary) {
                if (tactic.applicable(current.sequent())) {
                    Optional<List<Sequent>> result = tactic.apply(current.sequent());
                    if (result.isPresent()) {
                        List<Sequent> premises = result.get();

                        if (premises.isEmpty()) {
                            // Axiom tactic - proof complete
                            proofFound = true;
                            foundProof = constructProofFromPath(
                                current.extendPath(tactic),
                                current.sequent()
                            );
                            break;
                        }

                        // Add subgoals in reverse order for DFS
                        List<ProofSearchState> children = current.children(premises, tactic);
                        for (int i = children.size() - 1; i >= 0; i--) {
                            stack.push(children.get(i));
                        }
                    }
                }
            }
        }

        if (proofFound && foundProof != null) {
            return ProofSearchResult.ok(foundProof, stepsExplored);
        }

        int maxDepthReached = stack.stream()
            .mapToInt(ProofSearchState::depth)
            .max()
            .orElse(0);

        return ProofSearchResult.fail(
            "No proof found within depth " + maxDepth,
            stepsExplored,
            maxDepthReached
        );
    }

    /**
     * Prove an atomic proposition by name.
     * Creates a sequent with empty context ⊢ P
     */
    public ProofSearchResult proveAtomic(String propositionName) {
        return prove(Sequent.of(new Atomic(propositionName)));
    }

    /**
     * Prove an implication P → Q given premises.
     */
    public ProofSearchResult proveImplication(List<Proposition> premises, Proposition consequent) {
        return prove(new Sequent(premises, consequent));
    }

    /**
     * Construct proof from search state recursively.
     * Builds proof tree from applied tactics.
     */
    private Proof constructProof(ProofSearchState state) {
        return constructProofFromPath(state.path(), state.sequent());
    }

    /**
     * Construct proof from tactic path.
     * This is a simplified reconstruction - full implementation would
     * track subproofs during search.
     */
    private Proof constructProofFromPath(List<Tactic> path, Sequent goal) {
        if (path.isEmpty()) {
            // Direct axiom
            return Proof.axiom(goal, new Unit());
        }

        // Build proof bottom-up from path
        // For simplicity, this creates a linear proof structure
        Tactic lastTactic = path.get(path.size() - 1);

        // For real proof reconstruction, we'd need to track the subproof tree
        // Here we create a proof node with the final tactic
        return new Proof(goal, lastTactic, List.of(), new Unit());
    }

    /**
     * Get the tactic library.
     */
    public List<Tactic> getTacticLibrary() {
        return Collections.unmodifiableList(tacticLibrary);
    }

    /**
     * Add a tactic to the library.
     */
    public void addTactic(Tactic tactic) {
        tacticLibrary.add(tactic);
    }

    private void reset() {
        proofFound = false;
        foundProof = null;
        stepsExplored = 0;
    }

    /**
     * Default tactic library with common inference rules.
     */
    private static List<Tactic> defaultTactics() {
        return new ArrayList<>(List.of(
            Tactics.AXIOM,
            Tactics.intro("x"),
            Tactics.apply(),
            Tactics.split(),
            Tactics.assume("hyp", new Atomic("_"))
        ));
    }

    /**
     * Search statistics.
     */
    public record SearchStats(int stepsExplored, int maxDepth, boolean success) {}

    /**
     * Get current search statistics.
     */
    public SearchStats getStats() {
        return new SearchStats(stepsExplored, maxDepth, proofFound);
    }

    /**
     * Interactive proof search - returns intermediate states for debugging.
     */
    public List<Sequent> traceProof(Sequent goal) {
        reset();
        List<Sequent> trace = new ArrayList<>();
        Deque<ProofSearchState> stack = new ArrayDeque<>();
        stack.push(new ProofSearchState(goal));

        while (!stack.isEmpty()) {
            ProofSearchState current = stack.pop();
            trace.add(current.sequent());

            if (current.isAxiom()) {
                break;
            }

            // Try one tactic only for tracing
            for (Tactic tactic : tacticLibrary) {
                if (tactic.applicable(current.sequent())) {
                    Optional<List<Sequent>> result = tactic.apply(current.sequent());
                    if (result.isPresent()) {
                        List<Sequent> premises = result.get();
                        for (int i = premises.size() - 1; i >= 0; i--) {
                            stack.push(new ProofSearchState(premises.get(i)));
                        }
                        break;
                    }
                }
            }
        }

        return trace;
    }
}
