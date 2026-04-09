package com.adam.agri.planner.computer;

import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

import com.adam.agri.planner.agent.PerceptualMotorSystem;
import com.adam.agri.planner.agent.execution.ActionExecutor;
import com.adam.agri.planner.agent.execution.ActuationPublisher;
import com.adam.agri.planner.agent.execution.ActuationPublisher.ActuationSubscriber;
import com.adam.agri.planner.agent.execution.ExecutionContext;
import com.adam.agri.planner.agent.execution.ExecutionResult;
import com.adam.agri.planner.agent.execution.PhysicalExecutor;
import com.adam.agri.planner.agent.execution.SafetyMonitor;
import com.adam.agri.planner.agent.perception.Perception;
import com.adam.agri.planner.agent.perception.PerceptionEvent;
import com.adam.agri.planner.agent.perception.PerceptionPublisher;
import com.adam.agri.planner.agent.perception.PerceptionSubscriber;
import com.adam.agri.planner.agent.perception.PerceptionSubscriber.PerceptionSubscription;
import com.adam.agri.planner.agent.perception.SensorArray;
import com.adam.agri.planner.core.action.Action;
import com.adam.agri.planner.core.action.ActionId;
import com.adam.agri.planner.core.action.ActionOutcome;
import com.adam.agri.planner.core.action.Effect;
import com.adam.agri.planner.core.action.Precondition;
import com.adam.agri.planner.core.state.BeliefState;
import com.adam.agri.planner.core.state.PhysicalState;
import com.adam.agri.planner.core.state.State;
import com.adam.agri.planner.core.state.StateId;
import com.adam.agri.planner.core.state.StateType;
import com.adam.agri.planner.core.state.SymbolicState;
import com.adam.agri.planner.multiagent.Agent;
import com.adam.agri.planner.multiagent.AgentId;
import com.adam.agri.planner.physical.worldmodel.WorldModel;
import com.adam.agri.planner.planning.Planner;
import com.adam.agri.planner.symbolic.ontology.computer.ComputerSystem;
import com.adam.agri.planner.symbolic.ontology.computer.ExternalComputerAction;
import com.adam.agri.planner.symbolic.ontology.upper.EntityId;
import com.adam.agri.planner.symbolic.ontology.upper.Physical;

/**
 * Computer System as an Agent using structured data interface {@link AbstractComputerSystemsData}.
 *
 * Replaces scattered Map feature access with strongly-typed metrics:
 * - PUBLISHES: PerceptionEvent with structured AbstractComputerSystemsData
 * - SUBSCRIBES: Actuation commands for task execution
 */
public class ComputerSystemAgent extends Agent {

    // Perception publishing
    private final PerceptionPublisher perceptionPublisher;
    private final SensorArray systemSensors;

    // Actuation subscribing
    private final ActuationPublisher actuationPublisher;
    private final ActionExecutor executor;
    private final SafetyMonitor safety;

    // Perceptual-motor loop
    private final PerceptualMotorSystem pms;

    // Structured metrics (replaces scattered fields)
    private AbstractComputerSystemsData currentMetrics;

    // Tasks
    private final Set<ExternalComputerAction> activeTasks;
    private final Queue<ExternalComputerAction> taskQueue;

    public ComputerSystemAgent(AgentId id, String name,
                               WorldModel worldModel,
                               Planner planner,
                               ComputerSystem hardwareSpec) {
        super(id, name, worldModel, planner);

        // Perception
        this.perceptionPublisher = new PerceptionPublisher(true);
        this.systemSensors = new SensorArray();
        setupSystemSensors();

        // Actuation
        this.actuationPublisher = new ActuationPublisher(true);
        this.safety = new SafetyMonitor(0.1);
        setupSafetyRules();
        this.executor = new PhysicalExecutor(
            List.of(new ComputeActuator(), new NetworkActuator()),
            safety
        );

        // PMS
        this.pms = new PerceptualMotorSystem(
            wrapPerception(),
            new ComputerSystemReactiveSelector(this),
            executor,
            500
        );

        // Init state
        this.currentMetrics = ComputerSystemsData.zero();
        this.activeTasks = new HashSet<>();
        this.taskQueue = new LinkedList<>();

        setupDefaultSubscriptions();
    }

    // ==================== PERCEPTION PUBLISHING ====================

