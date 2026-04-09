package com.adam.agri.planner.demo;

import com.adam.agri.planner.deduction.*;
import com.adam.agri.planner.logic.*;
import com.adam.agri.planner.verification.*;

import java.util.*;
import java.util.function.Function;

/**
 * Demonstration of the Deduction and Verification Engine.
 *
 * This demo showcases:
 * 1. Sequent calculus proof construction
 * 2. Tactic application (intro, apply, split, axiom)
 * 3. Automated proof search with DeductionEngine
 * 4. Interactive proof construction with Prover
 * 5. Plan verification as proof validation
 * 6. Sheaf semantics for truth
 * 7. Curry-Howard correspondence (proofs as programs)
 *
 * Mathematical Basis:
 * - Sequent Calculus: Γ ⊢ A (goals and contexts)
 * - Natural Deduction: Introduction/elimination rules
 * - LCF-style tactics: Goal decomposition
 * - Curry-Howard: Propositions as Types, Proofs as Programs
 *
 * @see DeductionEngine for automated search
 * @see Prover for interactive construction
 * @see PlanVerifier for plan-as-proof validation
 */
public class DeductionVerificationDemo {

    public static void main(String[] args) {
        System.out.println("=== Deduction & Verification Engine Demo ===\n");
        System.out.println("Mathematical basis: Sequent Calculus + Curry-Howard + Sheaf Semantics\n");

        demoSequentCalculus();
        demoNaturalDeduction();
        demoAutomatedProofSearch();
        demoInteractiveProver();
        demoModalVerification();
        demoCurryHoward();
        demoSheafSemantics();

        System.out.println("\n=== Demo Complete ===");
        System.out.println("See DEDUCTION_VERIFICATION.md for full documentation.");
    }

    /**
     * Demo 1: Basic Sequent Calculus
     * Shows construction of sequents and axiom checking.
     */
    static void demoSequentCalculus() {
        System.out.println("--- Demo 1: Sequent Calculus ---\n");
        System.out.println("Representation: Γ ⊢ A (Context entails Goal)\n");

        // Create propositions
        Proposition p = new Atomic("P");
        Proposition q = new Atomic("Q");
        Proposition pq = new Implication(p, q);

        // Create sequents
        Sequent axiom = Sequent.of(List.of(p), p);
        Sequent implication = Sequent.of(List.of(p), q);
        Sequent modusPonens = Sequent.of(List.of(pq, p), q);

        System.out.println("Sequents:");
        System.out.println("  Axiom:           " + axiom);
        System.out.println("  Implication:     " + implication);
        System.out.println("  Modus Ponens:    " + modusPonens);
        System.out.println("\nAxiom check (P in context ⊢ P): " + axiom.isAxiom());
        System.out.println("Non-axiom check: " + !implication.isAxiom());

        // Demonstrate weakening
        Sequent weakened = axiom.weaken(q);
        System.out.println("\nWeakening (add Q): " + weakened);
        System.out.println("Still axiom: " + weakened.isAxiom());

        System.out.println("\n" + "=".repeat(50) + "\n");
    }

    /**
     * Demo 2: Natural Deduction Tactics
     * Shows how tactics decompose goals.
     */
    static void demoNaturalDeduction() {
        System.out.println("--- Demo 2: Natural Deduction Tactics ---\n");
        System.out.println("Tactics: intro, apply, split, axiom\n");

        Proposition p = new Atomic("P");
        Proposition q = new Atomic("Q");
        Proposition pImpQ = new Implication(p, q);

        // intro tactic: P → Q becomes P ⊢ Q
        Tactic intro = Tactics.intro("x");
        Sequent goal1 = Sequent.of(pImpQ);

        System.out.println("Original: " + goal1);
        System.out.println("Applicable intro? " + intro.applicable(goal1));

        intro.apply(goal1).ifPresent(premises -> {
            System.out.println("After intro: " + premises.get(0));
        });

        // axiom tactic
        Sequent goal2 = Sequent.of(List.of(p), p);
        System.out.println("\nAxiom goal: " + goal2);
        System.out.println("Axiom applicable? " + Tactics.AXIOM.applicable(goal2));
        Tactics.AXIOM.apply(goal2).ifPresent(premises -> {
            System.out.println("Axiom succeeds with " + premises.size() + " premises");
        });

        // split tactic: P ∧ Q becomes [P, Q]
        Proposition pAndQ = new Conjunction(p, q);
        Tactic split = Tactics.split();
        Sequent goal3 = Sequent.of(pAndQ);

        System.out.println("\nConjunction goal: " + goal3);
        split.apply(goal3).ifPresent(premises -> {
            System.out.println("After split:");
            for (Sequent s : premises) {
                System.out.println("  - " + s);
            }
        });

        System.out.println("\n" + "=".repeat(50) + "\n");
    }

