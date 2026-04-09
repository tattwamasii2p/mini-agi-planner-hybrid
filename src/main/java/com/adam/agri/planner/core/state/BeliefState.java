package com.adam.agri.planner.core.state;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Belief state for POMDP-like planning.
 * Represents a probability distribution over states.
 */
public final class BeliefState implements State {
    private final StateId id;
    private final Map<State, Double> distribution;
    private final double timestamp;

    public BeliefState(StateId id, Map<State, Double> distribution, double timestamp) {
        this.id = id;
        this.distribution = new HashMap<>(distribution);
        this.timestamp = timestamp;
        normalize();
    }

    public BeliefState(StateId id) {
        this(id, new HashMap<>(), 0.0);
    }

    /**
     * Create belief from single state (point mass).
     */
    public static BeliefState pointMass(State state) {
        Map<State, Double> dist = new HashMap<>();
        dist.put(state, 1.0);
        return new BeliefState(StateId.generate(), dist, state.getTimestamp());
    }

    @Override
    public StateId getId() {
        return id;
    }

    @Override
    public StateType getType() {
        return StateType.BELIEF;
    }

    @Override
    public boolean isCompatible(State other) {
        return other.getType() == StateType.BELIEF || other.getType() == StateType.PHYSICAL 
        		|| other.getType() == StateType.SYMBOLIC;
    }

    @Override
    public State copy() {
        return new BeliefState(StateId.generate(), new HashMap<>(distribution), timestamp);
    }

    /**
     * Normalize distribution to sum to 1.0.
     */
    public BeliefState normalize() {
        double sum = distribution.values().stream().mapToDouble(Double::doubleValue).sum();
        if (sum > 0 && Math.abs(sum - 1.0) > 1e-10) {
            distribution.replaceAll((k, v) -> v / sum);
        }
        return this;
    }

    /**
     * Set probability for a state.
     */
    public void setProbability(State state, double probability) {
        distribution.put(state, probability);
    }

    /**
     * Get probability for a state.
     */
    public double probability(State state) {
        return distribution.getOrDefault(state, 0.0);
    }

    /**
     * Get support (states with non-zero probability above threshold).
     */
    public Set<State> getSupport(double threshold) {
        Set<State> support = new HashSet<>();
        for (Map.Entry<State, Double> entry : distribution.entrySet()) {
            if (entry.getValue() >= threshold) {
                support.add(entry.getKey());
            }
        }
        return support;
    }

    public Set<State> getSupport() {
        return getSupport(1e-10);
    }

    /**
     * Shannon entropy of the belief distribution.
     */
    public double entropy() {
        double entropy = 0.0;
        for (double p : distribution.values()) {
            if (p > 0) {
                entropy -= p * Math.log(p);
            }
        }
        return entropy;
    }

    /**
     * Check overlap with another belief state.
     */
    public boolean overlaps(BeliefState other) {
        for (State state : distribution.keySet()) {
            if (other.distribution.containsKey(state)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Merge two belief states (mixture distribution).
     */
    public BeliefState merge(BeliefState other) {
        Map<State, Double> merged = new HashMap<>(distribution);
        for (Map.Entry<State, Double> entry : other.distribution.entrySet()) {
            merged.merge(entry.getKey(), entry.getValue(), Double::sum);
        }
        return new BeliefState(StateId.generate(), merged, Math.max(timestamp, other.timestamp));
    }

    /**
     * Bayesian belief update: b' ∝ p(o|s') * Σ_s p(s'|s,a) * b(s)
     */
    public BeliefState update(ActionModel model, com.adam.agri.planner.core.action.Action action, Observation obs) {
        Map<State, Double> updated = new HashMap<>();

        // Prediction step: propagate each state through action
        for (Map.Entry<State, Double> entry : distribution.entrySet()) {
            State s = entry.getKey();
            double b_s = entry.getValue();

            // Get transition distribution p(s'|s,a)
            Map<State, Double> transitions = model.getTransitions(s, action);
            for (Map.Entry<State, Double> trans : transitions.entrySet()) {
                State sPrime = trans.getKey();
                double p_sPrime_given_s = trans.getValue();
                updated.merge(sPrime, b_s * p_sPrime_given_s, Double::sum);
            }
        }

        // Correction step (Bayes update)
        Map<State, Double> corrected = new HashMap<>();
        for (Map.Entry<State, Double> entry : updated.entrySet()) {
            State sPrime = entry.getKey();
            double likelihood = model.observationLikelihood(sPrime, obs);
            corrected.put(sPrime, likelihood * entry.getValue());
        }

        return new BeliefState(StateId.generate(), corrected, timestamp + 1).normalize();
    }

    /**
     * Get expected value of a property.
     */
    public double expectedValue(String property) {
        double expected = 0.0;
        for (Map.Entry<State, Double> entry : distribution.entrySet()) {
            State key = entry.getKey();
            if(key instanceof PhysicalState physicalState) {
				Double value = physicalState.getMeasurableProperties().get(property);
	            if (value != null) {
	                expected += entry.getValue() * value;
	            }
            }
        }
        return expected;
    }

    public double getTimestamp() {
        return timestamp;
    }

    public Map<State, Double> getDistribution() {
        return new HashMap<>(distribution);
    }

    @Override
    public String toString() {
        return "BeliefState{" + "id=" + id + ", states=" + distribution.size() +
               ", entropy=" + entropy() + ", timestamp=" + timestamp + '}';
    }

    /**
     * Interface for action model (transitions and observations).
     */
    public interface ActionModel {
        Map<State, Double> getTransitions(State state,
                                                    com.adam.agri.planner.core.action.Action action);
        double observationLikelihood(State state, Observation obs);
    }

    public interface Observation {
        String getType();
    }
}
