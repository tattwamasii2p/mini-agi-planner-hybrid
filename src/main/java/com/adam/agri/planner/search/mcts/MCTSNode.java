package com.adam.agri.planner.search.mcts;

import com.adam.agri.planner.core.action.Action;
import com.adam.agri.planner.core.state.State;
import com.adam.agri.planner.core.state.BeliefState;

import java.util.*;

/**
 * Node in MCTS search tree.
 * Can represent either concrete state or belief state.
 */
public class MCTSNode {
    // State at this node
    private final State state;
    private final BeliefState belief;
    private final boolean isBeliefNode;

    // Action that led to this node
    private final Action action;

    // Parent in tree
    private final MCTSNode parent;

    // Children: action -> node
    private final Map<Action, MCTSNode> children;

    // Statistics for UCB
    private int visitCount;
    private double totalValue;
    private double totalSquaredValue; // For variance calculation

    // Action probabilities from prediction model (prior)
    private Map<Action, Double> priorProbabilities;

    // Untried actions
    private final List<Action> untriedActions;

    // Terminal flag
    private final boolean terminal;

    // Node depth
    private final int depth;

    public MCTSNode(State state, Action action, MCTSNode parent,
                    List<Action> availableActions, boolean terminal, int depth) {
        this.state = state;
        this.belief = null;
        this.isBeliefNode = false;
        this.action = action;
        this.parent = parent;
        this.children = new HashMap<>();
        this.untriedActions = new ArrayList<>(availableActions);
        this.terminal = terminal;
        this.depth = depth;
        this.visitCount = 0;
        this.totalValue = 0.0;
        this.totalSquaredValue = 0.0;
        this.priorProbabilities = new HashMap<>();
    }

    public MCTSNode(BeliefState belief, Action action, MCTSNode parent,
                    List<Action> availableActions, boolean terminal, int depth) {
        this.state = null;
        this.belief = belief;
        this.isBeliefNode = true;
        this.action = action;
        this.parent = parent;
        this.children = new HashMap<>();
        this.untriedActions = new ArrayList<>(availableActions);
        this.terminal = terminal;
        this.depth = depth;
        this.visitCount = 0;
        this.totalValue = 0.0;
        this.totalSquaredValue = 0.0;
        this.priorProbabilities = new HashMap<>();
    }

    /**
     * Check if fully expanded (no untried actions).
     */
    public boolean isFullyExpanded() {
        return untriedActions.isEmpty();
    }

    /**
     * Check if leaf node (no children and fully expanded).
     */
    public boolean isLeaf() {
        return children.isEmpty() && isFullyExpanded();
    }

    /**
     * Check if terminal state.
     */
    public boolean isTerminal() {
        return terminal;
    }

    /**
     * Select child using UCB1 formula with PUCT variant.
     * U(s,a) = Q(s,a) + c_puct * P(a|s) * sqrt(N(s)) / (1 + N(s,a))
     *
     * Where:
     * Q = average value
     * P = prior probability from policy network
     * N(s) = parent visit count
     * N(s,a) = child visit count
     */
    public MCTSNode selectChildUCB(double cPuct) {
        double bestScore = Double.NEGATIVE_INFINITY;
        MCTSNode bestChild = null;

        for (Map.Entry<Action, MCTSNode> entry : children.entrySet()) {
            MCTSNode child = entry.getValue();

            double qValue = child.getQValue();
            double prior = priorProbabilities.getOrDefault(entry.getKey(), 1.0 / children.size());

            double uValue = cPuct * prior * Math.sqrt(this.visitCount) / (1 + child.visitCount);
            double score = qValue + uValue;

            if (score > bestScore) {
                bestScore = score;
                bestChild = child;
            }
        }

        return bestChild;
    }

    /**
     * Get Q-value (mean return) for this node.
     */
    public double getQValue() {
        if (visitCount == 0) return Double.POSITIVE_INFINITY; // Encourage exploration
        return totalValue / visitCount;
    }

    /**
     * Get value variance for uncertainty estimation.
     */
    public double getValueVariance() {
        if (visitCount < 2) return 0;
        double mean = totalValue / visitCount;
        double meanSq = totalSquaredValue / visitCount;
        return meanSq - mean * mean;
    }

    /**
     * Expand with a new action.
     */
    public Action selectUntriedAction() {
        if (untriedActions.isEmpty()) {
            return null;
        }
        // Select randomly or use prior
        int index = (int) (Math.random() * untriedActions.size());
        return untriedActions.remove(index);
    }

    /**
     * Add child node.
     */
    public void addChild(Action action, MCTSNode child) {
        children.put(action, child);
    }

    /**
     * Backpropagate value up the tree.
     */
    public void backpropagate(double value) {
        this.visitCount++;
        this.totalValue += value;
        this.totalSquaredValue += value * value;

        if (parent != null) {
            parent.backpropagate(value);
        }
    }

    /**
     * Update prior probabilities from prediction model.
     */
    public void updatePriors(Map<Action, Double> priors) {
        this.priorProbabilities = new HashMap<>(priors);
        // Normalize
        double sum = priors.values().stream().mapToDouble(Double::doubleValue).sum();
        if (sum > 0) {
            priorProbabilities.replaceAll((k, v) -> v / sum);
        }
    }

    // Getters
    public State getState() { return state; }
    public BeliefState getBelief() { return belief; }
    public boolean isBeliefNode() { return isBeliefNode; }
    public Action getAction() { return action; }
    public MCTSNode getParent() { return parent; }
    public Collection<MCTSNode> getChildren() { return children.values(); }
    public int getVisitCount() { return visitCount; }
    public double getTotalValue() { return totalValue; }
    public int getDepth() { return depth; }

    /**
     * Get action probabilities (for policy extraction).
     */
    public Map<Action, Double> getActionProbabilities(double temperature) {
        Map<Action, Double> probs = new HashMap<>();

        if (visitCount == 0) {
            return probs;
        }

        for (Map.Entry<Action, MCTSNode> entry : children.entrySet()) {
            double visits = entry.getValue().visitCount;
            double prob;
            if (temperature == 0) {
                // Greedy: max visits
                prob = visits == Collections.max(children.values(),
                    Comparator.comparingInt(MCTSNode::getVisitCount)).visitCount ? 1.0 : 0.0;
            } else {
                prob = Math.pow(visits, 1.0 / temperature);
            }
            probs.put(entry.getKey(), prob);
        }

        // Normalize
        double sum = probs.values().stream().mapToDouble(Double::doubleValue).sum();
        if (sum > 0) {
            probs.replaceAll((k, v) -> v / sum);
        }

        return probs;
    }

    @Override
    public String toString() {
        return String.format("MCTSNode{visits=%d, value=%.2f, depth=%d, terminal=%s}",
            visitCount, getQValue(), depth, terminal);
    }
}