    /**
     * Perceive and publish current system state as structured data.
     */
    public PerceptionEvent publishSystemState() {
        // Build structured metrics
        currentMetrics = ComputerSystemsData.builder()
            .cpuLoad(sensorCpuLoad())
            .memoryUsage(sensorMemoryUsage())
            .bandwidth(sensorBandwidth())
            .queueDepth(taskQueue.size())
            .availableCompute(getAvailableCompute())
            .availableMemory(getAvailableMemory())
            .build();

        // Publish with structured data
        PerceptionEvent event = PerceptionEvent.builder()
            .source(new EntityId(getId().toString()))
            .timestamp(Instant.now())
            .type(PerceptionEvent.PerceptionType.INTERNAL)
            .data(currentMetrics)  // Structured data replaces .feature() calls
            .confidence(0.95)
            .salience(calculateSalience(currentMetrics))
            .build();

        perceptionPublisher.publish(event);
        return event;
    }

    /**
     * Publish event with typed data payload.
     */
    public void publishMetrics(AbstractComputerSystemsData data) {
        PerceptionEvent event = PerceptionEvent.builder()
            .source(new EntityId(getId().toString()))
            .timestamp(Instant.now())
            .data(data)
            .confidence(0.95)
            .salience(calculateSalience(data))
            .build();

        perceptionPublisher.publish(event);
    }

    /**
     * Publish task completion with result.
     */
    public void publishTaskCompleted(ExternalComputerAction task, ExecutionResult result) {
        TaskCompletedData data = new TaskCompletedData(
            task.getActionType(),
            result.getDurationMs(),
            result.isSuccess()
        );

        PerceptionEvent event = PerceptionEvent.builder()
            .source(new EntityId(getId().toString()))
            .timestamp(Instant.now())
            .data(data)
            .confidence(1.0)
            .salience(0.8)
            .build();

        perceptionPublisher.publish(event);
    }

    // ==================== SUBSCRIPTION MANAGEMENT ====================

    /**
     * Subscribe to perception events with typed data access.
     */
    public PerceptionSubscription subscribeToPerception(
            PerceptionSubscriber subscriber) {
        return perceptionPublisher.subscribe(subscriber);
    }

    /**
     * Subscribe with data type filter.
     */
    public <T extends AbstractComputerSystemsData> PerceptionSubscription subscribeToMetrics(
            PerceptionSubscriber subscriber,
            Class<T> dataType) {
        return perceptionPublisher.subscribe(
            subscriber,
            e -> e.getData(dataType).isPresent()
        );
    }

    /**
     * Subscribe to actuation commands.
     */
    public void subscribeToActuation(ActuationSubscriber subscriber) {
        actuationPublisher.subscribe(subscriber);
    }

    // ==================== ACTUATION HANDLING ====================

    /**
     * Receive task assignment.
     */
    public void assignTask(ExternalComputerAction task) {
        taskQueue.offer(task);
        publishSystemState(); // Immediate update
    }

    /**
     * Execute task with actuation publishing.
     */
    public ExecutionResult executeTask(ExternalComputerAction task) {
        ComputeAction action = new ComputeAction(task);

        // Wrap metrics in a State for execution context
        State metricsState = new MetricState(currentMetrics);
        actuationPublisher.publishActionStarted(action,
            ExecutionContext.builder(metricsState).build());

        ExecutionContext ctx = ExecutionContext.builder(metricsState)
            .timeout((long) task.getDuration().getDuration() * 1000)
            .build();

        ExecutionResult result = executor.execute(action, ctx);

        if (result.isSuccess()) {
            activeTasks.add(task);
            actuationPublisher.publishActionCompleted(action, ctx, result);
            publishTaskCompleted(task, result);
        } else {
            actuationPublisher.publishActionFailed(action, ctx,
                result.getException().orElse(null));
        }

        return result;
    }

    // ==================== STATE ACCESSORS ====================

    /**
     * Get current structured metrics.
     */
    public AbstractComputerSystemsData getMetrics() {
        return currentMetrics;
    }

    /**
     * Check if should offload - uses structured data.
     */
    public boolean shouldOffload() {
        return currentMetrics != null && currentMetrics.isOverloaded();
    }

    /**
     * Check if can accept task - uses typed interface.
     */
    public boolean canAccept(ExternalComputerAction task) {
        return currentMetrics != null &&
            currentMetrics.hasCapacity(task.getRequiredCompute(), task.getRequiredMemory());
    }

    // ==================== HELPER METHODS ====================

    private double sensorCpuLoad() {
        PerceptionEvent raw = systemSensors.perceive();
        return raw.getFeature("cpu_load", Double.class).orElse(0.0);
    }

    private double sensorMemoryUsage() {
        PerceptionEvent raw = systemSensors.perceive();
        return raw.getFeature("memory_usage", Double.class).orElse(0.0);
    }

    private double sensorBandwidth() {
        PerceptionEvent raw = systemSensors.perceive();
        return raw.getFeature("bandwidth", Double.class).orElse(1000.0);
    }

