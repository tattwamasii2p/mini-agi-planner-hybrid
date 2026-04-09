package com.adam.agri.planner.agent.execution;

import com.adam.agri.planner.core.state.State;

import java.util.Optional;

/**
 * Result of action execution.
 * Contains status, final state, timing, and error information.
 */
public class ExecutionResult {

    private final String executionId;
    private final Status status;
    private final State finalState;
    private final long durationMs;
    private final String errorMessage;
    private final Throwable exception;

    private ExecutionResult(String id, Status status, State state,
                           long duration, String error, Throwable ex) {
        this.executionId = id;
        this.status = status;
        this.finalState = state;
        this.durationMs = duration;
        this.errorMessage = error;
        this.exception = ex;
    }

    // Factory methods
    public static ExecutionResult success(String id, State finalState, long duration) {
        return new ExecutionResult(id, Status.SUCCESS, finalState, duration, null, null);
    }

    public static ExecutionResult failed(String id, String error) {
        return new ExecutionResult(id, Status.FAILED, null, 0, error, null);
    }

    public static ExecutionResult partial(String id, String message, long duration) {
        return new ExecutionResult(id, Status.PARTIAL, null, duration, message, null);
    }

    public static ExecutionResult exception(String id, Throwable ex) {
        return new ExecutionResult(id, Status.ERROR, null, 0, ex.getMessage(), ex);
    }

    // Getters
    public String getExecutionId() { return executionId; }
    public Status getStatus() { return status; }
    public Optional<State> getFinalState() { return Optional.ofNullable(finalState); }
    public long getDurationMs() { return durationMs; }
    public Optional<String> getErrorMessage() { return Optional.ofNullable(errorMessage); }
    public Optional<Throwable> getException() { return Optional.ofNullable(exception); }

    // Status checks
    public boolean isSuccess() { return status == Status.SUCCESS; }
    public boolean isFailed() { return status == Status.FAILED || status == Status.ERROR; }
    public boolean isPartial() { return status == Status.PARTIAL; }

    @Override
    public String toString() {
        return String.format("Execution[%s %s %dms]",
            executionId, status, durationMs);
    }

    public enum Status {
        SUCCESS,
        PARTIAL,
        FAILED,
        ERROR,
        CANCELLED
    }
}
