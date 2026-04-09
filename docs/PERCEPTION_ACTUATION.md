# Agent Perception and Action Execution Documentation

## Overview

The `agent.perception` and `agent.execution` packages implement the **sensorimotor loop** - the fundamental cycle connecting an agent to its environment.

```
┌─────────────┐         ┌─────────────┐         ┌─────────────┐
│             │         │             │         │             │
│   SENSORS   │ ──────► │   AGENT     │ ──────► │  ACTUATORS  │
│  (perceive) │         │  (decide)   │         │   (act)     │
│             │         │             │         │             │
└─────────────┘         └─────────────┘         └─────────────┘
       ▲                                              │
       │                                              │
       └───────────────── WORLD ─────────────────────┘
```

Mathematical model:
- **Perception**: World → ObservableState (epistemic morphism)
- **Action**: Intention → Effect (causal morphism)
- **Sensorimotor loop**: World → Perception → State → Action → World

## Perception Layer

### Core Concepts

**PerceptionEvent**: Raw and processed sensory data with spatiotemporal binding.

```java
PerceptionEvent event = PerceptionEvent.builder()
    .source(sensorId)
    .timestamp(Instant.now())
    .location(sensorPosition)
    .type(PerceptionType.VISUAL)
    .feature("object", "obstacle")
    .feature("distance", 1.5)
    .confidence(0.95)
    .salience(0.8)
    .build();
```

**Perception types:**
| Type | Description | Typical Sensors |
|------|-------------|-----------------|
| VISUAL | Light pattern | Camera, LIDAR, stereo |
| AUDITORY | Sound waves | Microphone, sonar |
| TACTILE | Physical contact | Pressure, touch |
| PROPRIOCEPTIVE | Body state | IMU, encoders |
| TEMPORAL | Time patterns | Clock, rate detectors |
| SOCIAL | Other agents | Communication, observation |
| INTERNAL | Self-monitoring | System metrics |

### Multi-Sensor Fusion

**SensorArray** combines multiple sensors with weighted confidence:

```java
SensorArray sensors = new SensorArray()
    .withSensor(new CameraSensor())
    .withSensor(new LidarSensor())
    .withSensor(new ImuSensor());

// Automatic fusion: weighted average of confidences
PerceptionEvent fused = sensors.perceive();

// Multi-modal: separate events
List<PerceptionEvent> multi = sensors.perceiveMultiModal();
```

**Fusion algorithm**:
```
fused_confidence = Σ(confidence_i × weight_i) / Σ(weight_i)
fused_features = merge(all_sensor_features)
fused_timestamp = now()
```

### Attention Mechanism

**AttentivePerception** filters sensory input - bottleneck from high-dimensional senses to limited processing capacity.

```java
AttentivePerception attention = new AttentivePerception(
    source,
    10,           // window size
    0.5,          // salience threshold
    0.7,          // relevance threshold
    AttentionMode.BALANCED
);

// Participate in Active Perception
attention.setAttentionCue("target_object");

List<PerceptionEvent> focused = attention.filter(allEvents);
```

**Attention modes**:

| Mode | Selection criterion | Use case |
|------|---------------------|----------|
| SALIENCE | Unexpected / intense | Detection, alert |
| RELEVANCE | Goal-related | Task execution |
| BALANCED | Combined score | General operation |

**Bottom-up attention** (saliency):
```
salience(event) = unexpectedness × intensity × novelty
```

**Top-down attention** (relevance):
```
relevance(event) = match(event, current_goal) × match(event, attention_cue)
```

### Active Perception

Actions configure sensors for optimal perception:

```java
// Agent moves head to see better
perception.updateWithAction(new TurnHead(30));

// Sensor reconfigures based on expected new view
followUp = perception.perceive();
```

## Action Execution Layer

### Execution Flow

**PhysicalExecutor** converts high-level Actions to low-level actuator commands:

```
┌──────────┐    ┌─────────────┐    ┌─────────────┐    ┌──────────┐
│  Action  │───►│  Precond    │───►│   Safety    │───►│ Map to   │
│  (plan)  │    │  Check      │    │   Check     │    │ Commands │
└──────────┘    └─────────────┘    └─────────────┘    └────┬─────┘
                                                            │
┌──────────┐    ┌─────────────┐    ┌─────────────┐    ┌────┴─────┐
│  Result  │◄───│  Effect     │◄───│  Execute    │◄───│ Actuator │
│  (State) │    │  Verify     │    │  (monitor)  │    │ Commands │
└──────────┘    └─────────────┘    └─────────────┘    └──────────┘
```

