package com.adam.agri.planner.core.trajectory;

import com.adam.agri.planner.core.action.Action;
import com.adam.agri.planner.core.state.StateId;

import java.util.*;

/**
 * Trajectory represents a sequence of actions connecting two states.
 * In sheaf theory terms: a section over an open set.
 */
public final class Trajectory {
    private final StateId start;
    private final StateId end;
    private final List<Action> actions;
    private final TrajectoryMetrics metrics;

    public Trajectory(StateId start, StateId end, List<Action> actions, TrajectoryMetrics metrics) {
        this.start = start;
        this.end = end;
        this.actions = new ArrayList<>(actions);
        this.metrics = metrics;
    }

    public Trajectory(StateId start, StateId end) {
        this(start, end, new ArrayList<>(), TrajectoryMetrics.zero());
    }

    /**
     * Start state of trajectory.
     */
    public StateId start() {
        return start;
    }

    /**
     * End state of trajectory.
     */
    public StateId end() {
        return end;
    }

    /**
     * Get cumulative cost.
     */
    public double cost() {
        return metrics.getCumulativeCost();
    }

    /**
     * Get total time.
     */
    public double time() {
        return metrics.getCumulativeTime();
    }

    /**
     * Get joint success probability (product of individual probabilities).
     */
    public double probability() {
        return metrics.getJointProbability();
    }

    /**
     * Get estimated risk.
     */
    public double risk() {
        return metrics.getEstimatedRisk();
    }

    public List<Action> getActions() {
        return Collections.unmodifiableList(actions);
    }

    public TrajectoryMetrics getMetrics() {
        return metrics;
    }

    /**
     * Check if this trajectory can merge with another.
     * True if this.end == other.start.
     */
    public boolean canMergeWith(Trajectory other) {
        return this.end.equals(other.start);
    }

    /**
     * Get intersection state (for sheaf compatibility).
     */
    public Optional<StateId> getIntersection(Trajectory other) {
        if (this.end.equals(other.start)) {
            return Optional.of(this.end);
        }
        if (this.start.equals(other.end)) {
            return Optional.of(this.start);
        }
        return Optional.empty();
    }

    /**
     * Glues two trajectories if they are compatible.
     * Returns null if incompatible (sheaf condition violation).
     *
     * Mathematical correspondence: Čech gluing
     *
     * @param a First trajectory
     * @param b Second trajectory (must start where a ends)
     * @return Combined trajectory or null if incompatible
     */
    public static Trajectory merge(Trajectory a, Trajectory b) {
        Optional<Trajectory> result = tryMerge(a, b);
        return result.orElse(null);
    }

    /**
     * Try to merge two trajectories, returning Optional instead of null.
     */
    public static Optional<Trajectory> tryMerge(Trajectory a, Trajectory b) {
        return tryMerge(a, b, MergeStrategy.SEQUENTIAL);
    }

    /**
     * Try to merge with a specific strategy.
     */
    public static Optional<Trajectory> tryMerge(Trajectory a, Trajectory b, MergeStrategy strategy) {
        if (!a.end.equals(b.start)) {
            return Optional.empty();
        }

        List<Action> combined = new ArrayList<>(a.actions);
        combined.addAll(b.actions);
        TrajectoryMetrics mergedMetrics = strategy.combine(a.metrics, b.metrics);

        return Optional.of(new Trajectory(a.start, b.end, combined, mergedMetrics));
    }

    /**
     * N-way gluing with constraint satisfaction.
     * Attempts to glue multiple trajectory fragments into one.
     *
     * Tries to find an ordering where each consecutive pair is compatible.
     */
    public static Optional<Trajectory> mergeMultiple(List<Trajectory> fragments) {
        return mergeMultiple(fragments, Collections.emptySet());
    }

    /**
     * N-way gluing with constraint checking.
     */
    public static Optional<Trajectory> mergeMultiple(
            List<Trajectory> fragments,
            Set<com.adam.agri.planner.core.constraints.Constraint> constraints) {

        if (fragments.isEmpty()) {
            return Optional.empty();
        }
        if (fragments.size() == 1) {
            return Optional.of(fragments.get(0));
        }

        // Find a valid ordering using greedy approach
        List<Trajectory> ordered = findOrdering(fragments);
        if (ordered == null) {
            return Optional.empty();
        }

        // Sequentially merge
        Trajectory result = ordered.get(0);
        for (int i = 1; i < ordered.size(); i++) {
            Trajectory next = ordered.get(i);
            Optional<Trajectory> merged = tryMerge(result, next);
            if (merged.isEmpty()) {
                return Optional.empty();
            }
            result = merged.get();
        }

        // Check constraints
        for (com.adam.agri.planner.core.constraints.Constraint c : constraints) {
            if (!c.isSatisfiedBy(result)) {
                return Optional.empty();
            }
        }

        return Optional.of(result);
    }

    /**
     * Find ordering of fragments where each connects to the next.
     */
    private static List<Trajectory> findOrdering(List<Trajectory> fragments) {
        // Simple greedy: find path through compatibility graph
        Map<StateId, Trajectory> startMap = new HashMap<>();
        Map<StateId, Trajectory> endMap = new HashMap<>();

        for (Trajectory t : fragments) {
            startMap.put(t.start, t);
            endMap.put(t.end, t);
        }

        // Find starting point (not an end of any fragment)
        Trajectory start = fragments.stream()
            .filter(t -> !startMap.containsKey(t.end))
            .findFirst()
            .orElse(fragments.get(0));

        List<Trajectory> ordered = new ArrayList<>();
        Trajectory current = start;
        ordered.add(current);

        while (startMap.containsKey(current.end)) {
            current = startMap.get(current.end);
            if (ordered.contains(current)) {
                // Cycle detected
                return null;
            }
            ordered.add(current);
        }

        if (ordered.size() != fragments.size()) {
            // Not all fragments connected
            return null;
        }

        return ordered;
    }

    /**
     * Check if trajectory satisfies sheaf condition (compatibility on overlaps).
     */
    public boolean satisfiesSheafCondition(Trajectory other) {
        // Two trajectories are compatible if their ends match
        return this.end.equals(other.start) || this.start.equals(other.end);
    }

    /**
     * Restrict trajectory to a subpath.
     */
    public Trajectory restrict(StateId from, StateId to) {
        // Find index of 'from' in path
        // Simplified: just check endpoints
        if (!this.start.equals(from) && !this.end.equals(to)) {
            throw new IllegalArgumentException("Cannot restrict: endpoints not in trajectory");
        }
        return new Trajectory(from, to, actions, metrics);
    }

    /**
     * Check if physically valid (for bridge layer).
     */
    public boolean isPhysicallyValid(com.adam.agri.planner.physical.worldmodel.WorldModel world) {
        // Simplified check
        return true;
    }

    @Override
    public String toString() {
        return "Trajectory{" + "start=" + start + ", end=" + end +
               ", actions=" + actions.size() + ", cost=" + cost() + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Trajectory that = (Trajectory) o;
        return start.equals(that.start) && end.equals(that.end) &&
               actions.equals(that.actions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end, actions);
    }
}
