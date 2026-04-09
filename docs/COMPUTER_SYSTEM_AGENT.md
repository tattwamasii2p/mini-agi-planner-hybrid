# Computer System Agent Documentation

## Overview

The `ComputerSystemAgent` extends static computer system ontology (`ComputerSystem`) with active behavior using **pub/sub architecture**.

**Key insight**: A computer system (server, VM, GPU cluster) is itself an agent with:
- **PUBLISHES**: System state (CPU, memory, network metrics) as perception events
- **SUBSCRIBES**: Actuation commands (execute task, migrate, throttle)
- **PLANS**: Local scheduling decisions

This transforms passive computational resources into active participants in the multi-agent planning system using event-driven architecture.

## Pub/Sub Architecture

```
PerceptionPublisher ActuationPublisher
┌────────────────┐ ┌────────────────┐
│ CPU Event      │ ───────────────────│ ASSIGN_TASK │
│ Memory Event   │ Perception ├────────────────┤
│ Network Event  │ Channel    │ Scheduler → │
│ Task Events    │            │ ┌───────────┐ │
│                │            │ │ Node A    │ │
└─────┬──────────┘            │ └───────────┘ │
      │                       └───────┬────────┘
      │                               │
      ▼                       ┌──────┴──────┐
Subscribers:                  Subscribers:
- Scheduler                 - Local Executor
- Other Nodes               - Safety Monitor
- Monitoring                - PMS Controller
```

## Mathematical Model

### State Space

```
State ∈ ℝ⁴: (CPU_load, memory_usage, bandwidth, queue_depth)
CPU_load ∈ [0, 1]     // Fraction of capacity used
memory_usage ∈ [0, 1] // Fraction of RAM used
bandwidth ∈ [0, ∞)    // Mbps available
queue_depth ∈ ℕ₀      // Number of tasks waiting
```

### Perception Function

```
Perceive: HardwareMetrics → SystemState

CPU(t) = Σ(task_i.rate) / capacity
default:
    Balance load
```

**Load balancing policy:**
```
if (system.shouldOffload()) {
    // Find least loaded peer
    target = peers.minBy(s -> s.getMetrics().getQueueDepth());
    migrateMostUrgentTask(target);
} else if (hasCapacity()) {
    executeFromQueue();
}
```

## Multi-Agent Coordination

### Distributed Scheduling

Computer systems coordinate via shared sheaf:

```java
// Global scheduler distributes tasks
SheafAggregator<ExternalComputerAction> aggregator = new SheafAggregator<>();

for (ComputerSystemAgent node : cluster) {
    Trajectory localSchedule = node.generateLocalPlan(initialState, goal);
    aggregator.collect(localSchedule, node.getId());
}

// Merge schedules globally
Optional<Trajectory> global = aggregator.aggregate();
```

### Task Migration

```java
// When node is overloaded
if (nodeA.shouldOffload()) {
    nodeA.migrateTask(nodeB)
        .ifPresent(task -> {
            // Task moved to less loaded node
            globalQueue.redistribute();
        });
}
```

## Perception Publishing

**ComputerSystemAgent** publishes perception events describing system state:

### Publishing Events

```java
ComputerSystemAgent node = new ComputerSystemAgent(...);

// Periodic publishing (by sensor loop)
node.startReactiveControl(); // 500ms loops

// Manual publishing
PerceptionEvent event = node.publishSystemState();
// Event contains structured AbstractComputerSystemsData

// Access typed metrics
AbstractComputerSystemsData metrics = event
    .getData(AbstractComputerSystemsData.class)
    .orElseThrow();
double cpu = metrics.getCpuLoad();
double memory = metrics.getMemoryUsage();

// Task completion notification
node.publishTaskCompleted(task, result);
```

### Event Structure

**Using Structured Data Interface (AbstractComputerSystemsData):**

