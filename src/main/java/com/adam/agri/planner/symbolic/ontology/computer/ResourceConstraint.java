package com.adam.agri.planner.symbolic.ontology.computer;

import com.adam.agri.planner.symbolic.ontology.upper.Property;
import com.adam.agri.planner.symbolic.ontology.upper.Entity;

/**
 * Property representing resource constraints on computer systems.
 */
public class ResourceConstraint implements Property {
    private final String name;
    private final double maxCompute;
    private final double maxMemory;
    private final double maxDuration;

    public ResourceConstraint(String name, double maxCompute, double maxMemory, double maxDuration) {
        this.name = name;
        this.maxCompute = maxCompute;
        this.maxMemory = maxMemory;
        this.maxDuration = maxDuration;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean holdsFor(Entity entity) {
        if (entity instanceof ComputerSystem) {
            ComputerSystem cs = (ComputerSystem) entity;
            return cs.getComputeCapacity() <= maxCompute &&
                   cs.getMemoryCapacity() <= maxMemory;
        }
        if (entity instanceof ExternalComputerAction) {
            ExternalComputerAction action = (ExternalComputerAction) entity;
            return action.getRequiredCompute() <= maxCompute &&
                   action.getRequiredMemory() <= maxMemory &&
                   (action.getDuration() == null || action.getDuration().getDuration() <= maxDuration);
        }
        return false;
    }

    public double getMaxCompute() {
        return maxCompute;
    }

    public double getMaxMemory() {
        return maxMemory;
    }

    public double getMaxDuration() {
        return maxDuration;
    }
}
