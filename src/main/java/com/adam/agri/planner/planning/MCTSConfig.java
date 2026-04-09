package com.adam.agri.planner.planning;

/**
 * Configuration for MCTS planner.
 */
public final class MCTSConfig {
    private final int numSimulations;
    private final double cPuct;
    private final int maxRolloutDepth;
    private final double discountFactor;
    private final double constraintViolationPenalty;
    private final double goalReward;
    private final double valueMixRatio;

    public MCTSConfig(int numSimulations, double cPuct, int maxRolloutDepth,
                      double discountFactor, double constraintViolationPenalty,
                      double goalReward, double valueMixRatio) {
        this.numSimulations = numSimulations;
        this.cPuct = cPuct;
        this.maxRolloutDepth = maxRolloutDepth;
        this.discountFactor = discountFactor;
        this.constraintViolationPenalty = constraintViolationPenalty;
        this.goalReward = goalReward;
        this.valueMixRatio = valueMixRatio;
    }

    public static MCTSConfig defaultConfig() {
        return new MCTSConfig(
            800,    // numSimulations
            1.414,  // cPuct (sqrt(2))
            20,     // maxRolloutDepth
            0.95,   // discountFactor
            -100,   // constraintViolationPenalty
            100,    // goalReward
            0.5     // valueMixRatio
        );
    }

    public int getNumSimulations() { return numSimulations; }
    public double getCPuct() { return cPuct; }
    public int getMaxRolloutDepth() { return maxRolloutDepth; }
    public double getDiscountFactor() { return discountFactor; }
    public double getConstraintViolationPenalty() { return constraintViolationPenalty; }
    public double getGoalReward() { return goalReward; }
    public double getValueMixRatio() { return valueMixRatio; }
}
