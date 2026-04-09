package com.adam.agri.planner.symbolic.ontology.computer;

import com.adam.agri.planner.symbolic.ontology.upper.Entity;
import com.adam.agri.planner.symbolic.ontology.upper.EntityId;
import com.adam.agri.planner.symbolic.ontology.upper.Property;
import com.adam.agri.planner.symbolic.ontology.upper.TimeInterval;

import java.util.Set;

/**
 * An action executed on an external computer system.
 *
 * Represents remote procedure calls, distributed computation tasks,
 * or cloud computing operations that the AGI can plan to use.
 */
public class ExternalComputerAction extends com.adam.agri.planner.symbolic.ontology.upper.Process {
    private final ComputerSystem targetSystem;
    private final double requiredCompute;
    private final double requiredMemory;
    private final String actionType; // "compute", "store", "retrieve", "process"
    private final int priority;

    public ExternalComputerAction(EntityId id, Set<Property> properties, TimeInterval duration,
                                  Entity participant, ComputerSystem targetSystem,
                                  double requiredCompute, double requiredMemory,
                                  String actionType, int priority) {
        super(id, properties, duration, participant);
        this.targetSystem = targetSystem;
        this.requiredCompute = requiredCompute;
        this.requiredMemory = requiredMemory;
        this.actionType = actionType;
        this.priority = priority;
    }

    public ComputerSystem getTargetSystem() {
        return targetSystem;
    }

    public double getRequiredCompute() {
        return requiredCompute;
    }

    public double getRequiredMemory() {
        return requiredMemory;
    }

    public String getActionType() {
        return actionType;
    }

    public int getPriority() {
        return priority;
    }

    /**
     * Calculate the estimated cost of this action on the target system.
     */
    public double estimatedCost() {
        if (targetSystem == null || !targetSystem.isAvailable()) {
            return Double.POSITIVE_INFINITY;
        }
        double dur = duration != null ? duration.getDuration() : 1.0;
        double loadFactor = requiredCompute / targetSystem.getComputeCapacity();
        return dur * loadFactor;
    }

    /**
     * Check if this action can be executed on its target system.
     */
    public boolean isExecutable() {
        return targetSystem != null && targetSystem.canExecute(this);
    }

    @Override
    public String toString() {
        return "ExternalComputerAction[" + actionType + " on " + targetSystem + "]";
    }

    /**
     * Convenience accessor for duration field.
     */
    public TimeInterval getDurationInterval() {
        return duration;
    }
}
