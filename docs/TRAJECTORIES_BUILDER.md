# TrajectoriesBuilder Documentation

## Overview

`TrajectoriesBuilder` is the **central observation integration component** of the Hybrid AGI Planner. It unifies multiple heterogeneous observation sources—symbolic knowledge, physical sensor data, and natural language—into a coherent world model for trajectory generation.

## Key Insight

> "Planning requires grounding in heterogenous observations"

Real-world planning agents must reason from:
- **Symbolic** - Explicit knowledge ("table is at coordinates (3,5)")
- **Physical** - Sensor readings (camera images, lidar, IMU)
- **Natural language** - Goals, constraints, preferences ("go to the kitchen fast but safe")

Each source has different **confidence levels**:
```
Physical sensor:  0.95  (high precision)
Symbolic KB:      0.90  (explicit facts)
Natural language: 0.70  (LLM-parsed text)
Inferred:         0.60  (derived conclusions)
Simulated:        0.50  (hypothetical)
```

## Architecture

```
                    Observation Sources
┌─────────────────────────────────────────────────────────────┐
│  Symbolic       Physical         Natural Language           │
│  ┌─────────┐   ┌──────────┐     ┌─────────────────┐         │
│  │Symbolic │   │Perception│     │   NL Adapter    │         │
│  │State    │   │Event     │     │                 │         │
│  │Predicate│   │          │     │ - Goals         │         │
│  └────┬────┘   └─────┬────┘     │ - Constraints   │         │
│       │              │          │ - World desc    │         │
│       └──────────────┬──────────┘                 │         │
│                      ▼                            │         │
│            ┌─────────────────┐                    │         │
│            │ObservationFusion│◄───────────────────┘         │
│            │                 │                              │
│            │ Weighted fusion │                              │
│            │ by confidence   │                              │
│            └────────┬────────┘                              │
│                     ▼                                       │
│            ┌─────────────────┐                              │
│            │IntegratedWorld  │                              │
│            │Model            │                              │
│            │                 │                              │
│            │- ComputerSystems│                              │
│            │- PhysicalWorld  │                              │
│            └────────┬────────┘                              │
│                     ▼                                       │
│            ┌─────────────────┐                              │
│            │GoalExtractor    │                              │
│            │                 │                              │
│            │ - Parse goals   │                              │
│            │ - Constraints   │                              │
│            │ - Desires       │                              │
│            └─────────────────┘                              │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │ Trajectories    │
                    │ Generation      │
                    └─────────────────┘
```

## Usage

### Basic Usage

```java
// Create builder with fluent API
TrajectoriesBuilder builder = TrajectoriesBuilder.create()
    .withSymbolicObservation(currentState)
    .withPhysicalObservation(sensorReading)
    .withGoal("Navigate to target location")
    .withConstraint("Avoid obstacles")
    .withConfidenceThreshold(0.6);

// Build unified world model
IntegratedWorldModel world = builder.buildWorldModel();

// Extract planning components
List<Goal> goals = builder.extractGoals();
ConstraintSet constraints = builder.extractConstraints();
DesireModel desires = builder.extractDesires();

// Generate trajectories
List<Trajectory> candidates = builder.generateTrajectories(world);
Trajectory optimal = builder.selectOptimal(candidates, Optimizer.BALANCED);
```

### Multi-Source Observation Example

```java
// Combine robot sensor data with high-level commands
TrajectoriesBuilder builder = TrajectoriesBuilder.create();

// 1. Add symbolic knowledge (from SLAM/ontology)
builder.withSymbolicObservation(mapState);
builder.withSymbolicPredicate(new Predicate("at_location", "kitchen"));

// 2. Add physical sensor data (lidar, camera, IMU)
builder.withPhysicalObservation(lidarScan);
builder.withPhysicalObservations(cameraFrames);

// 3. Add natural language command with type
builder.withNaturalLanguage(
    "Pick up the red box carefully",
    TrajectoriesBuilder.NLType.ACTION
);

// 4. Add goal and constraints
builder.withGoal("Have red box in inventory");
builder.withConstraint("grip_force < 10N");
builder.withDesire("minimize_execution_time");
```

## Observation Sources

The builder supports typed observation sources with confidence weighting:

### SymbolicObservationAdapter

```java
public interface ObservationSource<T> {
    void addObservation(T observation);
    List<T> getObservations();
    SourceType getSourceType();        // SYMBOLIC_KNOWLEDGE
    double getSourceConfidence();      // 0.90 default
    List<Entity> toEntities();
}
```

**Accepts:**
- `SymbolicState` - High-level state with predicates
- `Predicate` - Individual symbolic facts

### PhysicalObservationAdapter

```java
// SourceType: PHYSICAL_SENSOR, confidence: 0.95
builder.withPhysicalObservation(physicalState);
builder.withPhysicalObservation(perceptionEvent);
```

**Accepts:**
- `PhysicalState` - Measurable quantities
- `PerceptionEvent` - Timed sensor readings with confidence

### NaturalLanguageAdapter

```java
// SourceType: NATURAL_LANGUAGE, confidence: 0.70
builder.withNaturalLanguage(text, NLType.GOAL);
builder.withGoal("Navigate to exit");
builder.withConstraint("Battery > 20%");
builder.withWorldDescription("Office environment with 3 rooms");
builder.withDesire("Fast execution");
```

**NL Types:**
- `GOAL` - Planning target
- `CONSTRAINT` - Hard/soft constraints
- `WORLD_DESC` - Environment description
- `DESIRE` - Preference/utility hint
- `ACTION` - Specific action to execute

## Observation Fusion

The `buildWorldModel()` method performs **weighted sensor fusion**:

