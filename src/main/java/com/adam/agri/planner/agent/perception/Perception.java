package com.adam.agri.planner.agent.perception;

import com.adam.agri.planner.symbolic.ontology.upper.Physical;

import java.util.List;
import java.util.Optional;

/**
 * Agent perception interface.
 * Converts sensory input to structured perception events.
 *
 * Mathematical model:
 * - Perception: World → ObservableState (inverse of Action)
 * - Creates epistemic link between agent and environment
 */
public interface Perception {

    /**
     * Generate perception event from current sensory input.
     */
    PerceptionEvent perceive();

    /**
     * Generate multiple events (multi-modal).
     */
    List<PerceptionEvent> perceiveMultiModal();

    /**
     * Get confidence in current perception.
     */
    double getConfidence();

    /**
     * Check if perception is available.
     */
    boolean isAvailable();

    /**
     * Get perceived entity, if identifiable.
     */
    Optional<Physical> getPerceivedEntity();

    /**
     * Update perception with action context (active perception).
     */
    void updateWithAction(com.adam.agri.planner.core.action.Action action);
}
