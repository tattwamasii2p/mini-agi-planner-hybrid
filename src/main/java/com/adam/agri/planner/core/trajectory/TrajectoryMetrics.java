package com.adam.agri.planner.core.trajectory;

/**
 * Metrics for trajectory evaluation.
 */
public final class TrajectoryMetrics {
    private final double cumulativeCost;
    private final double cumulativeTime;
    private final double jointProbability;
    private final double estimatedRisk;
    private final double informationGain;

    public TrajectoryMetrics(double cumulativeCost, double cumulativeTime,
                               double jointProbability, double estimatedRisk,
                               double informationGain) {
        this.cumulativeCost = cumulativeCost;
        this.cumulativeTime = cumulativeTime;
        this.jointProbability = jointProbability;
        this.estimatedRisk = estimatedRisk;
        this.informationGain = informationGain;
    }

    public static TrajectoryMetrics zero() {
        return new TrajectoryMetrics(0.0, 0.0, 1.0, 0.0, 0.0);
    }

    public double getCumulativeCost() {
        return cumulativeCost;
    }

    public double getCumulativeTime() {
        return cumulativeTime;
    }

    public double getJointProbability() {
        return jointProbability;
    }

    public double getEstimatedRisk() {
        return estimatedRisk;
    }

    public double getInformationGain() {
        return informationGain;
    }

    /**
     * Sequential combination of metrics (one after another).
     */
    public TrajectoryMetrics combine(TrajectoryMetrics other) {
        return new TrajectoryMetrics(
            this.cumulativeCost + other.cumulativeCost,
            this.cumulativeTime + other.cumulativeTime,
            this.jointProbability * other.jointProbability,
            Math.max(this.estimatedRisk, other.estimatedRisk),
            this.informationGain + other.informationGain
        );
    }

    /**
     * Utility function combining all metrics.
     */
    public double computeUtility(double costWeight, double timeWeight,
                                  double probWeight, double riskWeight) {
        return probWeight * jointProbability
            - costWeight * cumulativeCost
            - timeWeight * cumulativeTime
            - riskWeight * estimatedRisk;
    }

    @Override
    public String toString() {
        return "TrajectoryMetrics{" +
               "cost=" + cumulativeCost +
               ", time=" + cumulativeTime +
               ", prob=" + jointProbability +
               ", risk=" + estimatedRisk +
               '}';
    }
}
