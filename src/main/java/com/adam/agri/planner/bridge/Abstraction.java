package com.adam.agri.planner.bridge;

import com.adam.agri.planner.core.state.PhysicalState;
import com.adam.agri.planner.core.state.SymbolicState;
import com.adam.agri.planner.core.state.Predicate;

import java.util.List;

/**
 * Converts physical states to symbolic representations.
 * Physical → Symbolic abstraction.
 *
 * φ: PhysicalState → SymbolicState
 *
 * From log (line 4759):
 * class Abstraction {
 *     SymbolicState fromPhysical(PhysicalState s);
 * }
 */
public interface Abstraction {

    /**
     * Abstract a physical state to symbolic state.
     * φ(s) = high-level symbolic representation
     *
     * @param physical Physical state
     * @return Symbolic abstraction
     */
    SymbolicState fromPhysical(PhysicalState physical);

    /**
     * Check if physical state satisfies symbolic predicate.
     *
     * @param physical Physical state to check
     * @param symbolic Symbolic predicate to verify
     * @return true if predicate holds
     */
    boolean satisfies(PhysicalState physical, Predicate symbolic);

    /**
     * Learn abstraction function from examples.
     * Can use neural network or rule learning.
     *
     * @param examples Pairs of (physical, symbolic) states
     */
    void train(List<Example> examples);

    /**
     * Check consistency: abstract then refine ≈ identity
     * φ(ρ(p)) ≈ p
     *
     * @param physical Original physical state
     * @param reAbstracted State after abstraction+refinement
     * @return true if approximately equal
     */
    default boolean checkRoundTrip(PhysicalState physical, PhysicalState reAbstracted) {
        // Check if re-abstraction is close to original
        double distance = physical.distance(reAbstracted);
        return distance < getTolerance();
    }

    /**
     * Get tolerance for approximate equality.
     */
    default double getTolerance() {
        return 0.01;
    }

    /**
     * Example pair for learning.
     */
    class Example {
        private final PhysicalState physical;
        private final SymbolicState symbolic;

        public Example(PhysicalState physical, SymbolicState symbolic) {
            this.physical = physical;
            this.symbolic = symbolic;
        }

        public PhysicalState getPhysical() { return physical; }
        public SymbolicState getSymbolic() { return symbolic; }
    }
}
