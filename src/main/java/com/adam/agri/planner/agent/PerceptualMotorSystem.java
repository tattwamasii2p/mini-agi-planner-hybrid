package com.adam.agri.planner.agent;

import com.adam.agri.planner.agent.execution.*;
import com.adam.agri.planner.agent.perception.*;
import com.adam.agri.planner.core.action.Action;
import com.adam.agri.planner.core.state.State;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Perceptual-Motor System: closes the sensorimotor loop.
 *
 * Mathematical model:
 * - Perception: World → Observation (state estimation)
 * - Action Selection: Observation → Action (policy)
 * - Execution: Action → World (actuation)
 * - Feedback loop: continuous cycle
 *
 * Perception → Action Selection → Execution → New Perception
 *      ↑___________________________________|
 *
 * This is the core reactive control loop of the agent.
 */
public class PerceptualMotorSystem {

    private final Perception perception;
    private final ReactiveActionSelector selector;
    private final ActionExecutor executor;

    // Loop control
    private final ExecutorService executorService;
    private final long cyclePeriodMs;
    private final AtomicBoolean running;
    private final AtomicLong cyclesExecuted;

    // Statistics
    private volatile double averageLatency;
    private volatile int successCount;
    private volatile int failureCount;

    public PerceptualMotorSystem(Perception perception,
                                 ReactiveActionSelector selector,
                                 ActionExecutor executor,
                                 long cyclePeriodMs) {
        this.perception = perception;
        this.selector = selector;
        this.executor = executor;
        this.cyclePeriodMs = cyclePeriodMs;
        this.running = new AtomicBoolean(false);
        this.cyclesExecuted = new AtomicLong(0);
        this.executorService = Executors.newSingleThreadExecutor();
    }

    /**
     * Start the perceptual-motor loop.
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            executorService.submit(this::runLoop);
        }
    }

    /**
     * Stop the loop.
     */
    public void stop() {
        running.set(false);
    }

    /**
     * Execute a single perception-action cycle.
     */
    public void step() {
        long startTime = System.currentTimeMillis();
        State currentState = null;
        try {
            // Phase 1: Perceive
            PerceptionEvent event = perception.perceive();
            if (event == null) return;

            // Update state from perception
            currentState = stateFromPerception(event);

            // Phase 2: Select action
            Action selected = selector.select(currentState);
            if (selected == null) return;

            // Phase 3: Execute
            ExecutionContext context = ExecutionContext.builder(currentState).build();
            ExecutionResult result = executor.execute(selected, context);

            // Update statistics
            cyclesExecuted.incrementAndGet();
            if (result.isSuccess()) {
                successCount++;
            } else {
                failureCount++;
            }

            // Feedback: update selector with result
            selector.updateWithFeedback(currentState, selected, result);

        } catch (Exception e) {
            failureCount++;
            // Error handling: trigger recovery
            if (currentState != null) {
                Action recovery = selector.selectRecovery(currentState, e);
                if (recovery != null) {
                    executor.execute(recovery, ExecutionContext.builder(currentState).build());
                }
            }
        }

        // Track latency
        long latency = System.currentTimeMillis() - startTime;
        updateAverageLatency(latency);

        // Maintain cycle period
        long sleep = cyclePeriodMs - latency;
        if (sleep > 0) {
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException ignored) {}
        }
    }

    /**
     * Execute one step with perception-driven execution monitoring.
     */
    public ExecutionResult executeWithPerception(Action action, State initialState) {
        // Initial execution
        ExecutionContext context = ExecutionContext.builder(initialState).build();
        ExecutionResult result = executor.execute(action, context);

        if (!result.isSuccess()) {
            // Perception-driven error recovery
            PerceptionEvent postError = perception.perceive();
            // Analyze what went wrong
            String errorAnalysis = analyzeError(postError, action);

            // Adjust and retry
            Action adjusted = selector.adjustForError(action, errorAnalysis);
            if (adjusted != null && !adjusted.equals(action)) {
                return executeWithPerception(adjusted, initialState);
            }
        }

        return result;
    }

    /**
     * Wait for specific perception before executing.
     * Conditional execution based on sensory input.
     */
    public ExecutionResult executeWhen(Action action, State initialState,
                                       PerceptionCondition condition,
                                       long timeout) {
        long deadline = System.currentTimeMillis() + timeout;

        while (System.currentTimeMillis() < deadline) {
            PerceptionEvent event = perception.perceive();
            if (condition.test(event)) {
                return executeWithPerception(action, initialState);
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return ExecutionResult.failed("timeout", "Timeout waiting for condition");
            }
        }

        return ExecutionResult.failed("timeout", "Timeout waiting for condition");
    }

    private void runLoop() {
        while (running.get()) {
            step();
        }
    }

    private State stateFromPerception(PerceptionEvent event) {
        // Convert perception to state
        // Uses physical state from perception
        return event.getFeatures().get("state") instanceof State
            ? (State) event.getFeatures().get("state")
            : null;
    }

    private void updateAverageLatency(long latency) {
        // Exponential moving average
        double alpha = 0.1;
        averageLatency = alpha * latency + (1 - alpha) * averageLatency;
    }

    private String analyzeError(PerceptionEvent postError, Action failedAction) {
        return "Error analysis: " + postError.getType() + " during " + failedAction.getName();
    }

    // Getters
    public long getCyclesExecuted() { return cyclesExecuted.get(); }
    public boolean isRunning() { return running.get(); }
    public double getAverageLatency() { return averageLatency; }
    public int getSuccessCount() { return successCount; }
    public int getFailureCount() { return failureCount; }

    /**
     * Reactive action selector interface.
     */
    public interface ReactiveActionSelector {
        /**
         * Select action based on current perception.
         */
        Action select(State perception);

        /**
         * Recovery action after failure.
         */
        default Action selectRecovery(State perception, Exception error) {
            return null;
        }

        /**
         * Adjust action based on error analysis.
         */
        default Action adjustForError(Action original, String errorAnalysis) {
            return original;
        }

        /**
         * Update with execution feedback for learning.
         */
        default void updateWithFeedback(State perception, Action action, ExecutionResult result) {}
    }

    /**
     * Perception condition for conditional execution.
     */
    @FunctionalInterface
    public interface PerceptionCondition {
        boolean test(PerceptionEvent event);
    }
}
