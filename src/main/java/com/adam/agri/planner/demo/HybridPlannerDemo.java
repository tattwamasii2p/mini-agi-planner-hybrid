package com.adam.agri.planner.demo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.adam.agri.planner.core.action.Action;
import com.adam.agri.planner.core.constraints.ConstraintSet;
import com.adam.agri.planner.core.constraints.CostConstraint;
import com.adam.agri.planner.core.constraints.TimeConstraint;
import com.adam.agri.planner.core.state.StateId;
import com.adam.agri.planner.core.trajectory.Trajectory;
import com.adam.agri.planner.core.trajectory.TrajectoryMetrics;
import com.adam.agri.planner.deduction.DeductionEngine;
import com.adam.agri.planner.deduction.ProofSearchResult;
import com.adam.agri.planner.deduction.Prover;
import com.adam.agri.planner.logic.Atomic;
import com.adam.agri.planner.logic.Conjunction;
import com.adam.agri.planner.logic.Context;
import com.adam.agri.planner.logic.Implication;
import com.adam.agri.planner.logic.Proposition;
import com.adam.agri.planner.logic.Sequent;
import com.adam.agri.planner.logic.Tactic;
import com.adam.agri.planner.multiagent.Agent;
import com.adam.agri.planner.multiagent.AgentId;
import com.adam.agri.planner.multiagent.SheafAggregator;
import com.adam.agri.planner.planning.WeightConfig;
import com.adam.agri.planner.sheaf.SheafGlue;
import com.adam.agri.planner.verification.PlanVerifier;
import com.adam.agri.planner.verification.SheafSemantics;

/**
 * Demonstration of the Hybrid AGI Planner.
 *
 * Based on thread log AgiIncremental_ThreadLog1.txt
 * Implements: symbolic + physical + sheaf + MCTS + belief-state + multi-agent
 */
public class HybridPlannerDemo {

    public static void main(String[] args) {
        System.out.println("=== Hybrid AGI Planner Demo ===\n");
        System.out.println("Based on thread log: AgiIncremental_ThreadLog1.txt\n");

        demoTrajectoryGluing();
        demoDijkstraPlanner();
        demoMultiAgentSheaf();
        demoDeductionEngine();
        demoPlanVerification();
    }

    static void demoTrajectoryGluing() {
        System.out.println("--- Demo 1: Sheaf Trajectory Gluing ---");
        System.out.println("Mathematical basis: Čech gluing");
        System.out.println("If end(a) == start(b) → can merge(a, b)\n");

        // Create state IDs
        StateId s0 = StateId.of("s0");
        StateId s1 = StateId.of("s1");
        StateId s2 = StateId.of("s2");
        StateId s3 = StateId.of("s3");

        // Create partial trajectories (local sections)
        // Path A: s0 → s1 (cost 5)
        Trajectory pathA = new Trajectory(s0, s1,
            new ArrayList<>(),
            new TrajectoryMetrics(5.0, 2.0, 0.9, 0.1, 1.0));

        // Path B: s1 → s2 (cost 3)
        Trajectory pathB = new Trajectory(s1, s2,
            new ArrayList<>(),
            new TrajectoryMetrics(3.0, 1.5, 0.95, 0.05, 0.5));

        // Path C: s2 → s3 (cost 4)
        Trajectory pathC = new Trajectory(s2, s3,
            new ArrayList<>(),
            new TrajectoryMetrics(4.0, 2.0, 0.92, 0.08, 0.8));

        // Path D: s0 → s2 (incompatible with A then B - overlap differs)
        Trajectory pathD = new Trajectory(s0, s2,
            new ArrayList<>(),
            new TrajectoryMetrics(10.0, 5.0, 0.8, 0.15, 0.3));

        System.out.println("Local sections:");
        System.out.println("  pathA: " + s0 + " → " + s1 + " (cost=" + pathA.cost() + ")");
        System.out.println("  pathB: " + s1 + " → " + s2 + " (cost=" + pathB.cost() + ")");
        System.out.println("  pathC: " + s2 + " → " + s3 + " (cost=" + pathC.cost() + ")");

        // Sheaf gluing
        SheafGlue glue = new SheafGlue();
        glue.collect(pathA, "agent_A");
        glue.collect(pathB, "agent_B");
        glue.collect(pathC, "agent_C");

        // Merge sequential paths
        Optional<Trajectory> merged1 = Trajectory.tryMerge(pathA, pathB);
        System.out.println("\nMerge pathA + pathB: " + merged1.map(t ->
            t.start() + " → " + t.end() + " (cost=" + t.cost() + ")"
        ).orElse("INCOMPATIBLE"));

        if (merged1.isPresent()) {
            Optional<Trajectory> merged2 = Trajectory.tryMerge(merged1.get(), pathC);
            System.out.println("Merge result + pathC: " + merged2.map(t ->
                t.start() + " → " + t.end() + " (cost=" + t.cost() + ")"
            ).orElse("INCOMPATIBLE"));
        }

        // Find global section
        Optional<Trajectory> global = glue.findGlobal(s0, s3);
        System.out.println("\nGlobal section s0→s3: " + global.map(t ->
            "FOUND (cost=" + t.cost() + ", time=" + t.time() + ", prob=" + t.probability() + ")"
        ).orElse("NOT FOUND"));

        // Verify sheaf condition
        boolean sheafOk = glue.verifySheafCondition();
        System.out.println("Sheaf condition verified: " + sheafOk);

        System.out.println("\n" + "=".repeat(50) + "\n");
    }

