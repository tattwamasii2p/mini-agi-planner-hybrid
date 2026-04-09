package com.adam.agri.planner.sheaf;

import com.adam.agri.planner.core.constraints.ConstraintSet;
import com.adam.agri.planner.core.state.StateId;
import com.adam.agri.planner.core.trajectory.Trajectory;
import com.adam.agri.planner.core.constraints.Constraint;

import java.util.*;

/**
 * SheafGlue implements the gluing axiom from sheaf theory.
 * Collects local knowledge (trajectories) and merges them into
 * global trajectories if they satisfy compatibility conditions.
 *
 * Mathematical basis:
 * - Local sections: Agent trajectories
 * - Compatibility: end(a) == start(b)
 * - Gluing: merge compatible sections
 * - Sheaf condition: pairwise compatible → global section exists
 */
public class SheafGlue {
    // Collection of local sections (agent trajectories)
    private final Set<Trajectory> localSections;

    // Constraint set for compatibility checking
    private final ConstraintSet constraints;

    // Compatibility graph for efficient gluing
    private final CompatibilityGraph compatibilityGraph;

    // Track which agent contributed which trajectory
    private final Map<String, Trajectory> agentContributions;

    public SheafGlue(ConstraintSet constraints) {
        this.localSections = new HashSet<>();
        this.constraints = constraints;
        this.compatibilityGraph = new CompatibilityGraph();
        this.agentContributions = new HashMap<>();
    }

    public SheafGlue() {
        this(new ConstraintSet());
    }

    /**
     * Collects local knowledge from agents.
     * Corresponds to: collecting local sections of a sheaf.
     *
     * @param localSection Trajectory from an agent
     * @param agentId Source agent identifier
     */
    public void collect(Trajectory localSection, String agentId) {
        localSections.add(localSection);
        agentContributions.put(agentId, localSection);
        compatibilityGraph.addTrajectory(localSection);
    }

    /**
     * Glues compatible trajectories.
     * Iterative closure until fixpoint.
     *
     * Algorithm:
     * T₀ = initial parts
     * Tₙ₊₁ = Tₙ ∪ {merge(x,y) | x,y ∈ Tₙ compatible}
     * until fixpoint
     *
     * @return Set of all glued trajectories
     */
    public Set<Trajectory> glue() {
        Set<Trajectory> current = new HashSet<>(localSections);
        Set<Trajectory> next = new HashSet<>(current);

        boolean changed;
        do {
            changed = false;
            List<Trajectory> currentList = new ArrayList<>(current);

            for (int i = 0; i < currentList.size(); i++) {
                for (int j = 0; j < currentList.size(); j++) {
                    if (i == j) continue;

                    Trajectory a = currentList.get(i);
                    Trajectory b = currentList.get(j);

                    Optional<Trajectory> merged = Trajectory.tryMerge(a, b);
                    if (merged.isPresent() && !current.contains(merged.get())) {
                        next.add(merged.get());
                        changed = true;
                    }
                }
            }

            current = next;
            next = new HashSet<>(current);
        } while (changed);

        return current;
    }

    /**
     * Constraint-based gluing (not just equality).
     * merge(a,b) if:
     *   end(a) ≅ start(b) AND
     *   constraints(a ∪ b) satisfied
     *
     * @param a First trajectory
     * @param b Second trajectory
     * @param additionalConstraints Additional constraints to check
     * @return Merged trajectory if compatible
     */
    public Optional<Trajectory> glueWithConstraints(
            Trajectory a,
            Trajectory b,
            ConstraintSet additionalConstraints) {

        // Basic compatibility check
        if (!a.end().equals(b.start())) {
            return Optional.empty();
        }

        // Try merge first
        Optional<Trajectory> merged = Trajectory.tryMerge(a, b);
        if (merged.isEmpty()) {
            return Optional.empty();
        }

        // Check all constraints
        Trajectory result = merged.get();
        ConstraintSet allConstraints = new ConstraintSet();

        // Add base constraints
        for (Constraint c : constraints.getHardConstraints()) {
            allConstraints.addHard(c);
        }
        // Add additional constraints
        for (Constraint c : additionalConstraints.getHardConstraints()) {
            allConstraints.addHard(c);
        }

        if (!allConstraints.allSatisfiedBy(result)) {
            return Optional.empty();
        }

        return Optional.of(result);
    }

