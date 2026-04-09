package com.adam.agri.planner.planning;

import com.adam.agri.planner.core.action.Action;
import com.adam.agri.planner.core.state.*;
import com.adam.agri.planner.core.trajectory.Trajectory;
import com.adam.agri.planner.physical.worldmodel.WorldModel;

import java.util.*;

/**
 * Dijkstra-based planner with modal reasoning (belief/risk).
 * Weight = cost + α * risk (modal HoTT correspondence).
 *
 * From thread log: "Dijkstra with weight = cost + α * risk
 * → зачаток modal reasoning (belief/risk)"
 */
public class DijkstraPlanner implements Planner {

    // Weight configuration for multi-objective optimization
    private final WeightConfig weights;

    // Action space (simplified - should be injected)
    private final List<Action> availableActions;

    // Statistics from last run
    private PlanningStats lastStats;

    public DijkstraPlanner(WeightConfig weights, List<Action> actions) {
        this.weights = weights;
        this.availableActions = new ArrayList<>(actions);
        this.lastStats = PlanningStats.empty();
    }

    public DijkstraPlanner(List<Action> actions) {
        this(WeightConfig.standard(), actions);
    }

    @Override
    public Optional<Plan> plan(State initial, Goal goal, PlanningContext context) {
        long startTime = System.currentTimeMillis();

        PriorityQueue<SearchNode> frontier = new PriorityQueue<>(
            Comparator.comparingDouble(this::evaluationFunction)
        );

        Map<StateId, Double> bestCost = new HashMap<>();
        Set<StateId> visited = new HashSet<>();

        SearchNode startNode = new SearchNode(initial, null, null, 0, 0, 0);
        frontier.add(startNode);
        bestCost.put(initial.getId(), 0.0);

        int nodesExpanded = 0;
        int nodesVisited = 0;

        while (!frontier.isEmpty()) {
            SearchNode current = frontier.poll();
            nodesVisited++;

            // Check timeout
            if (System.currentTimeMillis() - startTime > context.getTimeoutMs()) {
                lastStats = new PlanningStats(nodesExpanded, nodesVisited,
                    System.currentTimeMillis() - startTime, 0, 0);
                return Optional.empty();
            }

            // Goal check
            if (goal.isSatisfiedBy(current.getState())) {
                Trajectory trajectory = extractTrajectory(current);
                Plan plan = new Plan(trajectory, goal);
                lastStats = new PlanningStats(nodesExpanded, nodesVisited,
                    System.currentTimeMillis() - startTime, trajectory.getActions().size(),
                    trajectory.cost());
                return Optional.of(plan);
            }

            if (visited.contains(current.getState().getId())) {
                continue;
            }
            visited.add(current.getState().getId());
            nodesExpanded++;

            // Check max depth
            if (current.getDepth() >= context.getMaxDepth()) {
                continue;
            }

            // Expand successors
            for (Action action : getApplicableActions(current.getState())) {
                State nextState = action.apply((SymbolicState) current.getState());

                // Check hard constraints
                boolean violates = context.getHardConstraints().getHardConstraints().stream()
                    .anyMatch(c -> !c.isSatisfiedBy(nextState));
                if (violates) {
                    continue;
                }

                double newCost = computeCost(current, action, nextState);
                double existingCost = bestCost.getOrDefault(nextState.getId(), Double.POSITIVE_INFINITY);

                if (newCost < existingCost) {
                    bestCost.put(nextState.getId(), newCost);

                    // Estimate risk for modal reasoning component
                    double estimatedRisk = estimateRisk(current, action, nextState);
                    double modalCost = computeModalCost(newCost, estimatedRisk);

                    SearchNode nextNode = new SearchNode(
                        nextState, current, action,
                        newCost, estimatedRisk, current.getDepth() + 1
                    );
                    frontier.add(nextNode);
                }
            }
        }

        lastStats = new PlanningStats(nodesExpanded, nodesVisited,
            System.currentTimeMillis() - startTime, 0, 0);
        return Optional.empty();
    }

    @Override
    public Optional<Plan> plan(BeliefState initial, Goal goal, PlanningContext context) {
        // For belief states, plan for each hypothesis and merge
        Set<State> support = initial.getSupport(0.1);

        List<Plan> candidatePlans = new ArrayList<>();
        for (State state : support) {
            // Convert to symbolic state for planning
            // Simplified: use state as-is
            Optional<Plan> plan = plan((State) state, goal, context);
            plan.ifPresent(candidatePlans::add);
        }

        // Select most robust plan
        return candidatePlans.stream()
            .max(Comparator.comparingDouble(Plan::getSuccessProbability));
    }