    static void demoDijkstraPlanner() {
        System.out.println("--- Demo 2: Dijkstra Planner ---");
        System.out.println("Weight = cost + α * risk (modal reasoning)");
        System.out.println("Key feature: belief-state support via weighting\n");

        // Create simple actions
        List<Action> actions = createSimpleActions();

        // Create planner with risk weighting
        WeightConfig weights = new WeightConfig(1.0, 0.5, 0.3, 0.0); // cost, risk, time, prob

        System.out.println("Planner weights: cost=" + weights.getCostWeight() +
                          ", risk=" + weights.getRiskWeight());
        System.out.println("Actions available: " + actions.size());

        // Note: Full Dijkstra requires action graph setup
        System.out.println("(Full planning requires configured action graph)\n");

        System.out.println("Stats tracking: nodes expanded, visited, time, cost\n");

        System.out.println("=".repeat(50) + "\n");
    }

    static void demoMultiAgentSheaf() {
        System.out.println("--- Demo 3: Multi-Agent Sheaf Aggregation ---");
        System.out.println("Key insight: knowledge=distributed, truth=consensus\n");

        // Create agents with local planners
        Agent agent1 = createAgent("Agent1", StateId.of("a1_start"));
        Agent agent2 = createAgent("Agent2", StateId.of("a2_start"));
        Agent agent3 = createAgent("Agent3", StateId.of("a3_start"));

        System.out.println("Agents created:");
        System.out.println("  " + agent1.getName() + " (id=" + agent1.getId() + ")");
        System.out.println("  " + agent2.getName() + " (id=" + agent2.getId() + ")");
        System.out.println("  " + agent3.getName() + " (id=" + agent3.getId() + ")");

        // Create aggregator
        ConstraintSet globalConstraints = new ConstraintSet();
        globalConstraints.addHard(new CostConstraint(100.0));
        globalConstraints.addHard(new TimeConstraint(60.0));

        SheafAggregator aggregator = new SheafAggregator(globalConstraints);

        List<Agent> agents = Arrays.asList(agent1, agent2, agent3);
        aggregator.collectFromAgents(agents);

        System.out.println("\nCollected local plans from " +
                          aggregator.getAgentPlans().size() + " agents");

        // MCTS mention
        System.out.println("\nMCTS Planner (AlphaZero-like):");
        System.out.println("  - Selection: UCB1 with PUCT variant");
        System.out.println("  - Expansion: add child nodes");
        System.out.println("  - Simulation: constraint-aware rollouts");
        System.out.println("  - Backpropagation: update statistics");

        // Core insight
        System.out.println("\n" + "-".repeat(40));
        System.out.println("Core insight from thread log:");
        System.out.println("  'AGI = hierarchy of incompatible planners");
        System.out.println("   trying to agree'");
        System.out.println("  'Planning = coordination of different");
        System.out.println("   representations of reality'");
        System.out.println("-".repeat(40));

        System.out.println("\n" + "=".repeat(50));
    }

