package com.adam.agri.planner.multiagent;

import com.adam.agri.planner.core.constraints.ConstraintSet;
import com.adam.agri.planner.core.state.StateId;
import com.adam.agri.planner.core.trajectory.Trajectory;
import com.adam.agri.planner.planning.Goal;
import com.adam.agri.planner.planning.Plan;
import com.adam.agri.planner.sheaf.CompatibilityGraph;
import com.adam.agri.planner.sheaf.SheafGlue;
import com.adam.agri.planner.types.TypeChecker;

import java.util.*;

/**
 * Aggregates plans from multiple agents using sheaf gluing.
 *
 * From log: "Агрегирует планы нескольких агентов с помощью плетения пучков (sheaf gluing)."
 *
 * Key insight: "knowledge = distributed, truth = consensus"
 */
public class SheafAggregator {

    // Sheaf gluing engine
    private SheafGlue glueEngine;

    // Type checker for semantic validation
    private final TypeChecker typeChecker;

    // Map agent ID to their contributed trajectory
    private final Map<AgentId, Trajectory> agentPlans;

    // Consensus engine for conflict resolution
    private final ConsensusEngine consensusEngine;

    public SheafAggregator(ConstraintSet globalConstraints) {
        this.glueEngine = new SheafGlue(globalConstraints);
        this.typeChecker = new TypeChecker();
        this.agentPlans = new HashMap<>();
        this.consensusEngine = new ConsensusEngine();
    }

    /**
     * Gather local plans from all agents.
     *
     * @param agents List of agents to collect from
     */
    public void collectFromAgents(List<Agent> agents) {
        agentPlans.clear();
        glueEngine = new SheafGlue(new ConstraintSet()); // Reset

        for (Agent agent : agents) {
            Trajectory localPlan = agent.contributeToSheaf();
            if (localPlan != null) {
                agentPlans.put(agent.getId(), localPlan);
                glueEngine.collect(localPlan, agent.getId().getValue());
            }
        }
    }

    /**
     * Build global plan via sheaf gluing with type checking.
     *
     * Process:
     * 1. Type-check compatibility
     * 2. Build compatibility graph
     * 3. Glue trajectories
     * 4. Find global section
     *
     * @param globalGoal Global goal to achieve
     * @return Complete plan if consensus reached
     */
    public Optional<Plan> aggregate(Goal globalGoal) {
        if (agentPlans.isEmpty()) {
            return Optional.empty();
        }

        // 1. Type-check compatibility
        boolean typesValid = typeChecker.validate(agentPlans.values());
        if (!typesValid) {
            // Attempt type-level consensus
            return buildConsensusWithTypeRepair(globalGoal);
        }

        // 2. Build compatibility graph
        CompatibilityGraph graph = glueEngine.getCompatibilityGraph();

        // 3. Find global section (complete trajectory)
        Optional<Trajectory> global = glueEngine.findGlobal(
            findStartState(),
            globalGoal.getTargetState()
        );

        if (global.isPresent()) {
            return Optional.of(new Plan(global.get(), globalGoal));
        }

        // 4. No global section exists - negotiate consensus
        return buildConsensusPlan(globalGoal);
    }

    /**
     * Find start state from agent plans.
     * For now, assumes common start or uses first available.
     */
    private StateId findStartState() {
        return agentPlans.values().stream()
            .findFirst()
            .map(Trajectory::start)
            .orElse(null);
    }

    /**
     * Find end state from agent plans.
     */
    private StateId findEndState() {
        return agentPlans.values().stream()
            .findFirst()
            .map(Trajectory::end)
            .orElse(null);
    }

    /**
     * Consensus building when sheaf condition fails.
     * Iterative refinement until compatible.
     *
     * Algorithm:
     * 1. Identify conflicts
     * 2. Mediate between agents
     * 3. Replan affected agents
     * 4. Repeat until compatible or exhausted
     *
     * @param globalGoal Target goal
     * @return Consensus plan if possible
     */
    private Optional<Plan> buildConsensusPlan(Goal globalGoal) {
        List<Conflict> conflicts = findConflicts(agentPlans);
        int maxIterations = 10;
        int iteration = 0;

        while (!conflicts.isEmpty() && iteration < maxIterations) {
            iteration++;

            // Mediate first conflict
            Conflict conflict = conflicts.get(0);
            MediationResult mediation = consensusEngine.mediate(conflict);

            if (!mediation.isSuccessful()) {
                // Cannot resolve
                return Optional.empty();
            }

            // Replan affected agents
            ConstraintSet resolution = mediation.getResolution();
            for (AgentId agentId : conflict.getInvolvedAgents()) {
                replanAgent(agentId, resolution);
            }

            // Recollect and check for new conflicts
            Map<AgentId, Trajectory> newPlans = collectPlans();
            conflicts = findConflicts(newPlans);
        }

        // Try to glue again
        Optional<Trajectory> global = glueEngine.findGlobal(
            findStartState(),
            globalGoal.getTargetState()
        );

        return global.map(t -> new Plan(t, globalGoal));
    }