```java
// Build structured metrics using type-safe builder
ComputerSystemsData metrics = ComputerSystemsData.builder()
    .cpuLoad(0.75)
    .memoryUsage(0.60)
    .bandwidth(950.0) // Mbps
    .queueDepth(3)
    .availableCompute(250.0) // FLOPS
    .availableMemory(8.0)   // GB
    .build();

// Publish with structured data (replaces scattered .feature() calls)
PerceptionEvent event = PerceptionEvent.builder()
    .source(node.getId())
    .timestamp(Instant.now())
    .type(PerceptionType.INTERNAL)
    .data(metrics)  // Single structured object
    .confidence(0.95)
    .salience(0.7)
    .build();
```

### Typed Data Access

```java
// NEW: Type-safe access to structured data
public void onPerception(PerceptionEvent event) {
    // Old way (type-unsafe, string-based):
    // Double cpu = event.getFeature("cpu_load", Double.class).orElse(0.0);

    // NEW way (type-safe, structured):
    AbstractComputerSystemsData data = event
        .getData(AbstractComputerSystemsData.class)
        .orElse(null);

    if (data != null) {
        double cpu = data.getCpuLoad();      // [0.0, 1.0]
        double memory = data.getMemoryUsage(); // [0.0, 1.0]
        double bw = data.getBandwidth();     // Mbps
        int queue = data.getQueueDepth();      // tasks

        // Built-in capacity checks
        if (data.isOverloaded()) {
            scheduler.offloadFrom(this);
        }

        // Check if can accept new task
        if (data.hasCapacity(requiredCompute, requiredMemory)) {
            acceptTask(newTask);
        }
    }
}
```

### Subscribing to Node Perception

```java
// External agent subscribes to node state (using typed data)
PerceptionSubscription sub = node.subscribeToMetrics(
    event -> {
        AbstractComputerSystemsData data = event
            .getData(AbstractComputerSystemsData.class)
            .orElse(null);

        if (data != null && data.isOverloaded()) {
            // Trigger action
            scheduler.offloadFrom(node);
        }
        return true; // consumed
    },
    AbstractComputerSystemsData.class // Type filter
);

// Cancel subscription later
sub.cancel();
```

## Actuation Subscribing

**ComputerSystemAgent** subscribes to actuation commands:

### Receiving Commands

```java
// Remote scheduler publishes commands
Scheduler scheduler = new Scheduler();

// Node subscribes
node.subscribeToActuation(new ActuationSubscriber() {
    @Override
    public void onActuation(ActuationEvent event) {
        switch (event.getType()) {
            case ACTION_STARTED -> {
                ExternalComputerAction task = unwrapTask(event.action());
                node.assignTask(task);
            }
            case ACTION_COMPLETED -> {
                // Task finished
                recordSuccess(event.action(), event.result());
            }
            case ACTION_FAILED -> {
                // Handle failure
                retryOrReassign(event.action(), event.error());
            }
        }
    }

    @Override
    public boolean isSubscribedTo(EventType type) {
        // Only handle task commands
        return type == EventType.ACTION_STARTED
            || type == EventType.ACTION_COMPLETED
            || type == EventType.ACTION_FAILED;
    }
});

// Subscribe to specific types only
node.subscribeToActuationTypes(
    event -> handleFailure(event),
    ActuationEvent.EventType.ACTION_FAILED
);
```

### Command Pattern

```java
// Scheduler -> Node communication
ActuationPublisher schedulerPublisher = new ActuationPublisher(true);

// Node subscribes to scheduler
schedulerPublisher.subscribe(new ActuationSubscriber() {
    @Override
    public void onActuation(ActuationEvent event) {
        if (event.action() instanceof ExternalComputerAction task) {
            // Execute or queue
            if (canAccept(task)) {
                node.executeTask(task);
            } else {
                node.assignTask(task); // queue
            }
        }
    }
});

// Scheduler sends command
schedulerPublisher.publishActionStarted(
    new ExternalComputerAction(...),
    ExecutionContext.builder(state).build()
);
```

### Pub/Sub Sequence