    private double getAvailableCompute() {
        return ((ComputerSystem) getLocalWorldModel()).getComputeCapacity()
            * (1 - sensorCpuLoad());
    }

    private double getAvailableMemory() {
        return ((ComputerSystem) getLocalWorldModel()).getMemoryCapacity()
            * (1 - sensorMemoryUsage());
    }

    private double calculateSalience(AbstractComputerSystemsData data) {
        return Math.max(data.getCpuLoad(), data.getMemoryUsage());
    }

    // ==================== SETUP ====================

    private void setupSystemSensors() {
        systemSensors.withSensor(new CpuSensor());
        systemSensors.withSensor(new MemorySensor());
        systemSensors.withSensor(new NetworkSensor());
        systemSensors.withSensor(new QueueSensor());
    }

    private void setupSafetyRules() {
        safety.addRule(new CpuLimitRule(0.95));
        safety.addRule(new MemoryLimitRule(0.95));
    }

    private void setupDefaultSubscriptions() {
        // Self-monitoring
        subscribeToMetrics(e -> {
            AbstractComputerSystemsData data = e.getData(AbstractComputerSystemsData.class).orElse(null);
            if (data != null && data.isOverloaded()) {
                // Trigger reactive action
            }
            return false;
        }, AbstractComputerSystemsData.class);
    }

    private Perception wrapPerception() {
        return new Perception() {
            @Override public PerceptionEvent perceive() {
                return systemSensors.perceive();
            }
            @Override public List<PerceptionEvent> perceiveMultiModal() {
                return systemSensors.perceiveMultiModal();
            }
            @Override public double getConfidence() {
                return systemSensors.getConfidence();
            }
            @Override public boolean isAvailable() {
                return systemSensors.isAvailable();
            }
            @Override public Optional<Physical> getPerceivedEntity() {
                return systemSensors.getPerceivedEntity();
            }
            @Override public void updateWithAction(Action action) {
                systemSensors.updateWithAction(action);
            }
        };
    }

    public void startReactiveControl() { pms.start(); }
    public void stopReactiveControl() { pms.stop(); }

    // ==================== SENSOR CLASSES ====================

    private static class CpuSensor implements SensorArray.Sensor {
        @Override public String getSensorType() { return "cpu"; }
        @Override public double getSamplingRate() { return 10; }
        @Override public boolean isAvailable() { return true; }
        @Override public double getConfidence() { return 0.99; }
        @Override public void configureForAction(Action a) {}
        @Override public PerceptionEvent read() {
            return PerceptionEvent.builder()
                .type(PerceptionEvent.PerceptionType.INTERNAL)
                .feature("cpu_load", Math.random())
                .confidence(0.99)
                .build();
        }
    }

    private static class MemorySensor implements SensorArray.Sensor {
        @Override public String getSensorType() { return "memory"; }
        @Override public double getSamplingRate() { return 10; }
        @Override public boolean isAvailable() { return true; }
        @Override public double getConfidence() { return 0.99; }
        @Override public void configureForAction(Action a) {}
        @Override public PerceptionEvent read() {
            return PerceptionEvent.builder()
                .type(PerceptionEvent.PerceptionType.INTERNAL)
                .feature("memory_usage", Math.random())
                .confidence(0.99)
                .build();
        }
    }

    private static class NetworkSensor implements SensorArray.Sensor {
        @Override public String getSensorType() { return "network"; }
        @Override public double getSamplingRate() { return 5; }
        @Override public boolean isAvailable() { return true; }
        @Override public double getConfidence() { return 0.95; }
        @Override public void configureForAction(Action a) {}
        @Override public PerceptionEvent read() {
            return PerceptionEvent.builder()
                .type(PerceptionEvent.PerceptionType.INTERNAL)
                .feature("bandwidth", 1000.0)
                .confidence(0.95)
                .build();
        }
    }

    private static class QueueSensor implements SensorArray.Sensor {
        @Override public String getSensorType() { return "queue"; }
        @Override public double getSamplingRate() { return 100; }
        @Override public boolean isAvailable() { return true; }
        @Override public double getConfidence() { return 1.0; }
        @Override public void configureForAction(Action a) {}
        @Override public PerceptionEvent read() {
            return PerceptionEvent.builder()
                .type(PerceptionEvent.PerceptionType.INTERNAL)
                .feature("queue_depth", (int)(Math.random() * 20))
                .confidence(1.0)
                .build();
        }
    }

    // ==================== ACTUATORS ====================

