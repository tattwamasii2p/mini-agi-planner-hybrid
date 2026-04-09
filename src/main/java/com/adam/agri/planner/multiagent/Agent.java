package com.adam.agri.planner.multiagent;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.adam.agri.planner.core.state.BeliefState;
import com.adam.agri.planner.core.state.State;
import com.adam.agri.planner.core.trajectory.Trajectory;
import com.adam.agri.planner.physical.worldmodel.WorldModel;
import com.adam.agri.planner.planning.ExternalTool;
import com.adam.agri.planner.planning.Goal;
import com.adam.agri.planner.planning.Plan;
import com.adam.agri.planner.planning.Planner;
import com.adam.agri.planner.planning.PlanningContext;

/**
 * Agent with local planner and world section.
 * Implements local section of sheaf.
 *
 * From log: "Agent has its own Planner
 *  - its own piece of local world
 *    → these are local sections (in sheaf terms)"
 *  - local graph
 *  - generates local plan"
 */
public class Agent {
    private final AgentId id;
    private final String name;

    // Local world section (partial knowledge)
    private final WorldModel localWorldModel;

    // Local planner (Dijkstra/MCTS/Belief)
    private final Planner localPlanner;

    // Communication channel for consensus
    private CommunicationChannel comms;

    // Current beliefs and goals
    private Set<Belief> beliefs;
    private Set<Goal> goals;

    // Local context
    private PlanningContext localContext;

    // Generated local plan
    private Trajectory localPlan;

    // External tools available to this agent
    private Set<ExternalTool> availableTools;

    public Agent(AgentId id, String name, WorldModel localWorldModel,
                 Planner localPlanner) {
        this.id = id;
        this.name = name;
        this.localWorldModel = localWorldModel;
        this.localPlanner = localPlanner;
        this.beliefs = new HashSet<>();
        this.goals = new HashSet<>();
        this.localContext = new PlanningContext();
        this.availableTools = new HashSet<>();
    }

    /**
     * Generate local plan using local knowledge.
     * This is the agent's local section of the sheaf.
     *
     * @param localInitial Initial state (in agent's scope)
     * @param localGoal Goal to achieve
     * @return Local trajectory if plan found
     */
    public Optional<Trajectory> generateLocalPlan(State localInitial, Goal localGoal) {
        Optional<Plan> plan = localPlanner.plan(localInitial, localGoal, localContext);
        return plan.map(p -> {
            this.localPlan = p.toTrajectory();
            return this.localPlan;
        });
    }

    /**
     * Generate plan with belief state.
     * Returns trajectory representing the agent's belief about the plan.
     */
    public Optional<Trajectory> generateBeliefPlan(BeliefState initial, Goal goal) {
        Optional<Plan> plan = localPlanner.plan(initial, goal, localContext);
        return plan.map(p -> {
            this.localPlan = p.toTrajectory();
            return this.localPlan;
        });
    }

    /**
     * Contribute local plan to global sheaf.
     * This is how agents participate in distributed planning.
     */
    public Trajectory contributeToSheaf() {
        return localPlan;
    }

    /**
     * Receive consensus constraints and update local planning.
     * Called when sheaf aggregation identifies conflicts.
     *
     * @param globalConstraints Constraints from global consensus
     */
    public void receiveConsensus(com.adam.agri.planner.core.constraints.ConstraintSet globalConstraints) {
        // Merge global constraints with local
        for (var c : globalConstraints.getHardConstraints()) {
            localContext.getHardConstraints().addHard(c);
        }
        // Replan if necessary
        if (localPlan != null) {
            // Check if current plan still satisfies new constraints
            boolean stillValid = globalConstraints.allSatisfiedBy(localPlan);
            if (!stillValid) {
                localPlan = null; // Force replan
            }
        }
    }

    /**
     * Receive partial plan from another agent for coordination.
     */
    public void receivePartialPlan(Trajectory partialPlan, AgentId fromAgent) {
        // Store for potential coordination
        // Agent may adjust its plan based on others
    }

    public AgentId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public WorldModel getLocalWorldModel() {
        return localWorldModel;
    }

    public Planner getLocalPlanner() {
        return localPlanner;
    }

    public Trajectory getLocalPlan() {
        return localPlan;
    }

    public void addBelief(Belief belief) {
        beliefs.add(belief);
    }

    public void addGoal(Goal goal) {
        goals.add(goal);
    }

    public Set<Goal> getGoals() {
        return Collections.unmodifiableSet(goals);
    }

    public PlanningContext getLocalContext() {
        return localContext;
    }

    public void setLocalContext(PlanningContext context) {
        this.localContext = context;
    }

    public void addTool(ExternalTool tool) {
        availableTools.add(tool);
    }

    public Set<ExternalTool> getAvailableTools() {
        return Collections.unmodifiableSet(availableTools);
    }

    /**
     * Check if agent can use external tool.
     */
    public boolean canUse(ExternalTool tool) {
        return availableTools.contains(tool);
    }

    @Override
    public String toString() {
        return "Agent{" + "id=" + id + ", name='" + name + '\'' + '}';
    }
}