```
Timeline:
1. Scheduler: publishActionStarted(task)
   ↓
2. Node: receives ActuationEvent(ACTION_STARTED)
   ↓
3. Node: adds to queue
   ↓
4. Node: starts execution
   ↓
5. Node: publishSystemState() with updated metrics
   ↓
6. Scheduler: receives perception update (typed data)
   ↓
7. Node: execution complete
   ↓
8. Node: publishTaskCompleted(task, result)
   ↓
9. Node: publishActionCompleted(task, context, result)
   ↓
10. Scheduler: receives completion
```

## AbstractComputerSystemsData Interface

**Type-safe, structured metrics for computer system agents.**

### Interface Definition

```java
public interface AbstractComputerSystemsData {
    double getCpuLoad();          // [0.0, 1.0]
    double getMemoryUsage();      // [0.0, 1.0]
    double getBandwidth();        // Mbps
    int    getQueueDepth();       // tasks in queue
    double getAvailableCompute(); // FLOPS
    double getAvailableMemory();  // bytes

    default boolean isOverloaded() {
        return getCpuLoad() > 0.8
            || getMemoryUsage() > 0.9
            || getQueueDepth() > 10;
    }

    default boolean hasCapacity(double requiredCompute,
                               double requiredMemory) {
        return getAvailableCompute() >= requiredCompute
            && getAvailableMemory() >= requiredMemory;
    }
}
```

### Immutable Implementation

```java
// Zero state (no load, no resources)
ComputerSystemsData zero = ComputerSystemsData.zero();

// Factory from raw values
ComputerSystemsData data = ComputerSystemsData.of(
    0.5,   // cpu
    0.3,   // memory
    1000.0, // bandwidth
    2,     // queue
    500.0, // available compute
    16.0   // available memory
);

// Builder pattern with validation
ComputerSystemsData data = ComputerSystemsData.builder()
    .cpuLoad(0.75)          // Clamped to [0,1]
    .memoryUsage(0.60)      // Clamped to [0,1]
    .bandwidth(950.0)       // Must be >= 0
    .queueDepth(3)          // Must be >= 0
    .availableCompute(250.0) // Must be >= 0
    .availableMemory(8.0)   // Must be >= 0
    .build();
```

## Application Scenarios

### Cloud Resource Management

```java
// Auto-scaling cluster
Cluster cluster = new Cluster("production");

for (ComputerSystemAgent node : cluster.getNodes()) {
    node.startReactiveControl(); // 500ms PMS loop
}

// Monitor and scale
while (running) {
    double avgLoad = cluster.getNodes().stream()
        .mapToDouble(n -> n.getMetrics().getCpuLoad())
        .average()
        .orElse(0.0);

    if (avgLoad > 0.8) {
        cluster.addNode(new ComputerSystemAgent(...));
    }
}
```

**Features:**
- Reactive load balancing
- Predictive resource allocation
- Automatic failover
- Cost-aware scheduling

### Edge Computing

Computer systems as edge agents:

- Perceive: Sensor streams, network conditions (typed data)
- Plan: Filter, aggregate, forward data
- Act: Execute ML inference, cache results

### GPU Cluster Scheduling

```java
// GPU-optimized compute agent
ComputerSystemAgent gpu = new ComputerSystemAgent(
    AgentId.of("gpu-001"),
    "GPU Node",
    new GpuWorldModel(),
    new MCTSPlanner(),
    new GpuHardwareSpec(4096, "A100") // 4096 TFLOPS
);

// Schedule ML training jobs
gpu.assignTask(new ExternalComputerAction(
    TaskType.TRAINING,
    Resources.GPU_MEMORY(40),
    Duration.hours(2)
));
```

## References

- **Actor Model**: Hewitt et al. - "A Universal Modular ACTOR Formalism"
- **Resource Management**: Kubernetes Scheduler, Mesos
- **Queueing Theory**: Smith - "Performance Engineering of Software Systems"
- **Control Theory**: Callender, Hartree & Porter - "Time-Lag in a Control System"
