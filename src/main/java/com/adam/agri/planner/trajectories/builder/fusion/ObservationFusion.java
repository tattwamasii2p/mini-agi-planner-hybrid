package com.adam.agri.planner.trajectories.builder.fusion;

import com.adam.agri.planner.symbolic.ontology.upper.Concept;
import com.adam.agri.planner.symbolic.ontology.upper.Entity;
import com.adam.agri.planner.trajectories.builder.observations.ObservationSource;

import java.util.*;

/**
 * Fuses observations from multiple sources.
 * Uses sheaf theory to merge partial world models from different sensors and inputs.
 *
 * Each observation source produces a "local section" of the world model.
 * Fusion applies Čech gluing to combine these into a consistent global model.
 */
public class ObservationFusion {

 private final Map<ObservationSource.SourceType, Double> confidenceWeights;
 private final double compatibilityThreshold;

 public ObservationFusion() {
 this.confidenceWeights = new HashMap<>();
 this.compatibilityThreshold = 0.7;
 // Default: equal weight
 for (ObservationSource.SourceType type : ObservationSource.SourceType.values()) {
 confidenceWeights.put(type, type.getDefaultConfidence());
 }
 }

 /**
 * Set custom confidence weight for a source type.
 */
 public ObservationFusion setConfidenceWeight(ObservationSource.SourceType type, double weight) {
 confidenceWeights.put(type, Math.max(0, Math.min(1, weight)));
 return this;
 }

 /**
 * Fuse multiple observation sources into merged entities.
 */
 public List<FusedEntity> fuse(List<ObservationSource<?>> sources) {
 // Collect all entities with confidence weighting
 List<WeightedEntity> weightedEntities = new ArrayList<>();
 for (ObservationSource<?> source : sources) {
 if (!source.hasObservations()) {
 continue;
 }
 double sourceWeight = confidenceWeights.getOrDefault(
 source.getSourceType(),
 source.getSourceType().getDefaultConfidence()
 );
 double sourceConfidence = source.getSourceConfidence();

 for (Entity entity : source.toEntities()) {
 weightedEntities.add(new WeightedEntity(
 entity,
 sourceWeight * sourceConfidence,
 source.getSourceType()
 ));
 }
 }

 // Group entities by compatibility (sheaf gluing)
 List<Set<WeightedEntity>> compatibleGroups = findCompatibleGroups(weightedEntities);

 // Merge each compatible group
 List<FusedEntity> fused = new ArrayList<>();
 for (Set<WeightedEntity> group : compatibleGroups) {
 fused.add(mergeGroup(group));
 }

 return fused;
 }

 /**
 * Find groups of compatible entities.
 * Two entities are compatible if they refer to the same underlying entity.
 */
 private List<Set<WeightedEntity>> findCompatibleGroups(List<WeightedEntity> entities) {
 Map<String, Set<WeightedEntity>> groups = new HashMap<>();

 for (WeightedEntity entity : entities) {
 boolean added = false;

 // Try to add to existing group
 for (Set<WeightedEntity> group : groups.values()) {
 if (isCompatibleWithGroup(entity, group)) {
 group.add(entity);
 added = true;
 break;
 }
 }

 // Create new group if not compatible with any existing
 if (!added) {
 String key = entity.getEntity().getId().toString();
 Set<WeightedEntity> group = new HashSet<>();
 group.add(entity);
 groups.put(key, group);
 }
 }

 return new ArrayList<>(groups.values());
 }

 /**
 * Check if entity is compatible with a group.
 */
 /**
 * Check if entity is compatible with a group.
 */
 private boolean isCompatibleWithGroup(WeightedEntity entity, Set<WeightedEntity> group) {
 // Check compatibility with any member
 for (WeightedEntity member : group) {
 if (areCompatible(entity.getEntity(), member.getEntity())) {
 return true;
 }
 }
 return false;
 }

 /**
 * Check if two entities are compatible.
 */
 private boolean areCompatible(Entity a, Entity b) {
 // Same ID = definitely compatible
 if (a.getId().equals(b.getId())) {
 return true;
 }

 // Same name = probably compatible
 if (a.getName().equals(b.getName())) {
 return true;
 }

 // Check ontology compatibility
 try {
 return a.isCompatibleWith(b) || b.isCompatibleWith(a);
 } catch (Exception e) {
 return false;
 }
 }