    /**
     * Demo 3: Automated Proof Search
     * Uses DeductionEngine for automated theorem proving.
     */
    static void demoAutomatedProofSearch() {
        System.out.println("--- Demo 3: Automated Proof Search ---\n");
        System.out.println("DeductionEngine: Depth-first search with tactics\n");

        DeductionEngine engine = new DeductionEngine()
            .withMaxDepth(20)
            .withTimeout(10000);

        // Simple axiom: P ⊢ P
        Proposition p = new Atomic("P");
        Sequent axiomGoal = Sequent.of(List.of(p), p);

        System.out.println("Search: " + axiomGoal);
        ProofSearchResult result1 = engine.prove(axiomGoal);
        System.out.println("Result: " + (result1.isSuccess() ? "SUCCESS" : "FAILED"));
        System.out.println("Steps explored: " +
            (result1 instanceof ProofSearchResult.Success s ? s.stepsExplored() : "N/A"));

        // Implication: [] ⊢ P → P
        Sequent impGoal = Sequent.of(new Implication(p, p));
        System.out.println("\nSearch: " + impGoal);
        ProofSearchResult result2 = engine.prove(impGoal);
        System.out.println("Result: " + (result2.isSuccess() ? "SUCCESS" : "FAILED"));
        if (result2 instanceof ProofSearchResult.Success s) {
            System.out.println("Proof depth: " + s.proof().depth());
            System.out.println("Proof size: " + s.proof().size());
        }

        // Conjunction: P, Q ⊢ P ∧ Q
        Proposition q = new Atomic("Q");
        Sequent conjGoal = Sequent.of(List.of(p, q), new Conjunction(p, q));
        System.out.println("\nSearch: " + conjGoal);
        ProofSearchResult result3 = engine.prove(conjGoal);
        System.out.println("Result: " + (result3.isSuccess() ? "SUCCESS" : "FAILED"));

        // Complex: [] ⊢ (P → Q) → ((Q → R) → (P → R))
        Proposition r = new Atomic("R");
        Proposition pImpR = new Implication(p, r);
        Proposition qImpR = new Implication(q, r);
        Proposition inner = new Implication(pImpR, pImpR);  // Simplified
        Sequent complex = Sequent.of(inner);

        System.out.println("\nSearch (complex): " + complex);
        ProofSearchResult result4 = engine.prove(complex);
        System.out.println("Result: " + (result4.isSuccess() ? "SUCCESS" : "FAILED"));

        System.out.println("\n" + "=".repeat(50) + "\n");
    }

