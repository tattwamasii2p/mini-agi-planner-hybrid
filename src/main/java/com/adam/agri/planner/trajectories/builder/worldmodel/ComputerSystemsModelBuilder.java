package com.adam.agri.planner.trajectories.builder.worldmodel;

import com.adam.agri.planner.symbolic.ontology.computer.*;
import com.adam.agri.planner.symbolic.ontology.upper.*;

import java.util.*;

public class ComputerSystemsModelBuilder {

 private final List<ComputerSystem> systems;
 private final List<NetworkLatency> connections;
 private final List<ResourceConstraint> resourceConstraints;

 public ComputerSystemsModelBuilder() {
 this.systems = new ArrayList<>();
 this.connections = new ArrayList<>();
 this.resourceConstraints = new ArrayList<>();
 }

 public ComputerSystemsModelBuilder addSystem(ComputerSystem system) {
 systems.add(system);
 return this;
 }

 public ComputerSystemsModelBuilder addSystem(
 EntityId id,
 String hostname,
 Location location,
 double computeCapacity,
 double memoryCapacity,
 boolean available) {
 ComputerSystem system = new ComputerSystem(
 id,
 Collections.emptySet(),
 location,
 new EternalTimeInterval(),
 hostname,
 computeCapacity,
 memoryCapacity,
 available
 );
 systems.add(system);
 return this;
 }

 public ComputerSystemsModelBuilder addConnection(NetworkLatency connection) {
 connections.add(connection);
 return this;
 }

 public ComputerSystemsModelBuilder addConnection(
 String name,
 Location from,
 Location to,
 double latencyMs,
 double bandwidthMbps) {
 NetworkLatency latency = new NetworkLatency(name, from, to, latencyMs, bandwidthMbps);
 connections.add(latency);
 return this;
 }

 public ComputerSystemsModelBuilder addResourceConstraint(ResourceConstraint constraint) {
 resourceConstraints.add(constraint);
 return this;
 }

 public ComputerSystemsModelBuilder parseNaturalLanguage(String description) {
 String lower = description.toLowerCase();

 if (lower.contains("server") || lower.contains("node")) {
 int maybeServerCount = (int) extractNumber(lower, "(\\d+)\\s+(server|node)");
 if (maybeServerCount <= 0) maybeServerCount = 1;

 for (int i = 0; i < maybeServerCount; i++) {
 double cores = extractNumber(lower, "(\\d+)\\s*(core|cpu)");
 double memory = extractMemoryGB(lower);
 String region = extractRegion(lower);

 if (cores <= 0) cores = 4;
 if (memory <= 0) memory = 16;

 Location loc = new DatacenterLocation(region);
 addSystem(
 EntityId.of("server_" + i),
 "server-" + i + "." + region,
 loc,
 cores * 10e9,
 memory * 1024 * 1024 * 1024,
 true
 );
 }
 }

 if (lower.contains("latency") || lower.contains("bandwidth")) {
 double latency = extractNumber(lower, "latency\\s*(?:of|is)?\\s*(\\d+)");
 double bandwidth = extractNumber(lower, "bandwidth\\s*(?:of|is)?\\s*(\\d+)");
 if (latency > 0 || bandwidth > 0) {
 Location loc1 = new DatacenterLocation("region1");
 Location loc2 = new DatacenterLocation("region2");
 addConnection("default", loc1, loc2,
 latency > 0 ? latency : 10,
 bandwidth > 0 ? bandwidth : 1000);
 }
 }

 return this;
 }

 public ComputerSystemsModelBuilder fromEntities(Collection<Entity> entities) {
 for (Entity entity : entities) {
 if (entity instanceof ComputerSystem) {
 systems.add((ComputerSystem) entity);
 }
 }
 return this;
 }

 public ComputerSystemsModel build() {
 return new ComputerSystemsModel(systems, connections, resourceConstraints);
 }

 private double extractNumber(String text, String pattern) {
 try {
 java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
 java.util.regex.Matcher m = p.matcher(text);
 if (m.find()) {
 return Double.parseDouble(m.group(1));
 }
 } catch (Exception e) {
 // Ignore
 }
 return 0;
 }

 private double extractMemoryGB(String text) {
 double mem = extractNumber(text, "(\\d+)\\s*GB");
 if (mem == 0) {
 mem = extractNumber(text, "(\\d+)\\s*TB") * 1024;
 }
 if (mem == 0) {
 mem = extractNumber(text, "(\\d+)\\s*MB") / 1024;
 }
 return mem;
 }

 private String extractRegion(String text) {
 String[] regions = {"us-east", "us-west", "eu-west", "eu-central", "ap-south", "ap-northeast"};
 String lower = text.toLowerCase();
 for (String region : regions) {
 if (lower.contains(region)) return region;
 }
 return "default-region";
 }

 private static class DatacenterLocation implements Location {
 private final String region;

 DatacenterLocation(String region) {
 this.region = region;
 }

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
 if (other instanceof DatacenterLocation) {
 return this.region.equals(((DatacenterLocation) other).region);
 }
 return false;
 }

 @Override
 public String toString() {
 return "dc:" + region;
 }
 }

 private static class EternalTimeInterval implements TimeInterval {
 @Override
 public double getStart() { return Double.NEGATIVE_INFINITY; }
 @Override
 public double getEnd() { return Double.POSITIVE_INFINITY; }
 @Override
 public double getDuration() { return Double.POSITIVE_INFINITY; }
 @Override
 public boolean overlaps(TimeInterval other) { return true; }
 @Override
 public boolean contains(double timestamp) { return true; }
 }

 public static class ComputerSystemsModel {
 private final List<ComputerSystem> systems;
 private final List<NetworkLatency> connections;
 private final List<ResourceConstraint> resourceConstraints;

 public ComputerSystemsModel(
 List<ComputerSystem> systems,
 List<NetworkLatency> connections,
 List<ResourceConstraint> resourceConstraints) {
 this.systems = Collections.unmodifiableList(new ArrayList<>(systems));
 this.connections = Collections.unmodifiableList(new ArrayList<>(connections));
 this.resourceConstraints = Collections.unmodifiableList(new ArrayList<>(resourceConstraints));
 }

 public List<ComputerSystem> getSystems() { return systems; }

 public List<NetworkLatency> getConnections() { return connections; }

 public List<ResourceConstraint> getResourceConstraints() { return resourceConstraints; }

 public double getTotalComputeCapacity() {
 return systems.stream()
 .mapToDouble(ComputerSystem::getComputeCapacity)
 .sum();
 }

 public double getTotalMemoryCapacity() {
 return systems.stream()
 .mapToDouble(ComputerSystem::getMemoryCapacity)
 .sum();
 }

 public List<ComputerSystem> getAvailableSystems() {
 return systems.stream()
 .filter(ComputerSystem::isAvailable)
 .toList();
 }
 }
}
