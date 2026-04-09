# LLM Reasoning Bridge Documentation

## Overview

The `symbolic.reasoning.llm` package implements LLM as a **probabilistic functor** between text space and formal ontology:

```
L: Text → Ont
```

This is a **lax functor** - it approximates but doesn't strictly preserve categorical structure. LLM provides fast, intuitive reasoning (System 1) while formal verification provides rigorous, slow reasoning (System 2).

## Mathematical Foundation

### Neural Space ℝ^d

LLM embeddings live in high-dimensional space with operations:

- **Cosine similarity**: `(u·v) / (||u|| ||v||)` ∈ [-1, 1]
- **Confidence**: `1 / (1 + entropy)` ∈ (0, 1]
- **K-nearest neighbors**: semantic retrieval
- **Centroid**: `average(vectors)` for merging

```java
NeuralSpace space = new NeuralSpace(1536); // Claude embedding dimension
double[] u = llm.embed("Plan");
double[] v = llm.embed("Strategy");
double sim = space.cosineSimilarity(u, v); // ~0.85
```

### Modal Logic (Layer 9)

Dual operators formalize belief vs knowledge:

| Operator | Symbol | Meaning | Implementation |
|----------|--------|---------|----------------|
| Possibility | ◇φ | LLM believes φ plausible | `confidence > 0.7` |
| Necessity | □φ | φ has formal proof | `prover.prove(φ).isPresent()` |

**Key axioms:**
- `□φ → ◇φ` (necessity implies possibility)
- `◇φ ∧ (φ→ψ) → ◇ψ` (modus ponens for belief)
- `¬◇¬φ ↔ □φ` (duality)

```java
ModalOperators modal = new ModalOperators(llm);
boolean possible = modal.possibly(goalProposition);
boolean necessary = modal.necessarily(goalProposition, formalProver);
```

### Functor Properties

LLM as approximate functor has **composition loss:**

```
‖embed(traverse(a,b)) - compose(embed(a), embed(b))‖ < ε
```

Category-aware training minimizes this loss:

```java
LLMReasoningBridge bridge = new LLMReasoningBridge(llm, 1536);
double loss = bridge.compositionLoss(trajectory);
// Train to minimize: L_total = λ₁L_comp + λ₂L_sheaf + λ₃L_task
```

## Architecture

### Layer Integration

| Layer | Component | LLM Role |
|-------|-----------|----------|
| 1-2 | Discrete types | Tokenization → embeddings ℝ^d |
| 4 | Physics | Neural dynamics + analytical fusion |
| 7 | Planning | MCTS warm start with seeds |
| 8 | HoTT Paths | Chain-of-Thought → proof paths |
| 9 | Modal Logic | ◇ (belief) vs □ (knowledge) |
| 10 | Sheaf | Probabilistic local sections |
| 12 | Proofs | LLM guides, prover verifies |

### Core Classes

**LLMReasoningBridge** - Main entry point:

```java
// Initialize
LlmBackend llm = new ClaudeBackend(); // or OllamaBackend
LLMReasoningBridge bridge = new LLMReasoningBridge(llm, 1536);

// Layer 7: MCTS warm start
List<Trajectory<State>> seeds = bridge.suggestTrajectories(
    currentState, targetGoal, 5
);

// Layer 10: Neural sheaf section
ProbabilisticSection<JavaType> section = bridge.approximateSection(
    "function taking int to string",
    List.of(FunctionType.class)
);

// Layer 12: Proof synthesis with guidance
ProofAttempt attempt = bridge.proveWithGuidance(
    theorem,
    maxDepth
);
if (attempt.valid()) {
    // Success
} else {
    // LLM hallucinated - backtrack or retrain
}

// Layer 4: Hybrid dynamics
HybridVectorField<State> field = bridge.approximateDynamics(
    "ball falling under gravity with friction",
    analyticalPhysics
);
// field.apply(state, control) returns α·neural + (1-α)·analytical
```

**NeuralSheafSection** - Probabilistic local knowledge:

```java
NeuralSheafSection<Type> section = new NeuralSheafSection<>(
    nearestType,
    embedding,
    neuralSpace
);

// Modal properties
double confidence = section.getConfidence(); // P_LLM(valid)
boolean possible = section.isPossible();      // ◇: confidence > 0.7
boolean probable = section.isProbable();      // confidence > 0.9

// Sheaf operations
boolean compat = section.isCompatibleWith(otherSection);
Optional<Sheaf.LocalSection<Type>> merged = section.glueWith(other);
```

**ModalOperators** - Formalizing belief:

```java
ModalOperators modal = new ModalOperators(llm);

// Check if proposition is possible (LLM belief)
boolean maybe = modal.possibly(
    new ModalOperators.Proposition<>() {
        public boolean evaluate(Context ctx) { ... }
    }
);

// Check if formally proven (knowledge)
boolean definitely = modal.necessarily(
    proposition,
    formalProver
);

// Get complete signature
ModalSignature sig = modal.analyze(proposition, prover);
// sig.possible(), sig.necessary(), sig.confidence(), sig.formalProof()
```

## Backend Configuration

### Claude (Cloud)

```java
LlmBackend llm = new ClaudeBackend(
    System.getenv("ANTHROPIC_API_KEY"),
    "claude-3-sonnet-20240229",
    1536  // embedding dimension
);
```