**Execution pipeline**:

```java
PhysicalExecutor executor = new PhysicalExecutor(
    List.of(armActuator, baseActuator),
    safetyMonitor
);

ExecutionResult result = executor.execute(
    new PickUpObject(target),
    ExecutionContext.builder(currentState)
        .timeout(30000)
        .verifyEffects(true)
        .maxRetries(3)
        .build()
);

if (result.isSuccess()) {
    PhysicalState newState = result.getFinalState().get();
} else {
    String error = result.getErrorMessage().get();
}
```

### Safety Monitoring

**SafetyMonitor** validates actions against physical constraints:

```java
SafetyMonitor safety = new SafetyMonitor(0.1); // 10% safety margin

safety.addRule(new VelocityLimitRule(1.0));      // m/s
safety.addRule(new CollisionAvoidanceRule());    // Check obstacles
safety.addRule(new JointLimitRule(min, max));    // Joint angles

if (!safety.check(action, context)) {
    return ExecutionResult.failed("safety", "Velocity limit violated");
}
```

**Built-in safety rules**:

| Rule | Check | Failure |
|------|-------|---------|
| VelocityLimit | `v < v_max` | Excessive speed |
| CollisionAvoidance | `distance(obstacle) > margin` | Collision risk |
| JointLimit | `q_min < q < q_max` | Overextension |

### Execution Feedback

Each execution step provides feedback from actuators:

```java
ExecutionFeedback feedback = ExecutionFeedback.builder()
    .addStep(new ActuatorResult(true, null, position))
    .addStep(new ActuatorResult(false, "timeout", null))
    .withResultState(finalState)
    .build();

if (!feedback.isSuccess()) {
    // Handle partial execution
}
```

**Feedback contains**:
- Success/failure per actuator step
- Error messages for diagnosis
- Resulting state after execution
- Timing information

## Perceptual-Motor System

### Closing the Loop

**PerceptualMotorSystem** integrates perception and execution into a continuous control loop:

```java
PerceptualMotorSystem pms = new PerceptualMotorSystem(
    perception,          // Sense
    reactiveSelector,    // Decide
    executor,            // Act
    100                  // 100ms cycle
);

pms.start();  // Begin continuous loop

// Statistics
double latency = pms.getAverageLatency();  // ms
long cycles = pms.getCyclesExecuted();
int success = pms.getSuccessCount();
```

**Control loop**:
```
while (running) {
    // Phase 1: Perceive
    PerceptionEvent event = perception.perceive();
    State state = extractState(event);

    // Phase 2: Select
    Action action = selector.select(state);

    // Phase 3: Execute
    ExecutionResult result = executor.execute(action, context);

    // Phase 4: Learn
    selector.updateWithFeedback(state, action, result);

    // Maintain cycle
    sleep(cyclePeriod - elapsed);
}
```

### Reactive Control

**ReactiveActionSelector** for fast responses without planning:

```java
ReactiveActionSelector reflex = new ReactiveActionSelector() {
    @Override
    public Action select(State perception) {
        // Immediate response
        if (isObstacleClose(perception)) {
            return new EmergencyStop();
        }
        return followTrajectory();
    }

    @Override
    public Action selectRecovery(State perception, Exception error) {
        // Error recovery
        return new ResetToSafeState();
    }

    @Override
    public void updateWithFeedback(State p, Action a, ExecutionResult r) {
        // Reinforcement learning
        updatePolicy(p, a, r.isSuccess() ? 1 : -1);
    }
};
```

### Conditional Execution

Wait for specific perceptual conditions:

```java
// Execute only when target is visible
ExecutionResult result = pms.executeWhen(
    action,
    initialState,
    event -> event.getFeatures().containsKey("target"),
    10000  // timeout ms
);
```

### Perception-Driven Execution

Execution adapts based on real-time perception:

```java
// Monitor during execution
ExecutionResult result = pms.executeWithPerception(
    action,
    initialState
);

// If fails, perception analyzes why
PerceptionEvent postError = perception.perceive();
String analysis = analyzeWhatWentWrong(postError, action);
Action adjusted = adjustAction(original, analysis);
```

## Integration with Planning

### From Perception to State

```java
// Convert perception to planning state
State state = new PhysicalState(
    perceptionEvent.getFeatures().get("location"),
    perceptionEvent.getFeatures().get("velocity"),
    ...
);

// Update local world model
agent.getLocalWorldModel().update(state);

// Replan if needed
if (!currentPlan.isValidFor(state)) {
    newPlan = agent.generateLocalPlan(state, goal);
}
```

