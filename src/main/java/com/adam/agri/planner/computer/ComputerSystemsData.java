package com.adam.agri.planner.computer;

import com.adam.agri.planner.core.state.State;
import com.adam.agri.planner.core.state.StateId;
import com.adam.agri.planner.core.state.StateType;

import java.util.Objects;

/**
 * Immutable implementation of AbstractComputerSystemsData.
 * Provides validated metrics for computer system agents.
 * Also implements State interface for use in ExecutionContext.
 */
public final class ComputerSystemsData implements AbstractComputerSystemsData, State {

 private final double cpuLoad;
 private final double memoryUsage;
 private final double bandwidth;
 private final int queueDepth;
 private final double availableCompute;
 private final double availableMemory;

 public ComputerSystemsData(double cpuLoad, double memoryUsage, double bandwidth,
 int queueDepth, double availableCompute, double availableMemory) {
 this.cpuLoad = clamp(cpuLoad, 0.0, 1.0);
 this.memoryUsage = clamp(memoryUsage, 0.0, 1.0);
 this.bandwidth = Math.max(0.0, bandwidth);
 this.queueDepth = Math.max(0, queueDepth);
 this.availableCompute = Math.max(0.0, availableCompute);
 this.availableMemory = Math.max(0.0, availableMemory);
 }

 @Override
 public double getCpuLoad() { return cpuLoad; }

 @Override
 public double getMemoryUsage() { return memoryUsage; }

 @Override
 public double getBandwidth() { return bandwidth; }

 @Override
 public int getQueueDepth() { return queueDepth; }

 @Override
 public double getAvailableCompute() { return availableCompute; }

 @Override
 public double getAvailableMemory() { return availableMemory; }

 // State interface implementation
 @Override
 public StateId getId() { return StateId.generate(); }

 @Override
 public StateType getType() { return StateType.PHYSICAL; }

 @Override
 public boolean isCompatible(State other) {
 return other.getType() == StateType.PHYSICAL;
 }

 @Override
 public State copy() {
 return new ComputerSystemsData(cpuLoad, memoryUsage, bandwidth,
 queueDepth, availableCompute, availableMemory);
 }

 /**
 * Zero state (no load, no resources).
 */
 public static ComputerSystemsData zero() {
 return new ComputerSystemsData(0.0, 0.0, 0.0, 0, 0.0, 0.0);
 }

 /**
 * Factory from raw values.
 */
 public static ComputerSystemsData of(double cpu, double memory, double bandwidth,
 int queue, double availCpu, double availMem) {
 return new ComputerSystemsData(cpu, memory, bandwidth, queue, availCpu, availMem);
 }

 /**
 * Create builder.
 */
 public static Builder builder() {
 return new Builder();
 }

 /**
 * Builder for ComputerSystemsData.
 */
 public static class Builder {
 private double cpuLoad;
 private double memoryUsage;
 private double bandwidth;
 private int queueDepth;
 private double availableCompute;
 private double availableMemory;

 public Builder cpuLoad(double v) { this.cpuLoad = v; return this; }

 public Builder memoryUsage(double v) { this.memoryUsage = v; return this; }

 public Builder bandwidth(double v) { this.bandwidth = v; return this; }

 public Builder queueDepth(int v) { this.queueDepth = v; return this; }

 public Builder availableCompute(double v) { this.availableCompute = v; return this; }

 public Builder availableMemory(double v) { this.availableMemory = v; return this; }

 public ComputerSystemsData build() {
 return new ComputerSystemsData(cpuLoad, memoryUsage, bandwidth,
 queueDepth, availableCompute, availableMemory);
 }
 }

 @Override
 public boolean equals(Object o) {
 if (this == o) return true;
 if (!(o instanceof ComputerSystemsData)) return false;
 ComputerSystemsData that = (ComputerSystemsData) o;
 return Double.compare(cpuLoad, that.cpuLoad) == 0 &&
 Double.compare(memoryUsage, that.memoryUsage) == 0 &&
 queueDepth == that.queueDepth;
 }

 @Override
 public int hashCode() {
 return Objects.hash(cpuLoad, memoryUsage, queueDepth);
 }

 @Override
 public String toString() {
 return String.format("ComputerSystemsData[cpu=%.2f, mem=%.2f, bw=%.1f, q=%d]",
 cpuLoad, memoryUsage, bandwidth, queueDepth);
 }

 private static double clamp(double v, double min, double max) {
 return Math.max(min, Math.min(max, v));
 }
}