```java
public IntegratedWorldModel buildWorldModel() {
    // 1. Collect all sources
    List<ObservationSource<?>> allSources = List.of(
        symbolicAdapter, physicalAdapter, nlAdapter, ...custom
    );

    // 2. Filter by confidence threshold
    List<ObservationSource<?>> validSources = allSources.stream()
        .filter(s -> s.getSourceConfidence() >= confidenceThreshold)
        .filter(ObservationSource::hasObservations)
        .toList();

    // 3. Fuse observations (weighted by confidence)
    ObservationFusion fusion = new ObservationFusion();
    List<FusedEntity> fused = fusion.fuse(validSources);

    // 4. Build domain models
    ComputerSystemsModel computerModel = buildComputerModel(fused);
    PhysicalWorldModel physicalModel = buildPhysicalModel(fused);

    return new IntegratedWorldModel(computerModel, physicalModel);
}
```

## Goal Extraction

The `GoalConstraintExtractor` parses natural language into structured planning components:

### Goal Parsing

```java
// "Navigate to kitchen safely"
// ↓
Goal {
    condition: state -> distance(state, "kitchen") < epsilon
    utility: (from, to) -> speed(to) * safety(to)
}
```

### Constraint Parsing

```java
// "Avoid obstacles"
// "Battery > 20%"
// "Time < 60 seconds"
// ↓
ConstraintSet {
    hard: [battery_constraint, collision_constraint]
    soft: [time_preference, comfort_preference]
}
```

### Desire Parsing

```java
// "Fast execution", "Low energy", "Smooth motion"
// ↓
DesireModel {
    weights: {time: 0.5, energy: 0.3, comfort: 0.2}
}
```

## Trajectory Optimization

After generating candidate trajectories, select optimal based on strategy:

```java
public enum Optimizer {
    MIN_COST,      // Minimize cumulative cost
    MIN_TIME,      // Minimize execution time
    MIN_RISK,      // Minimize probability of failure
    MAX_UTILITY,   // Maximize (probability - risk)
    BALANCED,      // Weighted: 0.5*cost + 0.3*time + 0.2*risk
    PARETO         // Return first (dominates others)
}

Trajectory optimal = builder.selectOptimal(candidates, Optimizer.BALANCED);
```

## Configuration

### Confidence Threshold

Filter low-confidence observations:

```java
builder.withConfidenceThreshold(0.7);  // Ignore sources below 70% confidence
```

### LLM Integration

Enable LLM-based natural language understanding:

```java
LLMReasoningBridge llmBridge = new LLMReasoningBridge(config);
builder.withLLMBridge(llmBridge);
```

## Mathematical Model

### Observation Model

Each observation source `S_i` produces observations `o_i` with confidence `c_i ∈ [0,1]`:

```
O = {(o₁, c₁), (o₂, c₂), ..., (oₙ, cₙ)}
```

### Fusion

Fused entity confidence (weighted average):

```
c_fused = Σ(c_i * w_i) / Σ(w_i)
where w_i = confidence weight
```

### World Model

Integrated world model combines:

```
W = (W_computer, W_physical)
W_computer: Available systems, network topology, latency
W_physical: Spatial entities, obstacles, free space
```

### Trajectory Generation

Given world model `W` and goal `G`, generate trajectories:

```
T = {τ : τ.start = W.current ∧ G(τ.end)}
optimal = argmax_{τ ∈ T} utility(τ)
```

## Key Classes

| Class | Package | Purpose |
|-------|---------|---------|
| `TrajectoriesBuilder` | `trajectories.builder` | Main builder API |
| `SymbolicObservationAdapter` | `trajectories.builder.observations` | Handle symbolic state |
| `PhysicalObservationAdapter` | `trajectories.builder.observations` | Handle sensor data |
| `NaturalLanguageAdapter` | `trajectories.builder.observations` | Handle text input |
| `ObservationSource` | `trajectories.builder.observations` | Interface for sources |
| `ObservationFusion` | `trajectories.builder.fusion` | Weighted fusion |
| `GoalConstraintExtractor` | `trajectories.builder.goals` | Parse goals/constraints |
| `IntegratedWorldModel` | `trajectories.builder.worldmodel` | Unified model |

## Builder Pattern

The class uses **fluent builder pattern** with method chaining:

```java
TrajectoriesBuilder.create()
    .withSymbolicObservation(...)
    .withPhysicalObservation(...)
    .withGoal(...)
    .withConstraint(...)
    .withDesire(...)
    .withConfidenceThreshold(0.7)
    .buildWorldModel();
```

**Thread safety:** Not thread-safe. Create new instance per planning cycle.

**Clear/reuse:**

```java
builder.clear();  // Reset for new planning cycle
builder.withSymbolicObservation(newState);  // Add fresh observations
```

## Integration with Planning

Typical usage flow:

```java
// 1. Gather observations
TrajectoriesBuilder builder = TrajectoriesBuilder.create()
    .withSymbolicObservation(currentSymbolicState)
    .withPhysicalObservation(currentPhysicalState)
    .withGoal("Reach target")
    .withConstraint("Safety margin > 0.5m");

// 2. Build world model
IntegratedWorldModel world = builder.buildWorldModel();

// 3. Extract planning components
Goal goal = builder.extractMainGoal().orElseThrow();
ConstraintSet constraints = builder.extractConstraints();

// 4. Create planner and plan
Planner planner = new DijkstraPlanner(weights, actions);
Plan plan = planner.plan(initialState, goal, planningContext).orElseThrow();

// 5. Execute
executor.executePlan(plan.toTrajectory());
```

## References

- `HybridPlannerDemo.java` - Example usage in demo
- `HybridPlannerDemoAutoCoder.java` - Code generation via trajectories builder
- `ObservationFusion.java` - Fusion algorithm details
- `IntegratedWorldModel.java` - World model structure
