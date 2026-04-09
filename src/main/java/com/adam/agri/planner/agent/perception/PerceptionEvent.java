package com.adam.agri.planner.agent.perception;

import com.adam.agri.planner.symbolic.ontology.upper.EntityId;
import com.adam.agri.planner.symbolic.ontology.upper.Location;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Perception event from sensors.
 * Represents raw or processed sensory data with spatiotemporal binding.
 */
public class PerceptionEvent {

    private final EntityId source;
    private final Instant timestamp;
    private final Location sensorLocation;
    private final PerceptionType type;
    private final Map<String, Object> features;
    private final double confidence;
    private final double salience;
    private final Object rawData;

    public PerceptionEvent(EntityId source, Instant timestamp, Location sensorLocation,
                          PerceptionType type, Map<String, Object> features,
                          double confidence, double salience, Object rawData) {
        this.source = source;
        this.timestamp = timestamp;
        this.sensorLocation = sensorLocation;
        this.type = type;
        this.features = new HashMap<>(features);
        this.confidence = confidence;
        this.salience = salience;
        this.rawData = rawData;
    }

    public EntityId getSource() { return source; }
    public Instant getTimestamp() { return timestamp; }
    public Location getSensorLocation() { return sensorLocation; }
    public PerceptionType getType() { return type; }
    public Map<String, Object> getFeatures() { return new HashMap<>(features); }
    public double getConfidence() { return confidence; }
    public double getSalience() { return salience; }
    public Optional<Object> getRawData() { return Optional.ofNullable(rawData); }

    /**
     * Get typed feature.
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getFeature(String key, Class<T> type) {
        Object value = features.get(key);
        if (value == null) return Optional.empty();
        if (type.isInstance(value)) {
            return Optional.of((T) value);
        }
        return Optional.empty();
    }

    /**
     * Compute temporal distance to another event.
     */
    public long millisecondsTo(PerceptionEvent other) {
        return Math.abs(timestamp.toEpochMilli() - other.timestamp.toEpochMilli());
    }

    /**
     * Check if this event is newer than another.
     */
    public boolean isNewerThan(PerceptionEvent other) {
        return timestamp.isAfter(other.timestamp);
    }

    /**
     * Create simplified event with reduced features.
     */
    public PerceptionEvent simplify() {
        return new PerceptionEvent(
            source, timestamp, sensorLocation, type,
            Map.of("type", type, "confidence", confidence),
            confidence, salience, null
        );
    }

    @Override
    public String toString() {
        return String.format("Perception[%s@%s conf=%.2f sal=%.2f]",
            type, timestamp, confidence, salience);
    }

    /**
     * Builder for perception events.
     */
    /**
     * Typed access to structured data payload.
     * Use this instead of getFeature() for type-safe access to complex data.
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getData(Class<T> dataType) {
        Object data = features.get("_data");
        if (data != null && dataType.isInstance(data)) {
            return Optional.of((T) data);
        }
        return Optional.empty();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private EntityId source;
        private Instant timestamp = Instant.now();
        private Location sensorLocation;
        private PerceptionType type;
        private Map<String, Object> features = new HashMap<>();
        private double confidence = 1.0;
        private double salience = 0.5;
        private Object rawData;

        public Builder source(EntityId s) { this.source = s; return this; }
        public Builder timestamp(Instant t) { this.timestamp = t; return this; }
        public Builder location(Location l) { this.sensorLocation = l; return this; }
        public Builder type(PerceptionType t) { this.type = t; return this; }
        public Builder feature(String k, Object v) { this.features.put(k, v); return this; }
        public Builder features(Map<String, Object> f) { this.features.putAll(f); return this; }
        public Builder confidence(double c) { this.confidence = c; return this; }
        public Builder salience(double s) { this.salience = s; return this; }
        public Builder rawData(Object d) { this.rawData = d; return this; }

        /**
         * Attach structured data object.
         * Preferred over individual features for type-safe data access.
         */
        public Builder data(Object structuredData) {
            this.features.put("_data", structuredData);
            return this;
        }

        public PerceptionEvent build() {
            return new PerceptionEvent(source, timestamp, sensorLocation, type,
                features, confidence, salience, rawData);
        }
    }

    public enum PerceptionType {
        VISUAL,
        AUDITORY,
        TACTILE,
        PROPRIOCEPTIVE,
        TEMPORAL,
        SOCIAL,
        INTERNAL
    }
}
