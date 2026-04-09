package com.adam.agri.planner.planning;

import com.adam.agri.planner.core.action.Action;
import com.adam.agri.planner.core.action.ActionOutcome;
import com.adam.agri.planner.core.state.*;
import com.adam.agri.planner.physical.worldmodel.WorldModel;
import com.adam.agri.planner.search.mcts.MCTSNode;

import java.util.*;

/**
 * Monte Carlo Tree Search planner.
 * AlphaZero-like with UCB selection and learned dynamics model.
 *
 * Key features:
 * - UCB1 with PUCT variant
 * - MCTS loop: Selection -> Expansion -> Simulation -> Backpropagation
 * - Constraint-aware rollouts
 * - Integration with belief states
 */
public class MCTSPlanner implements Planner {

    // World model (can be learned neural network)
    private final WorldModel dynamicsModel;

    // Prediction model (value and policy heads)
    private final PredictionModel predictionModel;

    // MCTS configuration
    private final MCTSConfig config;

    // Root node of search tree
    private MCTSNode root;

    // Statistics
    private PlanningStats lastStats;

    // Available actions
    private final List<Action> availableActions;

    public MCTSPlanner(WorldModel dynamicsModel, PredictionModel predictionModel,
                       MCTSConfig config, List<Action> actions) {
        this.dynamicsModel = dynamicsModel;
        this.predictionModel = predictionModel;
        this.config = config;
        this.availableActions = new ArrayList<>(actions);
        this.lastStats = PlanningStats.empty();
    }

    public MCTSPlanner(WorldModel dynamicsModel, List<Action> actions) {
        this(dynamicsModel, null, MCTSConfig.defaultConfig(), actions);
    }

    @Override
    public Optional<Plan> plan(State initial, Goal goal, PlanningContext context) {
        long startTime = System.currentTimeMillis();

        // Initialize root with current state
        List<Action> legalActions = getLegalActions(initial);
        root = new MCTSNode(initial, null, null, legalActions, false, 0);

        // Initialize root priors from prediction model
        if (predictionModel != null) {
            Prediction prediction = predictionModel.predict(initial);
            root.updatePriors(prediction.getActionProbabilities(legalActions));
        }

        // Main MCTS loop
        int nodesExpanded = 0;
        for (int i = 0; i < config.getNumSimulations(); i++) {
            // Check timeout
            if (System.currentTimeMillis() - startTime > context.getTimeoutMs()) {
                break;
            }

            // 1. Selection: traverse tree using UCB
            MCTSNode selected = select(root);

            // 2. Expansion: add child nodes for untried actions
            if (!selected.isTerminal()) {
                selected = expand(selected);
                nodesExpanded++;
            }

            // 3. Simulation (rollout) with learned model
            double reward = rolloutWithConstraints(selected, context);

            // 4. Backpropagation: update statistics
            backpropagate(selected, reward);
        }

        // Extract best action sequence
        Optional<Plan> plan = extractBestPlan(root, goal);

        lastStats = new PlanningStats(
            nodesExpanded,
            root.getVisitCount(),
            System.currentTimeMillis() - startTime,
            plan.map(Plan::length).orElse(0),
            plan.map(Plan::getExpectedCost).orElse(0.0)
        );

        return plan;
    }

    @Override
    public Optional<Plan> plan(BeliefState initial, Goal goal, PlanningContext context) {
        // Belief-state MCTS: each node is a belief
        List<Action> legalActions = getLegalActionsForBelief(initial);
        root = new MCTSNode(initial, null, null, legalActions, false, 0);

        // Similar MCTS loop but with belief propagation
        long startTime = System.currentTimeMillis();
        int nodesExpanded = 0;

        for (int i = 0; i < config.getNumSimulations(); i++) {
            if (System.currentTimeMillis() - startTime > context.getTimeoutMs()) {
                break;
            }

            BeliefMCTSPath path = selectBelief(root);
            // Simplified: expansion not implemented for belief state
            double reward = 0.0; // Placeholder
            backpropagateBelief(path, reward);
            nodesExpanded++;
        }

        // Extract robust policy - placeholder
        return plan((State) initial.getSupport().iterator().next(), goal, context);
    }

