# Deduction & Verification Engine

This document describes the Deduction & Verification Engine for the Hybrid AGI Planner, implementing formal proof assistants and plan verification via Curry-Howard correspondence.

## Table of Contents

1. [Overview](#overview)
2. [Mathematical Foundations](#mathematical-foundations)
3. [Architecture](#architecture)
4. [Deduction Engine](#deduction-engine)
5. [Verification Engine](#verification-engine)
6. [Sheaf Semantics](#sheaf-semantics)
7. [Integration with Planning](#integration-with-planning)
8. [API Reference](#api-reference)
9. [Examples](#examples)

## Overview

The Deduction & Verification Engine transforms the Hybrid AGI Planner into an LCF-style proof assistant. Core concepts:

- **Plan = Proof**: Each trajectory represents a constructive proof
- **Action = Tactic**: Each planning step is a proof rule application
- **Goal = Proposition**: Planning targets are logical propositions
- **State = Context**: Physical/symbolic states are proof contexts

### Key Insight

> **Planning = Constructive Proof Search**
>
> Every valid plan corresponds to a proof of the goal proposition from the initial state, and the plan itself is the computational witness (Curry-Howard).

## Mathematical Foundations

### Sequent Calculus

Judgments are represented as sequents: **Γ ⊢ A**

- **Γ (Gamma)**: Context of assumptions/hypotheses
- **⊢ (Turnstile)**: Entails/proves relation
- **A**: Goal proposition to prove

```
Axiom:    A ∈ Γ → Γ ⊢ A
Intro:    Γ, A ⊢ B → Γ ⊢ A → B
Elim:     Γ ⊢ A → B, Γ ⊢ A → Γ ⊢ B
Split:    Γ ⊢ A, Γ ⊢ B → Γ ⊢ A ∧ B
```

### Curry-Howard Correspondence

| Logic | Programming |
|-------|-------------|
| Proposition | Type |
| Proof | Program (term) |
| Implication (A → B) | Function type (A → B) |
| Conjunction (A ∧ B) | Product type (A × B) |
| Disjunction (A ∨ B) | Sum type (A + B) |
| ⊥ (falsity) | Empty type |
| ⊤ (truth) | Unit type |

**Key theorem**: A proof of `P → Q` is a function of type `P → Q`.

### Modal Logic (Layer 9)

Three modalities supported:

- **□A (Necessity)**: A holds in all contexts (formal proof)
- **◇A (Possibility)**: A holds in some context (LLM belief)
- **Bel(A)** A holds with confidence weight

### Sheaf Semantics (Layer 10)

Truth is evaluated in a sheaf structure:

- **Global truth**: A is true everywhere (global section)
- **Local truth**: A is true in context U
- **Sheaf condition**: Compatible on overlaps

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                   Deduction & Verification                  │
├─────────────────────────────────────────────────────────────┤
│  Deduction Package        │  Verification Package          │
│  ─────────────────        │  ─────────────────               │
│  DeductionEngine            │  PlanVerifier                    │
│  ├─ Automated search        │  ├─ Map trajectory → proof       │
│  ├─ Depth-first             │  ├─ Validate proof structure   │
│  └─ Tactic library          │  └─ Extract witness              │
│                            │                                  │
│  Prover                     │  StateToPropositionTranslator   │
│  ├─ Interactive             │  ├─ State → Proposition         │
│  ├─ Step-by-step           │  └─ Multiple strategies          │
│  ├─ History tracking        │                                  │
│  └─ Export (Coq/Lean)       │  SheafSemantics                  │
│                            │  ├─ Global truth                 │
│  ProofSearchState           │  ├─ Local truth                  │
│  └─ Search node             │  └─ Geometric theories            │
└─────────────────────────────────────────────────────────────┘
                            │
                    ┌───────▼────────┐
                    │  Logic Layer   │
                    │  ────────────  │
                    │  Sequent       │
                    │  Proof         │
                    │  Tactic        │
                    │  Term          │
                    │  Proposition   │
                    └────────────────┘
```

## Deduction Engine

### Automated Proof Search

The `DeductionEngine` performs depth-first search through the space of tactic applications.

```java
DeductionEngine engine = new DeductionEngine()
    .withMaxDepth(50)
    .withTimeout(30000);

Sequent goal = Sequent.of(new Atomic("Goal"));
ProofSearchResult result = engine.prove(goal);

if (result.isSuccess()) {
    Proof proof = result.getProof().orElseThrow();
    System.out.println("Proof depth: " + proof.depth());
}
```

### Interactive Proving

The `Prover` class allows step-by-step proof construction:

```java
Prover prover = new Prover(Sequent.of(goal));

// Apply tactic
prover.apply("intro");  // or: prover.apply(Tactics.intro("x"))

// Check status
System.out.println("Goals remaining: " + prover.remainingGoals().size());
System.out.println("History: " + prover.getHistory());

// Export
String coqProof = prover.exportToCoq();
String leanProof = prover.exportToLean();
```

### Tactics Library

| Tactic | Rule | Effect |
|--------|------|--------|
| `axiom` | A ∈ Γ ⊢ A | Complete if goal in context |
| `intro` | →-R | Γ, A ⊢ B from Γ ⊢ A → B |
| `apply` | →-L | Use implication from context |
| `split` | ∧-R | Split A ∧ B into A, B |
| `left` | ∨-L | Choose left side of disjunction |
| `boxVerify` | □-R | Verify necessity across contexts |

### Tactic Composition

Tactics compose naturally:

```java
// Sequential: try t1, then if successful apply t2
Tactic composed = t1.then(t2);

// Choice: try t1, if fails try t2
Tactic alternative = t1.orElse(t2);
```

## Verification Engine

### Plan-as-Proof Verification

A trajectory is verified as a proof of reaching the goal:

```java
PlanVerifier verifier = new PlanVerifier();

ValidationResult result = verifier.verify(trajectory, goalState);

if (result.isValid()) {
    System.out.println("Plan is valid!");
    result.proof().ifPresent(proof -> {
        System.out.println("Witness: " + proof.extractWitness());
    });
}
```

### State Translation

The translation layer maps physical states to logical propositions:

```java
StateToPropositionTranslator translator = new StateToPropositionTranslator(
    TranslationStrategy.PREDICATE
);

Proposition p = translator.translate(state);
Implication transition = translator.translateTransition(fromState, toState);
```

### Action-to-Tactic Mapping

Planning actions are mapped to proof tactics:

```java
TrajectoryToProofMapper mapper = new TrajectoryToProofMapper();

Optional<Tactic> tactic = mapper.actionToTactic(action);
List<TacticApplication> applications = mapper.mapToTacticApplications(trajectory);
```

## Sheaf Semantics

### Truth Evaluation

```java
SheafSemantics semantics = new SheafSemantics();

// Create semantic model
List<Context> contexts = List.of(ctx1, ctx2, ctx3);
SemanticModel model = semantics.buildModel(contexts);

// Evaluate truth
boolean globallyTrue = semantics.isGloballyTrue(proposition, model);
boolean locallyTrue = semantics.isLocallyTrue(proposition, ctx1);

// Modal operators
boolean necessary = semantics.evaluateNecessity(prop, baseCtx, model);
boolean possible = semantics.evaluatePossibility(prop, baseCtx, model);
```

### Geometric Propositions

Propositions built from finite conjunctions, arbitrary disjunctions, and existentials are called **geometric**. For these, completeness holds: global truth implies provability.

```java
Proposition p = new Atomic("P");
Proposition q = new Atomic("Q");
Proposition geometric = new Conjunction(p, q);  // Geometric
Proposition nongeometric = new Implication(p, q);  // Not geometric

System.out.println("Is geometric: " + semantics.isGeometric(geometric));
```

## Integration with Planning

### VerifiedHybridPlanner

The verification engine integrates with planning:

```java
public class VerifiedHybridPlanner {
    private final DeductionEngine prover;
    private final PlanVerifier verifier;

    public Optional<Plan> plan(State start, State goal) {
        // 1. Formulate as sequent
        Sequent goalSeq = new Sequent(
            List.of(stateToProposition(start)),
            stateToProposition(goal)
        );

        // 2. Search for plan/proof
        ProofSearchResult result = prover.prove(goalSeq);

        // 3. Verify and return
        if (result.isSuccess()) {
            Trajectory traj = extractTrajectory(result.getProof());
            return Optional.of(new Plan(traj, ...));
        }

        return Optional.empty();
    }
}
```

## API Reference

### Classes

| Class | Package | Purpose |
|-------|---------|---------|
| `DeductionEngine` | `deduction` | Automated proof search |
| `Prover` | `deduction` | Interactive proof construction |
| `ProofSearchState` | `deduction` | Search node representation |
| `ProofSearchResult` | `deduction` | Search result |
| `PlanVerifier` | `verification` | Plan-as-proof validation |
| `ValidationResult` | `verification` | Verification outcome |
| `StateToPropositionTranslator` | `verification` | State translation |
| `TrajectoryToProofMapper` | `verification` | Trajectory mapping |
| `SheafSemantics` | `verification` | Semantic truth evaluation |
| `SemiringModel` | `verification` | Semantic model |

### Key Methods

**DeductionEngine**
- `prove(Sequent)` - Automated proof search
- `withMaxDepth(int)` - Set search depth
- `withTimeout(long)` - Set timeout

**Prover**
- `start(Sequent)` - Begin proof
- `apply(Tactic)` - Apply tactic
- `isComplete()` - Check completion
- `exportToCoq()` - Export to Coq
- `exportToLean()` - Export to Lean

**PlanVerifier**
- `verify(Trajectory, State)` - Verify trajectory
- `verify(Plan, State)` - Verify plan
- `extractWitness(ValidationResult)` - Get witness term

## Examples

### Example 1: Proving Implication

```java
// Prove: P → P (identity)
Proposition p = new Atomic("P");
Sequent goal = Sequent.of(new Implication(p, p));

DeductionEngine engine = new DeductionEngine();
ProofSearchResult result = engine.prove(goal);

if (result.isSuccess()) {
    System.out.println("Witness (identity function): " +
        result.getProof().get().extractWitness());
}
```

### Example 2: Conjunction Proof

```java
// Prove: P, Q ⊢ P ∧ Q
Proposition p = new Atomic("P");
Proposition q = new Atomic("Q");
Sequent goal = Sequent.of(List.of(p, q), new Conjunction(p, q));

Prover prover = new Prover(goal);
prover.apply("split");  // Splits into P and Q
prover.apply("axiom");  // Solves P
prover.apply("axiom");  // Solves Q

System.out.println("Proof complete: " + prover.isComplete());
System.out.println(prover.proofToString());
```

### Example 3: Modal Proof

```java
// Check □P (necessity)
Proposition p = new Atomic("Safe");
Modal necessary = new Modal(p, Modal.Modality.NECESSARY);

// In all contexts where Safe holds
Context ctx1 = Context.empty().extend(p);
Context ctx2 = Context.empty().extend(p);

SheafSemantics semantics = new SheafSemantics();
SemanticModel model = semantics.buildModel(List.of(ctx1, ctx2));

boolean holds = semantics.evaluateNecessity(p, ctx1, model);
System.out.println("Safe is necessary: " + holds);
```

### Example 4: Trajectory Verification

```java
// Create and verify a plan
StateId start = StateId.of("initial");
StateId end = StateId.of("goal");

Trajectory trajectory = new Trajectory(start, end, actions, metrics);
State goalState = // ...

PlanVerifier verifier = new PlanVerifier();
ValidationResult result = verifier.verify(trajectory, goalState);

if (result.isValid()) {
    System.out.println("Valid plan-as-proof!");
    result.proof().ifPresent(proof -> {
        // Extract and execute the witness
        Term witness = proof.extractWitness();
    });
}
```

## Running the Demos

```bash
# Compile
mvn compile

# Run deduction demo
java -cp target/classes com.adam.agri.planner.demo.DeductionVerificationDemo

# Run full demo
mvn package
java -jar target/agi-planner-1.0-SNAPSHOT.jar
```

## References

- **Sequent Calculus**: Gentzen, 1935
- **LCF**: Milner et al., Edinburgh, 1970s
- **Curry-Howard**: Howard, 1969; de Bruijn, 1968
- **Sheaf Semantics**: Lawvere-Tierney, 1970s
- **Modal Logic**: Kripke semantics for multimodal systems
