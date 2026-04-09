package com.adam.agri.planner.symbolic.ontology.upper;

/**
 * Property of an entity.
 */
public interface Property {
    String getName();
    boolean holdsFor(Entity entity);
}
