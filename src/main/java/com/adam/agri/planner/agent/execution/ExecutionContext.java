package com.adam.agri.planner.agent.execution;

import com.adam.agri.planner.core.state.State;

/**
 * Context for action execution.
 * Provides execution environment and configuration.
 */
public class ExecutionContext {

    private final State currentState;
    private final long timeout;
    private final boolean verifyEffects;
    private final int maxRetries;

    public ExecutionContext(State currentState, long timeout,
                           boolean verifyEffects, int maxRetries) {
        this.currentState = currentState;
        this.timeout = timeout;
        this.verifyEffects = verifyEffects;
        this.maxRetries = maxRetries;
    }

    public ExecutionContext(State currentState) {
        this(currentState, 30000, true, 3);
    }

    // Getters
    public State getCurrentState() { return currentState; }
    public long getTimeout() { return timeout; }
    public boolean shouldVerifyEffects() { return verifyEffects; }
    public int getMaxRetries() { return maxRetries; }

    // Builder
    public static Builder builder(State state) {
        return new Builder(state);
    }

    public static class Builder {
        private final State state;
        private long timeout = 30000;
        private boolean verifyEffects = true;
        private int maxRetries = 3;

        Builder(State state) { this.state = state; }

        public Builder timeout(long ms) { this.timeout = ms; return this; }
        public Builder verifyEffects(boolean v) { this.verifyEffects = v; return this; }
        public Builder maxRetries(int n) { this.maxRetries = n; return this; }

        public ExecutionContext build() {
            return new ExecutionContext(state, timeout, verifyEffects, maxRetries);
        }
    }
}