 /**
 * Merge a group of weighted entities into a fused entity.
 */
 private FusedEntity mergeGroup(Set<WeightedEntity> group) {
 if (group.isEmpty()) {
 throw new IllegalArgumentException("Cannot merge empty group");
 }
 if (group.size() == 1) {
 WeightedEntity single = group.iterator().next();
 return new FusedEntity(single.getEntity(), single.getWeight(),
 Collections.singletonList(single.getSourceType()));
 }

 // Compute weighted properties
 double totalWeight = group.stream().mapToDouble(WeightedEntity::getWeight).sum();

 // Use entity with highest weight as base
 Entity base = group.stream()
 .max(Comparator.comparingDouble(WeightedEntity::getWeight))
 .map(WeightedEntity::getEntity)
 .orElseThrow();

 List<ObservationSource.SourceType> sources = group.stream()
 .map(WeightedEntity::getSourceType)
 .distinct()
 .toList();

 return new FusedEntity(base, totalWeight, sources);
 }

 /**
 * Check sheaf condition (consistency on overlaps).
 */
 public boolean checkSheafCondition(List<FusedEntity> fused) {
 // All pairs should be compatible
 for (int i = 0; i < fused.size(); i++) {
 for (int j = i + 1; j < fused.size(); j++) {
 if (!areCompatible(fused.get(i).getEntity(), fused.get(j).getEntity())) {
 return false;
 }
 }
 }
 return true;
 }

 /**
 * Entity with associated confidence weight.
 */
 private static class WeightedEntity {
 private final Entity entity;
 private final double weight;
 private final ObservationSource.SourceType sourceType;

 WeightedEntity(Entity entity, double weight, ObservationSource.SourceType sourceType) {
 this.entity = entity;
 this.weight = weight;
 this.sourceType = sourceType;
 }

 Entity getEntity() { return entity; }
 double getWeight() { return weight; }
 ObservationSource.SourceType getSourceType() { return sourceType; }
 }

 /**
 * Fused entity from multiple observations.
 */
 public static class FusedEntity implements Entity {
 private final Entity baseEntity;
 private final double fusedConfidence;
 private final List<ObservationSource.SourceType> sourceTypes;

 public FusedEntity(Entity baseEntity, double fusedConfidence,
 List<ObservationSource.SourceType> sourceTypes) {
 this.baseEntity = baseEntity;
 this.fusedConfidence = fusedConfidence;
 this.sourceTypes = new ArrayList<>(sourceTypes);
 }

 public Entity getEntity() { return baseEntity; }
 public double getFusedConfidence() { return fusedConfidence; }
 public List<ObservationSource.SourceType> getSourceTypes() { return sourceTypes; }

 @Override
 public com.adam.agri.planner.symbolic.ontology.upper.EntityId getId() {
 return baseEntity.getId();
 }

 @Override
 public String getName() {
 return baseEntity.getName();
 }

 @Override
 public String getQualifiedName() {
 return baseEntity.getQualifiedName();
 }

 @Override
 public com.adam.agri.planner.symbolic.ontology.upper.Entity.ConceptType getConceptType() {
 return baseEntity.getConceptType();
 }

 @Override
 public boolean isCompatibleWith(com.adam.agri.planner.symbolic.ontology.upper.Entity other) {
 return baseEntity.isCompatibleWith(other);
 }

 @Override
 public boolean isCompatibleWith(Concept other) {
 return baseEntity.isCompatibleWith(other);
 }

 @Override
 public boolean isPhysical() {
 return baseEntity.isPhysical();
 }

 @Override
 public boolean isAbstract() {
 return baseEntity.isAbstract();
 }

 @Override
 public boolean isProcess() {
 return baseEntity.isProcess();
 }

 @Override
 public java.util.Set<com.adam.agri.planner.symbolic.ontology.upper.Property> getProperties() {
 return baseEntity.getProperties();
 }

 @Override
 public String toString() {
 return "FusedEntity{" + baseEntity.getName() +
 " conf=" + String.format("%.2f", fusedConfidence) +
 " sources=" + sourceTypes + "}";
 }
 }
}
