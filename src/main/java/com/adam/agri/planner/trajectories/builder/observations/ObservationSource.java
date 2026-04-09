package com.adam.agri.planner.trajectories.builder.observations;

import java.util.List;

import com.adam.agri.planner.symbolic.ontology.upper.Entity;

/**
 * Base interface for observation sources in the trajectories builder.
 * Implementations adapt different input types (symbolic, physical, natural language)
 * into a common observation format.
 *
 * @param <T> The raw observation type (SymbolicState, PerceptionEvent, String)
 */
public interface ObservationSource<T> {

    /**
     * Add a raw observation to this source.
     */
    void addObservation(T observation);

    /**
     * Get all observations from this source.
     */
    List<T> getObservations();

    /**
     * Get the source type for confidence weighting.
     */
    SourceType getSourceType();

    /**
     * Get confidence level for this source (0.0 - 1.0).
     * Physical sensors typically higher than LLM, which is higher than pure symbolic.
     */
    double getSourceConfidence();

    /**
     * Convert observations to symbolic entities.
     * Returns empty list if conversion not applicable.
     */
    List<Entity> toEntities();

    /**
     * Check if this source has valid observations.
     */
    default boolean hasObservations() {
        return !getObservations().isEmpty();
    }

    /**
     * Clear all observations.
     */
    void clear();

    /**
     * Observation source types with typical confidence levels.
     */
    enum SourceType {
        PHYSICAL_SENSOR(0.95),    // High precision sensor data
        SYMBOLIC_KNOWLEDGE(0.90), // Explicit symbolic KB
        NATURAL_LANGUAGE(0.70),   // LLM-parsed text
        INFERRED(0.60),          // Derived from other observations
        SIMULATED(0.50);         // Simulation/hypothetical

        private final double defaultConfidence;

        SourceType(double defaultConfidence) {
            this.defaultConfidence = defaultConfidence;
        }

        public double getDefaultConfidence() {
            return defaultConfidence;
        }
    }
}
