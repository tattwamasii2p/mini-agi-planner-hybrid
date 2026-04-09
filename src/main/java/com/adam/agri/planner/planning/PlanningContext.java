package com.adam.agri.planner.planning;

import com.adam.agri.planner.core.constraints.ConstraintSet;

import java.util.Set;
import java.util.HashSet;

/**
 * Planning context with constraints and preferences.
 */
public final class PlanningContext {
    private ConstraintSet hardConstraints;
    private ConstraintSet softConstraints;
    private WeightConfig weights;
    private long timeoutMs;
    private int maxDepth;
    private int maxCandidates;
    private Set<ExternalTool> availableTools;

    public PlanningContext() {
        this.hardConstraints = new ConstraintSet();
        this.softConstraints = new ConstraintSet();
        this.weights = new WeightConfig(1.0, 1.0, 1.0, 0.5); // cost, risk, time, prob
        this.timeoutMs = 30000; // 30 seconds
        this.maxDepth = 100;
        this.maxCandidates = 10;
        this.availableTools = new HashSet<>();
    }

    public PlanningContext withTimeout(long timeoutMs) {
        this.timeoutMs = timeoutMs;
        return this;
    }

    public PlanningContext withMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
        return this;
    }

    public PlanningContext withMaxCandidates(int maxCandidates) {
        this.maxCandidates = maxCandidates;
        return this;
    }

    public PlanningContext withTools(Set<ExternalTool> tools) {
        this.availableTools = new HashSet<>(tools);
        return this;
    }

    public ConstraintSet getHardConstraints() {
        return hardConstraints;
    }

    public ConstraintSet getSoftConstraints() {
        return softConstraints;
    }

    public WeightConfig getWeights() {
        return weights;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public int getMaxCandidates() {
        return maxCandidates;
    }

    public Set<ExternalTool> getAvailableTools() {
        return availableTools;
    }

    public boolean hasTool(ExternalTool tool) {
        return availableTools.contains(tool);
    }
}
