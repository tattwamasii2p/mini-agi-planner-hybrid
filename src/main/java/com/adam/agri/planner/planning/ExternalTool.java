package com.adam.agri.planner.planning;

import com.adam.agri.planner.core.action.Action;

/**
 * Marker for external tools/computers available to planner.
 */
public interface ExternalTool extends Action {
    String getName();
    boolean isAvailable();
}
