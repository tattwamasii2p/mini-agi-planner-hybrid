package com.adam.agri.planner.symbolic.ontology.computer;

import com.adam.agri.planner.symbolic.ontology.upper.*;

import java.util.Set;

/**
 * A physical computer system that can execute actions.
 *
 * Represents external computers in the world (not the AGI itself).
 * Enables planning for distributed/remote computation.
 */
public class ComputerSystem extends Physical {
    private final String hostname;
    private final double computeCapacity; // FLOPS or normalized capacity
    private final double memoryCapacity;  // bytes
    private final boolean isAvailable;

    public ComputerSystem(EntityId id, Set<Property> properties, Location location,
                          TimeInterval timeInterval, String hostname,
                          double computeCapacity, double memoryCapacity, boolean isAvailable) {
        super(id, properties, location, timeInterval);
        this.hostname = hostname;
        this.computeCapacity = computeCapacity;
        this.memoryCapacity = memoryCapacity;
        this.isAvailable = isAvailable;
    }

    public String getHostname() {
        return hostname;
    }

    public double getComputeCapacity() {
        return computeCapacity;
    }

    public double getMemoryCapacity() {
        return memoryCapacity;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    /**
     * Check if this computer can execute a given action.
     */
    public boolean canExecute(ExternalComputerAction action) {
        return isAvailable &&
               computeCapacity >= action.getRequiredCompute() &&
               memoryCapacity >= action.getRequiredMemory();
    }

    @Override
    public String toString() {
        return "ComputerSystem[" + hostname + "@" + location + "]";
    }
}
