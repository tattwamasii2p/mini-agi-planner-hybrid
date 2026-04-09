package com.adam.agri.planner.trajectories.builder.observations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.adam.agri.planner.core.state.Predicate;
import com.adam.agri.planner.core.state.State;
import com.adam.agri.planner.core.state.SymbolicState;
import com.adam.agri.planner.symbolic.ontology.upper.Abstract;
import com.adam.agri.planner.symbolic.ontology.upper.Concept;
import com.adam.agri.planner.symbolic.ontology.upper.Entity;
import com.adam.agri.planner.symbolic.ontology.upper.EntityId;

/**
 * Adapter for symbolic observations (SymbolicState, Predicate).
 * Converts high-level symbolic representations into observation format.
 */
public class SymbolicObservationAdapter implements ObservationSource<State> {

 private final List<State> observations;
 private double customConfidence = -1;

 public SymbolicObservationAdapter() {
 this.observations = new ArrayList<>();
 }

 @Override
 public void addObservation(State observation) {
 observations.add(observation);
 }

 public void addPredicate(Predicate predicate) {
 SymbolicState state = new SymbolicState(
 com.adam.agri.planner.core.state.StateId.of("pred_" + predicate.hashCode()),
 Collections.singleton(predicate),
 Collections.emptyMap()
 );
 observations.add(state);
 }

 public void addPredicates(Set<Predicate> predicates) {
 if (predicates != null) {
 predicates.forEach(this::addPredicate);
 }
 }

 @Override
 public List<State> getObservations() {
 return Collections.unmodifiableList(observations);
 }

 @Override
 public SourceType getSourceType() {
 return SourceType.SYMBOLIC_KNOWLEDGE;
 }

 @Override
 public double getSourceConfidence() {
 return customConfidence > 0 ? customConfidence : getSourceType().getDefaultConfidence();
 }

 public void setCustomConfidence(double confidence) {
 this.customConfidence = Math.max(0, Math.min(1, confidence));
 }

 @Override
 public List<Entity> toEntities() {
 List<Entity> entities = new ArrayList<>();
 for (State state : observations) {
 if (state instanceof SymbolicState) {
 entities.addAll(convertSymbolicState((SymbolicState) state));
 }
 }
 return entities;
 }

 private List<Entity> convertSymbolicState(SymbolicState state) {
 List<Entity> entities = new ArrayList<>();
 // Access bindings via reflection or use public methods
 // SymbolicState has getPredicates() but no getBindings(), access via public API
 state.getPredicates().forEach(predicate -> {
 entities.add(createPredicateEntity(predicate));
 });

 if (!state.getPredicates().isEmpty()) {
 Entity stateEntity = new SymbolicStateEntity(
 EntityId.of(state.getId().toString()),
 state.getPredicates()
 );
 entities.add(stateEntity);
 }

 return entities;
 }

 private Entity createPredicateEntity(Predicate predicate) {
 return new PredicateEntity(EntityId.of("pred_" + predicate.hashCode()), predicate);
 }

 @Override
 public void clear() {
 observations.clear();
 }

 /**
 * Entity representing a predicate.
 */
 private static class PredicateEntity extends Abstract {
 private final Predicate predicate;

 PredicateEntity(EntityId id, Predicate predicate) {
 super(id, Set.of());
 this.predicate = predicate;
 }

 @Override
 public String getName() {
 return predicate.name();
 }

 @Override
 public boolean isCompatibleWith(Entity other) {
 return other instanceof PredicateEntity;
 }

 @Override
 public boolean isCompatibleWith(Concept other) {
 return other instanceof PredicateEntity;
 }
 }

 /**
 * Entity representing a symbolic state.
 */
 private static class SymbolicStateEntity extends Abstract {
 private final Set<com.adam.agri.planner.core.state.Predicate> predicates;

 SymbolicStateEntity(EntityId id, Set<com.adam.agri.planner.core.state.Predicate> predicates) {
 super(id, Set.of());
 this.predicates = predicates;
 }

 @Override
 public String getName() {
 return getId().toString();
 }

 @Override
 public boolean isCompatibleWith(Entity other) {
 return other instanceof SymbolicStateEntity;
 }

 @Override
 public boolean isCompatibleWith(Concept other) {
 return other instanceof SymbolicStateEntity;
 }

 public Set<com.adam.agri.planner.core.state.Predicate> getPredicates() {
 return predicates;
 }

 @Override
 public String toString() {
 return "SymbolicStateEntity{id=" + getId() + ", predicates=" + predicates + "}";
 }
 }
}
