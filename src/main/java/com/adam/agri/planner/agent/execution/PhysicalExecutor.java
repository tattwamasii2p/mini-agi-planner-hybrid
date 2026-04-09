package com.adam.agri.planner.agent.execution;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import com.adam.agri.planner.core.action.Action;
import com.adam.agri.planner.core.state.State;

/**
 * Physical executor with actuators and safety. Converts high-level Action to
 * low-level actuator commands.
 */
public class PhysicalExecutor implements ActionExecutor {

	private final List<Actuator> actuators;
	private final SafetyMonitor safety;
	private final ExecutionStatus[] currentStatus;
	private final Map<String, ExecutionRecord> executionHistory;

	public PhysicalExecutor(List<Actuator> actuators, SafetyMonitor safety) {
		this.actuators = actuators;
		this.safety = safety;
		this.currentStatus = new ExecutionStatus[] { ExecutionStatus.READY };
		this.executionHistory = new ConcurrentHashMap<>();
	}

	@Override
	public ExecutionResult execute(Action action, ExecutionContext context) {
		ExecutionStatus status = ExecutionStatus.EXECUTING;
		currentStatus[0] = status;

		long startTime = System.currentTimeMillis();
		String executionId = generateExecutionId();

		try {
			// Phase 1: Verify preconditions
			if (!verifyPreconditions(action, context.getCurrentState())) {
				return ExecutionResult.failed(executionId, "Preconditions not met");
			}

			// Phase 2: Safety check
			if (!safety.check(action, context)) {
				return ExecutionResult.failed(executionId, "Safety violation");
			}

			// Phase 3: Execute
			ExecutionFeedback feedback = executeWithFeedback(action, context);

			// Phase 4: Verify effects
			boolean effectsVerified = verifyEffects(action, context.getCurrentState(), feedback.getResultState());

			long duration = System.currentTimeMillis() - startTime;

			if (effectsVerified) {
				currentStatus[0] = ExecutionStatus.SUCCESS;
				ExecutionResult result = ExecutionResult.success(executionId, feedback.getResultState(), duration);
				recordExecution(executionId, action, result);
				return result;
			} else {
				currentStatus[0] = ExecutionStatus.FAILED;
				return ExecutionResult.partial(executionId, "Effects not verified", duration);
			}

		} catch (Exception e) {
			currentStatus[0] = ExecutionStatus.FAILED;
			return ExecutionResult.exception(executionId, e);
		}
	}

	@Override
	public CompletableFuture<ExecutionResult> executeAsync(Action action, ExecutionContext context) {
		return CompletableFuture.supplyAsync(() -> execute(action, context));
	}

	@Override
	public boolean verifyPreconditions(Action action, State currentState) {
		return action.isApplicableIn(currentState);
	}

	@Override
	public boolean canExecute(Action action) {
		// Check if actuators support this action
		String actionType = action.getName();
		for (Actuator actuator : actuators) {
			if (actuator.supports(actionType)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean abort() {
		for (Actuator actuator : actuators) {
			actuator.emergencyStop();
		}
		currentStatus[0] = ExecutionStatus.FAILED;
		return true;
	}

	@Override
	public ExecutionStatus getStatus() {
		return currentStatus[0];
	}

	private ExecutionFeedback executeWithFeedback(Action action, ExecutionContext context) throws ExecutionException {
		// Map action to actuator commands
		List<ActuatorCommand> commands = mapToCommands(action);

		// Execute with feedback loop
		ExecutionFeedback.Builder feedback = ExecutionFeedback.builder();
		State resultState = context.getCurrentState();

		for (ActuatorCommand cmd : commands) {
			Actuator actuator = findActuator(cmd.getActuatorType());
			if (actuator == null) {
				throw new ExecutionException("No actuator for: " + cmd.getActuatorType());
			}

			// Execute with timeout
			ActuatorResult result = actuator.execute(cmd, context.getTimeout());

			if (!result.success()) {
				throw new ExecutionException("Actuator failed: " + result.error());
			}

			feedback.addStep(result);
		}

		return feedback.withResultState(resultState).build();
	}

	private List<ActuatorCommand> mapToCommands(Action action) {
		// Action → low-level commands
		return List.of(new ActuatorCommand(action.getName(), action.getEffects()));
	}

	private Actuator findActuator(String type) {
		return actuators.stream().filter(a -> a.getType().equals(type) || a.supports(type)).findFirst().orElse(null);
	}

	private boolean verifyEffects(Action action, State before, State after) {
		// Check that effects actually occurred
		// Simplified: assume success if no exception
		return true;
	}

	private String generateExecutionId() {
		return "exec_" + System.currentTimeMillis();
	}

	private void recordExecution(String id, Action action, ExecutionResult result) {
		executionHistory.put(id, new ExecutionRecord(id, action, result, System.currentTimeMillis()));
	}

	// Inner classes
	public interface Actuator {
		String getType();

		boolean supports(String actionType);

		ActuatorResult execute(ActuatorCommand command, long timeoutMillis);

		void emergencyStop();
	}

	public record ActuatorCommand(String actuatorType, Object parameters) {
		public String getActuatorType() {
			return actuatorType;
		}
	}

	public record ActuatorResult(boolean success, String error, Object outcome) {
	}

	public record ExecutionRecord(String id, Action action, ExecutionResult result, long timestamp) {
	}

	public static class ExecutionException extends Exception {
		private static final long serialVersionUID = 6729912006092658862L;

		public ExecutionException(String message) {
			super(message);
		}
	}
}
