package com.adam.agri.planner.trajectories.builder.worldmodel;

import com.adam.agri.planner.symbolic.ontology.computer.ComputerSystem;
import com.adam.agri.planner.symbolic.ontology.computer.NetworkLatency;
import com.adam.agri.planner.symbolic.ontology.upper.Entity;
import com.adam.agri.planner.symbolic.ontology.upper.Location;
import com.adam.agri.planner.symbolic.ontology.upper.Physical;
import com.adam.agri.planner.trajectories.builder.worldmodel.ComputerSystemsModelBuilder.ComputerSystemsModel;
import com.adam.agri.planner.trajectories.builder.worldmodel.PhysicalWorldModelBuilder.PhysicalWorldModel;

import java.util.*;

/**
 * Integrated world model combining computer systems and physical world.
 * Enables planning across both domains (e.g., deploying services on
 * servers based on physical location, network topology).
 *
 * Provides unified access to:
 * - Computer systems and network topology
 * - Physical entities and spatial relationships
 * - Cross-domain mappings (computer system locations → physical locations)
 */
public class IntegratedWorldModel {

    private final ComputerSystemsModel computerSystems;
    private final PhysicalWorldModel physicalWorld;
    private final Map<String, Object> metadata;
    private final Map<String, Entity> entityIndex;