    /**
     * Demo 4: Interactive Proof Construction
     * Uses Prover for step-by-step proof building.
     */
    static void demoInteractiveProver() {
        System.out.println("--- Demo 4: Interactive Proof Construction ---\n");
        System.out.println("Prover: LCF-style step-by-step construction\n");

        // Prove: P ∧ Q → Q ∧ P (commutativity of conjunction)
        Proposition p = new Atomic("P");
        Proposition q = new Atomic("Q");
        Proposition pAndQ = new Conjunction(p, q);
        Proposition qAndP = new Conjunction(q, p);
        Proposition commutativity = new Implication(pAndQ, qAndP);

        Prover prover = new Prover();
        prover.start(Sequent.of(commutativity));

        System.out.println("Goal: " + commutativity);
        System.out.println("Current goal: " + prover.currentGoal());
        System.out.println("Applicable tactics: " +
            prover.applicableTactics().stream().map(Tactic::name).toList());

        // Step 1: intro
        System.out.println("\nStep 1: intro");
        boolean step1 = prover.apply("intro");
        System.out.println("Result: " + (step1 ? "SUCCESS" : "FAILED"));
        prover.currentGoal().ifPresent(g -> System.out.println("New goal: " + g));

        // Step 2: split
        System.out.println("\nStep 2: split");
        List<Tactic> tactics = prover.applicableTactics();
        Tactic splitTactic = tactics.stream()
            .filter(t -> t.name().equals("split"))
            .findFirst()
            .orElse(null);

        if (splitTactic != null) {
            boolean step2 = prover.apply(splitTactic);
            System.out.println("Result: " + (step2 ? "SUCCESS" : "FAILED"));
            System.out.println("Remaining goals: " + prover.remainingGoals().size());
        }

        // Step 3: try axiom
        System.out.println("\nStep 3: Try axiom");
        boolean step3 = prover.apply(Tactics.AXIOM);
        System.out.println("Axiom result: " + (step3 ? "SUCCESS" : "FAILED"));

        System.out.println("Proof complete? " + prover.isComplete());
        System.out.println("History: " + prover.getHistory().stream().map(Tactic::name).toList());

        // Export formats
        prover.getProof().ifPresent(proof -> {
            System.out.println("\nCoq export:\n" + prover.exportToCoq());
            System.out.println("\nLean export:\n" + prover.exportToLean());
        });

        System.out.println("\n" + "=".repeat(50) + "\n");
    }

    /**
     * Demo 5: Modal Logic Verification
     * Demonstrates □ (necessity) and ◇ (possibility).
     */
    static void demoModalVerification() {
        System.out.println("--- Demo 5: Modal Logic Verification ---\n");
        System.out.println("Modal operators: □ (necessary), ◇ (possible), Belief\n");

        Proposition p = new Atomic("Safe");

        // □P - Necessary
        Modal necessary = new Modal(p, Modal.Modality.NECESSARY);
        System.out.println("Modal proposition: " + necessary);
        System.out.println("Is provable: " + necessary.isProvable());
        System.out.println("Requires proof in all contexts");

        // ◇P - Possible
        Modal possible = new Modal(p, Modal.Modality.POSSIBLE);
        System.out.println("\nModal proposition: " + possible);
        System.out.println("Is provable (trivially): " + possible.isProvable());
        System.out.println("True if exists context where P holds");

        // Belief
        Modal belief = new Modal(p, Modal.Modality.BELIEF);
        System.out.println("\nModal proposition: " + belief);
        System.out.println("Belief with confidence");

        // Create context and evaluate
        Context ctx1 = Context.empty().extend(p);
        System.out.println("\nContext evaluation:");
        System.out.println("  P locally true in ctx1: " + p.isLocallyTrue(ctx1));
        System.out.println("  □P locally true: " + necessary.isLocallyTrue(ctx1));

        // Tactic for box verification
        Tactic boxTactic = Tactics.boxVerify(Context.empty());
        Sequent boxGoal = Sequent.of(necessary);
        System.out.println("\nBox verify applicable? " + boxTactic.applicable(boxGoal));

        System.out.println("\n" + "=".repeat(50) + "\n");
    }

