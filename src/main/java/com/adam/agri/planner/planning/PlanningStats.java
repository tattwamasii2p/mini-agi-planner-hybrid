package com.adam.agri.planner.planning;

/**
 * Planning statistics.
 */
public final class PlanningStats {
    private final int nodesExpanded;
    private final int nodesVisited;
    private final double planningTimeMs;
    private final int planLength;
    private final double planCost;

    public PlanningStats(int nodesExpanded, int nodesVisited,
                         double planningTimeMs, int planLength, double planCost) {
        this.nodesExpanded = nodesExpanded;
        this.nodesVisited = nodesVisited;
        this.planningTimeMs = planningTimeMs;
        this.planLength = planLength;
        this.planCost = planCost;
    }

    public static PlanningStats empty() {
        return new PlanningStats(0, 0, 0, 0, 0);
    }

    public int getNodesExpanded() { return nodesExpanded; }
    public int getNodesVisited() { return nodesVisited; }
    public double getPlanningTimeMs() { return planningTimeMs; }
    public int getPlanLength() { return planLength; }
    public double getPlanCost() { return planCost; }

    @Override
    public String toString() {
        return String.format("PlanningStats{expanded=%d, visited=%d, time=%.2fms, length=%d, cost=%.2f}",
            nodesExpanded, nodesVisited, planningTimeMs, planLength, planCost);
    }
}
