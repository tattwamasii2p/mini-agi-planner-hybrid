# HybridPlannerDemoAutoCoder Documentation

## Overview

`HybridPlannerDemoAutoCoder` demonstrates **code generation via AI planning**. It combines the Hybrid AGI Planner's symbolic planning, sheaf-based trajectory merging, and physical execution to parse a codebase observation and a program specification, then generate and write Java source files.

## Key Insight

> "Code generation = planning over file system state space"

The demo treats code generation as a planning problem:
- **Initial State**: Empty or existing codebase (set of files, packages, classes)
- **Goal State**: Codebase satisfying the specification (required classes exist)
- **Actions**: Create package, write code file
- **Plan**: Sequence of file operations to achieve the goal

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     HybridPlannerDemoAutoCoder                   │
├─────────────────────────────────────────────────────────────────┤
│  Phase 1: Observation          Phase 2: Goal Extraction        │
│  ┌─────────────────┐           ┌─────────────────┐              │
│  │ Trajectories    │           │ Condition-based │              │
│  │ Builder         │──────────▶│ Goals           │              │
│  │                 │           └─────────────────┘              │
│  │ - codebase      │                                            │
│  │ - specification │                                            │
│  └─────────────────┘                                            │
├─────────────────────────────────────────────────────────────────┤
│  Phase 3: Planning           Phase 4: Sheaf Gluing             │
│  ┌─────────────────┐           ┌─────────────────┐              │
│  │ DijkstraPlanner │           │ SheafGlue       │              │
│  │                 │──────────▶│                 │              │
│  │ modal reasoning │           │ Čech gluing     │              │
│  │ cost + α*risk   │           │ of trajectories │              │
│  └─────────────────┘           └─────────────────┘              │
├─────────────────────────────────────────────────────────────────┤
│  Phase 5: Execution                                            │
│  ┌─────────────────┐                                            │
│  │ PhysicalExecutor│                                            │
│  │                 │                                            │
│  │ FileActuator    │───▶ writes files to ./generated          │
│  └─────────────────┘                                            │
└─────────────────────────────────────────────────────────────────┘
```

## Usage

### Command Line

```bash
java -cp target/classes com.adam.agri.planner.demo.HybridPlannerDemoAutoCoder \
    --codebase=<path>           # Root of existing codebase to analyze
    --spec=<text>               # Program specification text
    --spec-file=<path>          # File containing specification
    --output=<path>             # Output directory (default: ./generated)
```

### Examples

```bash
# Generate Calculator class from text spec
java -cp target/classes com.adam.agri.planner.demo.HybridPlannerDemoAutoCoder \
    --spec="Create a Calculator class with add and subtract methods" \
    --output=./generated

# Analyze existing codebase and add to it
java -cp target/classes com.adam.agri.planner.demo.HybridPlannerDemoAutoCoder \
    --codebase=./src \
    --spec-file=./specs/service.txt \
    --output=./src
```

## Implementation Details

### Phase 1: Observation

Uses `TrajectoriesBuilder` to collect multi-source observations:

```java
TrajectoriesBuilder builder = TrajectoriesBuilder.create();
builder.withSymbolicObservation(codebaseState);     // Existing code
builder.withGoal(specification);                    // NL specification
```

**Key classes:**
- `CodebaseState` - Symbolic representation of codebase (packages, classes, files)
- `TrajectoriesBuilder` - Multi-source observation integrator

### Phase 2: Goal Extraction

Parses natural language specification into `Condition`-based goals:

```java
// Create condition: "package com.example.generated exists"
Condition packageExists = state -> {
    if (state instanceof CodebaseState cs) {
        return cs.hasPackage("com.example.generated");
    }
    return false;
};
goals.add(new Goal(packageExists, UtilityFunction.standard()));
```

**Pattern matching for class names:**
- "Create a **Calculator** class" → extracts "Calculator"
- "class **Foo**" → extracts "Foo"
- "define **Bar**" → extracts "Bar"

### Phase 3: Planning (Dijkstra with Modal Reasoning)

Uses `DijkstraPlanner` with modal cost function:

```
weight = cost + α * risk
```

**Available actions:**
- `CreatePackageAction` - Create package directory structure
- `WriteCodeAction` - Write Java source file with generated content

### Phase 4: Sheaf Gluing

Combines multiple goal plans via Čech gluing:

```java
Trajectory combined = mergePlansWithSheaf(plans);
// If end(A) == start(B), merge(A, B) → larger trajectory
```

**SheafGlue** implements:
- Collect local plans (trajectories from each goal)
- Try merge: `Trajectory.tryMerge(t1, t2)`
- If incompatible, append actions sequentially

### Phase 5: Execution

`PhysicalExecutor` with `FileActuator` performs actual file I/O:

```java
List<Actuator> actuators = new ArrayList<>();
actuators.add(new FileActuator(outputRoot));

PhysicalExecutor executor = new PhysicalExecutor(actuators, safety);
ExecutionResult result = executor.execute(action, context);
```

**Execution flow:**
1. Verify preconditions (package doesn't exist yet)
2. Safety check (writable directory)
3. Execute via actuator (mkdirs, write file)
4. Verify effects (file exists)

## Mathematical Model

### Codebase State Space

```
CodebaseState = (P, C, F, R)
P ⊆ Packages      // e.g., {"com.example.generated"}
C ⊆ Classes       // e.g., {"Calculator", "Service"}
F ⊆ Files         // e.g., {"Calculator.java"}
R: C → P          // class → package mapping
```

### Planning Problem

**Initial state** s₀: (P₀, C₀, F₀, R₀) - existing codebase or empty

**Goal** G: Condition satisfied when required classes exist:
```
G(CodebaseState) = ⋀_{c ∈ requiredClasses} c ∈ C
```

**Action model** A: File operations with preconditions and effects
- `create_package(p)`: pre = p ∉ P, effect = P' = P ∪ {p}
- `write_code(f, content)`: pre = parent(f) ∈ P, effect = F' = F ∪ {f}

**Optimal plan**: π* = argmin_π cost(π) + α·risk(π)

## Key Components

| Component | File | Purpose |
|-----------|------|---------|
| `HybridPlannerDemoAutoCoder` | `demo/HybridPlannerDemoAutoCoder.java` | Main demo orchestrator |
| `CodebaseState` | `demo/state/CodebaseState.java` | Codebase as symbolic state |
| `FileAction` | `demo/actions/FileAction.java` | Base for file operations |
| `CreatePackageAction` | `demo/actions/CreatePackageAction.java` | Create package dirs |
| `WriteCodeAction` | `demo/actions/WriteCodeAction.java` | Write Java source |
| `FileActuator` | `agent/execution/FileActuator.java` | File I/O actuator |

## Sheaf Theory Connection

From the thread log:
> "AGI = hierarchy of incompatible planners trying to agree"

In code generation context:
- **Local sections**: Each goal generates a partial plan (trajectory)
- **Compatibility**: Trajectories can merge if end(t₁) = start(t₂)
- **Global section**: Combined plan via Čech gluing
- **Sheaf condition**: If plans agree on overlap, global plan exists

## References

- `docs_internal/AgiIncremental_ThreadLog1.txt` - Original research log
- `CLAUDE.md` - Project architecture overview
- `HybridPlannerDemo.java` - Parent demo with trajectory gluing + MCTS
