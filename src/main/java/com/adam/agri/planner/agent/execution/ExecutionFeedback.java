package com.adam.agri.planner.agent.execution;

import java.util.ArrayList;
import java.util.List;

import com.adam.agri.planner.core.state.State;

/**
 * Feedback from action execution.
 * Contains intermediate states and results from actuator steps.
 */
public class ExecutionFeedback {

    private final List<PhysicalExecutor.ActuatorResult> steps;
    private final State resultState;

    private ExecutionFeedback(List<PhysicalExecutor.ActuatorResult> steps,
                             State resultState) {
        this.steps = List.copyOf(steps);
        this.resultState = resultState;
    }

    public List<PhysicalExecutor.ActuatorResult> getSteps() {
        return steps;
    }

    public State getResultState() {
        return resultState;
    }

    public boolean isSuccess() {
        return steps.stream().allMatch(PhysicalExecutor.ActuatorResult::success);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final List<PhysicalExecutor.ActuatorResult> steps = new ArrayList<>();
        private State resultState;

        public Builder addStep(PhysicalExecutor.ActuatorResult step) {
            steps.add(step);
            return this;
        }

        public Builder withResultState(State state) {
            this.resultState = state;
            return this;
        }

        public ExecutionFeedback build() {
            return new ExecutionFeedback(steps, resultState);
        }
    }
}