    @Override
    public List<Plan> generateCandidates(State initial, Goal goal, int maxCandidates) {
        // Run MCTS multiple times with different exploration strengths
        List<Plan> candidates = new ArrayList<>();
        double[] temperatures = {0.0, 0.5, 1.0, 2.0}; // Greedy to exploratory

        for (double temp : temperatures) {
            if (candidates.size() >= maxCandidates) break;
            plan(initial, goal, new PlanningContext()).ifPresent(candidates::add);
        }

        return candidates;
    }

    @Override
    public PlanEvaluation evaluate(Plan plan, WorldModel world) {
        // Simulate through MCTS rollout
        double totalReward = 0;
        double discount = 1.0;

        for (Action action : plan.getActions()) {
            ActionOutcome outcome = dynamicsModel.simulate(null, action); // Simplified
            totalReward += discount * (outcome.getProbability() * 10 - outcome.getCost());
            discount *= config.getDiscountFactor();
        }

        return new PlanEvaluation(
            true, plan.getExpectedCost(), 0,
            plan.getSuccessProbability(), 0, null
        );
    }

    @Override
    public PlanningStats getLastRunStats() {
        return lastStats;
    }

    // MCTS Core Methods

    /**
     * Selection using UCB1 formula with PUCT variant.
     * U(s,a) = Q(s,a) + c_puct * P(a|s) * sqrt(N(s)) / (1 + N(s,a))
     */
    private MCTSNode select(MCTSNode node) {
        while (!node.isLeaf()) {
            node = node.selectChildUCB(config.getCPuct());
            if (node == null) break;
        }
        return node;
    }

    /**
     * Expand node with untried action.
     */
    private MCTSNode expand(MCTSNode node) {
        Action action = node.selectUntriedAction();
        if (action == null) return node;

        // Simulate action
        State nextState = action.apply((SymbolicState) node.getState());
        boolean terminal = isTerminal(nextState);
        List<Action> nextActions = terminal ? Collections.emptyList() : getLegalActions(nextState);

        // Create child node
        MCTSNode child = new MCTSNode(
            nextState, action, node, nextActions, terminal, node.getDepth() + 1
        );
        node.addChild(action, child);

        return child;
    }

    /**
     * Rollout with learned model and constraint checking.
     * Terminates on: goal, constraint violation, or max depth.
     */
    private double rolloutWithConstraints(MCTSNode node, PlanningContext context) {
        // Check constraint violation at current node
        if (violatesHardConstraints(node, context)) {
            return config.getConstraintViolationPenalty();
        }

        if (node.isTerminal()) {
            return evaluateTerminal(node);
        }

        // Value from prediction model or rollout
        if (predictionModel != null && Math.random() < config.getValueMixRatio()) {
            Prediction pred = predictionModel.predict(node.getState());
            return pred.getValue();
        }

        // Perform rollout with learned dynamics
        State current = node.getState();
        double cumulativeReward = 0;
        double discount = 1.0;

        for (int step = 0; step < config.getMaxRolloutDepth(); step++) {
            List<Action> actions = getLegalActions(current);
            if (actions.isEmpty()) break;

            // Select action (random or policy-guided)
            Action action = selectRolloutAction(actions);

            // Simulate with dynamics model
            ActionOutcome outcome = dynamicsModel.simulate(null, action);

            // Check constraint violation
            if (violatesHardConstraints(outcome, context)) {
                cumulativeReward += discount * config.getConstraintViolationPenalty();
                break;
            }

            cumulativeReward += discount * evaluateOutcome(outcome);
            discount *= config.getDiscountFactor();

            // Update state
            current = outcome.getResultState();

            // Check termination
            if (isTerminal(current)) {
                cumulativeReward += discount * config.getGoalReward();
                break;
            }
        }

        return cumulativeReward;
    }

    private void backpropagate(MCTSNode node, double reward) {
        if (node != null) {
            node.backpropagate(reward);
        }
    }