    private static class ComputeActuator implements PhysicalExecutor.Actuator {
        @Override public String getType() { return "compute"; }
        @Override public boolean supports(String actionType) {
            return actionType.equals("compute");
        }
        @Override public PhysicalExecutor.ActuatorResult execute(
                PhysicalExecutor.ActuatorCommand cmd, long timeout) {
            return new PhysicalExecutor.ActuatorResult(true, null, "completed");
        }
        @Override public void emergencyStop() {}
    }

    private static class NetworkActuator implements PhysicalExecutor.Actuator {
        @Override public String getType() { return "network"; }
        @Override public boolean supports(String actionType) {
            return actionType.equals("migrate");
        }
        @Override public PhysicalExecutor.ActuatorResult execute(
                PhysicalExecutor.ActuatorCommand cmd, long timeout) {
            return new PhysicalExecutor.ActuatorResult(true, null, "transferred");
        }
        @Override public void emergencyStop() {}
    }

    // ==================== SAFETY ====================

    private static class CpuLimitRule implements SafetyMonitor.SafetyRule {
        private final double limit;
        CpuLimitRule(double limit) { this.limit = limit; }
        @Override public boolean check(Action a, ExecutionContext ctx, double margin) {
            return true;
        }
    }

    private static class MemoryLimitRule implements SafetyMonitor.SafetyRule {
        private final double limit;
        MemoryLimitRule(double limit) { this.limit = limit; }
        @Override public boolean check(Action a, ExecutionContext ctx, double margin) {
            return true;
        }
    }

    // ==================== ACTIONS ====================

    private static class ComputeAction implements Action {
        final ExternalComputerAction task;
        ComputeAction(ExternalComputerAction t) { this.task = t; }
        @Override public ActionId getId() { return new ActionId(UUID.randomUUID().toString()); }
        @Override public String getName() { return "compute_" + task.getActionType(); }
        @Override public Set<Precondition> getPreconditions() { return Set.of(); }
        @Override public Set<Effect> getEffects() { return Set.of(); }
        @Override public boolean isApplicableIn(State s) { return true; }
        @Override public boolean isApplicableIn(BeliefState b) { return true; }
        @Override public State apply(SymbolicState s) { return s; }
        @Override public PhysicalState apply(PhysicalState s) { return s; }
        @Override public BeliefState apply(BeliefState b) { return b; }
        @Override public ActionOutcome simulate(WorldModel w, State i) {
            return new ActionOutcome(i, 1.0, 1.0, 0.0, 0.0);
        }
    }

    // ==================== DATA CLASSES ====================

    public record TaskCompletedData(String taskType, long durationMs, boolean success) {}

    // ==================== REACTIVE SELECTOR ====================

    private static class ComputerSystemReactiveSelector implements PerceptualMotorSystem.ReactiveActionSelector {
        private final ComputerSystemAgent system;
        ComputerSystemReactiveSelector(ComputerSystemAgent s) { this.system = s; }

        @Override
        public Action select(State perception) {
            if (system.currentMetrics == null) return null;

            if (system.currentMetrics.isOverloaded()) {
                return new ThrottleAction();
            }
            if (!system.taskQueue.isEmpty()) {
                ExternalComputerAction next = system.taskQueue.poll();
                if (next != null) return new ComputeAction(next);
            }
            return null;
        }

        @Override
        public Action selectRecovery(State p, Exception e) { return null; }
    }

    private static class ThrottleAction implements Action {
        @Override public ActionId getId() { return new ActionId("throttle"); }
        @Override public String getName() { return "throttle"; }
        @Override public Set<Precondition> getPreconditions() { return Set.of(); }
        @Override public Set<Effect> getEffects() { return Set.of(); }
        @Override public boolean isApplicableIn(State s) { return true; }
        @Override public boolean isApplicableIn(BeliefState b) { return true; }
        @Override public State apply(SymbolicState s) { return s; }
        @Override public PhysicalState apply(PhysicalState s) { return s; }
        @Override public BeliefState apply(BeliefState b) { return b; }
        @Override public ActionOutcome simulate(WorldModel w, State i) {
            return new ActionOutcome(i, 1.0, 1.0, 0.0, 0.0);
        }
    }

    // Wrapper to use metrics as State for ExecutionContext
    private static class MetricState implements State {
        private final AbstractComputerSystemsData metrics;

        MetricState(AbstractComputerSystemsData metrics) {
            this.metrics = metrics;
        }

        @Override public StateId getId() {
            return new StateId("metrics_" + System.currentTimeMillis());
        }

        @Override public StateType getType() {
            return StateType.SYMBOLIC;
        }

        @Override public boolean isCompatible(State other) {
            return other instanceof MetricState || other instanceof SymbolicState;
        }

        @Override public State copy() {
            return this;
        }

        AbstractComputerSystemsData getMetrics() {
            return metrics;
        }
    }
}