    public IntegratedWorldModel(
            ComputerSystemsModel computerSystems,
            PhysicalWorldModel physicalWorld) {
        this.computerSystems = computerSystems != null ? computerSystems :
            new ComputerSystemsModel(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        this.physicalWorld = physicalWorld != null ? physicalWorld :
            new PhysicalWorldModel(Collections.emptyList(), Collections.emptyList(), Collections.emptyMap());
        this.metadata = new HashMap<>();
        this.entityIndex = new HashMap<>();
        buildEntityIndex();
    }

    /**
     * Build index of all entities for quick lookup.
     */
    private void buildEntityIndex() {
        // Index computer systems
        for (ComputerSystem system : computerSystems.getSystems()) {
            entityIndex.put(system.getId().toString(), (Entity) system);
            entityIndex.put(system.getHostname(), (Entity) system);
        }

        // Index physical entities
        for (Physical physical : physicalWorld.getPhysicalEntities()) {
            entityIndex.put(physical.getId().toString(), (Entity) physical);
            entityIndex.put(physical.getName(), (Entity) physical);
        }
    }

    /**
     * Look up entity by ID or name.
     */
    public Optional<Entity> getEntity(String idOrName) {
        return Optional.ofNullable(entityIndex.get(idOrName));
    }

    /**
     * Get computer systems model.
     */
    public ComputerSystemsModel getComputerSystems() {
        return computerSystems;
    }

    /**
     * Get physical world model.
     */
    public PhysicalWorldModel getPhysicalWorld() {
        return physicalWorld;
    }

    /**
     * Get all entities in the integrated model.
     */
    public List<Entity> getAllEntities() {
        List<Entity> all = new ArrayList<>();
        all.addAll(computerSystems.getSystems());
        all.addAll(physicalWorld.getPhysicalEntities());
        return all;
    }

    /**
     * Find computer systems near a physical location.
     */
    public List<ComputerSystem> findSystemsNear(Location location, double maxDistance) {
        List<ComputerSystem> result = new ArrayList<>();
        for (ComputerSystem system : computerSystems.getSystems()) {
            double dist = estimateDistance(location, system.getLocation());
            if (dist <= maxDistance) {
                result.add(system);
            }
        }
        return result;
    }

    /**
     * Find physical entities near a computer system.
     */
    public List<Physical> findPhysicalNear(ComputerSystem system, double maxDistance) {
        return physicalWorld.getPhysicalEntities().stream()
            .filter(p -> estimateDistance(p.getLocation(), system.getLocation()) <= maxDistance)
            .toList();
    }

    /**
     * Get network latency between two systems.
     */
    public Optional<Double> getLatencyBetween(ComputerSystem from, ComputerSystem to) {
        for (NetworkLatency conn : computerSystems.getConnections()) {
            if (conn.holdsFor(from) && conn.holdsFor(to)) {
                return Optional.of(conn.getLatencyMs());
            }
        }
        return Optional.empty();
    }

    /**
     * Get bandwidth between two systems.
     */
    public Optional<Double> getBandwidthBetween(ComputerSystem from, ComputerSystem to) {
        for (NetworkLatency conn : computerSystems.getConnections()) {
            if (conn.holdsFor(from) && conn.holdsFor(to)) {
                return Optional.of(conn.getBandwidthMbps());
            }
        }
        return Optional.empty();
    }

    /**
     * Estimate transfer time for given data size.
     */
    public Optional<Double> estimateTransferTime(ComputerSystem from, ComputerSystem to, double megabytes) {
        return getLatencyBetween(from, to)
            .map(latency -> latency + (megabytes * 8 / getBandwidthBetween(from, to).orElse(1000.0)) * 1000);
    }

    /**
     * Find least loaded system that can execute a task.
     */
    public Optional<ComputerSystem> findLeastLoadedForTask(
            com.adam.agri.planner.symbolic.ontology.computer.ExternalComputerAction task) {
        return computerSystems.getAvailableSystems().stream()
            .filter(s -> s.canExecute(task))
            .min(Comparator.comparingDouble(this::estimateSystemLoad));
    }

    /**
     * Estimate load of a system (lower is better).
     */
    private double estimateSystemLoad(ComputerSystem system) {
        // Simple load estimation based on available capacity vs total
        // In a real implementation, this would use live metrics
        return 0.5; // placeholder
    }

    /**
     * Check if a computer system is co-located with a physical entity.
     */
    public boolean isCoLocated(ComputerSystem system, Physical physical) {
        return system.getLocation().overlaps(physical.getLocation());
    }

    /**
     * Get physical entities near network connections.
     */
    public Map<NetworkLatency, List<Physical>> getPhysicalNearConnections() {
        Map<NetworkLatency, List<Physical>> map = new HashMap<>();
        for (NetworkLatency conn : computerSystems.getConnections()) {
            List<Physical> near = physicalWorld.getPhysicalEntities().stream()
                .filter(p -> p.getLocation().overlaps(conn.getToLocation()) ||
                            p.getLocation().overlaps(conn.getFromLocation()))
                .toList();
            if (!near.isEmpty()) {
                map.put(conn, near);
            }
        }
        return map;
    }

    /**
     * Compute a trajectory from current to goal state.
     * Returns trajectory across both computer and physical domains.
     */
    public IntegratedTrajectory computeTrajectory(
            Object currentState,
            Object goalState,
            TrajectoryPreferences preferences) {

        List<TrajectorySegment> segments = new ArrayList<>();

        // 1. Physical movement segment (if system locations differ)
        if (currentState instanceof ComputerSystem && goalState instanceof ComputerSystem) {
            ComputerSystem current = (ComputerSystem) currentState;
            ComputerSystem goal = (ComputerSystem) goalState;

            if (!current.getLocation().overlaps(goal.getLocation())) {
                // Add physical movement to reach goal location
                segments.add(new TrajectorySegment(
                    SegmentType.PHYSICAL_MOVE,
                    "Move to " + goal.getLocation(),
                    estimateDistance(current.getLocation(), goal.getLocation()),
                    preferences.getPhysicalMoveCost()
                ));
            }
        }

        // 2. Network action segment
        if (currentState instanceof ComputerSystem && goalState instanceof ComputerSystem) {
            ComputerSystem current = (ComputerSystem) currentState;
            ComputerSystem goal = (ComputerSystem) goalState;

            Optional<Double> latency = getLatencyBetween(current, goal);
            segments.add(new TrajectorySegment(
                SegmentType.NETWORK_ACTION,
                "Network action " + current.getHostname() + " -> " + goal.getHostname(),
                latency.orElse(100.0),
                preferences.getNetworkLatencyCost()
            ));
        }

        return new IntegratedTrajectory(segments);
    }

    /**
     * Estimate distance between two locations.
     */
    private double estimateDistance(Location a, Location b) {
        // Simple implementation - would use proper distance metric
        if (a.overlaps(b)) return 0;
        return 100.0; // placeholder for non-overlapping locations
    }

    /**
     * Set metadata property.
     */
    public void setMetadata(String key, Object value) {
        metadata.put(key, value);
    }

    /**
     * Get metadata property.
     */
    public Optional<Object> getMetadata(String key) {
        return Optional.ofNullable(metadata.get(key));
    }

    @Override
    public String toString() {
        return "IntegratedWorldModel{" +
               "systems=" + computerSystems.getSystems().size() +
               ", physical=" + physicalWorld.getPhysicalEntities().size() +
               "}";
    }

    /**
     * Trajectory segment types.
     */
    public enum SegmentType {
        PHYSICAL_MOVE,  // Move from one location to another
        NETWORK_ACTION, // Network communication
        COMPUTATION,    // Compute on a system
        OBSERVATION,    // Gather information
        INTERACTION     // Interact with physical entity
    }

    /**
     * Single segment of a trajectory.
     */
    public static class TrajectorySegment {
        private final SegmentType type;
        private final String description;
        private final double estimatedTime;
        private final double cost;

        public TrajectorySegment(SegmentType type, String description, double estimatedTime, double cost) {
            this.type = type;
            this.description = description;
            this.estimatedTime = estimatedTime;
            this.cost = cost;
        }

        public SegmentType getType() { return type; }
        public String getDescription() { return description; }
        public double getEstimatedTime() { return estimatedTime; }
        public double getCost() { return cost; }

        @Override
        public String toString() {
            return type + ": " + description + " (" + estimatedTime + "ms, cost=" + cost + ")";
        }
    }

    /**
     * Trajectory across integrated computer and physical world.
     */
    public static class IntegratedTrajectory {
        private final List<TrajectorySegment> segments;

        public IntegratedTrajectory(List<TrajectorySegment> segments) {
            this.segments = new ArrayList<>(segments);
        }

        public List<TrajectorySegment> getSegments() {
            return Collections.unmodifiableList(segments);
        }

        public double getTotalTime() {
            return segments.stream().mapToDouble(TrajectorySegment::getEstimatedTime).sum();
        }

        public double getTotalCost() {
            return segments.stream().mapToDouble(TrajectorySegment::getCost).sum();
        }

        @Override
        public String toString() {
            return "IntegratedTrajectory{" + segments.size() + " segments, " +
                   "time=" + getTotalTime() + "ms, cost=" + getTotalCost() + "}";
        }
    }

    /**
     * Preferences for trajectory computation.
     */
    public static class TrajectoryPreferences {
        private double physicalMoveCost = 1.0;
        private double networkLatencyCost = 0.5;
        private double computationCost = 0.1;

        public double getPhysicalMoveCost() { return physicalMoveCost; }
        public void setPhysicalMoveCost(double cost) { this.physicalMoveCost = cost; }

        public double getNetworkLatencyCost() { return networkLatencyCost; }
        public void setNetworkLatencyCost(double cost) { this.networkLatencyCost = cost; }

        public double getComputationCost() { return computationCost; }
        public void setComputationCost(double cost) { this.computationCost = cost; }
    }
}