    /**
     * Build consensus with type repair.
     * When types don't match, attempt to cast or transform.
     */
    private Optional<Plan> buildConsensusWithTypeRepair(Goal globalGoal) {
        // For now: just try standard consensus
        // Real implementation would add type coercion
        return buildConsensusPlan(globalGoal);
    }

    /**
     * Replan a specific agent with new constraints.
     */
    private void replanAgent(AgentId agentId, ConstraintSet constraints) {
        // Find agent
        Agent agent = agentPlans.keySet().stream()
            .filter(id -> id.equals(agentId))
            .findFirst()
            .map(id -> {
                // Would need access to agent list here
                // Simplified: just mark for replan
                return (Agent) null;
            })
            .orElse(null);

        if (agent != null) {
            agent.receiveConsensus(constraints);
        }
    }

    /**
     * Find conflicts between agent plans.
     * Conflicts occur when:
     * - Two agents claim incompatible transitions from same state
     * - Trajectories have same start but different costs (conflict resolution needed)
     * - Sheaf condition fails on overlaps
     */
    private List<Conflict> findConflicts(Map<AgentId, Trajectory> plans) {
        List<Conflict> conflicts = new ArrayList<>();

        // Check for state conflicts
        Map<StateId, List<AgentId>> stateClaims = new HashMap<>();

        for (Map.Entry<AgentId, Trajectory> entry : plans.entrySet()) {
            AgentId agent = entry.getKey();
            Trajectory traj = entry.getValue();

            // Register claims on intermediate states
            stateClaims.computeIfAbsent(traj.start(), k -> new ArrayList<>()).add(agent);
            stateClaims.computeIfAbsent(traj.end(), k -> new ArrayList<>()).add(agent);
        }

        // Conflicts: multiple agents claiming same state with different actions
        for (Map.Entry<StateId, List<AgentId>> claim : stateClaims.entrySet()) {
            if (claim.getValue().size() > 1) {
                // Check if actually conflicting (different paths through state)
                conflicts.add(new Conflict(
                    claim.getKey(),
                    new HashSet<>(claim.getValue()),
                    ConflictType.STATE_CLAIM
                ));
            }
        }

        return conflicts;
    }

    private Map<AgentId, Trajectory> collectPlans() {
        return new HashMap<>(agentPlans);
    }

    // Getters
    public SheafGlue getGlueEngine() {
        return glueEngine;
    }

    public Map<AgentId, Trajectory> getAgentPlans() {
        return Collections.unmodifiableMap(agentPlans);
    }

    // Inner classes

    /**
     * Conflict between agents.
     */
    public static class Conflict {
        private final StateId state;
        private final Set<AgentId> involvedAgents;
        private final ConflictType type;

        public Conflict(StateId state, Set<AgentId> involvedAgents, ConflictType type) {
            this.state = state;
            this.involvedAgents = involvedAgents;
            this.type = type;
        }

        public StateId getState() { return state; }
        public Set<AgentId> getInvolvedAgents() { return involvedAgents; }
        public ConflictType getType() { return type; }
    }

    public enum ConflictType {
        STATE_CLAIM,      // Multiple agents claim same state
        COST_DISAGREEMENT, // Same path, different cost estimates
        SHEAF_VIOLATION   // Incompatibility in gluing
    }

    /**
     * Result of mediation.
     */
    public static class MediationResult {
        private final boolean successful;
        private final ConstraintSet resolution;

        public MediationResult(boolean successful, ConstraintSet resolution) {
            this.successful = successful;
            this.resolution = resolution;
        }

        public boolean isSuccessful() {
            return successful;
        }

        public ConstraintSet getResolution() {
            return resolution;
        }
    }

    /**
     * Consensus engine for conflict resolution.
     */
    public static class ConsensusEngine {
        public MediationResult mediate(Conflict conflict) {
            // Simplified: always succeeds with empty resolution
            // Real implementation would negotiate
            return new MediationResult(true, new ConstraintSet());
        }
    }
}
