package com.adam.agri.planner.trajectories.builder.goals;

import java.util.*;

/**
 * Model of user desires, preferences, and soft constraints.
 * Represents "nice to have" features that aren't strict requirements.
 *
 * Desires are weighted and can be combined into an overall utility function.
 * Unlike hard constraints (must be satisfied), desires can be traded off.
 */
public class DesireModel {

    // User preferences: key -> (description, weight)
    private final Map<String, Preference> preferences;

    // Soft constraints: text -> priority
    private final Map<String, Double> softConstraints;

    // Ideal scenarios
    private final List<String> ideals;

    // Preference weights for trajectory optimization
    private final PreferenceWeights weights;

    public DesireModel() {
        this.preferences = new HashMap<>();
        this.softConstraints = new HashMap<>();
        this.ideals = new ArrayList<>();
        this.weights = new PreferenceWeights();
    }

    /**
     * Add a user preference.
     */
    public void addPreference(String key, String description) {
        preferences.put(key, new Preference(description, 1.0));
    }

    /**
     * Add a user preference with custom weight.
     */
    public void addPreference(String key, String description, double weight) {
        preferences.put(key, new Preference(description, weight));
    }

    /**
     * Add a soft constraint (should try to satisfy, but not required).
     */
    public void addSoftConstraint(String constraint) {
        softConstraints.put(constraint, 0.7);
    }

    /**
     * Add a soft constraint with specific priority.
     */
    public void addSoftConstraint(String constraint, double priority) {
        softConstraints.put(constraint, Math.max(0, Math.min(1, priority)));
    }

    /**
     * Add an ideal scenario.
     */
    public void addIdeal(String ideal) {
        ideals.add(ideal);
    }

    /**
     * Set preference for minimizing cost.
     */
    public void setCostWeight(double baseWeight, double direction) {
        weights.setCostWeight(baseWeight * direction);
    }

    /**
     * Set preference for minimizing time.
     */
    public void setTimeWeight(double baseWeight, double direction) {
        weights.setTimeWeight(baseWeight * direction);
    }

    /**
     * Set preference for minimizing risk.
     */
    public void setRiskWeight(double baseWeight, double direction) {
        weights.setRiskWeight(baseWeight * direction);
    }

    /**
     * Get all preferences.
     */
    public Map<String, Preference> getPreferences() {
        return Collections.unmodifiableMap(preferences);
    }

    /**
     * Get preference by key.
     */
    public Optional<Preference> getPreference(String key) {
        return Optional.ofNullable(preferences.get(key));
    }

    /**
     * Get all soft constraints.
     */
    public Map<String, Double> getSoftConstraints() {
        return Collections.unmodifiableMap(softConstraints);
    }

    /**
     * Get all ideals.
     */
    public List<String> getIdeals() {
        return Collections.unmodifiableList(ideals);
    }

    /**
     * Get preference weights.
     */
    public PreferenceWeights getWeights() {
        return weights;
    }

    /**
     * Compute total utility of a trajectory candidate.
     * Higher = better.
     */
    public double computeUtility(double cost, double time, double risk,
                                  Map<String, Double> satisfiedPreferences) {
        double utility = 0.0;

        // Weighted components
        utility += weights.getCostWeight() * (cost == 0 ? 1.0 : 1.0 / (1.0 + cost));
        utility += weights.getTimeWeight() * (time == 0 ? 1.0 : 1.0 / (1.0 + time));
        utility += weights.getRiskWeight() * (1.0 - risk);

        // Preference bonuses
        for (Map.Entry<String, Double> satisfied : satisfiedPreferences.entrySet()) {
            Preference pref = preferences.get(satisfied.getKey());
            if (pref != null) {
                utility += pref.getWeight() * satisfied.getValue();
            }
        }

        return utility;
    }

    /**
     * Check if a candidate satisfies user desires well enough.
     */
    public boolean isSatisfactory(double cost, double time, double risk,
                                   int satisfiedConstraints,
                                   int totalConstraints) {
        double constraintRatio = totalConstraints > 0
            ? (double) satisfiedConstraints / totalConstraints
            : 1.0;

        // Within 20% of optimal for weighted criteria
        boolean costOk = cost < 1.2 * (weights.getCostWeight() > 0 ? 1 : 0);
        boolean timeOk = time < 1.2 * (weights.getTimeWeight() > 0 ? 1 : 0);
        boolean riskOk = risk < 0.5 || weights.getRiskWeight() >= 0;

        return costOk && timeOk && riskOk && constraintRatio > 0.7;
    }

    @Override
    public String toString() {
        return "DesireModel{" +
               "preferences=" + preferences.size() +
               ", softConstraints=" + softConstraints.size() +
               ", ideals=" + ideals.size() +
               "}";
    }

    /**
     * A user preference with description and weight.
     */
    public static class Preference {
        private final String description;
        private final double weight;

        public Preference(String description, double weight) {
            this.description = description;
            this.weight = weight;
        }

        public String getDescription() { return description; }
        public double getWeight() { return weight; }

        @Override
        public String toString() {
            return description + " (weight=" + weight + ")";
        }
    }

    /**
     * Preference weights for trajectory optimization.
     * Positive = minimize, negative = maximize (unusual for cost/time/risk).
     */
    public static class PreferenceWeights {
        private double costWeight = -1.0;    // Default: minimize cost
        private double timeWeight = -1.0;    // Default: minimize time
        private double riskWeight = -1.0;    // Default: minimize risk

        public double getCostWeight() { return costWeight; }
        public void setCostWeight(double costWeight) { this.costWeight = costWeight; }

        public double getTimeWeight() { return timeWeight; }
        public void setTimeWeight(double timeWeight) { this.timeWeight = timeWeight; }

        public double getRiskWeight() { return riskWeight; }
        public void setRiskWeight(double riskWeight) { this.riskWeight = riskWeight; }

        /**
         * Normalize weights to sum to 1.
         */
        public PreferenceWeights normalize() {
            double sum = Math.abs(costWeight) + Math.abs(timeWeight) + Math.abs(riskWeight);
            if (sum > 0) {
                PreferenceWeights normalized = new PreferenceWeights();
                normalized.costWeight = costWeight / sum;
                normalized.timeWeight = timeWeight / sum;
                normalized.riskWeight = riskWeight / sum;
                return normalized;
            }
            return this;
        }
    }
}
