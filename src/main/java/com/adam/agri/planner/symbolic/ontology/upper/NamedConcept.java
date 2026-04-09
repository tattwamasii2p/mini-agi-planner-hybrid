package com.adam.agri.planner.symbolic.ontology.upper;

/**
 * NamedConcept - interface for all named ontology entities.
 *
 * Provides a common contract for entities that have a human-readable name.
 * All ontology classes (Physical, Abstract, Process) implement this interface.
 */
public interface NamedConcept {

    /**
     * Get the name of this concept.
     */
    String getName();

    /**
     * Get the qualified/full name including scope.
     * May be same as getName() for simple entities.
     */
    default String getQualifiedName() {
        return getName();
    }

    /**
     * Get the simple name (without scope prefix).
     */
    default String getSimpleName() {
        String qualified = getQualifiedName();
        int lastDot = qualified.lastIndexOf('.');
        return lastDot >= 0 ? qualified.substring(lastDot + 1) : qualified;
    }

    /**
     * Get human-readable description.
     */
    default String getDescription() {
        return toString();
    }

    /**
     * Check if this concept has the given name.
     */
    default boolean isNamed(String name) {
        return getName().equals(name) || getQualifiedName().equals(name);
    }
}
