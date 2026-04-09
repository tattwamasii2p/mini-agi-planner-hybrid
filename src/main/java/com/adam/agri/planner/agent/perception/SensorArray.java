package com.adam.agri.planner.agent.perception;

import com.adam.agri.planner.symbolic.ontology.upper.Physical;

import java.time.Instant;
import java.util.*;

/**
 * Sensor array combining multiple sensors.
 * Fuses multi-modal perception into unified events.
 */
public class SensorArray implements Perception {

    private final List<Sensor> sensors;
    private final Map<String, PerceptionFilter> filters;
    private final PerceptionFusion fusion;

    // Calibration
    private final Map<Sensor, Double> confidenceWeights;

    public SensorArray() {
        this.sensors = new ArrayList<>();
        this.filters = new HashMap<>();
        this.fusion = new PerceptionFusion();
        this.confidenceWeights = new HashMap<>();
    }

    public SensorArray withSensor(Sensor sensor) {
        sensors.add(sensor);
        confidenceWeights.put(sensor, 1.0);
        return this;
    }

    public SensorArray withFilter(String name, PerceptionFilter filter) {
        filters.put(name, filter);
        return this;
    }

    @Override
    public PerceptionEvent perceive() {
        // Collect from all sensors
        List<PerceptionEvent> events = new ArrayList<>();

        for (Sensor sensor : sensors) {
            if (sensor.isAvailable()) {
                PerceptionEvent event = sensor.read();
                events.add(event);
            }
        }

        // Apply filters
        for (PerceptionFilter filter : filters.values()) {
            events = filter.filter(events);
        }

        // Fuse multi-modal
        return fusion.fuse(events);
    }

    @Override
    public List<PerceptionEvent> perceiveMultiModal() {
        List<PerceptionEvent> results = new ArrayList<>();

        for (Sensor sensor : sensors) {
            if (sensor.isAvailable()) {
                results.add(sensor.read());
            }
        }

        return results;
    }

    @Override
    public double getConfidence() {
        // Aggregate confidence across sensors
        if (sensors.isEmpty()) return 0.0;

        double weightedSum = 0.0;
        double totalWeight = 0.0;

        for (Sensor sensor : sensors) {
            if (sensor.isAvailable()) {
                double w = confidenceWeights.getOrDefault(sensor, 1.0);
                weightedSum += sensor.getConfidence() * w;
                totalWeight += w;
            }
        }

        return totalWeight > 0 ? weightedSum / totalWeight : 0.0;
    }

    @Override
    public boolean isAvailable() {
        return sensors.stream().anyMatch(Sensor::isAvailable);
    }

    @Override
    public Optional<Physical> getPerceivedEntity() {
        PerceptionEvent event = perceive();
        return event.getFeatures().containsKey("entity")
            ? Optional.of((Physical) event.getFeatures().get("entity"))
            : Optional.empty();
    }

    @Override
    public void updateWithAction(com.adam.agri.planner.core.action.Action action) {
        // Active perception: sensors configured based on action
        for (Sensor sensor : sensors) {
            sensor.configureForAction(action);
        }
    }

    /**
     * Sensor interface for array elements.
     */
    public interface Sensor {
        String getSensorType();
        double getSamplingRate();
        boolean isAvailable();
        PerceptionEvent read();
        double getConfidence();
        void configureForAction(com.adam.agri.planner.core.action.Action action);
    }

    /**
     * Perception filter interface.
     */
    public interface PerceptionFilter {
        List<PerceptionEvent> filter(List<PerceptionEvent> events);
    }

    /**
     * Multi-sensor fusion.
     */
    public static class PerceptionFusion {
        public PerceptionEvent fuse(List<PerceptionEvent> events) {
            if (events.isEmpty()) {
                return null;
            }
            if (events.size() == 1) {
                return events.get(0);
            }

            // Weighted average fusion
            Map<String, Object> fusedFeatures = new HashMap<>();
            double totalConfidence = 0.0;
            double weightedSalience = 0.0;

            for (PerceptionEvent e : events) {
                fusedFeatures.putAll(e.getFeatures());
                totalConfidence += e.getConfidence();
                weightedSalience += e.getSalience() * e.getConfidence();
            }

            double avgConfidence = totalConfidence / events.size();
            double avgSalience = weightedSalience / totalConfidence;

            return PerceptionEvent.builder()
                .source(events.get(0).getSource())
                .timestamp(Instant.now())
                .type(fuseType(events))
                .features(fusedFeatures)
                .confidence(avgConfidence)
                .salience(avgSalience)
                .build();
        }

        private PerceptionEvent.PerceptionType fuseType(List<PerceptionEvent> events) {
            // Return most confident type
            return events.stream()
                .max(Comparator.comparingDouble(PerceptionEvent::getConfidence))
                .map(PerceptionEvent::getType)
                .orElse(PerceptionEvent.PerceptionType.INTERNAL);
        }
    }
}