    @Override
    public List<Plan> generateCandidates(State initial, Goal goal, int maxCandidates) {
        // Run Dijkstra multiple times with different heuristics to get candidates
        List<Plan> candidates = new ArrayList<>();

        // Try with different weight configurations
        WeightConfig[] configs = {
            WeightConfig.standard(),
            WeightConfig.safetyFirst(),
            WeightConfig.speedFirst()
        };

        for (WeightConfig config : configs) {
            if (candidates.size() >= maxCandidates) break;

            DijkstraPlanner variant = new DijkstraPlanner(config, availableActions);
            PlanningContext ctx = new PlanningContext().withMaxCandidates(1);

            variant.plan(initial, goal, ctx).ifPresent(candidates::add);
        }

        return candidates;
    }

    @Override
    public PlanEvaluation evaluate(Plan plan, WorldModel world) {
        // Simulate each action
        double totalCost = 0;
        double totalTime = 0;
        double totalProb = 1.0;
        double totalRisk = 0;

        for (Action action : plan.getActions()) {
            // Simulate action outcome
            // Simplified evaluation
            totalCost += 1.0; // Placeholder
            totalTime += 1.0;
            totalProb *= 0.95; // 95% success per action
            totalRisk += 0.1;
        }

        return new PlanEvaluation(true, totalCost, totalTime, totalProb, totalRisk, null);
    }

    @Override
    public PlanningStats getLastRunStats() {
        return lastStats;
    }

    /**
     * Evaluation function for priority queue.
     * f(n) = g(n) + h(n) + α * risk(n)
     * g(n) = cumulative cost
     * α = risk weight (modal reasoning component)
     */
    private double evaluationFunction(SearchNode node) {
        double g = node.getCumulativeCost();
        double h = heuristic(node);
        double modal = weights.getRiskWeight() * node.getEstimatedRisk();
        return g + h + modal;
    }

    /**
     * Heuristic estimate to goal.
     */
    private double heuristic(SearchNode node) {
        // Simplified: constant
        return 0.0; // Could be A* with admissible heuristic
    }

    /**
     * Compute cost of transition.
     */
    private double computeCost(SearchNode current, Action action, State nextState) {
        // Cost = action cost + state cost
        return current.getCumulativeCost() + 1.0; // Simplified
    }

    /**
     * Modal reasoning: estimate risk at current node.
     */
    private double estimateRisk(SearchNode current, Action action, State nextState) {
        // Simplified risk estimation
        // Real implementation would use world model to estimate
        return 0.1; // Base risk
    }

    /**
     * Combine cost and risk with modal weighting.
     */
    private double computeModalCost(double cost, double risk) {
        return cost + weights.getRiskWeight() * risk;
    }

    private List<Action> getApplicableActions(State state) {
        if (!(state instanceof SymbolicState)) {
            return Collections.emptyList();
        }
        SymbolicState symState = (SymbolicState) state;

        return availableActions.stream()
            .filter(a -> a.isApplicableIn(symState))
            .toList();
    }

    private Trajectory extractTrajectory(SearchNode goalNode) {
        List<Action> actions = new ArrayList<>();
        SearchNode current = goalNode;

        while (current.getParent() != null) {
            if (current.getAction() != null) {
                actions.add(0, current.getAction());
            }
            current = current.getParent();
        }

        StateId start = actions.isEmpty() ? goalNode.getState().getId() : goalNode.getState().getId();
        // Need to properly track start through parent chain
        while (current.getParent() != null) {
            current = current.getParent();
        }
        start = current.getState().getId();

        return new com.adam.agri.planner.core.trajectory.Trajectory(
            start,
            goalNode.getState().getId(),
            actions,
            new com.adam.agri.planner.core.trajectory.TrajectoryMetrics(
                goalNode.getCumulativeCost(),
                0, // time
                Math.pow(0.95, actions.size()), // probability
                goalNode.getEstimatedRisk(),
                0 // info gain
            )
        );
    }

    /**
     * Search node for Dijkstra/A*.
     */
    private static class SearchNode {
        private final State state;
        private final SearchNode parent;
        private final Action action;
        private final double cumulativeCost;
        private final double estimatedRisk;
        private final int depth;

        public SearchNode(State state, SearchNode parent, Action action,
                          double cumulativeCost, double estimatedRisk, int depth) {
            this.state = state;
            this.parent = parent;
            this.action = action;
            this.cumulativeCost = cumulativeCost;
            this.estimatedRisk = estimatedRisk;
            this.depth = depth;
        }

        public State getState() { return state; }
        public SearchNode getParent() { return parent; }
        public Action getAction() { return action; }
        public double getCumulativeCost() { return cumulativeCost; }
        public double getEstimatedRisk() { return estimatedRisk; }
        public int getDepth() { return depth; }
    }
}
