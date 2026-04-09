package com.adam.agri.planner.core.state;

import java.util.HashMap;
import java.util.Map;

/**
 * Physical state - low-level world configuration.
 * Contains measurable quantities, positions, velocities.
 */
public final class PhysicalState implements State {
    private final StateId id;
    private final double[] continuousState;
    private final Map<String, Double> measurableProperties;
    private final double timestamp;

    public PhysicalState(StateId id, double[] continuousState,
                         Map<String, Double> measurableProperties, double timestamp) {
        this.id = id;
        this.continuousState = continuousState.clone();
        this.measurableProperties = new HashMap<>(measurableProperties);
        this.timestamp = timestamp;
    }

    @Override
    public StateId getId() {
        return id;
    }

    @Override
    public StateType getType() {
        return StateType.PHYSICAL;
    }

    @Override
    public boolean isCompatible(State other) {
        return other.getType() == StateType.PHYSICAL;
    }

    @Override
    public State copy() {
        return new PhysicalState(StateId.generate(), continuousState.clone(),
                new HashMap<>(measurableProperties), timestamp);
    }

    public double[] getContinuousState() {
        return continuousState.clone();
    }

    public Map<String, Double> getMeasurableProperties() {
        return new HashMap<>(measurableProperties);
    }

    public double getTimestamp() {
        return timestamp;
    }

    /**
     * Euclidean distance to another physical state.
     */
    public double distance(PhysicalState other) {
        double[] otherState = other.continuousState;
        if (continuousState.length != otherState.length) {
            return Double.POSITIVE_INFINITY;
        }
        double sum = 0;
        for (int i = 0; i < continuousState.length; i++) {
            double diff = continuousState[i] - otherState[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    /**
     * Check if satisfies physical constraint.
     */
    public boolean satisfies(PhysicalConstraint constraint) {
        return constraint.isSatisfiedBy(this);
    }

    @Override
    public String toString() {
        return "PhysicalState{" + "id=" + id + ", timestamp=" + timestamp + '}';
    }
}
