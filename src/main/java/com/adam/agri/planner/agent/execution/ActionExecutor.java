package com.adam.agri.planner.agent.execution;

import java.util.concurrent.CompletableFuture;

import com.adam.agri.planner.core.action.Action;
import com.adam.agri.planner.core.state.State;

/**
 * Action execution interface.
 * Grounds symbolic action in physical reality.
 *
 * Execution flow:
 * 1. Verify preconditions
 * 2. Configure actuators
 * 3. Execute with monitoring
 * 4. Verify effects
 * 5. Return result
 */
public interface ActionExecutor {

    /**
     * Execute action synchronously.
     */
    ExecutionResult execute(Action action, ExecutionContext context);

    /**
     * Execute action asynchronously.
     */
    CompletableFuture<ExecutionResult> executeAsync(Action action, ExecutionContext context);

    /**
     * Verify preconditions before execution.
     */
    boolean verifyPreconditions(Action action, State currentState);

    /**
     * Check if executor can handle this action.
     */
    boolean canExecute(Action action);

    /**
     * Get available actions for this executor.
     */
    default String[] getSupportedActionTypes() {
        return new String[]{"*"}; // Wildcard
    }

    /**
     * Abort current execution.
     */
    boolean abort();

    /**
     * Get execution status.
     */
    ExecutionStatus getStatus();

    /**
     * Registration with executor registry.
     */
    default void register() {
        ExecutionRegistry.register(this);
    }

    enum ExecutionStatus {
        READY,
        EXECUTING,
        PAUSED,
        FAILED,
        SUCCESS
    }
}
