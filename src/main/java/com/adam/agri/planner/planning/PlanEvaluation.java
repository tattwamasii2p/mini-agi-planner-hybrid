package com.adam.agri.planner.planning;

import com.adam.agri.planner.core.action.ActionOutcome;

/**
 * Result of evaluating a plan in physics model.
 */
public final class PlanEvaluation {
    private final boolean physicallyValid;
    private final double expectedCost;
    private final double expectedTime;
    private final double successProbability;
    private final double riskScore;
    private final String failureReason;

    public PlanEvaluation(boolean physicallyValid, double expectedCost,
                          double expectedTime, double successProbability,
                          double riskScore, String failureReason) {
        this.physicallyValid = physicallyValid;
        this.expectedCost = expectedCost;
        this.expectedTime = expectedTime;
        this.successProbability = successProbability;
        this.riskScore = riskScore;
        this.failureReason = failureReason;
    }

    public static PlanEvaluation success(ActionOutcome outcome) {
        return new PlanEvaluation(true, outcome.getCost(), outcome.getTime(),
                                  outcome.getProbability(), outcome.getRisk(), null);
    }

    public static PlanEvaluation failure(String reason) {
        return new PlanEvaluation(false, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
                                  0.0, Double.POSITIVE_INFINITY, reason);
    }

    public boolean isPhysicallyValid() {
        return physicallyValid;
    }

    public double getExpectedCost() {
        return expectedCost;
    }

    public double getExpectedTime() {
        return expectedTime;
    }

    public double getSuccessProbability() {
        return successProbability;
    }

    public double getRiskScore() {
        return riskScore;
    }

    public String getFailureReason() {
        return failureReason;
    }

    /**
     * Overall score combining all metrics.
     */
    public double getScore() {
        if (!physicallyValid) return Double.NEGATIVE_INFINITY;
        return successProbability * 100 - expectedCost - riskScore;
    }

    @Override
    public String toString() {
        if (physicallyValid) {
            return String.format("PlanEvaluation{valid=true, cost=%.2f, time=%.2f, prob=%.2f, risk=%.2f}",
                expectedCost, expectedTime, successProbability, riskScore);
        }
        return "PlanEvaluation{valid=false, reason=" + failureReason + '}';
    }
}
