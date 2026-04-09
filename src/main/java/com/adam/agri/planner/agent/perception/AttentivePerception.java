package com.adam.agri.planner.agent.perception;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Attention mechanism for filtering perception.
 * Bottleneck: sensory input >> processing capacity.
 *
 * Attention types:
 * - Bottom-up: driven by salience (unexpected, high intensity)
 * - Top-down: driven by goals (relevance to current task)
 * - Spatial: focus on region of interest
 * - Feature-based: focus on specific features
 */
public class AttentivePerception implements PerceptionFilter {

    private final Perception source;
    private final int windowSize;
    private final double salienceThreshold;
    private final double relevanceThreshold;
    private final AttentionMode mode;

    // Current focus
    private PerceptionEvent focusedEvent;
    private Object attentionCue; // Current focus target

    public AttentivePerception(Perception source, int windowSize,
                              double salienceThreshold, double relevanceThreshold,
                              AttentionMode mode) {
        this.source = source;
        this.windowSize = windowSize;
        this.salienceThreshold = salienceThreshold;
        this.relevanceThreshold = relevanceThreshold;
        this.mode = mode;
    }

    @Override
    public List<PerceptionEvent> filter(List<PerceptionEvent> events) {
        return events.stream()
            .filter(this::passesThresholds)
            .sorted(this::prioritize)
            .limit(windowSize)
            .collect(Collectors.toList());
    }

    /**
     * Evaluate event relevance to current attention.
     */
    public double evaluateRelevance(PerceptionEvent event) {
        double baseScore = mode == AttentionMode.SALIENCE
            ? event.getSalience()
            : event.getConfidence();

        // Boost if matches attention cue
        if (attentionCue != null && matchesCue(event, attentionCue)) {
            baseScore *= 1.5;
        }

        // Boost if matches current focus (temporal continuity)
        if (focusedEvent != null && temporalContinuity(event, focusedEvent)) {
            baseScore *= 1.3;
        }

        return Math.min(baseScore, 1.0);
    }

    /**
     * Set attention cue (top-down focus).
     */
    public void setAttentionCue(Object cue) {
        this.attentionCue = cue;
    }

    /**
     * Clear attention focus.
     */
    public void clearFocus() {
        this.attentionCue = null;
        this.focusedEvent = null;
    }

    private boolean passesThresholds(PerceptionEvent event) {
        return event.getSalience() >= salienceThreshold
            && evaluateRelevance(event) >= relevanceThreshold
            && event.getConfidence() > 0.3;
    }

    private int prioritize(PerceptionEvent a, PerceptionEvent b) {
        double scoreA = evaluateRelevance(a) * a.getSalience();
        double scoreB = evaluateRelevance(b) * b.getSalience();
        return Double.compare(scoreB, scoreA);
    }

    private boolean matchesCue(PerceptionEvent event, Object cue) {
        // Simple matching: check if event features contain cue
        return event.getFeatures().containsValue(cue);
    }

    private boolean temporalContinuity(PerceptionEvent current, PerceptionEvent previous) {
        long dt = current.millisecondsTo(previous);
        return dt < 1000 && current.getType() == previous.getType();
    }

    public enum AttentionMode {
        SALIENCE,      // Bottom-up
        RELEVANCE,     // Top-down
        BALANCED       // Weighted combination
    }
}