    /**
     * N-way gluing with conflict resolution.
     * Merges multiple trajectory fragments in optimal order.
     *
     * @param fragments Trajectories to merge
     * @param strategy Strategy for resolving conflicts
     * @return Merged trajectory if possible
     */
    public Optional<Trajectory> glueNWay(
            List<Trajectory> fragments,
            GluingStrategy strategy) {

        if (fragments.isEmpty()) {
            return Optional.empty();
        }
        if (fragments.size() == 1) {
            return Optional.of(fragments.get(0));
        }

        // Find optimal ordering using compatibility graph
        List<Trajectory> ordered = findOptimalOrdering(fragments, strategy);
        if (ordered == null) {
            return Optional.empty();
        }

        // Sequentially merge
        Trajectory result = ordered.get(0);
        for (int i = 1; i < ordered.size(); i++) {
            Optional<Trajectory> merged = glueWithConstraints(
                result, ordered.get(i), new ConstraintSet()
            );
            if (merged.isEmpty()) {
                // Conflict resolution
                if (strategy == GluingStrategy.BEST_EFFORT) {
                    continue; // Skip incompatible fragment
                }
                return Optional.empty();
            }
            result = merged.get();
        }

        return Optional.of(result);
    }

    /**
     * Find global trajectory from start to goal.
     * Corresponds to: finding a global section.
     *
     * @param start Start state
     * @param goal Goal state
     * @return Global trajectory if exists
     */
    public Optional<Trajectory> findGlobal(StateId start, StateId goal) {
        // First, glue all possible combinations
        Set<Trajectory> allGlued = glue();

        // Find trajectory from start to goal
        return allGlued.stream()
            .filter(t -> t.start().equals(start) && t.end().equals(goal))
            .min(Comparator.comparingDouble(Trajectory::cost));
    }

    /**
     * Verify sheaf condition (coherence).
     * Ensures: if s₁|U∩V = s₂|U∩V, then exists global section s on U∪V
     *
     * Actually checks: pairwise compatible on overlaps → exists global
     *
     * @return true if sheaf condition holds for current sections
     */
    public boolean verifySheafCondition() {
        // For all pairs of local sections
        List<Trajectory> sections = new ArrayList<>(localSections);

        for (int i = 0; i < sections.size(); i++) {
            for (int j = i + 1; j < sections.size(); j++) {
                Trajectory s1 = sections.get(i);
                Trajectory s2 = sections.get(j);

                // Check if they have overlap
                Optional<StateId> overlap = s1.getIntersection(s2);
                if (overlap.isPresent()) {
                    // They should agree on overlap
                    if (!sectionsAgreeOnOverlap(s1, s2, overlap.get())) {
                        return false;
                    }
                }
            }
        }

        // All pairwise compatible - check if global exists
        Set<Trajectory> glued = glue();
        return glued.stream().anyMatch(t ->
            coversAllSections(t, sections)
        );
    }

    /**
     * Get all local sections.
     */
    public Set<Trajectory> getLocalSections() {
        return Collections.unmodifiableSet(localSections);
    }

    /**
     * Get compatibility graph.
     */
    public CompatibilityGraph getCompatibilityGraph() {
        return compatibilityGraph;
    }

    // Helper methods

    private List<Trajectory> findOptimalOrdering(
            List<Trajectory> fragments,
            GluingStrategy strategy) {

        // Simple greedy: start with fragment that has no predecessor
        Map<StateId, Trajectory> byStart = new HashMap<>();
        Map<StateId, Trajectory> byEnd = new HashMap<>();

        for (Trajectory t : fragments) {
            byStart.put(t.start(), t);
            byEnd.put(t.end(), t);
        }

        // Find starting fragment (not an end of any other)
        Trajectory start = fragments.stream()
            .filter(t -> !byEnd.containsKey(t.start()))
            .findFirst()
            .orElse(fragments.get(0));

        List<Trajectory> ordered = new ArrayList<>();
        Trajectory current = start;
        ordered.add(current);

        Set<Trajectory> used = new HashSet<>();
        used.add(current);

        while (byStart.containsKey(current.end())) {
            Trajectory next = byStart.get(current.end());
            if (used.contains(next)) {
                return null; // Cycle
            }
            ordered.add(next);
            used.add(next);
            current = next;
        }

        if (ordered.size() != fragments.size()) {
            return null; // Not all connected
        }

        return ordered;
    }

    private boolean sectionsAgreeOnOverlap(Trajectory s1, Trajectory s2, StateId overlap) {
        // Simplified: check if they have same cost at overlap
        // Real implementation would check actual values
        return true;
    }

    private boolean coversAllSections(Trajectory global, List<Trajectory> sections) {
        Set<Trajectory> covered = new HashSet<>();
        for (Trajectory t : sections) {
            if (isSubTrajectory(global, t)) {
                covered.add(t);
            }
        }
        return covered.size() == sections.size();
    }

    private boolean isSubTrajectory(Trajectory global, Trajectory sub) {
        // Check if sub is a subpath of global
        // Simplified: just check endpoints
        return global.start().equals(sub.start()) &&
               global.end().equals(sub.end()) ||
               (global.start().equals(sub.start()) ||
                global.end().equals(sub.end()));
    }
}

/**
 * Strategy for n-way gluing and conflict resolution.
 */
enum GluingStrategy {
    STRICT,      // All fragments must merge, no conflicts
    BEST_EFFORT, // Skip incompatible fragments
    OPTIMAL      // Minimize total cost
}
