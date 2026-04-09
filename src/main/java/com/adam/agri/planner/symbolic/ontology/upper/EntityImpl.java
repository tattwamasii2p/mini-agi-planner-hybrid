package com.adam.agri.planner.symbolic.ontology.upper;

import java.util.Set;

/**
 * Implementation base class for Entity.
 *
 * From upper ontology (SUMO/BFO/DOLCE inspired).
 * Provides default implementations for Entity interface methods.
 */
public abstract class EntityImpl implements Entity {
 protected final EntityId id;
 protected final Set<Property> properties;

 public EntityImpl(EntityId id, Set<Property> properties) {
 this.id = id;
 this.properties = properties;
 }

 @Override
 public EntityId getId() {
 return id;
 }

 @Override
 public String getName() {
 return id.toString();
 }

 @Override
 public String getQualifiedName() {
 return getName();
 }

 @Override
 public ConceptType getConceptType() {
 if (this instanceof Physical) return ConceptType.PHYSICAL;
 if (this instanceof Abstract) return ConceptType.ABSTRACT;
 if (this instanceof Process) return ConceptType.PROCESS;
 return ConceptType.ABSTRACT;
 }

 @Override
 public boolean isCompatibleWith(Entity other) {
 return other instanceof EntityImpl;
 }

 @Override
 public boolean isCompatibleWith(Concept other) {
 return other instanceof Entity;
 }

 /**
 * Check if entity is physical (has spatio-temporal embodiment).
 */
 public boolean isPhysical() {
 return this instanceof Physical;
 }

 /**
 * Check if entity is abstract (conceptual).
 */
 public boolean isAbstract() {
 return this instanceof Abstract;
 }

 /**
 * Check if entity is a process (dynamic, happening in time).
 */
 public boolean isProcess() {
 return this instanceof Process;
 }

 public Set<Property> getProperties() {
 return properties;
 }
}