    private Optional<Plan> extractBestPlan(MCTSNode root, Goal goal) {
        // Select action with highest visit count (or Q-value)
        if (root.getChildren().isEmpty()) {
            return Optional.empty();
        }

        MCTSNode bestChild = root.getChildren().stream()
            .max(Comparator.comparingInt(MCTSNode::getVisitCount))
            .orElse(null);

        if (bestChild == null) return Optional.empty();

        // Build trajectory from root to deepest visited
        List<Action> actions = new ArrayList<>();
        MCTSNode current = bestChild;
        while (current != null && current.getAction() != null) {
            actions.add(current.getAction());
            // Follow most visited child
            MCTSNode next = current.getChildren().stream()
                .max(Comparator.comparingInt(MCTSNode::getVisitCount))
                .orElse(null);
            current = next;
        }

        if (actions.isEmpty()) return Optional.empty();

        // Build trajectory
        com.adam.agri.planner.core.trajectory.Trajectory trajectory =
            new com.adam.agri.planner.core.trajectory.Trajectory(
                root.getState().getId(),
                current != null ? current.getState().getId() : root.getState().getId(),
                actions,
                new com.adam.agri.planner.core.trajectory.TrajectoryMetrics(
                    actions.size(),
                    actions.size() * 1.0,
                    Math.pow(0.95, actions.size()),
                    0.1,
                    0
                )
            );

        return Optional.of(new Plan(trajectory, goal));
    }

    // Helper methods

    private List<Action> getLegalActions(State state) {
        if (!(state instanceof SymbolicState)) {
            return Collections.emptyList();
        }
        return availableActions.stream()
            .filter(a -> a.isApplicableIn(state))
            .toList();
    }

    private List<Action> getLegalActionsForBelief(BeliefState belief) {
        // Actions applicable to all states in support
        Set<State> support = belief.getSupport(0.1);
        return availableActions.stream()
            .filter(a -> support.stream().allMatch(s -> a.isApplicableIn(s)))
            .toList();
    }

    private boolean isTerminal(State state) {
        // Terminal if no legal actions
        return getLegalActions(state).isEmpty();
    }

    private boolean violatesHardConstraints(MCTSNode node, PlanningContext ctx) {
        return violatesHardConstraints(node.getState(), ctx);
    }

    private boolean violatesHardConstraints(State state, PlanningContext ctx) {
        return ctx.getHardConstraints().getHardConstraints().stream()
            .anyMatch(c -> !c.isSatisfiedBy(state));
    }

    private boolean violatesHardConstraints(ActionOutcome outcome, PlanningContext ctx) {
        // Check if outcome violates constraints
        return false; // Simplified
    }

    private double evaluateOutcome(ActionOutcome outcome) {
        return outcome.getProbability() * 10 - outcome.getCost() - outcome.getRisk();
    }

    private double evaluateTerminal(MCTSNode node) {
        // Terminal value based on state evaluation
        return config.getGoalReward();
    }

    private Action selectRolloutAction(List<Action> actions) {
        // Default: random
        return actions.get((int) (Math.random() * actions.size()));
    }

    // Belief-state MCTS helpers (simplified)
    private BeliefMCTSPath selectBelief(MCTSNode root) {
        // Simplified
        return new BeliefMCTSPath(root, Collections.emptyList());
    }

    private BeliefMCTSNode expandBelief(BeliefMCTSNode node) {
        return node;
    }

    private double rolloutBelief(BeliefMCTSNode node, PlanningContext ctx) {
        return 0.0;
    }

    private void backpropagateBelief(BeliefMCTSPath path, double reward) {
    }

    // TODO Stub classes for belief MCTS
    private static class BeliefMCTSNode {
        private final BeliefState belief;
        BeliefMCTSNode(BeliefState b) { this.belief = b; }
    }

    private static class BeliefMCTSPath {
        private final MCTSNode node;
        private final List<BeliefMCTSNode> path;
        BeliefMCTSPath(MCTSNode n, List<BeliefMCTSNode> p) {
            this.node = n; this.path = p;
        }
        MCTSNode getNode() { return node; }
    }

    // Interfaces for learned models
    public interface PredictionModel {
        Prediction predict(State state);
    }

    public interface Prediction {
        double getValue();
        Map<Action, Double> getActionProbabilities(List<Action> actions);
    }
}

