package com.adam.agri.planner.planning;

/**
 * Weight configuration for multi-objective planning.
 */
public final class WeightConfig {
    private final double costWeight;
    private final double riskWeight;
    private final double timeWeight;
    private final double probWeight;

    public WeightConfig(double costWeight, double riskWeight,
                        double timeWeight, double probWeight) {
        this.costWeight = costWeight;
        this.riskWeight = riskWeight;
        this.timeWeight = timeWeight;
        this.probWeight = probWeight;
    }

    public double getCostWeight() { return costWeight; }
    public double getRiskWeight() { return riskWeight; }
    public double getTimeWeight() { return timeWeight; }
    public double getProbWeight() { return probWeight; }

    /**
     * Standard configuration: minimize cost and risk.
     */
    public static WeightConfig standard() {
        return new WeightConfig(1.0, 0.5, 0.3, 0.0);
    }

    /**
     * Safety-first: minimize risk above all.
     */
    public static WeightConfig safetyFirst() {
        return new WeightConfig(0.5, 2.0, 0.3, 0.0);
    }

    /**
     * Speed-first: minimize time.
     */
    public static WeightConfig speedFirst() {
        return new WeightConfig(0.5, 0.3, 2.0, 0.0);
    }
}
