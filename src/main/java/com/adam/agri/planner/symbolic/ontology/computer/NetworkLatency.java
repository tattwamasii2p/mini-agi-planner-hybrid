package com.adam.agri.planner.symbolic.ontology.computer;

import java.util.Set;

import com.adam.agri.planner.symbolic.ontology.upper.Entity;
import com.adam.agri.planner.symbolic.ontology.upper.EntityId;
import com.adam.agri.planner.symbolic.ontology.upper.EntityImpl;
import com.adam.agri.planner.symbolic.ontology.upper.Location;
import com.adam.agri.planner.symbolic.ontology.upper.PhysicalProperty;

/**
 * Property representing network latency between computer systems.
 */
public class NetworkLatency extends EntityImpl implements PhysicalProperty {
 private final String name;
 private final Location from;
 private final Location to;
 private final double latencyMs;
 private final double bandwidthMbps;

 public NetworkLatency(String name, Location from, Location to, double latencyMs, double bandwidthMbps) {
 super(EntityId.of(name), Set.of());
 this.name = name;
 this.from = from;
 this.to = to;
 this.latencyMs = latencyMs;
 this.bandwidthMbps = bandwidthMbps;
 }

 @Override
 public String getName() {
 return name;
 }

 @Override
 public boolean holdsFor(Entity entity) {
 // Apply to ComputerSystem entities at matching locations
 if (entity instanceof ComputerSystem) {
 ComputerSystem cs = (ComputerSystem) entity;
 Location loc = cs.getLocation();
 return loc.equals(from) || loc.equals(to);
 }
 return false;
 }

 public double getLatencyMs() {
 return latencyMs;
 }

 public double getBandwidthMbps() {
 return bandwidthMbps;
 }

 public Location getFromLocation() {
 return from;
 }

 public Location getToLocation() {
 return to;
 }

 /**
 * Calculate transfer time for given data size in MB.
 */
 public double transferTimeMB(double megabytes) {
 return latencyMs + (megabytes * 8 / bandwidthMbps) * 1000;
 }
}
