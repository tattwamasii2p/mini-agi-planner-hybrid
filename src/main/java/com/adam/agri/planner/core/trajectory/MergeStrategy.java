package com.adam.agri.planner.core.trajectory;

/**
 * Strategy for merging trajectory metrics.
 */
public enum MergeStrategy {
    SEQUENTIAL {
        @Override
        public TrajectoryMetrics combine(TrajectoryMetrics a, TrajectoryMetrics b) {
            return a.combine(b);
        }
    },
    PARALLEL {
        @Override
        public TrajectoryMetrics combine(TrajectoryMetrics a, TrajectoryMetrics b) {
            // For parallel execution, take max of times, sum of costs/probabilities
            return new TrajectoryMetrics(
                a.getCumulativeCost() + b.getCumulativeCost(),
                Math.max(a.getCumulativeTime(), b.getCumulativeTime()),
                a.getJointProbability() * b.getJointProbability(),
                Math.max(a.getEstimatedRisk(), b.getEstimatedRisk()),
                a.getInformationGain() + b.getInformationGain()
            );
        }
    },
    ALTERNATIVE {
        @Override
        public TrajectoryMetrics combine(TrajectoryMetrics a, TrajectoryMetrics b) {
            // For alternative paths, take weighted by probability
            double totalProb = a.getJointProbability() + b.getJointProbability();
            if (totalProb == 0) return a;
            double w1 = a.getJointProbability() / totalProb;
            double w2 = b.getJointProbability() / totalProb;
            return new TrajectoryMetrics(
                w1 * a.getCumulativeCost() + w2 * b.getCumulativeCost(),
                w1 * a.getCumulativeTime() + w2 * b.getCumulativeTime(),
                1.0 - (1.0 - a.getJointProbability()) * (1.0 - b.getJointProbability()),
                w1 * a.getEstimatedRisk() + w2 * b.getEstimatedRisk(),
                w1 * a.getInformationGain() + w2 * b.getInformationGain()
            );
        }
    };

    public abstract TrajectoryMetrics combine(TrajectoryMetrics a, TrajectoryMetrics b);
}
