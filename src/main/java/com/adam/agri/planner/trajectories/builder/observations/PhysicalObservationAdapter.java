package com.adam.agri.planner.trajectories.builder.observations;

import com.adam.agri.planner.agent.perception.PerceptionEvent;
import com.adam.agri.planner.core.state.PhysicalState;
import com.adam.agri.planner.symbolic.ontology.upper.Concept;
import com.adam.agri.planner.symbolic.ontology.upper.Entity;
import com.adam.agri.planner.symbolic.ontology.upper.EntityId;
import com.adam.agri.planner.symbolic.ontology.upper.Location;
import com.adam.agri.planner.symbolic.ontology.upper.Physical;
import com.adam.agri.planner.symbolic.ontology.upper.TimeInterval;

import java.util.*;

public class PhysicalObservationAdapter implements ObservationSource<PhysicalObservationAdapter.PhysicalObservation> {

 private final List<PhysicalObservation> observations;
 private double customConfidence = -1;

 public interface PhysicalObservation {
 double getTimestamp();
 Optional<Location> getLocation();
 Map<String, Object> getFeatures();
 double getConfidence();
 }

 public PhysicalObservationAdapter() {
 this.observations = new ArrayList<>();
 }

 @Override
 public void addObservation(PhysicalObservation observation) {
 observations.add(observation);
 }

 public void addPerceptionEvent(PerceptionEvent event) {
 observations.add(new PerceptionEventWrapper(event));
 }

 public void addPhysicalState(PhysicalState state) {
 observations.add(new PhysicalStateWrapper(state));
 }

 @Override
 public List<PhysicalObservation> getObservations() {
 return Collections.unmodifiableList(observations);
 }

 @Override
 public SourceType getSourceType() {
 return SourceType.PHYSICAL_SENSOR;
 }

 @Override
 public double getSourceConfidence() {
 if (customConfidence > 0) {
 return customConfidence;
 }
 if (observations.isEmpty()) {
 return getSourceType().getDefaultConfidence();
 }
 return observations.stream()
 .mapToDouble(PhysicalObservation::getConfidence)
 .average()
 .orElse(getSourceType().getDefaultConfidence());
 }

 public void setCustomConfidence(double confidence) {
 this.customConfidence = Math.max(0, Math.min(1, confidence));
 }

 @Override
 public List<Entity> toEntities() {
 List<Entity> entities = new ArrayList<>();
 for (PhysicalObservation obs : observations) {
 entities.add(convertToPhysicalEntity(obs));
 }
 return entities;
 }

 private Physical convertToPhysicalEntity(PhysicalObservation obs) {
 String entityName = generateEntityName(obs);
 EntityId entityId = EntityId.of("phys_" + System.currentTimeMillis() + "_" + obs.hashCode());
 Location location = obs.getLocation().orElse(new DefaultLocation());
 TimeInterval timeInterval = new ConstantTimeInterval(obs.getTimestamp());
 Set<com.adam.agri.planner.symbolic.ontology.upper.Property> properties = extractProperties(obs.getFeatures());
 String type = (String) obs.getFeatures().getOrDefault("type", "unknown");

 return new ObservedPhysicalEntity(
 entityId, properties, location, timeInterval, entityName, type, obs.getFeatures()
 );
 }

 private String generateEntityName(PhysicalObservation obs) {
 String source = (String) obs.getFeatures().getOrDefault("source", "sensor");
 String type = (String) obs.getFeatures().getOrDefault("type", "object");
 return type + "_from_" + source;
 }

 private Set<com.adam.agri.planner.symbolic.ontology.upper.Property> extractProperties(Map<String, Object> features) {
 Set<com.adam.agri.planner.symbolic.ontology.upper.Property> properties = new HashSet<>();
 features.forEach((key, value) -> {
 if (value instanceof Number) {
 properties.add(new NumericProperty(key, (Number) value));
 }
 });
 return properties;
 }

 @Override
 public void clear() {
 observations.clear();
 }

 private static class PerceptionEventWrapper implements PhysicalObservation {
 private final PerceptionEvent event;

 PerceptionEventWrapper(PerceptionEvent event) {
 this.event = event;
 }

 @Override
 public double getTimestamp() {
 return event.getTimestamp().toEpochMilli();
 }

 @Override
 public Optional<Location> getLocation() {
 return Optional.ofNullable(event.getSensorLocation());
 }

