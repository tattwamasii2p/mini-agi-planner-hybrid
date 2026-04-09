package com.adam.agri.planner.symbolic.ontology.upper;

import java.util.Set;

/**
 * Physical entity - has spatio-temporal location.
 *
 * From upper ontology (SUMO/BFO/DOLCE inspired).
 */
public abstract class Physical extends EntityImpl {
    protected final Location location;
    protected final TimeInterval timeInterval;

    public Physical(EntityId id, Set<Property> properties, Location location, TimeInterval timeInterval) {
        super(id, properties);
        this.location = location;
        this.timeInterval = timeInterval;
    }

    public Location getLocation() {
        return location;
    }

    public TimeInterval getTimeInterval() {
        return timeInterval;
    }

    /**
     * Check if this physical entity physically overlaps with another.
     */
    public boolean physicallyOverlaps(Physical other) {
        if (this.location == null || other.location == null) {
            return false;
        }
        boolean spatialOverlap = this.location.overlaps(other.location);
        boolean temporalOverlap = this.timeInterval != null && other.timeInterval != null
            && this.timeInterval.overlaps(other.timeInterval);
        return spatialOverlap && temporalOverlap;
    }
}
