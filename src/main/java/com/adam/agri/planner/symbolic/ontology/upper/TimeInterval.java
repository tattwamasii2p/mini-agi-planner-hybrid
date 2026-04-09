package com.adam.agri.planner.symbolic.ontology.upper;

/**
 * Temporal interval.
 */
public interface TimeInterval {
    double getStart();
    double getEnd();
    double getDuration();
    boolean overlaps(TimeInterval other);
    boolean contains(double timestamp);
}
