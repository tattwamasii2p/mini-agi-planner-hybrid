package com.adam.agri.planner.symbolic.ontology.upper;

import java.util.Set;

/**
 * Top-level entity - everything in the system is an Entity.
 *
 * From upper ontology (SUMO/BFO/DOLCE inspired).
 * Implements NamedConcept for semantic integration.
 */
public interface Entity extends Concept {

 EntityId getId();

 @Override
 String getName();

 @Override
 String getQualifiedName();

 ConceptType getConceptType();

 /**
 * Check if this entity is compatible with another entity.
 * Returns true if they can be considered the same or related entity.
 */
 boolean isCompatibleWith(Entity other);

 /**
 * Check if entity is physical (has spatio-temporal embodiment).
 */
 boolean isPhysical();

 /**
 * Check if entity is abstract (conceptual).
 */
 boolean isAbstract();

 /**
 * Check if entity is a process (dynamic, happening in time).
 */
 boolean isProcess();

 Set<Property> getProperties();

 /**
 * Check if this concept is an instance of the given type.
 */
 default boolean isA(ConceptType type) {
 return getConceptType() == type;
 }

 /**
 * Get the IRI (Internationalized Resource Identifier) for this concept.
 * Used for semantic web integration.
 */
 default String getIRI() {
 return "agi://ontology/" + getId().toString();
 }
     /**                                                                                                                                                                                           
      * Ontology concept types.                                                                                                                                                                    
      */                                                                                                                                                                                           
     enum ConceptType {                                                                                                                                                                            
         PHYSICAL,      // Has spatio-temporal location                                                                                                                                            
         ABSTRACT,      // Conceptual, no physical form                                                                                                                                            
         PROCESS,       // Dynamic, happening in time                                                                                                                                              
         RELATION,      // Connects concepts                                                                                                                                                       
         PROPERTY,      // Attribute or characteristic                                                                                                                                             
         COLLECTION     // Group of concepts                                                                                                                                                       
     }      
}