### From Action to Plan

```java
// High-level plan actions are grounded
Trajectory plan = agent.getLocalPlan();
for (Action step : plan) {
    // Execute
    ExecutionResult r = pms.executeWithPerception(step, currentState);

    if (!r.isSuccess()) {
        // Replan on failure
        break; // Exit and replan
    }

    // Update state from reality
    currentState = r.getFinalState().get();
}
```

## Error Handling

### Perception Failures

```java
PerceptionEvent event = perception.perceive();
if (event == null || event.getConfidence() < 0.5) {
    // Sensor failure - use alternative
    event = alternativeSensor.perceive();
}
```

### Execution Failures

```java
ExecutionResult result = executor.execute(action, context);

switch (result.getStatus()) {
    case SUCCESS:
        // Continue normally
        break;
    case PARTIAL:
        // Adapt to partial success
        compensateForPartial(result);
        break;
    case FAILED:
        // Retry or replan
        retryAction(action, limit);
        break;
    case ERROR:
        // Exception occurred
        emergencyStop();
        break;
}
```

### Recovery Strategies

Automatic recovery from execution failures:

```java
@Override
public Action selectRecovery(State perception, Exception error) {
    // Analyze error
    if (error instanceof ExecutionException) {
        // Hardware issue
        return new ResetActuators();
    }
    if (perception.getFeatures().containsKey("obstacle")) {
        // Blocked path
        return new FindAlternativePath();
    }
    // Default: safe state
    return new HomingAction();
}
```

## Configuration

### Perception Settings

```properties
# Sampling rates
perception.camera.rate=30Hz
perception.lidar.rate=10Hz
perception.imu.rate=200Hz

# Attention parameters
attention.window_size=20
attention.salience_threshold=0.6
attention.relevance_threshold=0.7

# Fusion weights
fusion.camera_weight=0.5
fusion.lidar_weight=0.4
fusion.imu_weight=0.1
```

### Execution Settings

```properties
# Timeout and retry
execution.timeout=30000
execution.retry.max=3
execution.retry.delay=1000

# Safety margins
safety.velocity_margin=0.1
safety.distance_margin=0.5

# Actuator timing
actuator.command_timeout=5000
actuator.feedback_deadline=100
```

## Performance

| Component | Latency | Throughput |
|-----------|---------|------------|
| Perception (single) | ~10-50ms | Sensor-dependent |
| Sensor fusion | +5-10ms | O(n) |
| Attention filter | +1-5ms | O(m window) |
| Action selection | ~1-10ms | 100-1000 Hz |
| Execution (single) | ~100-500ms | Action-dependent |
| Full PMS cycle | 10-100ms | 10-100 Hz |

## Testing

```java
@Test
public void testSensorFusion() {
    MockSensor cam = new MockSensor("camera");
    cam.setReading("object", "box", 0.9);

    MockSensor lidar = new MockSensor("lidar");
    lidar.setReading("distance", 1.5, 0.95);

    SensorArray array = new SensorArray()
        .withSensor(cam).withSensor(lidar);

    PerceptionEvent result = array.perceive();

    assertTrue(result.getFeatures().containsKey("object"));
    assertTrue(result.getFeatures().containsKey("distance"));
}

@Test
public void testSafetyViolation() {
    Action fastAction = new MoveAction(10.0); // 10 m/s

    SafetyMonitor safety = new SafetyMonitor();
    safety.addRule(new VelocityLimitRule(1.0));

    assertFalse(safety.check(fastAction, mockContext));
}

@Test
public void testPerceptualMotorLoop() {
    PerceptualMotorSystem pms = new PerceptualMotorSystem(
        mockPerception, mockSelector, mockExecutor, 100
    );

    pms.start();
    Thread.sleep(500);
    pms.stop();

    assertTrue(pms.getCyclesExecuted() > 0);
    assertTrue(pms.getAverageLatency() > 0);
}
```

## References

- **Active Perception**: Aloimonos (1990) - "Purposive and Qualitative Active Vision"
- **Affordance Theory**: Gibson (1979) - "The Ecological Approach to Visual Perception"
- **Subsumption Architecture**: Brooks (1986) - "A Robust Layered Control System"
- **Perceptual Control Theory**: Powers (1973) - "Behavior: The Control of Perception"
