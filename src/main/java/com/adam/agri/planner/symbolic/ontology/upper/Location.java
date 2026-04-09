package com.adam.agri.planner.symbolic.ontology.upper;

/**
 * Spatial location.
 */
public interface Location {
    double[] getCoordinates();
    double distanceTo(Location other);
    boolean overlaps(Location other);
}
