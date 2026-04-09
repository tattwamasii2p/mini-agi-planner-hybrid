package com.adam.agri.planner.computer;

/**
 * This interface replaces scattered Map<String, Object> feature access
 * with strongly-typed, validated data structures.
 */
public interface AbstractComputerSystemsData {

    /**
     * CPU load as fraction of capacity [0.0, 1.0].
     */
    double getCpuLoad();

    /**
     * Memory usage as fraction of capacity [0.0, 1.0].
     */
    double getMemoryUsage();

    /**
     * Available bandwidth in Mbps.
     */
    double getBandwidth();

    /**
     * Number of tasks in queue.
     */
    int getQueueDepth();

    /**
     * Available compute capacity (FLOPS or normalized).
     */
    double getAvailableCompute();

    /**
     * Available memory in bytes.
     */
    double getAvailableMemory();

    /**
     * Check if system is overloaded.
     */
    default boolean isOverloaded() {
        return getCpuLoad() > 0.8 || getMemoryUsage() > 0.9 || getQueueDepth() > 10;
    }

    /**
     * Check if system has capacity for a task.
     */
    default boolean hasCapacity(double requiredCompute, double requiredMemory) {
        return getAvailableCompute() >= requiredCompute
            && getAvailableMemory() >= requiredMemory;
    }

    /**
     * Convert to builder for modification.
     */
    default ComputerSystemsData.Builder toBuilder() {
        return ComputerSystemsData.builder()
            .cpuLoad(getCpuLoad())
            .memoryUsage(getMemoryUsage())
            .bandwidth(getBandwidth())
            .queueDepth(getQueueDepth())
            .availableCompute(getAvailableCompute())
            .availableMemory(getAvailableMemory());
    }
}