 @Override
 public Map<String, Object> getFeatures() {
 return event.getFeatures();
 }

 @Override
 public double getConfidence() {
 return event.getConfidence();
 }
 }

 private static class PhysicalStateWrapper implements PhysicalObservation {
 private final PhysicalState state;

 PhysicalStateWrapper(PhysicalState state) {
 this.state = state;
 }

 @Override
 public double getTimestamp() {
 return state.getTimestamp();
 }

 @Override
 public Optional<Location> getLocation() {
 double[] coords = state.getContinuousState();
 if (coords.length >= 3) {
 return Optional.of(new PointLocation(coords[0], coords[1], coords[2]));
 }
 return Optional.empty();
 }

 @Override
 public Map<String, Object> getFeatures() {
 Map<String, Object> features = new HashMap<>();
 features.putAll(state.getMeasurableProperties());
 features.put("timestamp", state.getTimestamp());
 double[] coords = state.getContinuousState();
 if (coords.length >= 3) {
 features.put("x", coords[0]);
 features.put("y", coords[1]);
 features.put("z", coords[2]);
 }
 return features;
 }

 @Override
 public double getConfidence() {
 return 1.0;
 }
 }

 private static class PointLocation implements Location {
 private final double x, y, z;

 PointLocation(double x, double y, double z) {
 this.x = x;
 this.y = y;
 this.z = z;
 }

 @Override
 public double[] getCoordinates() {
 return new double[]{x, y, z};
 }

 @Override
 public double distanceTo(Location other) {
 if (other instanceof PointLocation) {
 PointLocation o = (PointLocation) other;
 double dx = this.x - o.x;
 double dy = this.y - o.y;
 double dz = this.z - o.z;
 return Math.sqrt(dx*dx + dy*dy + dz*dz);
 }
 return Double.POSITIVE_INFINITY;
 }

 @Override
 public boolean overlaps(Location other) {
 return distanceTo(other) < 0.001;
 }

 @Override
 public String toString() {
 return String.format("(%.3f, %.3f, %.3f)", x, y, z);
 }
 }

 private static class DefaultLocation implements Location {
 @Override
 public double[] getCoordinates() {
 return new double[]{0, 0, 0};
 }

 @Override
 public double distanceTo(Location other) {
 return Double.POSITIVE_INFINITY;
 }

 @Override
 public boolean overlaps(Location other) {
 return false;
 }
 }

 public static class ObservedPhysicalEntity extends Physical {
 private final String name;
 private final String type;
 private final Map<String, Object> features;

 public ObservedPhysicalEntity(
 EntityId id,
 Set<com.adam.agri.planner.symbolic.ontology.upper.Property> properties,
 Location location,
 TimeInterval timeInterval,
 String name,
 String type,
 Map<String, Object> features) {
 super(id, properties, location, timeInterval);
 this.name = name;
 this.type = type;
 this.features = new HashMap<>(features);
 }

 public String getType() { return type; }

 public Map<String, Object> getFeatures() {
 return Collections.unmodifiableMap(features);
 }

 @Override
 public String getName() {
 return name;
 }

 @Override
 public boolean isCompatibleWith(Concept other) {
 return other instanceof ObservedPhysicalEntity;
 }

 @Override
 public boolean isCompatibleWith(Entity other) {
 return other instanceof ObservedPhysicalEntity;
 }
 }

 public static class NumericProperty implements com.adam.agri.planner.symbolic.ontology.upper.Property {
 private final String name;
 private final Number value;

 NumericProperty(String name, Number value) {
 this.name = name;
 this.value = value;
 }

 @Override
 public String getName() {
 return name + "=" + value;
 }

 @Override
 public boolean holdsFor(Entity entity) {
 return entity instanceof ObservedPhysicalEntity;
 }
 }

 private static class ConstantTimeInterval implements TimeInterval {
 private final double timestamp;

 ConstantTimeInterval(double timestamp) {
 this.timestamp = timestamp;
 }

 @Override
 public double getStart() {
 return timestamp;
 }

 @Override
 public double getEnd() {
 return timestamp;
 }

 @Override
 public double getDuration() {
 return 0;
 }

 @Override
 public boolean overlaps(TimeInterval other) {
 return other.getStart() <= timestamp && timestamp <= other.getEnd();
 }

 @Override
 public boolean contains(double t) {
 return t == timestamp;
 }
 }
}
