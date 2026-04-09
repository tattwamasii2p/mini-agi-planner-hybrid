package com.adam.agri.planner.multiagent;

/**
 * Belief held by an agent.
 */
public final class Belief {
    private final String proposition;
    private final double confidence;
    private final long timestamp;

    public Belief(String proposition, double confidence) {
        this.proposition = proposition;
        this.confidence = confidence;
        this.timestamp = System.currentTimeMillis();
    }

    public String getProposition() { return proposition; }
    public double getConfidence() { return confidence; }
}
