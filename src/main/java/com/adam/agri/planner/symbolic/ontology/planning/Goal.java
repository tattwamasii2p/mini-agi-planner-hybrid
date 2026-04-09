package com.adam.agri.planner.symbolic.ontology.planning;

import com.adam.agri.planner.symbolic.ontology.upper.Abstract;
import com.adam.agri.planner.symbolic.ontology.upper.EntityId;
import com.adam.agri.planner.symbolic.ontology.upper.Property;

import java.util.Set;

/**
 * A planning goal - an abstract target state to achieve.
 *
 * Goals are abstract entities that can be realized through planning.
 */
public class Goal extends Abstract {
    private final String description;
    private final double priority;
    private final double deadline;
    private final boolean isAchieved;

    public Goal(EntityId id, Set<Property> properties, String description,
                double priority, double deadline) {
        super(id, properties);
        this.description = description;
        this.priority = priority;
        this.deadline = deadline;
        this.isAchieved = false;
    }

    public Goal(EntityId id, Set<Property> properties, String description,
                double priority, double deadline, boolean isAchieved) {
        super(id, properties);
        this.description = description;
        this.priority = priority;
        this.deadline = deadline;
        this.isAchieved = isAchieved;
    }

    public String getDescription() {
        return description;
    }

    public double getPriority() {
        return priority;
    }

    public double getDeadline() {
        return deadline;
    }

    public boolean isAchieved() {
        return isAchieved;
    }

    /**
     * Create a new Goal representing the achieved state.
     */
    public Goal achieved() {
        return new Goal(id, properties, description, priority, deadline, true);
    }

    /**
     * Check if deadline has passed at given time.
     */
    public boolean isExpired(double currentTime) {
        return currentTime > deadline;
    }

    @Override
    public String toString() {
        return "Goal[" + description + "]" + (isAchieved ? "(achieved)" : "");
    }
}
