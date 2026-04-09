package com.adam.agri.planner.core.action;

import java.util.HashMap;
import java.util.Map;

import com.adam.agri.planner.core.state.BeliefState;
import com.adam.agri.planner.core.state.State;

/**
 * Action outcome from physics simulation.
 */
public final class ActionOutcome {
    private final State resultState;
    private final BeliefState resultBelief;
    private final double probability;
    private final double time;
    private final double cost;
    private final double risk;
    private final Map<String, Object> metrics;

    public ActionOutcome(State resultState, double probability,
                         double time, double cost, double risk) {
        this.resultState = resultState;
        this.resultBelief = BeliefState.pointMass(resultState);
        this.probability = probability;
        this.time = time;
        this.cost = cost;
        this.risk = risk;
        this.metrics = new HashMap<>();
    }

    public ActionOutcome(BeliefState resultBelief, double probability,
                         double time, double cost, double risk) {
        this.resultState = null;
        this.resultBelief = resultBelief;
        this.probability = probability;
        this.time = time;
        this.cost = cost;
        this.risk = risk;
        this.metrics = new HashMap<>();
    }

    public State getResultState() {
        if (resultState == null) {
            throw new IllegalStateException("Outcome represents belief distribution, not single state");
        }
        return resultState;
    }

    public BeliefState getResultBelief() {
        return resultBelief;
    }

    public double getProbability() {
        return probability;
    }

    public double getTime() {
        return time;
    }

    public double getCost() {
        return cost;
    }

    public double getRisk() {
        return risk;
    }

    public Map<String, Object> getMetrics() {
        return new HashMap<>(metrics);
    }

    public void addMetric(String key, Object value) {
        metrics.put(key, value);
    }

    public double getUtility() {
        return probability * (1.0 / (1.0 + cost + risk));
    }

    @Override
    public String toString() {
        return "ActionOutcome{" +
               "probability=" + probability +
               ", time=" + time +
               ", cost=" + cost +
               ", risk=" + risk +
               '}';
    }
}
