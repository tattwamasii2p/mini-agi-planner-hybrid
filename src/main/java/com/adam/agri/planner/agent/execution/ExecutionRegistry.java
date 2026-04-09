package com.adam.agri.planner.agent.execution;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for action executors.
 * Supports executor discovery and selection by action type.
 */
public class ExecutionRegistry {

    private static final Map<String, ActionExecutor> executors = new ConcurrentHashMap<>();

    /**
     * Register an executor.
     */
    public static void register(ActionExecutor executor) {
        for (String type : executor.getSupportedActionTypes()) {
            executors.put(type, executor);
        }
    }

    /**
     * Get executor for action type.
     */
    public static Optional<ActionExecutor> get(String actionType) {
        // Exact match
        if (executors.containsKey(actionType)) {
            return Optional.of(executors.get(actionType));
        }
        // Wildcard
        if (executors.containsKey("*")) {
            return Optional.of(executors.get("*"));
        }
        return Optional.empty();
    }

    /**
     * Find executor that can execute action.
     */
    public static Optional<ActionExecutor> findFor(ActionExecution candidate) {
        return executors.values().stream()
            .filter(e -> e.canExecute(candidate.action()))
            .findFirst();
    }

    /**
     * Get all registered executors.
     */
    public static Collection<ActionExecutor> getAll() {
        return Collections.unmodifiableCollection(executors.values());
    }

    /**
     * Clear registry (for testing).
     */
    public static void clear() {
        executors.clear();
    }

    // Placeholder for action execution record
    public record ActionExecution(com.adam.agri.planner.core.action.Action action,
                                   ExecutionContext context) {}
}
