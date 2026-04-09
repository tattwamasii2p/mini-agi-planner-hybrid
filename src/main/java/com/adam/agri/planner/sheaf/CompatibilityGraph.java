package com.adam.agri.planner.sheaf;

import com.adam.agri.planner.core.state.StateId;
import com.adam.agri.planner.core.trajectory.Trajectory;

import java.util.*;

/**
 * Compatibility graph for efficient trajectory gluing.
 * Nodes = trajectories, Edges = compatible merge (a.end == b.start)
 */
public class CompatibilityGraph {
    // Map from state to trajectories that end there (parents)
    private final Map<StateId, Set<Trajectory>> endMap;

    // Map from state to trajectories that start there (children)
    private final Map<StateId, Set<Trajectory>> startMap;

    // All trajectories in graph
    private final Set<Trajectory> trajectories;

    // Adjacency list: trajectory -> compatible trajectories
    private final Map<Trajectory, Set<Trajectory>> adjacency;

    public CompatibilityGraph() {
        this.endMap = new HashMap<>();
        this.startMap = new HashMap<>();
        this.trajectories = new HashSet<>();
        this.adjacency = new HashMap<>();
    }

    /**
     * Add a trajectory to the graph.
     */
    public void addTrajectory(Trajectory t) {
        trajectories.add(t);
        adjacency.putIfAbsent(t, new HashSet<>());

        endMap.computeIfAbsent(t.end(), k -> new HashSet<>()).add(t);
        startMap.computeIfAbsent(t.start(), k -> new HashSet<>()).add(t);

        // Update adjacencies
        Set<Trajectory> compatible = startMap.get(t.end());
        if (compatible != null) {
            for (Trajectory other : compatible) {
                if (!other.equals(t)) {
                    adjacency.get(t).add(other);
                }
            }
        }

        // Update reverse adjacencies
        Set<Trajectory> predecessors = endMap.get(t.start());
        if (predecessors != null) {
            for (Trajectory other : predecessors) {
                adjacency.get(other).add(t);
            }
        }
    }

    /**
     * Get all trajectories compatible with given trajectory.
     * (Trajectories that can follow it, i.e., start where it ends)
     */
    public Set<Trajectory> getCompatible(Trajectory t) {
        return Collections.unmodifiableSet(adjacency.getOrDefault(t, Collections.emptySet()));
    }

    /**
     * Get predecessors (trajectories that end where t starts).
     */
    public Set<Trajectory> getPredecessors(Trajectory t) {
        if (endMap.containsKey(t.start())) {
            return Collections.unmodifiableSet(new HashSet<>(
                endMap.get(t.start())
            ));
        }
        return Collections.emptySet();
    }

    /**
     * Get successors (trajectories that start where t ends).
     */
    public Set<Trajectory> getSuccessors(Trajectory t) {
        if (startMap.containsKey(t.end())) {
            return Collections.unmodifiableSet(new HashSet<>(
                startMap.get(t.end())
            ));
        }
        return Collections.emptySet();
    }

    /**
     * Find a path through compatible trajectories from start to end.
     * Uses BFS for shortest path in terms of number of edges.
     *
     * @param start Trajectory to start from
     * @param goal Trajectory to reach
     * @return Path as list of trajectories, or null if no path
     */
    public List<Trajectory> findGluingPath(Trajectory start, Trajectory goal) {
        // BFS
        Queue<List<Trajectory>> queue = new LinkedList<>();
        Set<Trajectory> visited = new HashSet<>();

        queue.add(Collections.singletonList(start));
        visited.add(start);

        while (!queue.isEmpty()) {
            List<Trajectory> path = queue.poll();
            Trajectory current = path.get(path.size() - 1);

            if (current.equals(goal)) {
                return path;
            }

            for (Trajectory next : getCompatible(current)) {
                if (!visited.contains(next)) {
                    visited.add(next);
                    List<Trajectory> newPath = new ArrayList<>(path);
                    newPath.add(next);
                    queue.add(newPath);
                }
            }
        }

        return null; // No path found
    }

    /**
     * Find all possible gluing paths from start state to goal state.
     * Limited to avoid exponential blowup.
     *
     * @param start Start state
     * @param goal Goal state
     * @param maxPaths Maximum number of paths to return
     * @return List of paths
     */
    public List<List<Trajectory>> findAllPaths(StateId start, StateId goal, int maxPaths) {
        List<List<Trajectory>> paths = new ArrayList<>();

        // Find starting trajectories
        Set<Trajectory> starters = startMap.getOrDefault(start, Collections.emptySet());

        for (Trajectory t : starters) {
            dfsFindPaths(t, goal, new ArrayList<>(), paths, maxPaths, new HashSet<>());
        }

        return paths;
    }

    private void dfsFindPaths(Trajectory current, StateId goal, List<Trajectory> path,
                               List<List<Trajectory>> paths, int maxPaths, Set<Trajectory> visited) {
        if (paths.size() >= maxPaths) return;

        path.add(current);
        visited.add(current);

        if (current.end().equals(goal)) {
            paths.add(new ArrayList<>(path));
        } else {
            for (Trajectory next : getCompatible(current)) {
                if (!visited.contains(next)) {
                    dfsFindPaths(next, goal, path, paths, maxPaths, visited);
                }
            }
        }

        path.remove(path.size() - 1);
        visited.remove(current);
    }

    /**
     * Check if graph has a path from start state to goal state.
     */
    public boolean hasPath(StateId start, StateId goal) {
        Set<StateId> visited = new HashSet<>();
        Queue<StateId> queue = new LinkedList<>();
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            StateId current = queue.poll();
            if (current.equals(goal)) {
                return true;
            }

            Set<Trajectory> outgoing = startMap.getOrDefault(current, Collections.emptySet());
            for (Trajectory t : outgoing) {
                if (!visited.contains(t.end())) {
                    visited.add(t.end());
                    queue.add(t.end());
                }
            }
        }

        return false;
    }

    /**
     * Get all trajectories in graph.
     */
    public Set<Trajectory> getTrajectories() {
        return Collections.unmodifiableSet(trajectories);
    }

    /**
     * Get number of trajectories.
     */
    public int size() {
        return trajectories.size();
    }

    /**
     * Get number of edges (compatible pairs).
     */
    public int edgeCount() {
        return adjacency.values().stream().mapToInt(Set::size).sum() / 2;
    }

    /**
     * Clear the graph.
     */
    public void clear() {
        trajectories.clear();
        endMap.clear();
        startMap.clear();
        adjacency.clear();
    }

    /**
     * Find connected components in the graph.
     */
    public List<Set<Trajectory>> findConnectedComponents() {
        Set<Trajectory> unvisited = new HashSet<>(trajectories);
        List<Set<Trajectory>> components = new ArrayList<>();

        while (!unvisited.isEmpty()) {
            Trajectory start = unvisited.iterator().next();
            Set<Trajectory> component = new HashSet<>();
            Queue<Trajectory> queue = new LinkedList<>();

            queue.add(start);
            component.add(start);
            unvisited.remove(start);

            while (!queue.isEmpty()) {
                Trajectory t = queue.poll();

                for (Trajectory neighbor : getCompatible(t)) {
                    if (unvisited.contains(neighbor)) {
                        unvisited.remove(neighbor);
                        component.add(neighbor);
                        queue.add(neighbor);
                    }
                }

                for (Trajectory neighbor : getPredecessors(t)) {
                    if (unvisited.contains(neighbor)) {
                        unvisited.remove(neighbor);
                        component.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }

            components.add(component);
        }

        return components;
    }
}
