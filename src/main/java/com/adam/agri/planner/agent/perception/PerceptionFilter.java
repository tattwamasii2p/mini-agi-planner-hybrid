package com.adam.agri.planner.agent.perception;

import java.util.List;

/**
 * Filter for perception events.
 * Part of attention mechanism.
 */
public interface PerceptionFilter {
    /**
     * Filter events based on criteria.
     */
    List<PerceptionEvent> filter(List<PerceptionEvent> events);
}
