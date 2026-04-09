package com.adam.agri.planner.symbolic.ontology.upper;

/**
 * Concept - base interface for all ontology concepts.
 *
 * From upper ontology (SUMO/BFO/DOLCE inspired).
 * Provides common interface for entities, relations, and properties.
 */
public interface Concept extends NamedConcept {

 /**
 * Get the ID of this concept.
 */
 EntityId getId();

 /**
 * Check if this concept is compatible with another.
 * Returns true if they can coexist in the same model.
 */
 boolean isCompatibleWith(Concept other);
}
