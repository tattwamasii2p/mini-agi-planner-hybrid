package com.adam.agri.planner.verification;

import com.adam.agri.planner.logic.*;

import java.util.List;
import java.util.Objects;

/**
 * Sheaf semantics for truth evaluation.
 *
 * Truth in the sheaf model corresponds to existence of global sections.
 * A proposition is:
 * - Locally true at U if it holds in context U
 * - Globally true if it holds in all local sections and agrees on overlaps (sheaf condition)
 *
 * Mathematical basis (Layer 10):
 * - Sheaf of propositions over a topological space (state space)
 * - Global section = consistent truth assignment
 * - Sheaf condition = compatibility on overlaps
 *
 * This implements the connection between:
 * - Syntactic proofs (DeductionEngine, Prover)
 * - Semantic truth (SheafSemantics)
 */
public class SheafSemantics {

    /**
     * Check if proposition is true in all local sections (globally true).
     *
     * @param p proposition to check
     * @param model semantic model with contexts
     * @return true if globally true
     */
    public boolean isGloballyTrue(Proposition p, SemanticModel model) {
        Objects.requireNonNull(p);
        Objects.requireNonNull(model);

        // Check local truth in each section
        for (Context local : model.getContexts()) {
            if (!p.isLocallyTrue(local)) {
                return false;
            }
        }

        // Check compatibility on overlaps (sheaf condition)
        return verifyGluingCondition(p, model);
    }

    /**
     * Check if proposition is locally true in given context.
     *
     * @param p proposition
     * @param ctx local context
     * @return true if locally true
     */
    public boolean isLocallyTrue(Proposition p, Context ctx) {
        return p.isLocallyTrue(ctx);
    }

    /**
     * Verify sheaf condition: proofs agree on overlaps.
     *
     * Two local sections are compatible if they agree on the intersection.
     *
     * @param p proposition being evaluated
     * @param model semantic model
     * @return true if sheaf condition holds
     */
    public boolean verifyGluingCondition(Proposition p, SemanticModel model) {
        // Get all pairs of overlapping sections
        List<Context> sections = model.getContexts();

        for (int i = 0; i < sections.size(); i++) {
            for (int j = i + 1; j < sections.size(); j++) {
                Context section1 = sections.get(i);
                Context section2 = sections.get(j);

                // Check intersection agreement
                if (!agreeOnIntersection(p, section1, section2)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Check soundness: syntactic proof implies semantic truth.
     *
     * @param proof the proof to check
     * @return true if proof is structurally valid
     */
    public boolean checkSoundness(Proof proof) {
        // Structural validity check
        if (!proof.isValid()) {
            return false;
        }
        return true;
    }

    /**
     * Check completeness: semantic truth implies syntactic proof.
     *
     * @param p proposition that is semantically true
     * @param model the semantic model
     * @return true if provability follows
     */
    public boolean checkCompleteness(Proposition p, SemanticModel model) {
        // Heuristic: if proposition is geometric and globally true, it should be provable
        return isGloballyTrue(p, model) && isGeometric(p);
    }

    /**
     * Evaluate modal proposition □A in sheaf semantics.
     *
     * @param a inner proposition
     * @param baseCtx base context
     * @param model semantic model
     * @return true if □A holds
     */
    public boolean evaluateNecessity(Proposition a, Context baseCtx, SemanticModel model) {
        // □A true in U iff A true in all extensions of U
        for (Context ext : model.getExtensions(baseCtx)) {
            if (!a.isLocallyTrue(ext)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Evaluate modal proposition ◇A in sheaf semantics.
     *
     * @param a inner proposition
     * @param baseCtx base context
     * @param model semantic model
     * @return true if ◇A holds
     */
    public boolean evaluatePossibility(Proposition a, Context baseCtx, SemanticModel model) {
        // ◇A true in U iff exists extension V of U where A true
        for (Context ext : model.getExtensions(baseCtx)) {
            if (a.isLocallyTrue(ext)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if two contexts agree on their intersection.
     */
    private boolean agreeOnIntersection(Proposition p, Context ctx1, Context ctx2) {
        // Get common facts (intersection)
        List<Proposition> facts1 = ctx1.facts();
        List<Proposition> facts2 = ctx2.facts();

        // Check if p has same truth value in both
        boolean inCtx1 = facts1.contains(p);
        boolean inCtx2 = facts2.contains(p);

        // For compatibility, either both true or both false
        return inCtx1 == inCtx2;
    }

    /**
     * Check if proposition is geometric.
     *
     * Geometric propositions are built using finite conjunctions,
     * arbitrary disjunctions, and existential quantification.
     */
    public boolean isGeometric(Proposition p) {
        return switch (p.getType()) {
            case ATOMIC -> true;
            case CONJUNCTION -> {
                Conjunction c = (Conjunction) p;
                yield isGeometric(c.left()) && isGeometric(c.right());
            }
            case DISJUNCTION -> {
                Disjunction d = (Disjunction) p;
                yield isGeometric(d.left()) && isGeometric(d.right());
            }
            case EXISTS -> true;
            case FORALL -> false;
            case IMPLICATION -> false;
            case MODAL -> {
                Modal m = (Modal) p;
                yield switch (m.mode()) {
                    case NECESSARY -> false;
                    case POSSIBLE -> isGeometric(m.inner());
                    case BELIEF -> isGeometric(m.inner());
                };
            }
        };
    }

    /**
     * Create a semantic model from contexts.
     */
    public SemanticModel buildModel(List<Context> contexts) {
        return new SemanticModel(contexts);
    }

    /**
     * Semantic model for context evaluation.
     */
    public static class SemanticModel {
        private final List<Context> contexts;

        SemanticModel(List<Context> contexts) {
            this.contexts = List.copyOf(contexts);
        }

        /**
         * Get all contexts in the model.
         */
        public List<Context> getContexts() {
            return contexts;
        }

        /**
         * Get contexts that extend base (contain all facts of base).
         */
        public List<Context> getExtensions(Context base) {
            return contexts.stream()
                .filter(c -> c.facts().containsAll(base.facts())
                    && c.facts().size() >= base.facts().size())
                .toList();
        }
    }
}
