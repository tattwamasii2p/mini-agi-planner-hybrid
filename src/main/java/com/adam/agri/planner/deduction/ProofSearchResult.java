package com.adam.agri.planner.deduction;

import com.adam.agri.planner.logic.Proof;

import java.util.Objects;
import java.util.Optional;

/**
 * Result of automated proof search.
 * Either a successful proof or failure with diagnostic information.
 */
public sealed interface ProofSearchResult permits ProofSearchResult.Success, ProofSearchResult.Failure {

    /**
     * Check if proof was found.
     */
    boolean isSuccess();

    /**
     * Get the proof if successful.
     */
    Optional<Proof> getProof();

    /**
     * Success case - proof found.
     */
    record Success(Proof proof, int stepsExplored) implements ProofSearchResult {
        public Success {
            Objects.requireNonNull(proof);
        }

        @Override
        public boolean isSuccess() {
            return true;
        }

        @Override
        public Optional<Proof> getProof() {
            return Optional.of(proof);
        }

        /**
         * Get the extracted witness term (Curry-Howard).
         */
        public String witnessDescription() {
            return proof.extractWitness().toString();
        }
    }

    /**
     * Failure case - no proof found.
     */
    record Failure(String reason, int stepsExplored, int maxDepthReached) implements ProofSearchResult {
        public Failure {
            Objects.requireNonNull(reason);
        }

        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public Optional<Proof> getProof() {
            return Optional.empty();
        }
    }

    /**
     * Factory for success.
     */
    static ProofSearchResult ok(Proof proof, int stepsExplored) {
        return new Success(proof, stepsExplored);
    }

    /**
     * Factory for failure.
     */
    static ProofSearchResult fail(String reason, int stepsExplored, int maxDepth) {
        return new Failure(reason, stepsExplored, maxDepth);
    }

    /**
     * Factory for failure without depth information.
     */
    static ProofSearchResult fail(String reason) {
        return new Failure(reason, 0, 0);
    }
}