    /**
     * Demo 4: Deduction Engine (Tactics and Proof Search)
     * Demonstrates automated theorem proving via tactic search.
     */
    static void demoDeductionEngine() {
        System.out.println("\n--- Demo 4: Deduction Engine ---");
        System.out.println("Mathematical basis: Sequent Calculus + LCF-style Tactics");
        System.out.println("Key insight: Planning = Proof Search\n");

        // Create propositions
        Proposition p = new Atomic("State_A");
        Proposition q = new Atomic("State_B");
        Proposition goal = new Implication(p, q);

        System.out.println("Goal to prove: " + goal);
        System.out.println("This corresponds to: transition from State_A to State_B\n");

        // Create deduction engine
        DeductionEngine engine = new DeductionEngine();
        System.out.println("Tactic library: " + engine.getTacticLibrary().stream()
            .map(Tactic::name).toList());

        // Automated proof search
        Sequent sequent = Sequent.of(goal);
        System.out.println("\nStarting proof search for: " + sequent);

        ProofSearchResult result = engine.prove(sequent);
        System.out.println("Result: " + (result.isSuccess() ? "SUCCESS" : "FAILED"));

        if (result instanceof ProofSearchResult.Success success) {
            System.out.println("Proof found in " + success.stepsExplored() + " steps");
            System.out.println("Proof depth: " + success.proof().depth());
            System.out.println("Proof witness (Curry-Howard): " + success.witnessDescription());
        }

        // Interactive prover
        Prover prover = new Prover(Sequent.of(new Conjunction(p, q)));
        System.out.println("\nInteractive prover started for: " + prover.currentGoal());
        System.out.println("Applicable tactics: " + prover.applicableTactics().stream()
            .map(Tactic::name).toList());

        System.out.println("\n" + "=".repeat(50));
    }

    /**
     * Demo 5: Plan Verification (Plan as Proof)
     * Demonstrates verification of plans as proofs.
     */
    static void demoPlanVerification() {
        System.out.println("\n--- Demo 5: Plan Verification ---");
        System.out.println("Mathematical basis: Curry-Howard (Plan = Proof)");
        System.out.println("Key insight: Each trajectory is a verifiable witness\n");

        // Create a trajectory (plan)
        StateId s0 = StateId.of("initial");
        StateId s1 = StateId.of("intermediate");
        StateId s2 = StateId.of("goal");

        Trajectory traj = new Trajectory(s0, s2,
            new ArrayList<>(),
            new TrajectoryMetrics(10.0, 3.0, 0.95, 0.1, 1.0));

        System.out.println("Trajectory: " + s0 + " -> " + s2);
        System.out.println("Cost: " + traj.cost() + ", Probability: " + traj.probability());

        // Create verifier
        PlanVerifier verifier = new PlanVerifier();
        System.out.println("\nVerifying trajectory as proof...");

        // Note: Real verification requires proper state mapping
        // This is a conceptual demonstration
        System.out.println("Verification concept: Map trajectory to proof");
        System.out.println("  - Start state → Initial sequent");
        System.out.println("  - Goal state → Target proposition");
        System.out.println("  - Actions → Tactic applications");

        // Sheaf semantics demonstration
        SheafSemantics semantics = new SheafSemantics();
        Context ctx = Context.empty()
            .extend(new Atomic("Precondition"))
            .extend(new Atomic("Invariant"));

        System.out.println("\nSheaf semantics:");
        System.out.println("  Context: " + ctx.facts());
        System.out.println("  Local truth check: " + semantics.isLocallyTrue(new Atomic("Precondition"), ctx));

        System.out.println("\nKey results:");
        System.out.println("  Valid plan = Constructive proof");
        System.out.println("  Proof witness = Executable program");
        System.out.println("  Sheaf semantics = Distributed truth");

        System.out.println("\n" + "=".repeat(50));
    }

    static List<Action> createSimpleActions() {
        // Simplified - would create full action objects in real use
        return new ArrayList<>();
    }

    static Agent createAgent(String name, StateId startId) {
        // Simplified - wire up dependencies properly in real use
        AgentId id = AgentId.generate();
        return new Agent(id, name, null, null) {
            // Stub implementation for demo
        };
    }
}