### Ollama (Local)

```java
LlmBackend llm = new OllamaBackend(
    "http://localhost:11434",
    "llama2",
    4096
);

// Check availability
if (llm.isAvailable()) {
    String response = llm.complete("Plan a route from A to B");
}
```

## Hybrid Planning

**LLMHybridPlanner** combines neural and formal search:

```java
LLMHybridPlanner<State> planner = new LLMHybridPlanner<>(
    config,
    weights,
    bridge,     // LLM reasoning
    5,          // 5 seed trajectories
    true,       // use seeds as prior
    true        // verify seeds formally
);

// Plan with LLM assistance
Optional<Trajectory<State>> result = planner.planWithSeeds(
    startState,
    goalState,
    1000  // iterations
);
```

**Algorithm:**
1. LLM suggests candidate trajectories (fast, heuristic)
2. Validate seeds (transition ◇ → □)
3. Inject valid seeds into MCTS initial population
4. Formal planner refines (rigorous search)
5. Return best verified trajectory

## Category-Aware Training

Train LLM to respect categorical structure:

```java
CategoryAwareTraining trainer = new CategoryAwareTraining(bridge, 1536);

// Composition loss: embed(traverse) ≈ compose(embeddings)
double compLoss = trainer.compositionalLoss(aToBTrajectory);

// Sheaf loss: compatible contexts → close embeddings
double sheafLoss = trainer.sheafLoss(knowledgeSheaf);

// Total loss with weighting
double total = trainer.totalLoss(trajectory, sheaf);
// L = 0.5·L_comp + 0.3·L_sheaf + 0.2·L_task

// Training epoch
trainer.trainEpoch(sheaf, trajectories);
```

## Chain of Thought

**Reasoning as approximate proof paths:**

```java
// Generate explicit reasoning
List<Either<Morphism, FuzzyRule>> steps = planner.reasonExplicitly(
    "How to achieve goal G from state S?",
    currentContext
);

// Check sheaf compatibility
boolean compatible = planner.isSheafCompatible(steps);
```

## Confidence Calibration

**Entropy-based confidence:**

```java
NeuralSpace space = new NeuralSpace(dim);
double[] logits = llm.getLogits(text);
double entropy = space.entropy(logits);
double confidence = 1.0 / (1.0 + entropy);

// Interpretation:
// confidence > 0.9: High certainty (□)
// confidence > 0.7: Possible (◇)
// confidence < 0.5: Uncertain, needs verification
```

## Common Patterns

### Modal Seed Validation

```java
// Is LLM seed merely possible or formally necessary?
boolean isNecessary = planner.isNecessarySeed(seed, formalProver);
if (!isNecessary) {
    // Treat as heuristic, not verified
    seed.markAsHeuristic();
}
```

### Neural-Formal Fusion

```java
// Dynamics: α·neural + (1-α)·analytical
bridge.setFusionAlpha(0.3); // 30% neural, 70% analytical

HybridVectorField<State> hybrid = bridge.approximateDynamics(
    "robot arm movement",
    newtonsLaws
);

// Apply hybrid dynamics
double[] deltaS = hybrid.apply(currentState, controlInput);
```

### Type Synthesis with Refinement

```java
// Generate code satisfying spec
Optional<JavaType> result = bridge.synthesizeType(
    "Sort function for List<Integer>",
    new Constraints(IsSorted, PreservesElements),
    3  // max refinements
);

// LLM generates → Parser extracts → Type checker verifies
// If fails: feedback loop with error message
```

## Error Handling

```java
try {
    List<Trajectory> seeds = bridge.suggestTrajectories(start, goal, 5);
} catch (LLMUnavailableException e) {
    // Fallback: pure formal planning
    return formalPlanner.plan(start, goal);
} catch (LLMHallucinationException e) {
    // LLM response unparseable
    // Log for retraining, backtrack
}
```

## Performance

| Operation | Latency | Notes |
|-----------|---------|-------|
| Embedding | ~50-100ms | Local: fast, Cloud: network |
| Complete | ~0.5-2s | Depends on model |
| Trajectory suggestion | ~2-5s | Multiple calls |
| Proof guidance | ~1-3s | LLM + prover |
| Cache hit | <1ms | Formal → embedding |

## Testing

```java
@Test
public void testCosineSimilarity() {
    NeuralSpace space = new NeuralSpace(4);
    double[] u = {1, 0, 0, 0};
    double[] v = {0.9, 0.1, 0, 0};
    double sim = space.cosineSimilarity(u, v);
    assert sim > 0.85; // Similar
}

@Test
public void testModalDuality() {
    ModalOperators modal = new ModalOperators(mockLlm);
    // ¬◇¬φ ↔ □φ
    assert modal.checkDuality(proposition, mockProver);
}

@Test
public void testCompositionLoss() {
    // Smaller loss = better functor preservation
    double loss = bridge.compositionLoss(trajectory);
    assert loss < 0.1; // Well-trained
}
```

## References

- **Modal Logic**: JLS-style type safety as modal operators
- **Sheaf Theory**: Neural sections and gluing
- **Category Theory**: Functor composition preservation
- **HoTT**: Path approximation via chain-of-thought
