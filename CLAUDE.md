# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository Overview

This is a **Hybrid AGI Planner** implementation combining symbolic planning, physics simulation, sheaf-based trajectory merging, MCTS search, belief-state planning, and multi-agent coordination. The implementation is based on the research log in `docs_internal/AgiIncremental_ThreadLog1.txt`.

## Build System

**Maven** - Java 17 project with JGraphT for graph operations.

```bash
mvn compile                 # Compile all sources
mvn test                    # Run all tests
mvn test -Dtest=ClassName   # Run single test class
mvn package                 # Build JAR
java -jar target/agi-planner-1.0-SNAPSHOT.jar  # Run main demo (HybridPlannerDemo)
```

## Architecture

```
com.adam.agri.planner/
├── core/                      # State, Action, Trajectory, Constraints
│   ├── state/                 # State, SymbolicState, PhysicalState, BeliefState
│   ├── action/                # Action, SymbolicAction, Precondition, Effect
│   ├── trajectory/            # Trajectory with merge() (sheaf sections)
│   └── constraints/           # CostConstraint, TimeConstraint, RiskConstraint
├── symbolic/                  # Symbolic layer
│   └── ontology/              # Upper & computer ontology
│       ├── upper/             # Entity, Physical, Abstract, Process, Property
│       ├── computer/          # ComputerSystem, ExternalComputerAction
│       │   └── code/java/     # Java type system (JavaType, JavaMember, etc.)
│       └── planning/          # Goal, Plan, Step
├── physical/                  # Physics simulation layer
│   └── worldmodel/            # WorldModel, DeterministicWorldModel
├── bridge/                    # Symbolic-physical bridge
│   ├── Abstraction.java       # φ: Physical→Symbolic
│   └── Refinement.java        # ρ: Symbolic→Physical
├── planning/                  # Planners
│   ├── Planner.java           # Base interface
│   ├── DijkstraPlanner.java   # Cost+risk weighted
│   ├── MCTSPlanner.java       # AlphaZero-like MCTS
│   ├── BeliefStatePlanner.java # POMDP-lite
│   └── WeightConfig.java      # Cost/risk/time/probability weights
├── search/
│   └── mcts/                  # MCTSNode for tree search
├── sheaf/                     # Sheaf-based trajectory gluing
│   ├── Sheaf.java             # Sheaf interface (LocalSection, CompatibilityPair)
│   ├── SheafGlue.java         # Trajectory merging engine
│   └── CompatibilityGraph.java # For n-way merging
├── multiagent/                # Multi-agent coordination
│   ├── Agent.java             # Agent with local planner
│   ├── SheafAggregator.java   # Plan aggregation
│   ├── Belief.java            # Distributed beliefs
│   └── CommunicationChannel.java # Agent messaging
├── agent/                     # Perception and execution
│   ├── perception/            # PerceptionEvent, SensorArray, Attention
│   └── execution/             # PhysicalExecutor, SafetyMonitor, Actuation
├── computer/                  # Computer system as agent
│   ├── ComputerSystemAgent.java # Pub/sub agent for compute resources
│   ├── AbstractComputerSystemsData.java # Typed metrics interface
│   └── ComputerSystemsData.java # Immutable metrics implementation
├── jls/                       # Java Language Specification reader
│   └── reader/                # JlsReader, JvmsReader, JlsSection, JlsClassifier
├── logic/                     # Formal logic layer
│   ├── Term.java, Proposition.java, Tactic.java
│   └── DatalogReasoner.java, LogicConfiguration.java
└── demo/
    └── HybridPlannerDemo.java # Main demo entry point
```

## Key Concepts

**Sheaf Theory for Planning**: Trajectories are "sections" that can be merged (Čech gluing) when end/start states match. Local partial plans combine into global plans. See `Trajectory.tryMerge()` and `SheafGlue`.

**Modal Weighting**: Dijkstra uses weight = cost + α*risk (line 3835 in log). `WeightConfig` holds the weights.

**MCTS/PUCT**: U(s,a) = Q(s,a) + c_puct * P(a|s) * √N(s)/(1+N(s,a))

**Multi-Agent**: "Knowledge=distributed, truth=consensus" via sheaf gluing. `SheafAggregator.collectFromAgents()` merges local plans.

**Bridge Pattern**: `Abstraction` (Physical→Symbolic, φ) and `Refinement` (Symbolic→Physical, ρ) connect layers.

**Perception-Action Loop**: `PerceptualMotorSystem` closes the sensorimotor loop with perception → decide → execute → feedback.

**JLS Integration**: `JlsReader` parses Java Language Specification sections for type system integration.

**Multilanguage**: Rather than being Java-specific, use abstract notions suitable for various programming languages, rewrite
all Java-oriented code to use this.
 
## Files to Know

| Critical File | Purpose |
|--------------|---------|
| `core/trajectory/Trajectory.java` | Sheaf sections with `tryMerge()` capability |
| `sheaf/SheafGlue.java` | Trajectory merging engine, global section finding |
| `planning/MCTSPlanner.java` | Monte Carlo Tree Search with UCB1/PUCT |
| `planning/DijkstraPlanner.java` | Cost+risk weighted planner |
| `physical/worldmodel/WorldModel.java` | Physics simulation interface |
| `multiagent/SheafAggregator.java` | Multi-agent plan aggregation |
| `agent/perception/PerceptionEvent.java` | Structured sensory data |
| `agent/execution/PhysicalExecutor.java` | Action execution pipeline |
| `computer/ComputerSystemAgent.java` | Computer as reactive agent |
| `demo/HybridPlannerDemo.java` | Main demo: trajectory gluing + MCTS + multi-agent |

## Documentation

- `docs_internal/AgiIncremental_ThreadLog1.txt` - Research log with mathematical foundations, sheaf theory notes, architecture discussions (in Russian/English mix)
- `docs/LLM_REASONING.md` - LLM as probabilistic functor, modal logic, neural sheaf sections
- `docs/COMPUTER_SYSTEM_AGENT.md` - Computer system pub/sub architecture, `ComputerSystemAgent` usage
- `docs/PERCEPTION_ACTUATION.md` - Sensorimotor loop, `PerceptualMotorSystem`, attention mechanisms
- `docs/JLS_PARSER.md` - Java Language Specification reader documentation

## Dependencies

- **JGraphT** 1.5.2 - Graph operations for planning
- **JUnit Jupiter** 5.10.0 - Testing

## Testing

No tests currently exist in `src/test/`. When adding tests:

```bash
mvn test                           # Run all tests
mvn test -Dtest=TrajectoryTest   # Run single test
mvn test -Dtest=SheafTest#testGluing  # Run single method
```
