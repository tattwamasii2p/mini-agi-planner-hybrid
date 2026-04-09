package com.adam.agri.planner.symbolic.ontology.upper;

import java.util.Set;

/**
 * Process - dynamic entity unfolding in time.
 *
 * Can be physical (running) or abstract (computational).
 * From upper ontology (SUMO/BFO/DOLCE inspired).
 */
public abstract class Process extends EntityImpl {
    protected final TimeInterval duration;
    protected final Location location;
    protected final Set<Entity> participants;

    public Process(EntityId id, Set<Property> properties, TimeInterval duration, Entity participant) {
        super(id, properties);
        this.duration = duration;
        this.participants = Set.of(participant);
        this.location = null;
    }

    public Process(EntityId id, Set<Property> properties, Location location, TimeInterval timeInterval) {
        super(id, properties);
        this.duration = timeInterval;
        this.location = location;
        this.participants = Set.of();
    }

    public TimeInterval getDuration() {
        return duration;
    }

    public Set<Entity> getParticipants() {
        return participants;
    }

    /**
     * Check if this process is happening now (at given time).
     */
    public boolean isHappeningAt(double timestamp) {
        return duration != null && duration.contains(timestamp);
    }

    /**
     * Check if this process overlaps temporally with another.
     */
    public boolean temporallyOverlaps(Process other) {
        if (this.duration == null || other.duration == null) {
            return false;
        }
        return this.duration.overlaps(other.duration);
    }

    /**
     * Processes can be physical (running) or abstract (computational).
     */
    public boolean isPhysicalProcess() {
        for (Entity p : participants) {
            if (p instanceof Physical) return true;
        }
        return false;
    }
}