    /**
     * Demo 6: Curry-Howard Correspondence
     * Proofs as Programs (Types = Propositions).
     */
    static void demoCurryHoward() {
        System.out.println("--- Demo 6: Curry-Howard Correspondence ---\n");
        System.out.println("Propositions as Types, Proofs as Programs\n");

        Proposition p = new Atomic("P");
        Proposition q = new Atomic("Q");

        // Identity: P → P corresponds to λx.x
        Term identity = new Lam("x", new Var("x", p), p, p);
        System.out.println("Identity term: " + identity);
        System.out.println("Type (proposition): " + identity.type());
        System.out.println("Is normal form: " + identity.isNormal());

        // Application: (P → Q) → P → Q corresponds to function application
        Term f = new Var("f", new Implication(p, q));
        Term x = new Var("x", p);
        Term app = new App(f, x);
        System.out.println("\nApplication term: " + app);
        System.out.println("Type: " + app.type());
        System.out.println("Is normal form: " + app.isNormal());

        // Pair: P ∧ Q corresponds to (term1, term2)
        Term y = new Var("y", q);
        Term pair = new Pair(x, y, p, q);
        System.out.println("\nPair term: " + pair);
        System.out.println("Type: " + pair.type());
        System.out.println("Is normal form: " + pair.isNormal());

        // Projection
        Term proj1 = new Proj1(pair, p);
        System.out.println("\nProjection term: " + proj1);
        System.out.println("Type: " + proj1.type());

        // Substitution
        Term substituted = x.substitute("x", y);
        System.out.println("\nSubstitution: x[x := y] = " + substituted);

        // Unit term (⊤)
        Term unit = new Unit();
        System.out.println("\nUnit term: " + unit);
        System.out.println("Type (⊤): " + unit.type());

        System.out.println("\nKey insight: A proof of P → Q is a program of type P → Q");
        System.out.println("The proof term IS the computational witness!");

        System.out.println("\n" + "=".repeat(50) + "\n");
    }

    /**
     * Demo 7: Sheaf Semantics
     * Truth as global sections in a sheaf.
     */
    static void demoSheafSemantics() {
        System.out.println("--- Demo 7: Sheaf Semantics ---\n");
        System.out.println("Truth = Global section in sheaf of propositions\n");

        SheafSemantics semantics = new SheafSemantics();

        // Create contexts
        Proposition p = new Atomic("P");
        Proposition q = new Atomic("Q");

        Context ctx1 = Context.empty().extend(p).extend(q);
        Context ctx2 = Context.empty().extend(p);
        Context ctx3 = Context.empty().extend(q);

        // Build semantic model
        SheafSemantics.SemanticModel model = semantics.buildModel(List.of(ctx1, ctx2, ctx3));

        System.out.println("Contexts:");
        for (int i = 0; i < model.getContexts().size(); i++) {
            System.out.println("  Context " + i + ": " + model.getContexts().get(i).facts());
        }

        // Check local truth
        System.out.println("\nLocal truth evaluation:");
        System.out.println("  P in ctx1: " + semantics.isLocallyTrue(p, ctx1));
        System.out.println("  P in ctx2: " + semantics.isLocallyTrue(p, ctx2));
        System.out.println("  Q in ctx2: " + semantics.isLocallyTrue(q, ctx2));

        // Check global truth
        System.out.println("\nGlobal truth evaluation:");
        System.out.println("  P globally true: " + semantics.isGloballyTrue(p, model));
        System.out.println("  Q globally true: " + semantics.isGloballyTrue(q, model));

        // Check geometric
        Proposition pAndQ = new Conjunction(p, q);
        Proposition pImpQ = new Implication(p, q);
        System.out.println("\nGeometric propositions:");
        System.out.println("  P ∧ Q is geometric: " + semantics.isGeometric(pAndQ));
        System.out.println("  P → Q is geometric: " + semantics.isGeometric(pImpQ));

        // Extensions
        System.out.println("\nContext extensions:");
        List<Context> extensions = model.getExtensions(ctx2);
        System.out.println("  Contexts extending ctx2 (has P): " + extensions.size());

        // Necessity check
        System.out.println("\nModal evaluation:");
        boolean necessary = semantics.evaluateNecessity(p, ctx2, model);
        System.out.println("  □P holds at ctx2? " + necessary);
        boolean possible = semantics.evaluatePossibility(q, ctx2, model);
        System.out.println("  ◇Q holds at ctx2? " + possible);

        System.out.println("\n" + "=".repeat(50) + "\n");
    }
}
